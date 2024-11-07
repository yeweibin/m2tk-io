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

import m2tk.io.RxChannel;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

final class MulticastRxChannel implements RxChannel
{
    private final String uri;
    private final MulticastSocket socket;
    private final DatagramPacket packet;
    private final SocketAddress socketAddress;
    private final NetworkInterface networkInterface;
    private int timeout;
    private int packetReadOffset;

    private static final int BUFFER_SIZE = 1500; // TSOverIP规定一个UDP包里最多放7个TS包，所以这里将缓存设成一个以太MTU大小就够了。

    MulticastRxChannel(String address, int port) throws IOException
    {
        NetworkInterface usableInterface = null;
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements())
        {
            NetworkInterface nif = enumeration.nextElement();
            if (nif.isLoopback() || nif.isVirtual() || nif.isPointToPoint())
                continue;
            if (nif.isUp() && nif.supportsMulticast())
            {
                usableInterface = nif;
                break;
            }
        }
        if (usableInterface == null)
            throw new IllegalArgumentException("没有可用的网络接口");

        socketAddress = new InetSocketAddress(address, port);
        networkInterface = usableInterface;

        uri = "udp://" + address + ":" + port;
        socket = new MulticastSocket(port);
        socket.joinGroup(socketAddress, networkInterface);

        packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        timeout = 30000; // 30s
        packetReadOffset = BUFFER_SIZE;
        socket.setSoTimeout(timeout);
        socket.setReceiveBufferSize(10 * 1024 * 1024); // 10MB缓存，以应对高码率输入，减少丢包概率。
    }

    @Override
    public boolean hasProperty(String property)
    {
        return "source name".equals(property) || "timeout".equals(property);
    }

    @Override
    public String[] getPropertyList()
    {
        return new String[]{"source name", "timeout"};
    }

    @Override
    public Object query(String property)
    {
        if ("source name".equals(property))
            return uri;
        if ("timeout".equals(property))
            return timeout;
        return null;
    }

    @Override
    public boolean hasCommand(String command)
    {
        return "timeout".equals(command);
    }

    @Override
    public String[] getCommandList()
    {
        return new String[]{"timeout"};
    }

    @Override
    public void control(String command, Object... arguments) throws IOException
    {
        if ("timeout".equals(command))
            doSetTimeout(arguments);
    }

    private void doSetTimeout(Object[] arguments) throws IOException
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

        if (value < 0)
            throw new IllegalArgumentException("无效超时：" + arg);
        timeout = value;
        socket.setSoTimeout(timeout);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (socket.isClosed())
            throw new IOException("通道已关闭");

        if (offset < 0 || buffer.length - offset < length)
            throw new IllegalArgumentException("无效的偏移量：" + offset);

        int received = 0;
        int toRead = length;
        while (toRead > 0)
        {
            receive();

            int n = read0(buffer, offset, toRead);
            toRead -= n;
            offset += n;
            received += n;
        }

        return received;
    }

    private int read0(byte[] buffer, int offset, int length)
    {
        int available = Math.min(length, packet.getLength() - packetReadOffset);
        if (available > 0)
        {
            System.arraycopy(packet.getData(), packetReadOffset, buffer, offset, available);
            packetReadOffset += available;
        }
        return available;
    }

    private void receive() throws IOException
    {
        if (packet.getLength() > packetReadOffset)
            return; // 还有缓存的数据，直接返回

        socket.receive(packet); // 这里是阻塞调用，除非超时或被中断。
        packetReadOffset = 0;
    }

    @Override
    public void close() throws IOException
    {
        socket.leaveGroup(socketAddress, networkInterface);
        socket.close();
    }
}
