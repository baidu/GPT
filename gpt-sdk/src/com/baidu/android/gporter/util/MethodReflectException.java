/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.android.gporter.util;

/**
 * 方法反射调用异常
 *
 * @author liuhaitao
 * @since 2018-01-09
 */
public class MethodReflectException extends Exception {
    /**
     * 序列化ID
     */
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     *
     * @param msg 异常信息
     */
    public MethodReflectException(String msg) {
        super(msg);
    }

}






