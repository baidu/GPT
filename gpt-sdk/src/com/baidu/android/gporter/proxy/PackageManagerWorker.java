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
package com.baidu.android.gporter.proxy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import com.baidu.android.gporter.plug.TargetManager;
import com.baidu.android.gporter.plug.TargetMapping;
import com.baidu.android.gporter.plug.TargetMapping.IntentInfo;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 采用 java Proxy 的方式，接管系统的 PackageManager 的实现。
 * <p>
 * 替换掉ApplicationPackageManager.mPM
 * <p>
 * android.content.pm.IPackageManager 接口的代理类。需要考虑版本兼容性。
 *
 * @author liuhaitao
 * @since 2014-4-22
 */
public class PackageManagerWorker extends InterfaceProxy {

    /**
     * log tag
     */
    final static String TAG = "PackageManagerWorker";
    /**
     * host包名
     */
    private String mPackageName;
    /**
     * 主程序的 context
     */
    private Context mHostContext;

    /**
     * AppllicationPackageManager.mPM 真正的系统IPackageManager
     */
    public IPackageManager mTarget;

    /**
     * 用于低版本没有 userid的 接口，重用高版本函数时的标记。 表示是从低版本的函数调过来的。需要调用
     * IPackageManager 的对应的低版本接口
     */
    private static final int INVALID_USER_ID = -11111;

    /**
     * 构造方法
     *
     * @param hostContext 宿主程序的context
     */
    public PackageManagerWorker(Context hostContext) {
        super(Constants.PACKAGE_MANAGER_CLASS);

        mHostContext = hostContext;
    }


    /**
     * 设置对应插件的host包名
     *
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    /**
     * 判断是插件。
     * <p>
     * 因为通过 PM 接口获取 pkg信息，我们优先返回插件的，不是插件再通过系统接口返回。
     *
     * @param packageName
     * @return
     */
    private boolean isPlugin(String packageName) {
        return GPTPackageManager.getInstance(mHostContext).getPackageInfo(packageName) != null;
    }

    /**
     * 获取插件信息
     *
     * @param packageName 插件包名
     * @return 插件PackageInfo
     */
    private PackageInfo getPluginPackageInfo(String packageName) {
        TargetMapping maping = TargetManager.getInstance(mHostContext).getTargetMapping(packageName);
        if (maping == null) {
            return null;
        }
        return maping.getPackageInfo();
    }

    /**
     * 获取插件信息
     *
     * @param packageName 插件包名
     * @param flags       flags
     * @return 插件PackageInfo
     */
    public PackageInfo getPackageInfo(String packageName, int flags) {
        return getPackageInfo(packageName, flags, INVALID_USER_ID);
    }

    /**
     * 获取插件信息
     *
     * @param packageName 插件包名
     * @param flags       flags
     * @param userId      userId
     * @return 插件PackageInfo
     */
    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        if (isPlugin(packageName)) {
            // 插件模式取自身包信息，返回插件的信息
            return getPluginPackageInfo(packageName);
        } else {
            PackageInfo packageInfo = null;
            if (userId == INVALID_USER_ID) {
                packageInfo = mTarget.getPackageInfo(packageName, flags);
            } else {
                packageInfo = mTarget.getPackageInfo(packageName, flags, userId);
            }
            return packageInfo;
        }
    }


    /**
     * android 4.0 以下，没有多user的IPackageManager接口，调用此函数。
     */
    public ApplicationInfo getApplicationInfo(String packageName, int flags) {
        return getApplicationInfo(packageName, flags, INVALID_USER_ID);
    }

    /**
     * 高版本IPackageManager接口。 4.x
     */
    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        if (isPlugin(packageName)) {
            return getPluginPackageInfo(packageName).applicationInfo;
        } else {
            if (userId == INVALID_USER_ID) {
                return mTarget.getApplicationInfo(packageName, flags);
            } else {
                return mTarget.getApplicationInfo(packageName, flags, userId);
            }
        }
    }

    /**
     * getActivityInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @return ActivityInfo
     */
    public ActivityInfo getActivityInfo(ComponentName component, int flags) {
        return getActivityInfo(component, flags, INVALID_USER_ID);
    }

    /**
     * getActivityInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @param userId    userId
     * @return ActivityInfo
     */
    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {

        String packageName = component.getPackageName();

        ActivityInfo result = null;

        if (isPlugin(packageName)) {
            PackageInfo pkgInfo = getPluginPackageInfo(packageName);
            if (pkgInfo == null) {
                return null;
            }
            for (int i = 0; i < pkgInfo.activities.length; i++) {

                // TODO 加后缀匹配，临时方案，需要修改
                if (component.getClassName().equalsIgnoreCase(pkgInfo.activities[i].name)
                        /*|| (component.getClassName() + Constants.GPT_SUFFIX).equalsIgnoreCase(pkgInfo.activities[i].name)*/) {

                    result = pkgInfo.activities[i];
                    break;
                }
            }
        } else {
            if (userId == INVALID_USER_ID) {
                result = mTarget.getActivityInfo(component, flags);
            } else {
                result = mTarget.getActivityInfo(component, flags, userId);
            }
        }

        return result;
    }

    /**
     * getReceiverInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @return ActivityInfo
     */
    public ActivityInfo getReceiverInfo(ComponentName component, int flags) {
        return getReceiverInfo(component, flags, INVALID_USER_ID);
    }

    /**
     * getReceiverInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @param userId    userId
     * @return ActivityInfo
     */
    public ActivityInfo getReceiverInfo(ComponentName component, int flags, int userId) {
        String packageName = component.getPackageName();

        ActivityInfo result = null;

        if (isPlugin(packageName)) {
            PackageInfo pkgInfo = getPluginPackageInfo(packageName);
            if (pkgInfo == null) {
                return null;
            }
            for (int i = 0; i < pkgInfo.receivers.length; i++) {
                if (component.getClassName().equalsIgnoreCase(pkgInfo.receivers[i].name)) {

                    result = pkgInfo.receivers[i];
                    break;
                }
            }
        } else {
            if (userId == INVALID_USER_ID) {
                result = mTarget.getReceiverInfo(component, flags);
            } else {
                result = mTarget.getReceiverInfo(component, flags, userId);
            }
        }

        return result;
    }

    /**
     * getServiceInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @return ServiceInfo
     */
    public ServiceInfo getServiceInfo(ComponentName component, int flags) {
        return getServiceInfo(component, flags, INVALID_USER_ID);
    }

    /**
     * getServiceInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @param userId    userId
     * @return ServiceInfo
     */
    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {

        String packageName = component.getPackageName();

        ServiceInfo result = null;

        if (isPlugin(packageName)) {
            PackageInfo pkgInfo = getPluginPackageInfo(packageName);
            if (pkgInfo == null) {
                return null;
            }
            for (int i = 0; i < pkgInfo.services.length; i++) {
                if (component.getClassName().equalsIgnoreCase(pkgInfo.services[i].name)) {

                    result = pkgInfo.services[i];
                    break;
                }
            }
        } else {
            if (userId == INVALID_USER_ID) {
                result = mTarget.getServiceInfo(component, flags);
            } else {
                result = mTarget.getServiceInfo(component, flags, userId);
            }
        }

        return result;
    }

    /**
     * getProviderInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @return ProviderInfo
     */
    public ProviderInfo getProviderInfo(ComponentName component, int flags) {
        return getProviderInfo(component, flags, INVALID_USER_ID);
    }

    /**
     * getProviderInfo
     *
     * @param component ComponentName
     * @param flags     flags
     * @param userId    userId
     * @return ProviderInfo
     */
    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {

        String packageName = component.getPackageName();

        ProviderInfo result = null;
        if (isPlugin(packageName)) {
            PackageInfo pkgInfo = getPluginPackageInfo(packageName);
            if (pkgInfo == null) {
                return null;
            }
            for (int i = 0; i < pkgInfo.providers.length; i++) {
                if (component.getClassName().equalsIgnoreCase(pkgInfo.providers[i].name)) {

                    result = pkgInfo.providers[i];
                    break;
                }
            }
        } else {
            if (userId == INVALID_USER_ID) {
                result = mTarget.getProviderInfo(component, flags);
            } else {
                result = mTarget.getProviderInfo(component, flags, userId);
            }
        }

        return result;
    }

    /**
     * addPermission
     *
     * @param info PermissionInfo
     * @return true or false
     */
    public boolean addPermission(PermissionInfo info) {
        String packageName = info.packageName;

        if (isPlugin(packageName)) {
            // 给插件加，相当于给 host app 加
            info.packageName = mPackageName;
        }
        return mTarget.addPermission(info);
    }

    /**
     * addPermissionAsync
     *
     * @param info PermissionInfo
     * @return true or false
     */
    public boolean addPermissionAsync(PermissionInfo info) {
        String packageName = info.packageName;

        if (isPlugin(packageName)) {
            info.packageName = mPackageName;
        }
        return mTarget.addPermissionAsync(info);
    }

    /**
     * queryIntentActivities
     *
     * @param intent       Intent
     * @param resolvedType resolvedType
     * @param flags        flags
     * @return List<ResolveInfo>
     */
    public List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags) {
        return queryIntentActivities(intent, resolvedType, flags, INVALID_USER_ID);
    }

    /**
     * queryIntentActivities
     *
     * @param intent       Intent
     * @param resolvedType resolvedType
     * @param flags        flags
     * @param userId       userId
     * @return List<ResolveInfo>
     */
    public List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {

        List<ResolveInfo> rInfos = null;

        if (userId == INVALID_USER_ID) {
            rInfos = mTarget.queryIntentActivities(intent, resolvedType, flags);
        } else {
            rInfos = mTarget.queryIntentActivities(intent, resolvedType, flags, userId);
        }

        // 如果没有从系统安装中找到，则从插件找
        if (rInfos == null || rInfos.size() <= 0) {
            return queryIntentActivitiesFromPlugins(mHostContext, intent, flags);
        }
        return rInfos;
    }

    /**
     * 从插件列表中查找intent
     *
     * @param hostCtx 宿主context
     * @param intent  目标Intent
     * @param flags   intent flag （这个目前没有处理，需要完善）
     * @return 查找结果
     */
    public static List<ResolveInfo> queryIntentActivitiesFromPlugins(Context hostCtx, Intent intent, int flags) {
        List<ResolveInfo> rInfos = null;

        // intent里设置了package了，直接判断这个插件就可以
        String packageName = intent.getPackage();
        if (!TextUtils.isEmpty(packageName)
                && GPTPackageManager.getInstance(hostCtx).getPackageInfo(packageName) != null) {
            return queryIntentActivitiesFromPkg(hostCtx, intent, flags, packageName);
        }

        List<GPTPackageInfo> pluginPkgs = GPTPackageManager.getInstance(hostCtx).getInstalledApps();
        if (pluginPkgs == null || pluginPkgs.size() == 0) { // 容错
            return null;
        }

        rInfos = new ArrayList<ResolveInfo>();
        for (GPTPackageInfo pluginPkg : pluginPkgs) {
            List<ResolveInfo> infos = queryIntentActivitiesFromPkg(hostCtx, intent, flags, pluginPkg.packageName);

            if (infos != null) {
                rInfos.addAll(infos);
            }
        }

        return rInfos;
    }

    /**
     * 从某个插件中查找 intent
     *
     * @param hostCtx 宿主context
     * @param intent  目标Intent
     * @param flags   intent flag
     * @param pkgName 包名
     * @return 查找结果
     */
    public static List<ResolveInfo> queryIntentActivitiesFromPkg(Context hostCtx, Intent intent, int flags,
                                                                 String pkgName) {

        // 不是插件直接返回
        if (GPTPackageManager.getInstance(hostCtx).getPackageInfo(pkgName) == null) {
            return null;
        }

        List<ResolveInfo> rInfos = null;

        TargetMapping maping = TargetManager.getInstance(hostCtx).getTargetMapping(pkgName);
        if (maping == null) {
            return null;
        }
        PackageInfo pkgInfo = maping.getPackageInfo();

        String pkg = intent.getPackage();

        // 没有执行pkg，可以进行隐式查询。
        // 或者指定了pkg，就是当前插件pkg，或者指定为host的，则进行查询
        if (pkg == null || pkg.length() == 0 || pkgName.equalsIgnoreCase(pkg) || hostCtx.getPackageName().equals(pkg)) {
            String className = null;
            if (intent.getComponent() != null) {
                className = intent.getComponent().getClassName();
            }
            String[] categories = null;
            if (intent.getCategories() != null) {
                categories = intent.getCategories().toArray(new String[]{});
            }
            String action = intent.getAction();

            if (TargetManager.getInstance(hostCtx).getTargetMapping(pkgName) == null) {
                return rInfos;
            }

            Vector<IntentInfo> intentInfos =
                    TargetManager.getInstance(hostCtx).getTargetMapping(pkgName).getIntentInfos();
            rInfos = new ArrayList<ResolveInfo>();
            for (int i = 0; i < intentInfos.size(); i++) {
                TargetMapping.IntentInfo intentInfo = intentInfos.get(i);
                if (!intentInfo.type.equalsIgnoreCase("activity")) {
                    continue;
                }
                if (className != null && !className.equalsIgnoreCase(intentInfo.className)) {
                    continue;
                }
                if (action != null && action.length() > 0) {
                    boolean hasAction = false;
                    for (int j = 0; j < intentInfo.actions.size(); j++) {
                        if (action.equalsIgnoreCase(intentInfo.actions.get(j))) {
                            hasAction = true;
                            break;
                        }
                    }
                    if (!hasAction) {
                        continue;
                    }
                }

                if (categories != null && categories.length > 0) {
                    boolean hasAllCategory = true;
                    for (int j = 0; j < categories.length; j++) {
                        boolean hasCategory = false;
                        for (int k = 0; k < intentInfo.categories.size(); k++) {
                            if (categories[j].equalsIgnoreCase(intentInfo.categories.get(k))) {
                                hasCategory = true;
                                break;
                            }
                        }
                        if (!hasCategory) {
                            hasAllCategory = false;
                            break;
                        }
                    }

                    if (!hasAllCategory) {
                        continue;
                    }
                }
                ResolveInfo resolveInfo = new ResolveInfo();
                for (int j = 0; j < pkgInfo.activities.length; j++) {
                    if (pkgInfo.activities[j].name.equalsIgnoreCase(intentInfo.className)) {
                        resolveInfo.activityInfo = new ActivityInfo(pkgInfo.activities[j]);
                        resolveInfo.activityInfo.name = intentInfo.className;
                        resolveInfo.icon = pkgInfo.activities[j].icon;
                        resolveInfo.labelRes = pkgInfo.activities[j].labelRes;
                        resolveInfo.match = IntentFilter.MATCH_CATEGORY_MASK;
                        resolveInfo.resolvePackageName = pkgName;
                        resolveInfo.nonLocalizedLabel = pkgInfo.activities[j].nonLocalizedLabel;
                        resolveInfo.preferredOrder = 0;
                        resolveInfo.priority = 0;
                        break;
                    }
                }
                if (resolveInfo.activityInfo == null) {
                    continue;
                }

                boolean hasFilter = false;
                IntentFilter intentFilter = new IntentFilter();

                if (action != null && action.length() > 0) {
                    intentFilter.addAction(action);
                    hasFilter = true;
                }

                resolveInfo.isDefault = false;

                if (categories != null) {
                    for (int j = 0; j < categories.length; j++) {
                        intentFilter.addCategory(categories[j]);
                        if (categories[j].equalsIgnoreCase(Intent.CATEGORY_DEFAULT)) {
                            resolveInfo.isDefault = true;
                        }
                        hasFilter = true;
                    }
                }

                if (hasFilter) {
                    resolveInfo.filter = intentFilter;
                }

                rInfos.add(resolveInfo);

            }
        }

        return rInfos;
    }

    // 测试下。不需要重写
    //public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo);

    // 测试下。不需要重写
    // public Drawable getApplicationIcon(String packageName) throws NameNotFoundException
    //测试下。不需要重写
    // public Drawable getActivityLogo(ComponentName activityName)

    // 测试下。不需要重写
    //public Drawable getApplicationLogo(String packageName)

    // 测试下。不需要重写
    // public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo)

    // 测试下。不需要重写
    // public Resources getResourcesForApplication(String appPackageName)

    // 测试一下 不需要重写
    // public Drawable getActivityIcon(ComponentName activityName)

    /**
     * setInstallerPackageName
     *
     * @param targetPackage        目标包名
     * @param installerPackageName installerPackageName
     */
    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        if (isPlugin(targetPackage)) {
            // 什么也不干
        } else {
            mTarget.setInstallerPackageName(targetPackage, installerPackageName);
        }
    }

    /**
     * setInstallerPackageName
     *
     * @param packageName 包名
     * @return InstallerPackageName
     */
    public String getInstallerPackageName(String packageName) {
        if (isPlugin(packageName)) {
            return null;
        } else {
            return mTarget.getInstallerPackageName(packageName);
        }
    }

    /**
     * setComponentEnabledSetting
     *
     * @param componentName ComponentName
     * @param newState      newState
     * @param flags         flags
     */
    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
        setComponentEnabledSetting(componentName, newState, flags, INVALID_USER_ID);
    }

    /**
     * setComponentEnabledSetting
     *
     * @param componentName ComponentName
     * @param newState      newState
     * @param flags         flags
     * @param userId        userId
     */
    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
        String packageName = componentName.getPackageName();

        if (isPlugin(packageName)) {
            // 暂时不做处理
        } else {
            if (userId == INVALID_USER_ID) {
                mTarget.setComponentEnabledSetting(componentName, newState, flags);
            } else {
                mTarget.setComponentEnabledSetting(componentName, newState, flags, userId);
            }
        }
    }

    /**
     * getComponentEnabledSetting
     *
     * @param componentName ComponentName
     * @return ComponentEnabledSetting
     */
    public int getComponentEnabledSetting(ComponentName componentName) {
        return getComponentEnabledSetting(componentName, INVALID_USER_ID);
    }

    /**
     * getComponentEnabledSetting
     *
     * @param componentName ComponentName
     * @param userId        userId
     * @return ComponentEnabledSetting
     */
    public int getComponentEnabledSetting(ComponentName componentName, int userId) {
        String packageName = componentName.getPackageName();
        if (isPlugin(packageName)) {
            return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        } else {
            if (userId == INVALID_USER_ID) {
                return mTarget.getComponentEnabledSetting(componentName);
            } else {
                return mTarget.getComponentEnabledSetting(componentName, userId);
            }
        }
    }

    /**
     * checkPermission
     *
     * @param permName permName
     * @param pkgName  包名
     * @return checkPermission结果
     */
    public int checkPermission(String permName, String pkgName) {
        String pkg = null;
        if (TargetManager.getInstance(mHostContext).getTargetMapping(pkgName) != null) {

            // TODO 校验插件是否声明此权限
            // 校验插件权限的时候传递宿主包名
            pkg = mHostContext.getPackageName();
        } else {
            pkg = pkgName;
        }
        return mTarget.checkPermission(permName, pkg);
    }

    /**
     * checkPermission
     *
     * @param permName permName
     * @param pkgName  包名
     * @param userId   userId
     * @return checkPermission结果
     */
    public int checkPermission(String permName, String pkgName, int userId) {
        String pkg = null;
        if (TargetManager.getInstance(mHostContext).getTargetMapping(pkgName) != null) {

            // TODO 校验插件是否声明此权限
            // 校验插件权限的时候传递宿主包名
            pkg = mHostContext.getPackageName();
        } else {
            pkg = pkgName;
        }
        return mTarget.checkPermission(permName, pkg, userId);
    }

}
