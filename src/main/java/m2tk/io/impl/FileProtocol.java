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

import m2tk.io.Protocol;
import m2tk.io.RxChannel;
import m2tk.io.TxChannel;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class FileProtocol implements Protocol
{
    @Override
    public boolean accepts(String resource)
    {
        Objects.requireNonNull(resource, "resource should not be null");
        // 文件路径在创建通道时再进行判断，不再要求必须符合URI格式。
        return true;
    }

    @Override
    public RxChannel openRxChannel(String resource) throws IOException
    {
        return new FileRxChannel(new File(resource));
    }

    @Override
    public TxChannel openTxChannel(String resource) throws IOException
    {
        return new FileTxChannel(new File(resource));
    }

    @Override
    public boolean hasProperty(String property)
    {
        switch (property)
        {
            case "name":
            case "version":
                return true;
            default:
                return false;
        }
    }

    @Override
    public String[] getPropertyList()
    {
        return new String[]{"name", "version"};
    }

    @Override
    public Object query(String property)
    {
        switch (property)
        {
            case "name":
                return "File Protocol";
            case "version":
                return "1.0.0";
            default:
                return null;
        }
    }
}
