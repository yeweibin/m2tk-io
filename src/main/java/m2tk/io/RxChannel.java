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

import java.io.Closeable;
import java.io.IOException;

/**
 * 输入通道
 */
public interface RxChannel extends Closeable, Controllable, Queryable
{
    /**
     * 读取通道数据
     *
     * @param buffer 结果缓冲区
     * @param offset 缓冲区写入位置的偏移量
     * @param length 期望读取的长度
     * @return 实际读取的长度
     * @throws IOException IO异常
     */
    int read(byte[] buffer, int offset, int length) throws IOException;
}
