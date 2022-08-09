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

package m2tk.io;

import java.io.IOException;
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

    public static RxChannel openRxChannel(String resource) throws IOException
    {
        Objects.requireNonNull(resource, "resource should not be null");

        for (Protocol protocol : PROTOCOLS)
        {
            try
            {
                if (protocol.accepts(resource))
                    return protocol.openRxChannel(resource);
            } catch (Exception ignored)
            {
                ignored.printStackTrace(System.out);
                // ignored
            }
        }

        throw new IllegalArgumentException("No suitable protocol accepts " + resource);
    }

    public static TxChannel openTxChannel(String resource) throws IOException
    {
        Objects.requireNonNull(resource, "resource should not be null");

        for (Protocol protocol : PROTOCOLS)
        {
            try
            {
                if (protocol.accepts(resource))
                    return protocol.openTxChannel(resource);
            } catch (Exception ignored)
            {
                // ignored
            }
        }

        throw new IllegalArgumentException("No suitable protocol accepts " + resource);
    }

    public static Protocol getProtocol(String resource)
    {
        Objects.requireNonNull(resource, "resource should not be null");

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

        throw new IllegalArgumentException("No suitable protocol accepts " + resource);
    }
}
