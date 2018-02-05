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
import android.content.Intent;
import android.util.Pair;

/**
 * 插件的基本数据统计
 *
 * @author liuhaitao
 * @since 2017-01-12
 */
public interface IPluginReport {
    /**
     * 插件开始启动
     *
     * @param context Context
     * @param pkg     插件包名
     * @param intent  Intent
     */
    void onPluginLoadStart(Context context, String pkg, Intent intent);

    /**
     * 插件加载成功
     *
     * @param context   Context
     * @param pkg       插件包名
     * @param intent    Intent
     * @param coastTime 启动耗时
     */
    void onPluginLoadSucess(Context context, String pkg, Intent intent, long coastTime);

    /**
     * 插件加载失败
     *
     * @param context Context
     * @param pkg     插件包名
     * @param intent  Intent
     */
    void onPluginLoadFail(Context context, String pkg, Intent intent);

    /**
     * 插件热启动
     *
     * @param context   Context
     * @param pkg       插件包名
     * @param coastTime 启动耗时
     */
    void onPluginHotLoad(Context context, String pkg, long coastTime);

    /**
     * 从桌面icon启动插件
     *
     * @param context Context
     * @param pkg     包名
     */
    void onPluginStartByShortcut(Context context, String pkg);

    /**
     * 开始安装
     *
     * @param context     Context
     * @param pkg         包名
     * @param versionCode 版本号
     */
    void onInstallPluginStart(Context context, String pkg, String versionCode);

    /**
     * 安装成功
     *
     * @param context     Context
     * @param pkg         包名
     * @param versionCode 版本号
     */
    void onInstallPluginSuccess(Context context, String pkg, String versionCode);

    /**
     * 安装失败
     *
     * @param context     Context
     * @param pkg         包名
     * @param versionCode 版本号
     * @param reason      reason
     */
    void onInstallPluginFail(Context context, String pkg, String versionCode, String reason);

    /**
     * 插件卸载开始
     *
     * @param context Context
     * @param pkg     包名
     */
    void onUninstallPluginStart(Context context, String pkg);

    /**
     * 插件卸载结果
     *
     * @param context Context
     * @param pkg     包名
     * @param type    卸载成功，失败，下次卸载
     */
    void onUninstallPluginResult(Context context, String pkg, int type);

    /**
     * 插件的启动时间记录
     *
     * @param context  Context
     * @param pkg      包名
     * @param timeLine 时间记录
     */
    void onPluginTimeLine(Context context, String pkg, PluginTimeLine timeLine);

    /**
     * 出现异常
     *
     * @param context         Context
     * @param packageName     插件包名
     * @param exceptionStack  异常信息
     * @param key             key，不同地方的异常信息的key不一样
     * @param customKeyValues KeyValue
     */
    void onException(Context context, String packageName, String exceptionStack, String key,
                     Pair<String, String>... customKeyValues);

    /**
     * 在crash之前，启动其他进程保存这个异常到文件并在后续触发上传时统一上报
     *
     * @param context         Context
     * @param packageName     插件包名
     * @param exceptionStack  异常信息
     * @param key             不同地方的异常信息的key不一样
     * @param customKeyValues keyValue
     */
    void onExceptionByLogService(Context context, String packageName, String exceptionStack, String key,
                                 Pair<String, String>... customKeyValues);
}




