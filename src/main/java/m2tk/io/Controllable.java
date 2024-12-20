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

import java.io.IOException;

/**
 * 受控对象接口
 */
public interface Controllable
{
    /**
     * 查询受控对象是否支持特定指令。
     *
     * @param command 指令名。
     * @return 查询结果。
     */
    default boolean hasCommand(String command)
    {
        return false;
    }

    /**
     * 获取指令列表。
     *
     * @return 指令列表。
     */
    default String[] getCommandList()
    {
        return new String[0];
    }

    /**
     * 向受控对象发送指令。
     *
     * @param command 指令
     * @param arguments 参数
     * @throws IOException IO异常
     */
    default void control(String command, Object... arguments) throws IOException
    {
    }
}
