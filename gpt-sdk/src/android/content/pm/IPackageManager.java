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
package android.content.pm;


import java.util.List;

import android.content.ComponentName;
import android.content.Intent;

/**
 * Hook系统的对应接口类方法
 *
 * @author liuhaitao
 * @since 2015-05-13
 */
public interface IPackageManager {
    /**
     * 2.x - 3.x
     */
    PackageInfo getPackageInfo(String packageName, int flags);

    /**
     * 4.x 5.x 6.x
     */
    PackageInfo getPackageInfo(String packageName, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    ApplicationInfo getApplicationInfo(String packageName, int flags);

    /**
     * 4.x 5.x 6.x
     */
    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    ActivityInfo getActivityInfo(ComponentName className, int flags);

    /**
     * 4.x 5.x 6.x
     */
    ActivityInfo getActivityInfo(ComponentName className, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    ActivityInfo getReceiverInfo(ComponentName className, int flags);

    /**
     * 4.x 5.x 6.x
     */
    ActivityInfo getReceiverInfo(ComponentName className, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    ServiceInfo getServiceInfo(ComponentName className, int flags);

    /**
     * 4.x 5.x 6.x
     */
    ServiceInfo getServiceInfo(ComponentName className, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    ProviderInfo getProviderInfo(ComponentName className, int flags);

    /**
     * 4.x 5.x 6.x
     */
    ProviderInfo getProviderInfo(ComponentName className, int flags, int userId);

    /**
     * 2.x 3.x 4.x 5.x 6.x
     */
    boolean addPermission(PermissionInfo info);

    /**
     * 2.x 3.x 4.x 5.x 6.x
     */
    boolean addPermissionAsync(PermissionInfo info);

    /**
     * 2.x - 3.x
     */
    List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags);

    /**
     * 4.x 5.x 6.x
     */
    List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId);

    /**
     * 2.x 3.x 4.x 5.x 6.x
     */
    void setInstallerPackageName(String targetPackage, String installerPackageName);

    /**
     * 2.x 3.x 4.x 5.x 6.x
     */
    String getInstallerPackageName(String packageName);

    /**
     * setComponentEnabledSetting
     */
    void setComponentEnabledSetting(ComponentName componentName, int newState, int flags);

    /**
     * 4.x 5.x 6.x
     */
    void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId);

    /**
     * 2.x - 3.x
     */
    int getComponentEnabledSetting(ComponentName componentName);

    /**
     * 4.x 5.x 6.x
     */
    int getComponentEnabledSetting(ComponentName componentName, int userId);

    /**
     * 2.X - 5.X
     */
    int checkPermission(String permName, String pkgName);

    /**
     * 6.X,7.X, 8.X
     */
    int checkPermission(String permName, String pkgName, int userId);
}
