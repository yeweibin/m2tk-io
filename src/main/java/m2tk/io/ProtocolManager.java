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
package m2tk.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * 基于SPI模式加载当前Classpath中的Protocol实例。
 */
public final class ProtocolManager
{
    private static final List<Protocol> PROTOCOLS;

    static
    {
        PROTOCOLS = new ArrayList<>();
        ServiceLoader<Protocol> available = ServiceLoader.load(Protocol.class);
        for (Protocol protocol : available)
        {
            PROTOCOLS.add(protocol);
        }
    }

    private ProtocolManager()
    {
    }

    public static RxChannel openRxChannel(String resource)
    {
        Objects.requireNonNull(resource, "资源描述为空");

        for (Protocol protocol : PROTOCOLS)
        {
            try
            {
                if (protocol.accepts(resource))
                    return protocol.openRxChannel(resource);
            } catch (Exception ex)
            {
                ex.printStackTrace(System.err);
            }
        }

        throw new IllegalArgumentException("当前没有协议能为指定资源创建输入通道");
    }

    public static TxChannel openTxChannel(String resource)
    {
        Objects.requireNonNull(resource, "资源描述为空");

        for (Protocol protocol : PROTOCOLS)
        {
            try
            {
                if (protocol.accepts(resource))
                    return protocol.openTxChannel(resource);
            } catch (Exception ex)
            {
                ex.printStackTrace(System.err);
            }
        }

        throw new IllegalArgumentException("当前没有协议能为指定资源创建输出通道");
    }

    public static Protocol getProtocol(String resource)
    {
        Objects.requireNonNull(resource, "资源描述为空");

        for (Protocol protocol : PROTOCOLS)
        {
            try
            {
                if (protocol.accepts(resource))
                    return protocol;
            } catch (Exception ignored)
            {
                // ignored
            }
        }

        throw new IllegalArgumentException("当前没有协议认可指定资源");
    }
}
