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
package com.baidu.android.gporter.install;

/**
 * 插件安装回调
 *
 * @author liuhaitao
 * @since 2014年7月14日
 */
public interface IInstallCallBack {

    /**
     * 安装成功回调
     *
     * @param packageName 插件包名
     */
    void onPacakgeInstalled(String packageName);

    /**
     * 安装失败回调
     *
     * @param packageName 插件包名
     * @param failReason  失败原因
     */
    void onPackageInstallFail(String packageName, String failReason);

}
