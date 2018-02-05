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

import android.text.format.DateUtils;

/**
 * Constants
 *
 * @author liuhaitao
 * @since 2014年6月6日
 */
public final class Constants {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false;
    /**
     * SDK的版本号 TODO 升级版本时需同步修改此值和脚本配置以保持一致。
     */
    public static final String VERSON = "6.0";

    /**
     * 插件未继承application时，默认的application名字
     */
    public static final String DEFAULT_APPLICATION_CLASS_NAME = "Application";
    /**
     * 加载插件超时的时间
     */
    public static final long MAX_LOADING_TARGET_TIME = DateUtils.MINUTE_IN_MILLIS;
    /**
     * 创建快捷方式action
     */
    public static final String ACTION_INSTALL_SHORT_CUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    /**
     * 删除快捷方式action
     */
    public static final String ACTION_UNINSTALL_SHORT_CUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    /**
     * android.app.ActivityManagerNative
     */
    public static final String ACTIVE_MANAGER_NATIVE_CLASS = "android.app.ActivityManagerNative";
    /**
     * android.app.IActivityManager
     */
    public static final String IACTIVE_MANAGER_CLASS = "android.app.IActivityManager";
    /**
     * android.app.ActivityManager
     */
    public static final String ACTIVE_MANAGER_CLASS = "android.app.ActivityManager";
    /**
     * android.app.INotificationManager
     */
    public static final String NOTIFICATION_MANAGER_NATIVE_CLASS = "android.app.INotificationManager";
    /**
     * android.content.pm.IPackageManager
     */
    public static final String PACKAGE_MANAGER_CLASS = "android.content.pm.IPackageManager";
    /**
     * android.view.IWindowSession
     */
    public static final String WINDOW_SESSION_CLASS = "android.view.IWindowSession";
    /**
     * android.net.wifi.IWifiManager
     */
    public static final String WIFI_MANAGER_CLASS = "android.net.wifi.IWifiManager";
    /**
     * android.app.IAlarmManager
     */
    public static final String IALARM_MANAGERR_CLASS = "android.app.IAlarmManager";
    /**
     * android.os.ServiceManager
     */
    public static final String SERVICE_MANAGER_CLASS = "android.os.ServiceManager";
    /**
     * android.os.storage.IMountService
     */
    public static final String IMOUNT_SERVICE_CLASS = "android.os.storage.IMountService";


    /**
     * GPT插件在非主进程运行的进程数
     */
    public static final int GPT_PROCESS_NUM = 2;

    /**
     * 宿主主进程
     */
    public static final int GPT_PROCESS_MAIN = -1;

    /**
     * GPT插件默认进程，也就是最初的:gpt进程，或者在unionProcess的情况是就是主进程 TODO 多进程扩展管理时需注意
     */
    public static final int GPT_PROCESS_DEFAULT = 0x0;

    /**
     * 调起插件时插件未安装的提示文案
     */
    public static final String HINT_TARGET_NOT_INSTALLED = "插件未安装";

    /**
     * 插件运行的gpt进程的后缀
     */
    public static final String PROCESS_GPT_SUFFIX = "gpt";

    /**
     * 插件安装进程的后缀
     */
    public static final String PROCESS_GPT_INSTALL_SUFFIX = "gptinstaller";

    /**
     * 合并插件的version name
     */
    public static final String MERGE_PLUGIN_VERSION_NAME = "merge_plugin_version_name";

    /**
     * 构造方法,不实例化
     */
    private Constants() {

    }

}
