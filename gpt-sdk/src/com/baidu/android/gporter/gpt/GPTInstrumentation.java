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
package com.baidu.android.gporter.gpt;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Window;

import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.ProxyPhoneLayoutInflater;
import com.baidu.android.gporter.proxy.ProxyUtil;
import com.baidu.android.gporter.proxy.WindowCallbackWorker;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.PluginTimeLine;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * GPTInstrumentation
 *
 * @author liuhaitao
 * @since 2015年9月22日
 */
public class GPTInstrumentation extends Instrumentation {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "GPTInstrumentation";

    /**
     * 判断是否是插件。
     * 因为这通过PM接口获取pkg信息，我们优先返回插件的，不是插件再通过系统接口返回。
     *
     * @param packageName 包名
     * @return true or false
     */
    public static boolean isPlugin(String packageName) {
        /*
         * 这里简单处理，如果插件没有被加载，也返回false。只有插件初始化了才认为是插件。同时也有效率方面的考虑。
         * 因为运行到这里。插件已经被初始化。可以根据有没有实例进行判断。
         */
        return ProxyEnvironment.hasInstance(packageName);
    }

    @Override
    public void callApplicationOnCreate(Application app) {

        onCallApplicationOnCreate(app);

        long start = SystemClock.elapsedRealtime();
        super.callApplicationOnCreate(app);
        // 统计插件自己Application onCreate的时长
        long time = SystemClock.elapsedRealtime() - start;
        PluginTimeLine timeLine = ProxyEnvironment.pluginTimeLineMap.get(app.getPackageName());
        if (timeLine != null) {
            timeLine.createApplicationTime = time;
        }
        ReportManger.getInstance().onCreateApplication(app.getApplicationContext(), app.getPackageName(),
                time);
    }

    /**
     * onCallApplicationOnCreate
     *
     * @param app Application
     */
    private void onCallApplicationOnCreate(Application app) {
        String packageName = app.getPackageName();
        boolean isPlugin = isPlugin(packageName);

        if (!isPlugin) {
            return;
        }

        // Begin:【FixBug】解决在中兴手机上找不到资源的问题，中兴部分手机的ROM上自己继承ContextImpl实现了一个AppContextImpl，
        // 里面做一些BaseContext的复用，导致插件获取Resources时可能会取到宿主的。
        try {
            Class<?> clsCtxImpl = Class.forName("android.app.ContextImpl");
            Object base = app.getBaseContext();
            if (base.getClass() != clsCtxImpl) {
                Constructor<?> cst = clsCtxImpl.getConstructor(clsCtxImpl);
                Object impl = cst.newInstance(base);
                JavaCalls.setField(app, "mBase", impl);
            }
        } catch (Exception e) {

            if (DEBUG) {
                e.printStackTrace();
            }
            if (ProxyEnvironment.hasInstance(app.getPackageName())) {
                ReportManger.getInstance().onException(
                        ProxyEnvironment.getInstance(app.getPackageName()).getApplicationProxy(), packageName,
                        Util.getCallStack(e), ExceptionConstants.TJ_78730010);
            }
        }

        replacePluginPackageName2Host(app);
        replaceSystemServices(app);
        replaceExternalDirs(app);

        ProxyUtil.replaceSystemServices(app);
    }


    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {

        onCallActivityOnCreate(activity);

        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {

        onCallActivityOnCreate(activity);

        super.callActivityOnCreate(activity, icicle, persistentState);
    }

    /**
     * onCallActivityOnCreate
     *
     * @param activity Activity
     */
    private void onCallActivityOnCreate(Activity activity) {

        String packageName = activity.getPackageName();
        boolean isPlugin = isPlugin(packageName);

        if (!isPlugin) {
            return;
        }

        if (ProxyEnvironment.pluginHotStartTimeMap.get(packageName) != null) {
            long stamp = ProxyEnvironment.pluginHotStartTimeMap.get(packageName);
            long millis = SystemClock.elapsedRealtime() - stamp;
            if (stamp > -1 && millis > 0) {
                ReportManger.getInstance().onPluginHotLoad(activity.getApplicationContext(), packageName, millis);
                ProxyEnvironment.pluginHotStartTimeMap.remove(packageName);
            }
        }
        replacePluginPackageName2Host(activity);

        replaceSystemServices(activity);

        replaceWindow(activity);

        replaceExternalDirs(activity);

        // 初始化 activity layoutinflator and localactivity manager
        Activity parent = activity.getParent();
        if (parent != null && parent instanceof ActivityProxy) {
            ((ActivityProxy) parent).onBeforeCreate(activity);
        }

        if (Build.VERSION.SDK_INT < 23 /*Android m 6.0*/) {
            // bindservice trick begin
            // 如果在actiivty的 oncreate 中 binder service，token 中的 activity 对象为 null
            String className = "android.app.LocalActivityManager$LocalActivityRecord";
            Class clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

            IBinder token = JavaCalls.callMethod(activity.getBaseContext(), "getActivityToken");

            if (clazz != null && token != null && token.getClass().equals(clazz)) {
                Activity a = (Activity) JavaCalls.getField(token, "activity");
                if (a == null) {
                    JavaCalls.setField(token, "activity", activity);
                }
            }
            // bindservice trick end
        } else {
            // 6.0 以上 Activity.mBase.mActvityToken 一直为 null，不使用也可以工作。
        }

    }

    /**
     * 替换自己的LayoutInflator，用于解决setContextView，
     * 如果插件和主进程在同一个进程，并且两个classloader中有相同的 view class。比如 support v4。
     * LayoutInflator 中 sConstructorMap 静态变量，导致的类型转换错误。
     *
     * @param context             Context
     * @param superLayoutInflator superGetSystemService  调用 super.getSystemService 返回的 service
     * @return LayoutInflater
     */
    private static LayoutInflater getProxyLayoutInflator(Context context, LayoutInflater superLayoutInflator) {
        LayoutInflater layoutInflator = new ProxyPhoneLayoutInflater(superLayoutInflator, context);

        // ProxyPhoneLayoutInflater在第一次setFactory时会生成代理Factory
        // set一个null就是为了生成我们自己的代理Factory，保证在宿主和插件有"控件类"冲突时，能够获取正确的类 —— by chenyangkun
        layoutInflator.setFactory(null);

        return layoutInflator;
    }

    /**
     * 替换目标对象的window。
     *
     * @param activity Activity
     */
    private static void replaceWindow(Activity activity) {
        Activity parent = activity.getParent();
        if (parent != null && parent instanceof ActivityProxy) {
            JavaCalls.setField(activity, "mWindow", parent.getWindow());
        }

        replaceWindowCallback(activity);
    }

    /**
     * 替换WindowCallback
     *
     * @param activity Activity
     */
    public static void replaceWindowCallback(Activity activity) {
        activity.getWindow().setCallback(activity);

        Window.Callback callback = activity.getWindow().getCallback();

        WindowCallbackWorker callbackWorker = new WindowCallbackWorker();
        callbackWorker.mTarget = callback;
        callbackWorker.mActivity = activity;

        activity.getWindow().setCallback(callbackWorker);
    }

    /**
     * 在必要的地方把插件的包名替换成宿主的
     *
     * @param context application or activity
     */
    public static void replacePluginPackageName2Host(ContextWrapper context) {

        // OpPackageName调用的地方一般都是aidl接口传递包名给system server，所以要用宿主的包名。
        Context baseContext = context.getBaseContext();
        String hostPackageName = ProxyEnvironment.getInstance(context.getPackageName()).getHostPackageName();
        JavaCalls.setField(baseContext, "mOpPackageName", hostPackageName);

        // 可能要访问其他应用或者系统应用的ContentProvider，Resolver的包名一定是要宿主的。
        ContentResolver resolver = context.getContentResolver();
        if (resolver != null) {
            JavaCalls.setField(resolver, "mPackageName", hostPackageName);
        }
    }

    /**
     * 替换SystemServices
     *
     * @param context Application
     */
    private static void replaceSystemServices(Application context) {
        String packageName = context.getPackageName();

        boolean isPlugin = isPlugin(packageName);

        if (isPlugin) {
            // replayce layout inflater
            Context base = context.getBaseContext();

            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            LayoutInflater gptLayoutInflater = getProxyLayoutInflator(context, layoutInflater);

            if (Build.VERSION.SDK_INT >= 23 /*Android m 6.0*/) {

                HashMap serviceMap = (HashMap) JavaCalls.getStaticField("android.app.SystemServiceRegistry",
                        "SYSTEM_SERVICE_FETCHERS");
                Object serviceFetcher = serviceMap.get(Context.LAYOUT_INFLATER_SERVICE);

                int cacheIndex = (Integer) JavaCalls.getField(serviceFetcher, "mCacheIndex");
                Object[] serviceCache = (Object[]) JavaCalls.getField(base, "mServiceCache");

                serviceCache[cacheIndex] = gptLayoutInflater;

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

                HashMap serviceMap = (HashMap) JavaCalls.getField(base, "SYSTEM_SERVICE_MAP");
                Object serviceFetcher = serviceMap.get(Context.LAYOUT_INFLATER_SERVICE);

                int cacheIndex = (Integer) JavaCalls.getField(serviceFetcher, "mContextCacheIndex");
                ArrayList serviceCache = (ArrayList) JavaCalls.getField(base, "mServiceCache");

                serviceCache.set(cacheIndex, gptLayoutInflater);

            } else {
                // 2.3 以及以下

                JavaCalls.setField(base, "mLayoutInflater", gptLayoutInflater);
            }
        }

    }

    /**
     * 替换SystemServices
     *
     * @param context Activity
     */
    private static void replaceSystemServices(Activity context) {
        // replayce layout inflater
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LayoutInflater gptLayoutInflater = getProxyLayoutInflator(context, layoutInflater);
        // ContextThemeWrapper.mInflater
        JavaCalls.setField(context, "mInflater", gptLayoutInflater);

        ProxyUtil.replaceSystemServices(context);
    }

    /**
     * 替换ExternalDirs
     *
     * @param context ContextWrapper
     */
    private static void replaceExternalDirs(ContextWrapper context) {
        Context baseContext = context.getBaseContext();

        String packageName = context.getPackageName();

        Context hostApplicationContext = ProxyEnvironment.getInstance(packageName).getApplicationProxy();

        // external cache dirs
        File[] externalCacheDirs = getTargetExternalCacheDir(hostApplicationContext, packageName);
        JavaCalls.setField(baseContext, "mExternalCacheDirs", externalCacheDirs);

        File[] externalFilesDir = getTargetExternalFilesDir(hostApplicationContext, packageName, null);
        JavaCalls.setField(baseContext, "mExternalFilesDirs", externalFilesDir);

    }

    /**
     * getTargetExternalCacheDir
     * android 5.0 由于 AppOps 限制，不能创建插件目录。同时我们也需要把插件的external目录接管过来。
     * <p>
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * 注意:该函数用于 GPTActivity 调用，所以不要随便改变函数名字参数等。如需改动，老函数需要保留。
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param hostContext   Context
     * @param pluginPkgName pluginPkgName
     * @return File[]
     */
    public static File[] getTargetExternalCacheDir(Context hostContext, String pluginPkgName) {

        File hostExternalFilesDir = hostContext.getExternalFilesDir(null);

        File targetExternalCacheDir = Util.buildPath(hostExternalFilesDir, pluginPkgName, "cache");
        if (!targetExternalCacheDir.exists()) {
            targetExternalCacheDir.mkdirs();
        }

        if (!targetExternalCacheDir.exists()) {
            return null;
        } else {
            return new File[]{targetExternalCacheDir};
        }
    }

    /**
     * getTargetExternalFilesDir
     * android 5.0 由于 AppOps 限制，不能创建插件目录。同时我们也需要把插件的external目录接管过来。
     * <p>
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * 注意:该函数用于 GPTActivity 调用，所以不要随便改变函数名字参数等。如需改动，老函数需要保留。
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param hostContext   Context
     * @param pluginPkgName pluginPkgName
     * @param type          type
     * @return File[]
     */
    public static File[] getTargetExternalFilesDir(Context hostContext, String pluginPkgName, String type) {

        File hostExternalFilesDir = hostContext.getExternalFilesDir(null);

        File targetExternalFilesDir = Util.buildPath(hostExternalFilesDir, pluginPkgName, "files");

        if (!targetExternalFilesDir.exists()) {
            targetExternalFilesDir.mkdirs();
        }

        if (!targetExternalFilesDir.exists()) {
            return null;
        }

        if (type == null) {
            return new File[]{targetExternalFilesDir};
        }

        File dir = new File(targetExternalFilesDir, type);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
        }
        return new File[]{dir};
    }

}



