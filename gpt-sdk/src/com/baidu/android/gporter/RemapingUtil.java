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
package com.baidu.android.gporter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.plug.TargetManager;
import com.baidu.android.gporter.plug.TargetMapping;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.BroadcastReceiverProxy;
import com.baidu.android.gporter.proxy.BroadcastReceiverProxyExt;
import com.baidu.android.gporter.proxy.PackageManagerWorker;
import com.baidu.android.gporter.proxy.service.ServiceProxy;
import com.baidu.android.gporter.proxy.service.ServiceProxyExt;
import com.baidu.android.gporter.util.Constants;

import java.util.List;

/**
 * 启动插件Component的Intent重映射工具
 *
 * @author liuhaitao
 * @since 2015-10-20
 */
public final class RemapingUtil {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "RemapingUtil";

    /**
     * 工具类，不可实例化
     */
    private RemapingUtil() {

    }

    /**
     * 显示已知插件包名类名情况下的Activity重映射方法
     *
     * @param hostCtx        宿主context
     * @param intent         原始intent
     * @param packageName    插件包名
     * @param targetActivity 插件类名
     * @param dealMode       是否处理LaunchMode
     */
    public static void remapActivityIntent(Context hostCtx, Intent intent, String packageName, String targetActivity,
                                           boolean dealMode) {

        // 获取插件信息
        GPTPackageInfo gptPkgInfo = GPTPackageManager.getInstance(hostCtx).getPackageInfo(packageName);
        if (gptPkgInfo == null) {
            return;
        }
        TargetMapping targetMapping = TargetManager.getInstance(hostCtx).getTargetMapping(packageName);
        if (targetMapping == null) {
            return;
        }

/*        if (targetActivity != null && targetActivity.endsWith(Constants.GPT_SUFFIX)) {
            
             * 从 PrefrenceActivity 中传进来的可能是 GPT 结束的类名。 ApiDemos app/device admin
             * 测试用例
             
            targetActivity = targetActivity.substring(0, (targetActivity.length() - Constants.GPT_SUFFIX.length()));
        }*/

        // 获取不到activity info，说明不是插件的Activity，不需要重映射
        if (targetMapping.getActivityInfo(targetActivity) == null) {
            return;
        }

        GPTComponentInfo info = new GPTComponentInfo();
        info.packageName = targetMapping.getPackageName();
        info.className = targetActivity;
        info.addSelf2Intent(intent);
        intent.setClass(
                hostCtx,
                ProxyActivityCounter.getInstance().getNextAvailableActivityClass(hostCtx,
                        targetMapping.getActivityInfo(targetActivity).packageName,
                        targetMapping.getActivityInfo(targetActivity), gptPkgInfo));
        if (dealMode) {
            // 实现launchMode
            ProxyEnvironment.dealLaunchMode(hostCtx, intent, packageName, targetActivity);
        }

        // packagename设置成插件，可能导致setResult失效
        if (TextUtils.equals(packageName, intent.getPackage())) {
            intent.setPackage(null);
        }
    }

    /**
     * 插件包名类名未知的情况下重映射插件Activity的方法
     *
     * @param hostCtx
     * @param originIntent
     * @param dealMode     是否处理LaunchMode
     */
    public static void remapActivityIntent(Context hostCtx, Intent originIntent, boolean dealMode) {
        if (DEBUG) {
            Log.d(TAG, "--- remapActivityIntent : " + originIntent.getComponent());
        }
        // host app的Intent，直接return
        String pkgFromIntent = getPackageFromIntent(originIntent);
        if (!TextUtils.isEmpty(pkgFromIntent) && pkgFromIntent.equals(hostCtx.getPackageName())) {
            return;
        }
        /**
         * 启动系统的Activity，例如卸载、安装， getComponent 为null。
         * 这样的Intent，需要通过查询插件activity的action和category来找到合适的activity.
         */
        if (originIntent.getComponent() != null) {
            remapActivityIntent(hostCtx, originIntent, originIntent.getComponent().getPackageName(), originIntent
                    .getComponent().getClassName(), dealMode);
        } else {
            List<ResolveInfo> ris = hostCtx.getPackageManager().queryIntentActivities(originIntent, 0);
            ResolveInfo pluginResolveInfo = null;
            if (ris != null && ris.size() > 0) {
                for (ResolveInfo info : ris) {
                    // 找到了插件的Activity
                    if (GPTPackageManager.getInstance(hostCtx).isPackageInstalled(info.activityInfo.packageName)) {
                        pluginResolveInfo = info;
                        break;
                    }
                }
            }
            if (ris != null && ris.size() > 0 && pluginResolveInfo == null) {
                // pluginResolveInfo 为null。没有匹配到插件中的Activity。匹配到了其他应用的Activity
                return;
            }
            if (pluginResolveInfo == null) {
                ris = PackageManagerWorker.queryIntentActivitiesFromPlugins(hostCtx, originIntent, 0);
                if (ris != null && ris.size() > 0) { // 取第一个匹配到的
                    pluginResolveInfo = ris.get(0);
                }
            }
            if (pluginResolveInfo != null) {
                remapActivityIntent(hostCtx, originIntent, pluginResolveInfo.activityInfo.packageName,
                        pluginResolveInfo.activityInfo.name, dealMode);
            }
        }

    }

    /**
     * 插件包名类名未知的情况下重映射插件Activity的方法
     * 默认处理LaunchMode
     *
     * @param hostCtx      Context
     * @param originIntent 原始Intent
     */
    public static void remapActivityIntent(Context hostCtx, Intent originIntent) {
        remapActivityIntent(hostCtx, originIntent, true);
    }

    /**
     * remapServiceIntent
     *
     * @param hostCtx      Context
     * @param originIntent 原始Intent
     */
    public static void remapServiceIntent(Context hostCtx, Intent originIntent) {
        // 隐式启动Service不支持
        if (originIntent.getComponent() == null) {
            return;
        }

        String targetService = originIntent.getComponent().getClassName();
        if (targetService == null) {
            return;
        }

        TargetMapping targetMapping = TargetManager.getInstance(hostCtx).getTargetMapping(
                originIntent.getComponent().getPackageName());
        if (targetMapping == null) {
            return;
        }

        if (targetMapping.getServiceInfo(targetService) == null) {
            return;
        }
        remapServiceIntent(hostCtx, originIntent, targetService);
    }

    /**
     * remapServiceIntent
     *
     * @param hostCtx       Context
     * @param intent        Intent
     * @param targetService targetService
     */
    public static void remapServiceIntent(Context hostCtx, Intent intent, String targetService) {
        TargetMapping targetMapping = TargetManager.getInstance(hostCtx).getTargetMapping(
                intent.getComponent().getPackageName());
        if (targetMapping == null) {
            return;
        }

        // 获取插件信息
        GPTPackageInfo gptPkgInfo = GPTPackageManager.getInstance(hostCtx).getPackageInfo(
                targetMapping.getPackageName());
        if (gptPkgInfo == null) {
            return;
        }

        ServiceInfo servInfo = targetMapping.getServiceInfo(targetService);
        if (servInfo == null) {
            return;
        }

        switch (gptPkgInfo.extProcess) {
            case Constants.GPT_PROCESS_DEFAULT:
            default:
                if (!gptPkgInfo.isUnionProcess) {
                    intent.setClass(hostCtx, ServiceProxyExt.class);
                } else {
                    intent.setClass(hostCtx, ServiceProxy.class);
                }
                break;
        }
        GPTComponentInfo info = new GPTComponentInfo();
        info.packageName = targetMapping.getPackageName();
        info.className = targetService;
        intent.addCategory(info.toString());
    }

    /**
     * remapReceiverIntent
     *
     * @param hostCtx      Context
     * @param originIntent Intent
     */
    public static void remapReceiverIntent(Context hostCtx, Intent originIntent) {
        // 注意:pkg设置了插件包名的话，要替换成宿主包名，不然插件收不到广播
        String pkg = originIntent.getPackage();
        if (pkg != null && GPTPackageManager.getInstance(hostCtx).getPackageInfo(pkg) != null) {
            originIntent.setPackage(hostCtx.getPackageName());
        }

        if (originIntent.getComponent() == null) {
            return;
        }

        TargetMapping targetMapping = TargetManager.getInstance(hostCtx).getTargetMapping(
                originIntent.getComponent().getPackageName());
        if (targetMapping == null) {
            return;
        }

        // 获取插件信息
        GPTPackageInfo gptPkgInfo = GPTPackageManager.getInstance(hostCtx).getPackageInfo(
                targetMapping.getPackageName());
        if (gptPkgInfo == null) {
            return;
        }

        String targetReceiver = originIntent.getComponent().getClassName();
        ActivityInfo recvInfo = targetMapping.getReceiverInfo(targetReceiver);
        if (recvInfo == null) {
            return;
        }

        GPTComponentInfo info = new GPTComponentInfo();
        info.packageName = targetMapping.getPackageName();
        info.className = targetReceiver;
        originIntent.addCategory(info.toString());
        switch (gptPkgInfo.extProcess) {
            case Constants.GPT_PROCESS_DEFAULT:
            default:
                if (!gptPkgInfo.isUnionProcess) {
                    originIntent.setClass(hostCtx, BroadcastReceiverProxyExt.class);
                } else {
                    originIntent.setClass(hostCtx, BroadcastReceiverProxy.class);
                }
                break;
        }

        // 注意:pkg要替换成宿主，不然，插件收不到广播
        originIntent.setPackage(hostCtx.getPackageName());
    }

    /**
     * 从Intent中获取包名
     *
     * @param originIntent
     * @return 包名
     */
    private static String getPackageFromIntent(Intent originIntent) {
        String pkgFromIntent = originIntent.getPackage();
        if (TextUtils.isEmpty(pkgFromIntent) && originIntent.getComponent() != null) {
            pkgFromIntent = originIntent.getComponent().getPackageName();
        }
        return pkgFromIntent;
    }

}
