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
package com.baidu.android.gporter.api;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.IGPTBinder;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.RemapingUtil;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.ContentResolverProxy;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;

import java.lang.reflect.Constructor;

/**
 * 插件控制器, 加载插件，启动插件，卸载插件等。
 *
 * @author liuhaitao
 * @since 2014-4-24
 */
public final class TargetActivator {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "TargetActivator";

    /**
     * 工具类，不需要构造方法
     */
    private TargetActivator() {

    }

    /**
     * 加载并启动插件。
     *
     * @param context host的Activity
     * @param intent  目标intent，可以为 activity， service， broadcast
     */
    public static void loadTargetAndRun(final Context context, final Intent intent) {
        loadTargetAndRun(context, intent, false);
    }

    /**
     * 加载并启动插件
     *
     * @param context host的Activity
     * @param intent  目标Intent
     * @param creator loading界面创建器
     */
    public static void loadTargetAndRun(final Context context, final Intent intent, ILoadingViewCreator creator) {
        ProxyEnvironment.putLoadingViewCreator(intent.getComponent().getPackageName(), creator);
        loadTargetAndRun(context, intent, false);
    }

    /**
     * 加载并启动插件
     *
     * @param context   host的Activity
     * @param intent    目标Intent
     * @param isSilence 是否是静默加载插件
     */
    public static void loadTargetAndRun(final Context context, final Intent intent, boolean isSilence) {
        Context hostContext = Util.getHostContext(context); // 有可能从插件中调过来的，这时候获取到host context
        ProxyEnvironment.enterProxy(hostContext, intent, isSilence, false);
    }

    /**
     * 加载并启动插件
     *
     * @param context       host的Activity
     * @param componentName 目标Component
     */
    public static void loadTargetAndRun(final Context context, final ComponentName componentName) {
        Intent intent = new Intent();
        intent.setComponent(componentName);
        loadTargetAndRun(context, intent);
    }

    /**
     * 加载并启动插件
     *
     * @param context       host的Activity
     * @param componentName 目标Component
     * @param creator       loading界面创建器
     */
    public static void loadTargetAndRun(final Context context, final ComponentName componentName,
                                        ILoadingViewCreator creator) {
        ProxyEnvironment.putLoadingViewCreator(componentName.getPackageName(), creator);
        loadTargetAndRun(context, componentName);
    }

    /**
     * 加载并启动插件， 启动插件的默认 launcher activity
     *
     * @param context     host的application context
     * @param packageName 插件包名
     */
    public static void loadTargetAndRun(final Context context, String packageName) {
        loadTargetAndRun(context, new ComponentName(packageName, ""));
    }

    /**
     * 加载并启动插件
     *
     * @param context     host的application context
     * @param packageName 插件包名
     * @param creator     插件loading界面的创建器
     */
    public static void loadTargetAndRun(final Context context, String packageName, ILoadingViewCreator creator) {
        ProxyEnvironment.putLoadingViewCreator(packageName, creator);
        loadTargetAndRun(context, new ComponentName(packageName, ""));
    }

    /**
     * 静默加载插件，异步加载
     *
     * @param context     application Context
     * @param packageName 插件包名
     */
    public static void loadTarget(final Context context, String packageName) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, ProxyEnvironment.LOADTARGET_STUB_TARGET_CLASS));
        loadTargetAndRun(context, intent, true);
    }

    /**
     * 静默加载插件，异步加载，可以设置callback
     *
     * @param context     application Context
     * @param packageName 插件包名
     * @param callback    加载成功的回调
     */
    public static void loadTarget(final Context context, final String packageName,
                                  final ITargetLoadedCallBack callback) {

        Context hostContext = Util.getHostContext(context); // 有可能从插件中调过来的，这时候获取到host context

        // 插件已经加载
        if (ProxyEnvironment.isEnterProxy(packageName)) {
            if (callback != null) {
                callback.onTargetLoaded(packageName, true);
            }
            return;
        }

        if (callback == null) {
            loadTarget(hostContext, packageName);
            return;
        }
        BroadcastReceiver recv = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {

                String curPkg = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);

                if (ProxyEnvironment.ACTION_TARGET_LOADED.equals(intent.getAction())
                        && TextUtils.equals(packageName, curPkg)) {
                    boolean isSucc = intent.getBooleanExtra(ProxyEnvironment.EXTRA_TARGET_LOADED_RESULT, false);
                    callback.onTargetLoaded(packageName, isSucc);
                    try {
                        ctx.unregisterReceiver(this);
                    } catch (RuntimeException e) {
                        // 某些2.3手机上会crash，暂时先捕获一下
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        hostContext.getApplicationContext().registerReceiver(recv, filter);

        Intent intent = new Intent();
        intent.setAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        intent.setComponent(new ComponentName(packageName, recv.getClass().getName()));
        ProxyEnvironment.enterProxy(hostContext, intent, true, false);
    }

    /**
     * 同步获取插件的ClassLoader，可能有几百毫秒的开销，注意anr风险
     *
     * @param context     宿主application context
     * @param packageName 插件包名
     * @return 插件ClassLoader
     */
    public static ClassLoader loadAndGetClassLoaderSync(Context context, String packageName) {
        Context hostContext = Util.getHostContext(context);
        if (GPTPackageManager.getInstance(hostContext).getPackageInfo(packageName) != null) {
            if (!ProxyEnvironment.hasInstance(packageName)) {
                ProxyEnvironment.initProxyEnvironment(hostContext, packageName);
            }
            if (!ProxyEnvironment.hasInstance(packageName)) {
                return null;
            }
            ProxyEnvironment targetEnv = ProxyEnvironment.getInstance(packageName);
            if (targetEnv != null) {
                return targetEnv.getDexClassLoader();
            }
        }
        return null;
    }

    /**
     * 同步获取插件application context的方法，慎用，时间开销比较大
     *
     * @param context     host application context
     * @param packageName 插件包名
     * @return 插件application context
     */
    public static Context loadAndGetApplicationSync(Context context, String packageName) {
        Context hostContext = Util.getHostContext(context);
        if (GPTPackageManager.getInstance(hostContext).getPackageInfo(packageName) != null) {
            if (!ProxyEnvironment.isEnterProxy(packageName)) {
                ProxyEnvironment.initProxyEnvironment(hostContext, packageName);
                Intent myIntent = new Intent();
                myIntent.setClassName(packageName, ProxyEnvironment.LOADTARGET_STUB_TARGET_CLASS);
                ProxyEnvironment.launchIntent(context, myIntent, null);
            }
            if (!ProxyEnvironment.hasInstance(packageName)) {
                return null;
            }
            ProxyEnvironment targetEnv = ProxyEnvironment.getInstance(packageName);
            return targetEnv.getApplication();
        }
        return null;
    }

    /**
     * 获取 package 对应的 classLoader。一般情况下不需要获得插件的classloader。
     * 只有那种纯 jar sdk形式的插件，需要获取classloader。 获取过程为异步回调的方式。此函数存在消耗ui线程100ms-200ms级别。
     * <p>
     * 只有插件运行在主进程中，此函数才能工作。
     *
     * @param context     application Context
     * @param packageName 插件包名
     * @param callback    回调，classloader 通过此异步回调返回给hostapp
     */
    public static void loadAndGetClassLoader(final Context context, final String packageName,
                                             final IGetClassLoaderCallback callback) {

        loadTarget(context, packageName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName, boolean isSucc) {
                if (!isSucc) {
                    return;
                }
                // 如果调用进程和插件进程不是一个，会导致load成功之后，instance还没创建，因此crash，先容错一下
                if (!ProxyEnvironment.hasInstance(packageName)) {
                    ProxyEnvironment.initProxyEnvironment(context, packageName);
                }
                if (!ProxyEnvironment.hasInstance(packageName)) {
                    return;
                }
                ProxyEnvironment targetEnv = ProxyEnvironment.getInstance(packageName);
                ClassLoader classLoader = targetEnv.getDexClassLoader();
                callback.getClassLoaderCallback(classLoader);

            }
        });

    }

    /**
     * 同步获取classloader.
     *
     * @param context         app context
     * @param packageName     packageName
     * @param initApplication 是否初始化 Application，并调用其 onCreate函数
     * @return 如果没有安装，或者正在安装， 返回 false。只有安装完成了才返回true.
     */
/*    public static synchronized ClassLoader loadAndGetClassLoader(final Context context, final String packageName,
            boolean initApplication) {
        ClassLoader classLoader = null;

        // 安装了进行初始化
        if (GPTPackageManager.getInstance(context).isPackageInstalled(packageName)) {
            ProxyEnvironment.initProxyEnvironment(context, packageName);
            ProxyEnvironment targetEnv = ProxyEnvironment.getInstance(packageName);

            if (initApplication && targetEnv.getApplication() == null) {
                ComponentName cn = new ComponentName(packageName, ProxyEnvironment.EXTRA_VALUE_LOADTARGET_STUB);
                final Intent intent = new Intent();
                intent.setComponent(cn);

                if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                    // 主线程直接进行初始化
                    ProxyEnvironment.launchIntent(context, intent); // 初始化 application。
                } else {
                    // 非主线程，把初始化任务交给主线程处理。然后阻塞，等待主线程完成
                    final CountDownLatch latch = new CountDownLatch(1);
                    new AsyncTask<String, Integer, String>() {
                        @Override
                        protected String doInBackground(String... params) {
                            String pPackageName = params[0];
                            return pPackageName;
                        };

                        @Override
                        protected void onPostExecute(String result) {
                            // 使用主线程初始化 application。
                            try {
                                ProxyEnvironment.launchIntent(context, intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            latch.countDown(); // 通知await 任务完成
                        }
                    }.execute(packageName);

                    try {
                        latch.await(); // 等主线程完成
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            classLoader = targetEnv.getDexClassLoader();
        }

        return classLoader;
    }
*/

    /**
     * 加载插件并获取插件的Application Context
     * <p>
     * 目前此函数只支持插件和主程序运行在同一进程的情况。
     *
     * @param context     host的 application context
     * @param packageName 插件包名
     * @param callback    获取成功的回调
     */
    public static void loadAndApplicationContext(Context context, String packageName,
                                                 final IGetContextCallBack callback) {

        loadTarget(context, packageName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName, boolean isSucc) {
                if (!isSucc) {
                    return;
                }
                // 如果调用进程和插件进程不是一个，会导致load成功之后，instance还没创建，因此crash，先容错一下
                if (ProxyEnvironment.hasInstance(packageName)) {
                    callback.getTargetApplicationContext(ProxyEnvironment.getInstance(packageName).getApplication());
                }
            }
        });

    }

    /**
     * 加载插件并创建插件内的View，View的Context是插件的Application Context
     * <p>
     * 目前此函数只支持插件和主程序运行在同一进程的情况。
     *
     * @param context     host的 application context
     * @param packageName 插件包名
     * @param viewClass   view的类名
     * @param callback    view创建成功的回调
     */
    public static void loadAndCreateView(Context context, final String packageName, final String viewClass,
                                         final ICreateViewCallBack callback) {

        loadTarget(context, packageName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName, boolean isSucc) {
                if (!isSucc) {
                    return;
                }
                // 如果调用进程和插件进程不是一个，会导致load成功之后，instance还没创建，因此crash，先容错一下
                if (!ProxyEnvironment.hasInstance(packageName)) {
                    return;
                }

                View view = null;
                try {
                    Class<?> targetClass = ProxyEnvironment.getInstance(packageName).getDexClassLoader()
                            .loadClass(viewClass);
                    Constructor<?> constructor = targetClass.getConstructor(new Class<?>[]{Context.class});
                    view = (View) constructor.newInstance(ProxyEnvironment.getInstance(packageName).getApplication());
                } catch (Exception e) {
                    if (DEBUG) {
                        Log.e("TargetActivitor", "*** Create View Fail : \r\n" + e.getMessage());
                    }
                }
                callback.onViewCreated(packageName, view);
            }
        });

    }

    /**
     * 目标插件是否已Loaded
     *
     * @param packageName 插件包名
     * @return true or false
     */
    public static boolean isTargetLoaded(String packageName) {
        return ProxyEnvironment.isEnterProxy(packageName);
    }

    /**
     * 注销插件App，把插件从内存中销毁，默认不强制销毁。
     *
     * @param packageName 插件包名
     * @return 是否成功
     */
    public static boolean unLoadTarget(final String packageName) {
        return unLoadTarget(packageName, false);
    }

    /**
     * 注销插件App, 把插件从内存中销毁。
     *
     * @param packageName 插件包名
     * @param force       如果在加载过程中是否也强制销毁，谨慎使用，插件运行在独立进程时，插件进程会被杀死，
     *                    插件运行在主进程时，如果插件的onTerminal没有处理干净，可能会导致主进程crash
     * @return true表示注销成功
     */
    public static boolean unLoadTarget(final String packageName, boolean force) {

        IGPTBinder[] binders = ProxyEnvironment.sGPTBinders;

        boolean exitRelt = true;
        try {
            for (IGPTBinder binder : binders) {
                if (binder != null && !binder.exitProxy(packageName, force)) {

                    // 任何一个进程退出失败，则认为退出失败
                    exitRelt = false;
                }
            }

        } catch (RemoteException e1) {
            if (DEBUG) {
                e1.printStackTrace();
            }
            exitRelt = false;
        }

        if (!ProxyEnvironment.exitProxy(packageName, force)) {
            exitRelt = false;
        }

        return exitRelt;
    }


    /**
     * 注意:这个方法不能删除。com.baidu.megapp.ma.Util里面有引用。
     *
     * @param packageName   包名
     * @param resourcesName 资源名称
     * @param resourceType  资源类型
     * @return 资源id
     */
    @Deprecated
    public static int getHostResourcesId(String packageName, String resourcesName, String resourceType) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            return ProxyEnvironment.getInstance(packageName).getHostResourcesId(resourcesName, resourceType);
        } else {
            return 0;
        }
    }

    /**
     * 注意:这个方法不能删除。com.baidu.megapp.ma.Util里面有引用
     * 获取主程序的包名
     *
     * @param context 插件的任意context
     * @return 插件包名
     */
    @Deprecated
    public static String getHostPackageName(Context context) {
        ProxyEnvironment proxyEnvironment = ProxyEnvironment.getInstance(context.getPackageName());
        if (proxyEnvironment != null) {
            return proxyEnvironment.getHostPackageName();
        }
        return null;
    }

    /**
     * remap 启动Service的Intent
     *
     * @param packageName 插件包名
     * @param intent      原来的intent
     * @deprecated
     */
    public static void remapStartServiceIntent(String packageName, Intent intent) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            ProxyEnvironment.getInstance(packageName).remapStartServiceIntent(intent);
        }
    }

    /**
     * remap 启动Service的Intent
     *
     * @param context host context
     * @param intent  原来的intent
     */
    public static void remapStartServiceIntent(Context context, Intent intent) {
        RemapingUtil.remapServiceIntent(Util.getHostContext(context), intent);
    }

    /**
     * remap 启动Activity的Intent
     *
     * @param packageName  插件包名
     * @param originIntent 原来的intent
     * @deprecated 建议使用
     * {@link TargetActivator#remapActivityIntent(Context, Intent)}
     */
    public static void remapStartActivityIntent(String packageName, Intent originIntent) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            ProxyEnvironment.getInstance(packageName).remapStartActivityIntent(originIntent);
        }
    }

    /**
     * remap 启动Receiver的Intent
     *
     * @param packageName  插件包名
     * @param originIntent 原来的intent
     * @deprecated
     */
    public static void remapReceiverIntent(String packageName, Intent originIntent) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            ProxyEnvironment.getInstance(packageName).remapReceiverIntent(originIntent);
        }
    }

    /**
     * remap 启动Receiver的Intent
     *
     * @param context      host context
     * @param originIntent 原来的intent
     */
    public static void remapReceiverIntent(Context context, Intent originIntent) {
        RemapingUtil.remapReceiverIntent(Util.getHostContext(context), originIntent);
    }

    /**
     * 这个方法可以remap任意一个插件，不管是否运行
     *
     * @param hostContext  宿主的context
     * @param originIntent 启动插件Intent
     * @param dealMode     是否处理
     */
    public static void remapActivityIntent(Context hostContext, Intent originIntent, boolean dealMode) {
        RemapingUtil.remapActivityIntent(Util.getHostContext(hostContext), originIntent, dealMode);
    }

    /**
     * 这个方法可以remap任意一个插件，不管是否运行
     *
     * @param hostContext  宿主的context
     * @param originIntent 启动插件Intent
     */
    public static void remapActivityIntent(Context hostContext, Intent originIntent) {
        RemapingUtil.remapActivityIntent(Util.getHostContext(hostContext), originIntent);
    }

    /**
     * 获取ContentResolver ，可以直接操作插件的contentprovider。
     *
     * @param hostContext     主程序context
     * @param contentResolver 主程序中通过getContentResolver获取到的对象。
     * @return 封装过的contentresolver，可以直接操作插件中的contentprovider。
     */
    public static ContentResolver getContentResolver(Context hostContext, ContentResolver contentResolver) {
        return new ContentResolverProxy(hostContext, contentResolver);
    }

    /**
     * 从hostapp bind 插件的service 进行通信。<br>
     * 注意:使用这个有个限制，也就是需要在ServiceConnection的onServiceDisconnected函数中，ubind之前的connection。<br>
     * 因为由于技术限制，如果disconnected后不进行unbind，那么service restart后自动调用的onServiceConnected返回的数据不对，
     * 所以每次disconnected后都需要重新bind。
     *
     * @param hostContext host context
     * @param service     插件service的intent，需要指定packagename 和 servicename
     * @param conn        ServiceConnection
     * @param flags       flags
     * @return 是否成功
     */
    public static boolean bindService(final Context hostContext, final Intent service, final ServiceConnection conn,
                                      final int flags) {
        RemapingUtil.remapServiceIntent(hostContext, service);

        GPTComponentInfo info = GPTComponentInfo.parseFromIntent(service);
        if (info == null) {
            // 在插件中没有找到。直接去系统中去处理
            return hostContext.bindService(service, conn, flags);
        }

        String packageName = info.packageName;

        // 如果在插件中存在该 service，那么先load到内存，然后remap后,进行bind
        loadTarget(hostContext, packageName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName, boolean isSucc) {
                if (isSucc) {
                    hostContext.bindService(service, conn, flags);
                }
            }
        });

        return true;
    }

    /**
     * 替换PackageManager
     *
     * @param hostContext Context
     */
    public static void replacePackageManager(final Context hostContext) {
        ProxyEnvironment.replacePackageManager(hostContext);
    }

}
