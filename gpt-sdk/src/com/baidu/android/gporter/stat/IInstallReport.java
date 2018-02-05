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

import android.content.Context;

/**
 * 安装进程的统计
 *
 * @author liuhaitao
 * @since 2017-01-12
 */
public interface IInstallReport {

    /**
     * 开始安装
     *
     * @param context Context
     * @param pkg     包名
     */
    void onInstallStart(Context context, String pkg);

    /**
     * 安装成功
     *
     * @param context Context
     * @param pkg     包名
     */
    void onInstallSuccess(Context context, String pkg);

    /**
     * 安装失败
     *
     * @param context    Context
     * @param pkg        包名
     * @param failReason 失败原因
     */
    void onInstallFail(Context context, String pkg, String failReason);

    /**
     * 下次切换路径
     *
     * @param context Context
     * @param pkg     包名
     */
    void onNextSwitchInstallDir(Context context, String pkg);

    /**
     * 插件卸载开始
     *
     * @param context Context
     * @param pkg     包名
     */
    void onUninstallStart(Context context, String pkg);

    /**
     * 插件卸载结果
     *
     * @param context Context
     * @param pkg     包名
     * @param type    卸载成功，失败，下次卸载
     */
    void onUninstallResult(Context context, String pkg, int type);

    /**
     * 开始切换安装目录
     *
     * @param context Context
     * @param pkg     包名
     */
    void onStartSwitchInstallDir(Context context, String pkg);

    /**
     * 开始切换安装结果
     *
     * @param context Context
     * @param pkg     包名
     */
    void onStartSwitchDirSuccess(Context context, String pkg);

    /**
     * 删除无用安装目录
     *
     * @param context Context
     */
    void onDeleteTempInstallDir(Context context);

}