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
package com.baidu.android.gporter.hostapi;

import android.content.Context;

import com.baidu.android.gporter.ProxyEnvironment;

/**
 * 用于获取主程序的一些接口
 *
 * @author liuhaitao
 * @since 2015年7月29日    GPT 1.2
 */
public class HostUtil {

    /**
     * 获取插件宿主的包名
     *
     * @param context 插件context
     * @return 宿主包名，如果插件是独立安装运行，则返回插件自身包名
     */
    public static String getHostPackageName(Context context) {

        String pkgName = null;

        if (context == null) {
            return pkgName;
        }

        Context appContext = getHostApplicationContextPrivate(context);

        pkgName = appContext.getPackageName();

        return pkgName;
    }

    /**
     * 获取 host app 的 context
     *
     * @param context 插件context
     * @return host的application context
     * @deprecated 该接口会引起插件不稳定，接口删除，返回的Context是传入Context的application context
     */
    public static Context getHostApplicationContext(Context context) {
        return context.getApplicationContext();
    }

    /**
     * 获取 host app 的 context
     *
     * @param context 插件context
     * @return host的application context
     */
    private static Context getHostApplicationContextPrivate(Context context) {
        /*
         * 注意:修改此函数一定要小心。因为这个代码会被打包到插件中。有新老版本兼容性问题。
         */

        String packageName = context.getPackageName();
        Context hostApplicationContext = context.getApplicationContext(); // 默认直接取宿主的context

        try {
            if (ProxyEnvironment.hasInstance(packageName)) { // 插件如果没有初始化，说明也是独立安装的模式，或者该方法是在宿主调用的
                hostApplicationContext = ProxyEnvironment.getInstance(packageName).getApplicationProxy();
            }
        } catch (NoClassDefFoundError e) {
            // class not found 此时为插件独立安装模式
        }

        return hostApplicationContext;
    }

    /**
     * 获取host中的资源，宿主和插件耦合，此方法慎用
     *
     * @param context       Context
     * @param packageName   插件应用的包名
     * @param resourcesName 资源名称
     * @param resourceType  资源类型
     * @return 资源id
     */
    public static int getHostResourcesId(Context context, String packageName, String resourcesName, String resourceType) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            return ProxyEnvironment.getInstance(packageName).getHostResourcesId(resourcesName, resourceType);
        } else {
            return context.getResources().getIdentifier(resourcesName, resourceType, packageName);
        }
    }

    /* BEGIN : for KO，给KO interface 包的时候打开注释

    public static boolean installApkFile(Context paramContext, String paramString) {
        try {
            return GPTPackageManager.getInstance(paramContext).installApkFileExt(paramString);
        } catch (NoClassDefFoundError localNoClassDefFoundError) {
        } catch (NoSuchMethodError methoderr) {
        }
        return false;
    }

    public static boolean uninstallTarget(Context paramContext, String paramString) {
        try {
            GPTPackageManager.getInstance(paramContext).deletePackage(paramString, true);
            return true;
        } catch (NoClassDefFoundError localNoClassDefFoundError) {
        }
        return false;
    }

    public static void launchTarget(Context paramContext, Intent intent) {
        try {
            TargetActivator.loadTargetAndRun(paramContext, intent, true);
        } catch (NoClassDefFoundError localNoClassDefFoundError) {
            paramContext.startActivity(intent);
        } catch (NoSuchMethodError methoderr) {
            paramContext.startActivity(intent);
        }

    }
    
    END : for KO */

    /**
     * 判断当前是否以插件运行
     *
     * @param context 插件context，**如果使用了宿主context会导致误判的**
     * @return true表示以插件运行，false表示独立运行
     */
    public static boolean isInPluginMode(Context context) {
        return !context.getPackageName().equals(getHostPackageName(context));
    }

    /**
     * 添加通知id到显示白名单
     *
     * @param packageName    插件应用的包名
     * @param notificationId 通知id
     * @return true表示添加成功，false表示添加失败
     */
    public static boolean addNotificationId(String packageName, int notificationId) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            return ProxyEnvironment.getInstance(packageName).addNotificationId(notificationId);
        } else {
            return false;
        }
    }
}
