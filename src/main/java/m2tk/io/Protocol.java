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

public interface Protocol extends Queryable
{
    /**
     * 查询当前协议是否支持流通道资源描述。
     *
     * @param resource 流通道资源描述（格式由实现决定）
     * @return 支持，返回<code>true</code>；不支持，返回<code>false</code>。
     */
    boolean accepts(String resource);

    /**
     * 打开输入流通道。
     *
     * @param resource 流通道资源描述
     * @return 流通道实例
     * @throws IllegalArgumentException 所给资源描述无效或无法识别
     * @throws IOException 出现I/O异常（通道被占用或无法打开时）
     */
    RxChannel openRxChannel(String resource) throws IOException;

    /**
     * 打开输出流通道。
     *
     * @param resource 流通道资源描述
     * @return 流通道实例
     * @throws IllegalArgumentException 所给资源描述无效或无法识别
     * @throws IOException 出现I/O异常（通道被占用或无法打开时）
     */
    TxChannel openTxChannel(String resource) throws IOException;
}
