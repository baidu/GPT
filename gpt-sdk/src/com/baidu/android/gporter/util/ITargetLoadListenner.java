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
 * 插件加载回调接口
 *
 * @author liuhaitao
 * @since 2014-4-24
 */
public interface ITargetLoadListenner {

    /**
     * 加载成功的回调，主线程回调
     *
     * @param packageName  加载成功的插件包名
     * @param unionProcess 运行在同一进程
     */
    void onLoadFinished(String packageName, boolean unionProcess);

    /**
     * 加载插件失败
     *
     * @param packageName 插件包名
     * @param failReason  失败原因
     */
    void onLoadFailed(String packageName, String failReason);
}

