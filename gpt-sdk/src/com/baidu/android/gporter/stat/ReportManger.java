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
 * 统计上报管理
 *
 * @author liuhaitao
 * @since 2017-01-12
 */
public class ReportManger implements IPluginReport, IInstallReport, ILoadReport {

    /**
     * IPluginReport
     */
    private IPluginReport mPluginReport;
    /**
     * IInstallReprot
     */
    private IInstallReport mInstallReport;
    /**
     * ILoadReport
     */
    private ILoadReport mLoadReport;

    /**
     * ReportManger 单例
     */
    private static ReportManger mInstance;
    /**
     * 插件启动的TimeLine是否写入文件
     */
    private boolean mWritePluginTimeLineToFile = false;
    /**
     * 插件启动时间限制，超过这个限制，上传到服务端
     */
    public static final int DEFAULT_PLUGIN_START_TIME_LIMT = 5000;

    /**
     * 插件启动时间限制，超过这个限制，上传到服务端
     */
    public int mPluginStartTimeLimt = DEFAULT_PLUGIN_START_TIME_LIMT;

    /**
     * 获取单例
     *
     * @return ReportManger
     */
    public static synchronized ReportManger getInstance() {

        if (mInstance == null) {
            mInstance = new ReportManger();
        }
        return mInstance;
    }

    /**
     * 设置PluginReport
     *
     * @param mReport IPluginReport
     */
    public void setPluginReport(IPluginReport mReport) {
        this.mPluginReport = mReport;
    }

    /**
     * 设置InstallReport
     *
     * @param installReport IInstallReport
     */
    public void setInstallReport(IInstallReport installReport) {
        this.mInstallReport = installReport;
    }

    /**
     * 设置LoadReport
     *
     * @param loadReprot ILoadReport
     */
    public void setLoadReprot(ILoadReport loadReprot) {
        this.mLoadReport = loadReprot;
    }

    /**
     * 返回是否写入文件
     *
     * @return 是否写入文件
     */
    public boolean isWritePluginTimeLineToFile() {
        return mWritePluginTimeLineToFile;
    }

    /**
     * 使用DefaultReportImpl 才有效
     * 自己实现统计，无需关注这个设置
     * 设置插件的启动TimeLine是否写入文件
     *
     * @param writePluginTimeLineToFile 是否写入文件
     */
    public void setWritePluginTimeLineToFile(boolean writePluginTimeLineToFile) {
        this.mWritePluginTimeLineToFile = writePluginTimeLineToFile;
    }

    /**
     * 使用DefaultReportImpl 才有效
     * 自己实现统计，无需关注这个设置
     * 设置插件启动时间限制，超过这个时间，则上传到服务端
     *
     * @param timeLimt 时间限制
     */
    public void setPluginStartTimeLimt(int timeLimt) {
        this.mPluginStartTimeLimt = timeLimt;
    }

    /**
     * 插件启动时间限制
     *
     * @return 插件启动时间限制
     */
    public int getPluginStartTimeLimt() {
        return mPluginStartTimeLimt;
    }

    @Override
    public void onPluginLoadStart(Context context, String pkg, Intent intent) {
        if (mPluginReport != null) {
            mPluginReport.onPluginLoadStart(context, pkg, intent);
        }
    }

    @Override
    public void onPluginLoadSucess(Context context, String pkg, Intent intent, long coastTime) {
        if (mPluginReport != null) {
            mPluginReport.onPluginLoadSucess(context, pkg, intent, coastTime);
        }
    }

    @Override
    public void onPluginLoadFail(Context context, String pkg, Intent intent) {
        if (mPluginReport != null) {
            mPluginReport.onPluginLoadFail(context, pkg, intent);
        }
    }

    @Override
    public void onPluginHotLoad(Context context, String pkg, long coastTime) {
        if (mPluginReport != null) {
            mPluginReport.onPluginHotLoad(context, pkg, coastTime);
        }

    }

    @Override
    public void onPluginStartByShortcut(Context context, String pkg) {
        if (mPluginReport != null) {
            mPluginReport.onPluginStartByShortcut(context, pkg);
        }
    }

    @Override
    public void onInstallPluginStart(Context context, String pkg, String versionCode) {
        if (mPluginReport != null) {
            mPluginReport.onInstallPluginStart(context, pkg, versionCode);
        }
    }

    @Override
    public void onInstallPluginSuccess(Context context, String pkg, String versionCode) {
        if (mPluginReport != null) {
            mPluginReport.onInstallPluginSuccess(context, pkg, versionCode);
        }
    }

    @Override
    public void onInstallPluginFail(Context context, String pkg, String versionCode, String reason) {
        if (mPluginReport != null) {
            mPluginReport.onInstallPluginFail(context, pkg, versionCode, reason);
        }
    }

    @Override
    public void onUninstallPluginStart(Context context, String pkg) {
        if (mPluginReport != null) {
            mPluginReport.onUninstallPluginStart(context, pkg);
        }

    }

    @Override
    public void onUninstallPluginResult(Context context, String pkg, int type) {
        if (mPluginReport != null) {
            mPluginReport.onUninstallPluginResult(context, pkg, type);
        }

    }

    @Override
    public void onPluginTimeLine(Context context, String pkg, PluginTimeLine timeLine) {
        if (mPluginReport != null) {
            mPluginReport.onPluginTimeLine(context, pkg, timeLine);
        }
    }


    @Override
    public void onInstallStart(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onInstallStart(context, pkg);
        }
    }

    @Override
    public void onInstallSuccess(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onInstallSuccess(context, pkg);
        }
    }

    @Override
    public void onInstallFail(Context context, String pkg, String failReason) {
        if (mInstallReport != null) {
            mInstallReport.onInstallFail(context, pkg, failReason);
        }
    }

    @Override
    public void onNextSwitchInstallDir(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onNextSwitchInstallDir(context, pkg);
        }
    }


    @Override
    public void onUninstallStart(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onUninstallStart(context, pkg);
        }
    }

    @Override
    public void onUninstallResult(Context context, String pkg, int type) {
        if (mInstallReport != null) {
            mInstallReport.onUninstallResult(context, pkg, type);
        }
    }

    @Override
    public void onStartSwitchInstallDir(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onStartSwitchInstallDir(context, pkg);
        }
    }

    @Override
    public void onStartSwitchDirSuccess(Context context, String pkg) {
        if (mInstallReport != null) {
            mInstallReport.onStartSwitchDirSuccess(context, pkg);
        }
    }

    @Override
    public void onDeleteTempInstallDir(Context context) {
        if (mInstallReport != null) {
            mInstallReport.onDeleteTempInstallDir(context);
        }
    }

    @Override
    public void startMainProcessService(Context context, String pkgName, int toExtProcess) {
        if (mLoadReport != null) {
            mLoadReport.startMainProcessService(context, pkgName, toExtProcess);
        }
    }

    @Override
    public void startPluginMainProcess(Context context, String pkgName) {
        if (mLoadReport != null) {
            mLoadReport.startPluginMainProcess(context, pkgName);
        }
    }

    @Override
    public void startPluginGPTProcess(Context context, String pkgName) {
        if (mLoadReport != null) {
            mLoadReport.startPluginGPTProcess(context, pkgName);
        }
    }

    @Override
    public void cacheIntent(Context context, String pkgName) {
        if (mLoadReport != null) {
            mLoadReport.cacheIntent(context, pkgName);
        }
    }

    @Override
    public void startIntentOnLoaded(Context context, String pkgName) {
        if (mLoadReport != null) {
            mLoadReport.startIntentOnLoaded(context, pkgName);
        }
    }

    @Override
    public void startRootActivity(Context context, String pkgName) {
        if (mLoadReport != null) {
            mLoadReport.startRootActivity(context, pkgName);
        }
    }

    @Override
    public void silentOrHasInstanceInGPTProcess(Context context, String pkgName, boolean isSilent) {
        if (mLoadReport != null) {
            mLoadReport.silentOrHasInstanceInGPTProcess(context, pkgName, isSilent);
        }
    }

    @Override
    public void onLoadApplication(Context context, String pkgName, long coastTime) {
        if (mLoadReport != null) {
            mLoadReport.onLoadApplication(context, pkgName, coastTime);
        }
    }

    @Override
    public void onCreateApplication(Context context, String pkgName, long coastTime) {
        if (mLoadReport != null) {
            mLoadReport.onCreateApplication(context, pkgName, coastTime);
        }
    }

    @Override
    public void initProxyEnvironment(Context context, String pkgName, long coastTime) {
        if (mLoadReport != null) {
            mLoadReport.initProxyEnvironment(context, pkgName, coastTime);
        }
    }

    @Override
    public void activityElapsedTime(Context context, String pkgName, String className, long coastTime) {
        if (mLoadReport != null) {
            mLoadReport.activityElapsedTime(context, pkgName, className, coastTime);
        }
    }

    @Override
    public void onException(Context context, String packageName, String exceptionStack, String key,
                            Pair<String, String>... customKeyValues) {
        if (mPluginReport != null) {
            mPluginReport.onException(context, packageName, exceptionStack, key, customKeyValues);
        }
    }

    @Override
    public void onExceptionByLogService(Context context, String packageName, String exceptionStack,
                                        String key, Pair<String, String>... customKeyValues) {
        if (mPluginReport != null) {
            mPluginReport.onExceptionByLogService(context, packageName, exceptionStack, key, customKeyValues);
        }
    }

}

