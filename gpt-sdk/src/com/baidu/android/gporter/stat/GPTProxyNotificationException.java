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
package com.baidu.android.gporter.stat;

/**
 * 代理Notification Manger时的异常
 *
 * @author liuhaitao
 * @since 2017-03-27
 */
public class GPTProxyNotificationException extends RuntimeException {

    /**
     * Constructs a new Exception that includes the current stack trace.
     */
    public GPTProxyNotificationException() {
    }

    /**
     * Constructs a new Exception with the current stack trace
     * and the specified detail message.
     *
     * @param detailMessage the detail message for this exception.
     */
    public GPTProxyNotificationException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new Exception with the current stack trace,
     * the specified detail message and the specified cause.
     *
     * @param detailMessage the detail message for this exception.
     * @param throwable     the cause of this exception.
     */
    public GPTProxyNotificationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}



