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

/**
 * 属性查询接口
 */
public interface Queryable
{
    /**
     * 查询是否含有指定属性。
     *
     * @param property 属性名称。
     * @return 查询结果。
     */
    default boolean hasProperty(String property)
    {
        return false;
    }

    /**
     * 获取属性列表。
     *
     * @return 属性列表。
     */
    default String[] getPropertyList()
    {
        return new String[0];
    }

    /**
     * 获取属性值。
     *
     * @param property 属性名称
     * @return 属性（当前）值。
     */
    default Object query(String property)
    {
        return null;
    }
}
