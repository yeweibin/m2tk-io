/*
 * Copyright (c) M2TK Project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package m2tk.io.impl;

import m2tk.io.TxChannel;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;

final class MulticastTxChannel implements TxChannel
{
    private static final int FRAME_SIZE = 188 * 7; // 一个UDP报文里最多放7个TS包
    private final MulticastSocket socket;
    private final DatagramPacket packet;
    private final SocketAddress socketAddress;
    private final NetworkInterface networkInterface;
    private int bitrate;

    private final byte[] buf;
    private int buffered;
    private long lastTimePoint;
    private static final byte[] NULL_PACKET = new byte[188];

    static
    {
        Arrays.fill(NULL_PACKET, (byte) 0xFF);
        NULL_PACKET[0] = 0x47;
        NULL_PACKET[1] = 0x1F;
        NULL_PACKET[2] = (byte) 0xFF;
        NULL_PACKET[3] = 0x1F;  // scrambling_control: 00
                                // adaptation_field_control: 01
                                // continuity_counter: 1111
    }

    MulticastTxChannel(String address, Integer port) throws IOException
    {
        NetworkInterface usableInterface = null;
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements())
        {
            NetworkInterface nif = enumeration.nextElement();
            if (nif.isLoopback() || nif.isVirtual() || nif.isPointToPoint())
                continue;
            if (nif.isUp() && nif.supportsMulticast() &&
                nif.inetAddresses().anyMatch(addr -> addr instanceof Inet4Address))
            {
                usableInterface = nif;
                break;
            }
        }
        if (usableInterface == null)
            throw new IllegalArgumentException("没有可用的网络接口");

        socketAddress = new InetSocketAddress(address, port);
        networkInterface = usableInterface;

        socket = new MulticastSocket(port);
        socket.joinGroup(socketAddress, networkInterface);

        packet = new DatagramPacket(new byte[FRAME_SIZE], FRAME_SIZE, socketAddress);
        bitrate = -1;
        buf = new byte[FRAME_SIZE * 10];
        resetBuffer();
    }

    @Override
    public boolean hasCommand(String command)
    {
        return "bitrate".equals(command);
    }

    @Override
    public String[] getCommandList()
    {
        return new String[]{"bitrate"};
    }

    @Override
    public void control(String command, Object... arguments)
    {
        if ("bitrate".equals(command))
            doSetBitrate(arguments);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException
    {
        if (socket.isClosed())
            throw new IOException("通道已关闭");

        if (offset < 0 || bytes.length - offset < length)
            throw new IllegalArgumentException("无效的偏移量：" + offset);

        if (length % 188 != 0)
            throw new IllegalArgumentException("数据长度必须为188字节的整数倍");

        while (length > 0)
        {
            int cached = cache(bytes, offset, length);
            transmit(isCacheFull());
            length -= cached;
            offset += cached;
        }
    }

    @Override
    public void close() throws IOException
    {
        if (!socket.isClosed())
        {
            bitrate = -1;
            transmit(true);
            socket.leaveGroup(socketAddress, networkInterface);
            socket.close();
        }
    }

    private void doSetBitrate(Object[] arguments)
    {
        if (arguments.length == 0)
            throw new IllegalArgumentException("缺少必要参数");

        Object arg = arguments[0];
        int value = -1;
        if (arg instanceof Integer)
            value = (int) arg;
        if (arg instanceof Long)
            value = (int) (long) arg;
        if (arg instanceof String)
            value = Integer.parseInt((String) arg);

        if (value <= 0)
            throw new IllegalArgumentException("无效比特率：" + arg);
        bitrate = value;
    }

    private int cache(byte[] bytes, int offset, int length)
    {
        int count = Math.min(length, buf.length - buffered);
        System.arraycopy(bytes, offset, buf, buffered, count);
        buffered += count;
        return count;
    }

    private boolean isCacheFull()
    {
        return (buffered == buf.length);
    }

    private void transmit(boolean immediately) throws IOException
    {
        if (!immediately && buffered < buf.length)
            return;

        // 批量输出，并按照带宽要求控制输出速率
        long t0 = lastTimePoint;
        int offset = 0;
        int packetCount = 0;
        while (buffered > 0)
        {
            int blockSize = Math.min(FRAME_SIZE, buffered);

            // 保证每个UDP报文都是7个TS包（不够7个，用空包填充）
            System.arraycopy(buf, offset, packet.getData(), 0, blockSize);
            for (int i = blockSize; i < FRAME_SIZE; i += 188)
                System.arraycopy(NULL_PACKET, 0, packet.getData(), i, 188);
            packet.setLength(FRAME_SIZE);

            socket.send(packet);

            offset += blockSize;
            buffered -= blockSize;
            packetCount++;
        }

        resetBuffer();
        long elapsedTimeNanos = System.nanoTime() - t0;
        long expectedTimeNanos = packetCount * FRAME_SIZE * 8 * 1000_000_000L / bitrate; // 按照指定带宽输出需要的时间

        // 当bitrate为负数时，表示不限速输出。
        if (bitrate > 0)
        {
            if (elapsedTimeNanos < expectedTimeNanos)
            {
                // 实际输出速度高于额定速度，需要减速（等待）
                long waitMillis = (expectedTimeNanos - elapsedTimeNanos) / 1000_000; // 1000_000 ns = 1 ms
                sleep(waitMillis);
            }
            lastTimePoint = System.nanoTime();
        }
    }

    private void sleep(long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void resetBuffer()
    {
        for (int i = 0; i < buf.length; i += 188)
            System.arraycopy(NULL_PACKET, 0, buf, i, 188);
        buffered = 0;
    }
}
