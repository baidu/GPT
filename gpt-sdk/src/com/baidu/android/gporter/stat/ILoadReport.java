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
 * 启动插件时统计
 *
 * @author liuhaitao
 * @since 2017-01-12
 */
public interface ILoadReport {

    /**
     * 插件启动时，不是从主进程启动的，则先启动主进程Service
     *
     * @param context      Context
     * @param pkgName      包名
     * @param toExtProcess 启动的目标进程
     */
    void startMainProcessService(Context context, String pkgName, int toExtProcess);

    /**
     * 主进程启动插件
     *
     * @param context Context
     * @param pkgName 包名
     */
    void startPluginMainProcess(Context context, String pkgName);

    /**
     * GPT进程启动插件
     *
     * @param context Context
     * @param pkgName 包名
     */
    void startPluginGPTProcess(Context context, String pkgName);


    /**
     * 插件正在loading，把Intent缓存起来
     *
     * @param context Context
     * @param pkgName 包名
     */
    void cacheIntent(Context context, String pkgName);

    /**
     * 插件已经加载过，直接启动插件
     *
     * @param context Context
     * @param pkgName 包名
     */
    void startIntentOnLoaded(Context context, String pkgName);

    /**
     * 启动RootActivity
     *
     * @param context Context
     * @param pkgName 包名
     */
    void startRootActivity(Context context, String pkgName);

    /**
     * 静默启动插件，或者已经有实例在gpt进程
     *
     * @param context  Context
     * @param pkgName  包名
     * @param isSilent 是否静默
     */
    void silentOrHasInstanceInGPTProcess(Context context, String pkgName, boolean isSilent);

    /**
     * makeApplication使用时长
     *
     * @param context   Context
     * @param pkgName   包名
     * @param coastTime 时长
     */
    void onLoadApplication(Context context, String pkgName, long coastTime);

    /**
     * Application onCreate使用时长
     *
     * @param context   Context
     * @param pkgName   包名
     * @param coastTime 时长
     */
    void onCreateApplication(Context context, String pkgName, long coastTime);

    /**
     * initProxyEnvironment 初始化时长
     *
     * @param context   Context
     * @param pkgName   包名
     * @param coastTime 时长
     */
    void initProxyEnvironment(Context context, String pkgName, long coastTime);

    /**
     * Activity的使用时长
     *
     * @param context   context
     * @param pkgName   包名
     * @param className Activity名称
     * @param coastTime 时长
     */
    void activityElapsedTime(Context context, String pkgName, String className, long coastTime);

}

