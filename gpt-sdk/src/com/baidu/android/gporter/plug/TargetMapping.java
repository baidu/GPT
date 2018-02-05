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
package com.baidu.android.gporter.plug;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

/**
 * 插件信息映射接口
 *
 * @author liuhaitao
 * @since 2014-4-25
 */
public interface TargetMapping {

    /**
     * IntentInfo
     */
    public class IntentInfo {
        /**
         * type
         */
        public String type;
        /**
         * className
         */
        public String className;
        /**
         * categories
         */
        public ArrayList<String> categories;
        /**
         * actions
         */
        public ArrayList<String> actions;

    }

    /**
     * getTheme
     */
    int getTheme();

    /**
     * getPackageName
     *
     * @return PackageName
     */
    String getPackageName();

    /**
     * getVersionName
     *
     * @return VersionName
     */
    String getVersionName();

    /**
     * getVersionCode
     *
     * @return VersionCode
     */
    int getVersionCode();

    /**
     * getPackageInfo
     *
     * @return PackageInfo
     */
    PackageInfo getPackageInfo();

    /**
     * getThemeResource
     *
     * @return ThemeResource
     */
    int getThemeResource(String activity);

    /**
     * getActivityInfo
     *
     * @return ActivityInfo
     */
    ActivityInfo getActivityInfo(String activity);

    /**
     * getServiceInfo
     *
     * @return ServiceInfo
     */
    ServiceInfo getServiceInfo(String service);

    /**
     * getReceiverInfo
     *
     * @return ActivityInfo
     */
    ActivityInfo getReceiverInfo(String receiver);

    /**
     * getProviderInfo
     *
     * @return ProviderInfo
     */
    ProviderInfo getProviderInfo(String provider);

    /**
     * getRecvIntentFilters
     *
     * @return Map<String, ArrayList<ActivityIntentInfo>>
     */
    Map<String, ArrayList<ActivityIntentInfo>> getRecvIntentFilters();

    /**
     * getApplicationClassName
     *
     * @return ApplicationClassName
     */
    String getApplicationClassName();

    /**
     * getDefaultActivityName
     *
     * @return DefaultActivityName
     */
    String getDefaultActivityName();

    /**
     * getPermissions
     *
     * @return PermissionInfo[]
     */
    PermissionInfo[] getPermissions();

    /**
     * getMetaData
     *
     * @return Bundle
     */
    Bundle getMetaData();

    /**
     * getApplicationName
     *
     * @return ApplicationName
     */
    String getApplicationName();

    /**
     * getIntentInfos
     *
     * @return IntentInfos
     */
    Vector<IntentInfo> getIntentInfos();

    /**
     * getApkPath
     *
     * @return ApkPath
     */
    String getApkPath();

}
