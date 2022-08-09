/*
 * Copyright (c) Ye Weibin. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Objects;

final class FileTxChannel implements TxChannel
{
    private final RandomAccessFile file;
    private int bitrate;
    private long limit;
    private final byte[] buf;
    private int buffered;
    private long lastTimePoint;

    FileTxChannel(File f) throws IOException
    {
        file = new RandomAccessFile(f, "rw");
        file.setLength(0);
        bitrate = -1;
        limit = 100 * 1024 * 1024L; // 100MB
        buf = new byte[188 * 100]; // 缓存小反而能减少等待时间
        resetBuffer();
    }

    @Override
    public boolean hasCommand(String command)
    {
        return Objects.equals(command, "bitrate") ||
               Objects.equals(command, "limit");
    }

    @Override
    public String[] getCommandList()
    {
        return new String[]{"bitrate","limit"};
    }

    @Override
    public void control(String command, Object... arguments)
    {
        if (Objects.equals(command, "bitrate"))
            doSetBitrate(arguments);
        if (Objects.equals(command, "limit"))
            doSetLimit(arguments);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException
    {
        if (offset < 0 || bytes.length - offset < length)
            throw new IllegalArgumentException("Invalid offset: " + offset);

        if (length % 188 != 0)
            throw new IllegalArgumentException("Invalid length: " + length + ", must be multiple of 188 bytes.");

        while (length > 0)
        {
            int cached = cache(bytes, offset, length);
            writeFile(isCacheFull());
            length -= cached;
            offset += cached;
        }
    }

    @Override
    public void close() throws IOException
    {
        bitrate = -1; // 取消限速，避免等待。
        writeFile(true);
        file.setLength(file.getFilePointer());
        file.close();
    }

    private void doSetBitrate(Object[] arguments)
    {
        if (arguments.length != 1)
            throw new IllegalArgumentException("Command 'bitrate' requires one argument: [bitrate]");

        Object arg = arguments[0];
        int value = -1;
        if (arg instanceof Integer)
            value = (int) arg;
        if (arg instanceof Long)
            value = (int) (long) arg;
        if (arg instanceof String)
            value = Integer.parseInt((String) arg);

        if (value > 0)
            bitrate = value;
    }

    private void doSetLimit(Object[] arguments)
    {
        if (arguments.length != 1)
            throw new IllegalArgumentException("Command 'limit' requires one argument: [limit], unit: MB");

        Object arg = arguments[0];
        long value = 0;
        if (arg instanceof Integer)
            value = (int) arg;
        if (arg instanceof Long)
            value = (long) arg;
        if (arg instanceof String)
            value = Long.parseLong((String) arg);

        if (value < 1)
            throw new IllegalArgumentException("Bad limit value: " + arg);

        value = Math.max(value, 100);
        limit = value * 1024 * 1024;
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

    private void writeFile(boolean immediately) throws IOException
    {
        if (!immediately && buffered < buf.length)
            return;

        // 批量输出，并按照带宽要求控制输出速率
        long expectedTimeNanos = buffered * 8 * 1000_000_000L / bitrate; // 按照指定带宽输出需要的时间
        long t0 = lastTimePoint;
        if (file.getFilePointer() < limit)
        {
            file.write(buf, 0, buffered);
            resetBuffer();
        } else
        {
            file.seek(0);
            resetBuffer();
            sleep(100);
        }

        long elapsedTimeNanos = System.nanoTime() - t0;

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
            // do nothing
        }
    }

    private void resetBuffer()
    {
        Arrays.fill(buf, (byte)0xFF);
        for (int i = 0; i < buf.length; i += 188)
        {
            buf[i] = 0x47;
            buf[i+1] = 0b00011111;
            buf[i+2] = (byte) 0b11111111;
            buf[i+3] = 0b00011111;  // scrambling_control: '00'
                                    // adaptation_field_control: '01'
                                    // continuity_counter: '1111'
        }
        buffered = 0;
    }
}
