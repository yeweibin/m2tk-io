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

import m2tk.io.RxChannel;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.util.Objects;

final class FileRxChannel implements RxChannel
{
    private final RandomAccessFile file;
    private final String filename;
    private volatile boolean closed;
    private volatile boolean rewindEnabled;

    FileRxChannel(File f) throws IOException
    {
        file = new RandomAccessFile(f, "r");
        filename = f.getName();
        closed = false;
        rewindEnabled = false;
    }

    @Override
    public boolean hasProperty(String property)
    {
        return Objects.equals(property, "source name");
    }

    @Override
    public String[] getPropertyList()
    {
        return new String[]{"source name"};
    }

    @Override
    public Object query(String property)
    {
        switch (property)
        {
            case "source name":
                return filename;
            default:
                return null;
        }
    }

    @Override
    public boolean hasCommand(String command)
    {
        return Objects.equals(command, "sync") ||
               Objects.equals(command, "rewind");
    }

    @Override
    public void control(String command, Object... arguments) throws IOException
    {
        if (Objects.equals(command, "sync"))
            doSync();
        if (Objects.equals(command, "rewind"))
            doSetRewind(arguments);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (closed)
            throw new IOException("Channel closed");

        if (offset < 0 || buffer.length - offset < length)
            throw new IllegalArgumentException("Invalid offset: " + offset);

        int nRead = file.read(buffer, offset, length);
        if (nRead == -1 && rewindEnabled)
        {
            file.seek(0);
            return file.read(buffer, offset, length);
        }

        return nRead;
    }

    @Override
    public synchronized void close() throws IOException
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
            throw new IOException("Channel closed");

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
        if (arguments.length != 1)
            throw new IllegalArgumentException("Command 'rewind' requires one argument: [true|false]");

        Object arg = arguments[0];
        if (arg instanceof Boolean)
            rewindEnabled = (boolean) arg;
        else if (arg instanceof String)
            rewindEnabled = Boolean.parseBoolean((String) arg);
        else
            throw new IllegalArgumentException("Invalid 'rewind' argument: " + arguments[0]);
    }
}
