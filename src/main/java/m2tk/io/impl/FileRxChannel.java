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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

final class FileRxChannel implements RxChannel
{
    private final RandomAccessFile file;
    private final String filename;
    private boolean closed;
    private boolean rewindEnabled;

    FileRxChannel(File f) throws IOException
    {
        file = new RandomAccessFile(f, "r");
        filename = f.getAbsolutePath();
        closed = false;
        rewindEnabled = false;
    }

    @Override
    public boolean hasProperty(String property)
    {
        return "source name".equals(property);
    }

    @Override
    public String[] getPropertyList()
    {
        return new String[]{"source name"};
    }

    @Override
    public Object query(String property)
    {
        if ("source name".equals(property))
            return filename;
        return null;
    }

    @Override
    public boolean hasCommand(String command)
    {
        return "sync".equals(command) || "rewind".equals(command);
    }

    @Override
    public void control(String command, Object... arguments) throws IOException
    {
        if ("sync".equals(command))
            doSync();
        if ("rewind".equals(command))
            doSetRewind(arguments);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (closed)
            throw new IOException("通道已关闭");

        if (offset < 0 || buffer.length - offset < length)
            throw new IllegalArgumentException("无效的偏移量：" + offset);

        int nRead = file.read(buffer, offset, length);
        if (nRead == -1 && rewindEnabled)
        {
            file.seek(0);
            return file.read(buffer, offset, length);
        }

        return nRead;
    }

    @Override
    public void close() throws IOException
    {
        if (!closed)
        {
            file.close();
            closed = true;
        }
    }

    private void doSync() throws IOException
    {
        if (closed)
            throw new IOException("通道已关闭");

        int c = 0;
        while (c < 5)
        {
            int b = file.read();
            if (b == -1)
            {
                if (!rewindEnabled)
                    throw new EOFException();

                file.seek(0);
                continue;
            }

            if (b == 0x47)
            {
                c++;
                file.skipBytes(187);
            }
        }
    }

    private void doSetRewind(Object[] arguments)
    {
        if (arguments.length == 0)
            throw new IllegalArgumentException("缺少必要参数");

        Object arg = arguments[0];
        if (arg instanceof Boolean)
            rewindEnabled = (boolean) arg;
        else if (arg instanceof String)
            rewindEnabled = Boolean.parseBoolean((String) arg);
        else
            throw new IllegalArgumentException("无效参数：" + arguments[0]);
    }
}
