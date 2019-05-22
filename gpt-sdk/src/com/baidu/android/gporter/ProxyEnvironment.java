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

import android.app.Activity;
import android.app.Application;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Singleton;
import android.view.IWindowSession;
import android.widget.Toast;

import com.baidu.android.gporter.api.ILoadingViewCreator;
import com.baidu.android.gporter.api.TargetActivator;
import com.baidu.android.gporter.gpt.GPTInstrumentation;
import com.baidu.android.gporter.install.ApkInstaller;
import com.baidu.android.gporter.install.IInstallCallBack;
import com.baidu.android.gporter.plug.ApkTargetMapping;
import com.baidu.android.gporter.plug.TargetManager;
import com.baidu.android.gporter.plug.TargetMapping;
import com.baidu.android.gporter.pm.GPTPackageDataModule;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.ActivityManagerNativeWorker;
import com.baidu.android.gporter.proxy.MethodProxy;
import com.baidu.android.gporter.proxy.NotificationManagerNativeWorker;
import com.baidu.android.gporter.proxy.PackageManagerWorker;
import com.baidu.android.gporter.proxy.WindowSessionWorker;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.proxy.activity.RootActivity;
import com.baidu.android.gporter.proxy.activity.ShortcutActivityProxy;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.GPTProxyAmsException;
import com.baidu.android.gporter.stat.GPTProxyNotificationException;
import com.baidu.android.gporter.stat.GPTProxyPmsException;
import com.baidu.android.gporter.stat.GPTProxyWmsException;
import com.baidu.android.gporter.stat.PluginTimeLine;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.ITargetLoadListenner;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dalvik.system.PathClassLoader;

/**
 * ProxyEnvironment
 *
 * @author liuhaitao
 * @since 2017-12-19
 */
public class ProxyEnvironment {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ProxyEnvironment";

    /**
     * GPT Component信息的前缀，因为信息是放到了category里，为了方便区分这个category是存放component的，加前缀
     */
    public static final String TARGET_BEGIN_FLAG = "gpt_info:";
    /**
     * 【GPT Component Json key】启动的目标类名
     */
    public static final String JKEY_TARGET_ACTIVITY = "gpt_extra_target_activity";
    /**
     * 【GPT Component Json key】 启动的目标包名
     */
    public static final String JKEY_TARGET_PACKAGNAME = "gpt_extra_target_pacakgename";
    /**
     * 【GPT Component Json key】 标示Intent是启动时发现插件还没初始化，先去加载插件，重新启动传过来的intent
     */
    public static final String JKEY_TARGET_RESCHEDULE = "gpt_target_reschedule";
    /**
     * 【GPT Component Json key】 桩类名，加载插件后，不需要启动任何组件，则类名传递这个
     */
    public static final String LOADTARGET_STUB_TARGET_CLASS = "gpt_loadtarget_stub";

    /**
     * gpt开关:data是否和host的路径相同，默认独立路径 TODO 待实现
     */
    public static final String META_KEY_DATAINHOST = "gpt_cfg_datainhost";
    /**
     * gpt开关:data是否【去掉】包名前缀，默认加载包名前缀
     */
    public static final String META_KEY_DATA_WITHOUT_PREFIX = "gpt_cfg_data_without_prefix";
    /**
     * gpt开关：class是否注入到host，默认不注入
     */
    public static final String META_KEY_CLASSINJECT = "gpt_class_inject";

    /**
     * 插件加载成功的广播
     */
    public static final String ACTION_TARGET_LOADED = "com.baidu.android.porter.action.TARGET_LOADED";
    /**
     * extra package name
     */
    public static final String EXTRA_TARGET_PACKAGNAME = "extra_pkg_name";
    /**
     * extra 加载结果
     */
    public static final String EXTRA_TARGET_LOADED_RESULT = "extra_load_result";
    /**
     * 插件包名对应Environment的Hash
     */
    private static HashMap<String, ProxyEnvironment> sPluginsMap = new HashMap<String, ProxyEnvironment>();
    /**
     * 插件的启动时间统计
     */
    public static HashMap<String, PluginTimeLine> pluginTimeLineMap = new HashMap<String, PluginTimeLine>();
    /**
     * 插件热启动时间点
     */
    public static HashMap<String, Long> pluginHotStartTimeMap = new HashMap<String, Long>();

    /**
     * 宿主的application context
     */
    private final Context mHostAppContext;
    /**
     * apkFile
     */
    private final File apkFile;
    /**
     * PackageName
     */
    private final String mPackageName;

    /**
     * 插件的class loader
     */
    private ClassLoader dexClassLoader;
    /**
     * targetResources
     */
    private Resources targetResources;
    /**
     * targetAssetManager
     */
    private AssetManager targetAssetManager;
    /**
     * targetTheme
     */
    private Theme targetTheme;
    /**
     * targetMapping
     */
    private TargetMapping targetMapping;
    /**
     * parentPackagename
     */
    private String parentPackagename;
    /**
     * 所有插件的Activity栈
     */
    private static LinkedList<Activity> sActivityStack = new LinkedList<Activity>();
    /**
     * 插件虚拟的Application实例
     */
    private Application application;
    /**
     * 插件数据根目录
     */
    private File targetDataRoot;
    /**
     * 是否初始化了插件Application
     */
    private boolean bIsApplicationInit = false;
    /**
     * 是否运行在主进程
     */
    private boolean bIsUnionProcess = false;
    /**
     * 对应着 ActivityThread 中的
     * final HashMap<String, WeakReference<LoadedApk>> mPackages
     * = new HashMap<String, WeakReference<LoadedApk>>();
     * 因为我们需要主动设置 LoadedApk 中的classloader 等。一旦这个弱引用被释放，再次系统创建的时候，我们无法自己设置
     * classloader等属性。导致错误。
     * <p>
     * 所以我们这里主动强引用一下。
     * <p>
     * 等以后随着ProxyEnvironment 缓存弱引用化（解决内存问题），会解决同样问题。
     */
    private Object mLoadedApk;

    /**
     * Loading Map，正在loading中的插件
     */
    private static Map<String, List<Intent>> gLoadingMap = new HashMap<String, List<Intent>>();
    /**
     * 插件loading样式的创建器
     */
    private static Map<String, ILoadingViewCreator> gLoadingViewCreators = new HashMap<String, ILoadingViewCreator>();

    /**
     * 插件的代理pm
     */
    public static PackageManagerWorker sProxyPm;
    /**
     * mNotificationManagerProxy
     */
    private static Object mNotificationManagerProxy;
    /**
     * mNotificationManagerNativeWorker
     */
    public static NotificationManagerNativeWorker mNotificationManagerNativeWorker;
    /**
     * mActivityManagerNativeProxy
     */
    private static Object mActivityManagerNativeProxy;
    /**
     * mActivityManagerNativeWorker
     */
    public static ActivityManagerNativeWorker mActivityManagerNativeWorker;
    /**
     * sWindowSessionProxy
     */
    private static Object sWindowSessionProxy;
    /**
     * 插件代理的WindowSession
     */
    private static WindowSessionWorker sProxyWs;

    /**
     * 已经注册的从静态广播转化过来的动态广播
     */
    public static List<BroadcastReceiver> mRegisteredRecvs = null;


    /**
     * 构造方法，解析apk文件，创建插件运行环境
     *
     * @param context     host application context
     * @param packageName 插件包名
     */
    private ProxyEnvironment(Context context, String packageName) {
        this.mHostAppContext = context.getApplicationContext();
        this.apkFile = ApkInstaller.getInstalledApkFile(context, packageName);
        GPTPackageInfo info = GPTPackageManager.getInstance(context).getPackageInfo(packageName);
        if (info != null && info.isUnionProcess) {
            bIsUnionProcess = true;
        }
        mPackageName = packageName;
        parentPackagename = context.getPackageName();
        createTargetMapping();
        createDataRoot();
        createClassLoader();
        createTargetResource();
        addPermissions();
    }

    /**
     * 获取插件数据根路径
     *
     * @return 根路径文件
     */
    public File getTargetDataRoot() {
        return targetDataRoot;
    }

    /**
     * 获取插件apk路径
     *
     * @return 绝对路径
     */
    public String getTargetPath() {
        return this.apkFile.getAbsolutePath();
    }

    /**
     * 获取插件lib的绝对路径
     *
     * @return 绝对路径
     */
    public String getTargetLibPath() {
        return getTargetLibPath(mHostAppContext, mPackageName);
    }

    /**
     * 返回 packagename 对应的 lib 目录
     * 老版本 lib 目录就是  lib
     * 新版本 格式为  gptlib + timestamp 例如   gptlib1435904813558， 用于热插拔, so 只能被一个classloader加载。
     *
     * @param context
     * @param packageName
     * @return
     */
    public static String getTargetLibPath(Context context, String packageName) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();

                if (name.equals(ApkInstaller.NATIVE_LIB_PATH)) {
                    return true;
                } else if (name.startsWith(ApkInstaller.NATIVE_LIB_PATH_GPT)) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        // lib始终放在子目录下，不能跟宿主同目录
        File dataDir = new File(ApkInstaller.getGreedyPorterRootPath(context), packageName);

        File[] libs = dataDir.listFiles(filter);
        if (libs != null && libs.length > 0) {
            return libs[0].getAbsolutePath();
        } else {
            return null;
        }
    }

    /**
     * 获取插件运行环境实例，调用前保证已经初始化，否则会抛出异常
     *
     * @param packageName 插件包名
     * @return 插件环境对象
     */
    public static ProxyEnvironment getInstance(String packageName) {
        ProxyEnvironment env = null;
        if (packageName != null) {
            env = sPluginsMap.get(packageName);
        }
        if (env == null) {
            throw new IllegalArgumentException(packageName + " not loaded, Make sure you have call the init method!");
        }
        return env;
    }

    /**
     * 获取插件包名和对应Environment的Map
     *
     * @return 插件包名和对应Environment的Map
     */
    public static Map<String, ProxyEnvironment> getPluginsMap() {

        return sPluginsMap;
    }

    /**
     * 是否已经建立对应插件的environment
     *
     * @param packageName 包名，已经做非空判断
     * @return true表示已经建立
     */
    public static boolean hasInstance(String packageName) {
        if (packageName == null) {
            return false;
        }
        return sPluginsMap.containsKey(packageName);
    }

    /**
     * 是否已经建立对应插件的environment
     *
     * @param packageName 包名，已经做非空判断
     * @return true表示已经建立
     */
    public static boolean hasInstanceInGPTProcess(String packageName) {
        if (packageName == null) {
            return false;
        }

        try {
            // GPT等进程查找
            for (IGPTBinder gptBinder : sGPTBinders) {
                if (gptBinder == null) {
                    continue;
                }

                if (gptBinder.hasInstance(packageName)) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            return false;
        } catch (NullPointerException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    /**
     * 插件是否已经进入了代理模式
     *
     * @param packageName 插件包名
     * @return true or false
     */
    public static boolean isEnterProxy(String packageName) {
        if (packageName == null) {
            return false;
        }
        // 注意加锁和查找细节
        synchronized (gLoadingMap) {
            ProxyEnvironment env = sPluginsMap.get(packageName);
            if (env != null && env.bIsApplicationInit) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清除等待队列，防止异常情况，导致所有Intent都阻塞在等待队列，插件再也起不来。
     *
     * @param packageName 包名
     */
    private static void clearLoadingIntent(String packageName) {
        if (packageName == null) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "clearLoadingIntent(String packageName): packageName = " + packageName);
        }

        synchronized (gLoadingMap) {
            gLoadingMap.remove(packageName);
        }
    }

    /**
     * 移除等待队列，防止异常情况，导致所有Intent都阻塞在等待队列，插件再也起不来。
     *
     * @param packageName 包名
     * @return List<Intent> or null
     */
    private static List<Intent> removeLoadingIntents(String packageName) {
        if (packageName == null) {
            return null;
        }

        if (DEBUG) {
            Log.d(TAG, "removeLoadingIntents(String packageName): packageName = " + packageName);
        }

        synchronized (gLoadingMap) {
            return gLoadingMap.remove(packageName);
        }
    }

    /**
     * 设置插件加载的loadingView creator
     *
     * @param packageName 插件包名
     * @param creator     loadingview creator
     */
    public static void putLoadingViewCreator(String packageName, ILoadingViewCreator creator) {
        if (packageName == null) {
            return;
        }
        gLoadingViewCreators.put(packageName, creator);
    }

    /**
     * 获取插件加载的loadingview creator
     *
     * @param packageName 插件包名
     * @return creator
     */
    public static ILoadingViewCreator getLoadingViewCreator(String packageName) {
        if (packageName == null) {
            return null;
        }
        return gLoadingViewCreators.get(packageName);
    }

    /**
     * 插件是否正在loading中
     *
     * @param packageName 插件包名
     * @return true or false
     */
    public static boolean isLoading(String packageName) {
        if (packageName == null) {
            return false;
        }

        boolean ret = false;
        synchronized (gLoadingMap) {
            ret = gLoadingMap.containsKey(packageName);
        }
        return ret;
    }

    /**
     * 判断是否加载中或者已经加载了，调用要加gLoadingMap作为锁
     *
     * @param packageName 包名
     * @return true or false
     */
    private static boolean isLoadingOrLoaded(String packageName) {
        if (packageName == null) {
            return false;
        }

        boolean ret = false;

        // 加载中
        if (gLoadingMap.containsKey(packageName)) {
            ret = true;
        }

        // 已经加载
        if (sPluginsMap.containsKey(packageName) && sPluginsMap.get(packageName).bIsApplicationInit) {
            ret = true;
        }
        return ret;
    }

    /**
     * 运行插件代理
     *
     * @param context host 的application context
     * @param intent  加载插件运行的intent
     */
    public static void enterProxy(final Context context, final Intent intent) {
        enterProxy(context, intent, false, false);
    }

    /**
     * 运行插件代理，注意:此函数必须从主进程调用。
     *
     * @param context   host 的application context
     * @param intent    加载插件运行的intent
     * @param isSilent  是否静默启动插件
     * @param forReboot true表示进程杀死，重启时的调用
     */
    public static void enterProxy(final Context context, final Intent intent, boolean isSilent, boolean forReboot) {
        enterProxy(context, intent, isSilent, forReboot, false, Constants.GPT_PROCESS_DEFAULT);
    }

    /**
     * 运行插件代理，注意:此函数必须从主进程调用。
     *
     * @param context         host 的application context
     * @param intent          加载插件运行的intent
     * @param isSilent        是否静默启动插件
     * @param forReboot       true表示进程杀死，重启时的调用
     * @param fromMainProcess 确定是从主进程调用过来的，可以不判断当前是否主进程，因为
     *                        {@link Util#isHostProcess(Context)}
     *                        有一定的误判率，默认非主进程，防止调用成环，只要从MainProcessService调用过来的，就认为是主进程
     * @param toExtProcess    要在哪个进程启动插件组件，只在reboot的情况会传进来，主要防止在主进程通过api调用的情况下，也解析packageinfo才知道启动哪个进程
     *                        TODO 多进程时需要关注相关逻辑。
     */
    public static void enterProxy(final Context context, final Intent intent, boolean isSilent, boolean forReboot,
                                  boolean fromMainProcess, int toExtProcess) {
        String packageName = null;
        if (intent.getComponent() != null) {
            packageName = intent.getComponent().getPackageName();
        }
        // 统计添加
        ReportManger.getInstance().onPluginLoadStart(context, packageName, intent);
        if (intent.getComponent() != null) {
            PluginTimeLine pluginTimeLine = new PluginTimeLine();
            pluginTimeLine.packageName = packageName;
            pluginTimeLine.startTimeStamp = System.currentTimeMillis();
            pluginTimeLine.startElapsedRealTime = SystemClock.elapsedRealtime();
            pluginTimeLineMap.put(packageName, pluginTimeLine);
        }
        // 判断是否主进程调用过来的
        boolean isHostProcess = false;
        if (fromMainProcess) {
            isHostProcess = true;
        } else {
            isHostProcess = Util.isHostProcess(context);
        }

        if (DEBUG) {
            Log.d(TAG, "--- enterProxy, intent = " + intent.toString() + ", toExtProcess" + toExtProcess + ", isHost = "
                    + isHostProcess + ",isSilent = " + isSilent);
        }

        if (!isHostProcess) {
            // 如果是从插件独立进程启动的，此时需要调用主进程进行加载插件。
            Intent tempIntent = new Intent(MainProcessService.ACTION_ENTER_PROXY);

            int pToExtProcess = toExtProcess;
            if (toExtProcess == Constants.GPT_PROCESS_DEFAULT) {
                Intent testIntent = new Intent(intent);

                // 判断一下插件组件需要在哪个进程启动
                RemapingUtil.remapActivityIntent(context, testIntent, false);
                RemapingUtil.remapReceiverIntent(context, testIntent);
                RemapingUtil.remapServiceIntent(context, testIntent);
            }

            if (DEBUG) {
                Log.d(TAG, "--- Go to Main process start intent = " + intent + ", toExtProcess" + pToExtProcess);
            }

            tempIntent.setClass(context, MainProcessService.class);
            tempIntent.putExtra(MainProcessService.EXTRA_INTENT, intent);
            tempIntent.putExtra(MainProcessService.EXTRA_IS_SILENT, true);
            tempIntent.putExtra(MainProcessService.EXTRA_FOR_REBOOT, false);
            tempIntent.putExtra(MainProcessService.EXTRA_EXT_PROCESS, pToExtProcess);

            context.startService(tempIntent);
            ReportManger.getInstance().startMainProcessService(context, intent.getComponent() == null ?
                    "" : intent.getComponent().getPackageName(), pToExtProcess);
            return;
        }

        if (TextUtils.isEmpty(packageName)) {
            throw new RuntimeException("*** loadTarget with null packagename!");
        }

        boolean isEnterProxy = false;
        synchronized (gLoadingMap) {
            List<Intent> cacheIntents = gLoadingMap.get(packageName);
            if (cacheIntents != null) {

                if (DEBUG) {
                    Log.d(TAG, "--- Add to loading map, intent = " + intent);
                }

                // 正在loading，直接返回，等着loading完调起
                // 把intent都缓存起来
                if (forReboot) {
                    cacheIntents.add(0, intent);
                } else {
                    cacheIntents.add(intent);
                }
                ReportManger.getInstance().cacheIntent(context, packageName);
                return;
            }

            // isEnterProxy的判断关系
            isEnterProxy = isEnterProxy(packageName) || hasInstanceInGPTProcess(packageName);
            if (!isEnterProxy) {

                if (DEBUG) {
                    Log.d(TAG, "--- Create loading map, intent = " + intent);
                }

                List<Intent> intents = new ArrayList<Intent>();
                intents.add(intent);
                gLoadingMap.put(packageName, intents);
            } else {
                // 热启动，把冷启动的记录去掉,记录热启动
                PluginTimeLine timeLine = pluginTimeLineMap.remove(packageName);
                if (timeLine != null) {
                    pluginHotStartTimeMap.put(packageName, timeLine.startElapsedRealTime);
                }
            }
        }

        if (isEnterProxy(packageName) && !(isHostProcess && toExtProcess != Constants.GPT_PROCESS_DEFAULT)) {

            // 独立进程的应用，不是静默的话，继续往下走，每次都出loading，因为不确定另一个进程是否已经enter proxy了
            GPTPackageInfo info = GPTPackageManager.getInstance(context).getPackageInfo(packageName);
            if (info == null || info.isUnionProcess) {

                // 已经初始化，直接起Intent
                launchIntent(context, intent, null);
                ReportManger.getInstance().startIntentOnLoaded(context, packageName);
                return;
            }
        }

        if (isSilent || hasInstanceInGPTProcess(packageName)) {

            initTargetAndLaunchIntent(context.getApplicationContext(), intent, null, toExtProcess);
            ReportManger.getInstance().silentOrHasInstanceInGPTProcess(context, packageName, isSilent);

        } else {

            if (DEBUG) {
                Log.d(TAG, "--- Show RootActivity, intent = " + intent);
            }

            // 显示loading界面
            Intent newIntent = new Intent(intent);
            newIntent.setClass(context, RootActivity.class);
            if (!(context instanceof Activity)) {
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            GPTComponentInfo info = new GPTComponentInfo();
            info.packageName = packageName;
            info.className = intent.getComponent().getClassName();
            info.reschedule = forReboot;
            info.addSelf2Intent(newIntent);
            context.startActivity(newIntent);
            ReportManger.getInstance().startRootActivity(context, packageName);
        }
    }

    /**
     * 进入代理模式，要保证在主线程调用
     *
     * @param context               host 的 application context
     * @param currentActivityThread 一般调用传递 null 即可。
     *                              但在android 4.1 contentProvider 的query 函数中获取不到的情况，需要在onCreate中获取，然后传递过来。
     * @return true表示启动了activity，false表示启动失败，或者启动非activity
     * @throws Exception 调用逻辑异常
     */
    public synchronized static boolean launchIntent(final Context context, Intent intent, Object currentActivityThread) {

        if (DEBUG) {
            Log.d(TAG, "--- launchIntent, intent = " + intent);
        }

        String packageName = intent.getComponent().getPackageName();
        ProxyEnvironment env = sPluginsMap.get(packageName);
        if (env == null) {
            clearLoadingIntent(packageName);
            if (DEBUG) {
                Log.w(TAG, "### launchIntent while env removed!");
            }
            sendLoadResult(context, intent, packageName, false);
            return false;
        }

        getSettingProvider(context);

        // Hook代理替换
        replacePackageManager(context);
        replaceWindowSession(context);
        replaceActivityManagerNative(context);
        replaceNotificationManagerNative(context);
        List<Intent> cacheIntents = null;
        if (!env.bIsApplicationInit && env.application == null) {

            // For 统计
            long start = SystemClock.elapsedRealtime();
            try {

                // 获取ActivityThread
                Object curActivityThread = currentActivityThread;

                if (curActivityThread == null) {
                    curActivityThread = JavaCalls.callStaticMethodOrThrow("android.app.ActivityThread",
                            "currentActivityThread", new Object[]{});
                }

                // 创建LoadedApk或者PackageInfo
                Object loadedApk = null;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // 2.3及以下少一个参数
                    loadedApk = JavaCalls.invokeMethodThrowException(curActivityThread, "getPackageInfo",
                            new Class<?>[]{ApplicationInfo.class, int.class},
                            new Object[]{env.targetMapping.getPackageInfo().applicationInfo,
                                    Context.CONTEXT_INCLUDE_CODE});
                } else {
                    Class<?> compatibilityInfoClazz = env.getDexClassLoader().loadClass(
                            "android.content.res.CompatibilityInfo");
                    loadedApk = JavaCalls.invokeMethodThrowException(curActivityThread, "getPackageInfo",
                            new Class<?>[]{ApplicationInfo.class, compatibilityInfoClazz, int.class}, new Object[]{
                                    env.targetMapping.getPackageInfo().applicationInfo, null,
                                    Context.CONTEXT_INCLUDE_CODE});
                }

                // 给LoadedApk或者PackageInfo赋值自定义的class loader
                JavaCalls.setFieldOrThrow(loadedApk, "mClassLoader", env.getDexClassLoader());

                // 把资源替换成Proxy
                JavaCalls.setFieldOrThrow(loadedApk, "mResources", env.getTargetResources());

                // 这里强引用。防止系统的弱引用回收掉。
                env.mLoadedApk = loadedApk;

                // 创建Application
                GPTInstrumentation gptInstrumentation = new GPTInstrumentation();
                // 设置自己的 instrumentation
                JavaCalls.setFieldOrThrow(curActivityThread, "mInstrumentation", gptInstrumentation);


                // delete by caohaitao 20150928 由于重写了 packageManager，所以这个分支不再需要。
//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) { // 兼容5.0。5.0以上makeApplication方法会调用initializeJavaContextClassLoader方法，里面会去取PackageInfo，取不到会报异常
//                    Object contextImpl = JavaCalls.callStaticMethodOrThrow("android.app.ContextImpl",
//                            "createAppContext", curActivityThread, loadedApk);
//                    env.application = (Application) JavaCalls.invokeMethod(mInstrumentation, "newApplication",
//                            new Class<?>[] { ClassLoader.class, String.class, Context.class }, new Object[] {
//                                    env.dexClassLoader, env.targetMapping.getApplicationClassName(), contextImpl });
//                    JavaCalls.callMethodOrThrow(contextImpl, "setOuterContext", env.application);
//                    JavaCalls.setField(loadedApk, "mApplication", env.application);
//                    env.application.onCreate();
//
//                    // 上边过程不会触发 Instrumentation.callApplicationOnCreate。自己主动触发
//                    mInstrumentation.callApplicationOnCreate(env.application);
//
//                } else {
                env.application = (Application) JavaCalls.invokeMethodThrowException(
                        loadedApk,
                        "makeApplication",
                        new Class<?>[]{boolean.class,
                                env.getDexClassLoader().loadClass("android.app.Instrumentation")}, new Object[]{
                                false, gptInstrumentation});
//                }
                // For 统计
                long time = SystemClock.elapsedRealtime() - start;
                PluginTimeLine timeLine = pluginTimeLineMap.get(packageName);
                if (timeLine != null) {
                    timeLine.loadApplicationTime = time;
                }
                ReportManger.getInstance().onLoadApplication(context, packageName, time);

                // 提前验证classLoader,有部分情况下unable to open DEX
                // file。出现ClassNotFoundException的异常
                perVerifyClassLoader(context, env, intent);

            } catch (Exception e) {
                ReportManger.getInstance().onException(context, packageName, Util.getCallStack(e),
                        ExceptionConstants.TJ_78730009);
                ReportManger.getInstance().onPluginLoadFail(context, packageName, intent);
                synchronized (gLoadingMap) {
                    gLoadingMap.remove(packageName);
                }
                sendLoadResult(context, intent, packageName, false);
                // 启动失败，退出环境，便于下次重新初始化
                ProxyEnvironment.exitProxy(packageName, true);
                return false;
            }

            synchronized (gLoadingMap) {
                env.bIsApplicationInit = true;
                cacheIntents = gLoadingMap.remove(packageName);
                if (sPluginsMap.get(packageName) != env) {
                    return false;
                }
                // 注册广播
                env.registerStaticBroadcasts();
            }

            // 插件申请了largeHeap
            if ((env.getTargetMapping().getPackageInfo().applicationInfo.flags & ApplicationInfo.FLAG_LARGE_HEAP) > 0) {

                // TODO 从MTJ统计来看，这个地方可能有兼容性问题导致异常，暂时先容错一下，以后看看怎么兼容处理一下
                Object obj = JavaCalls.callStaticMethod("dalvik.system.VMRuntime", "getRuntime", new Object[]{});
                if (obj != null) {
                    JavaCalls.callMethod(obj, "clearGrowthLimit", new Object[]{});
                }
            }
        }

        if (cacheIntents == null) {

            // 没有缓存的Intent，取当前的Intent;
            cacheIntents = new ArrayList<Intent>();
            cacheIntents.add(intent);
        }

        boolean haveLaunchActivity = false;
        for (Intent curIntent : cacheIntents) {

            // 获取目标class
            String targetClassName = curIntent.getComponent().getClassName();
            if (TextUtils.equals(targetClassName, LOADTARGET_STUB_TARGET_CLASS)) {
                // For 统计，stub也算上，因为接口调用的时候都无差别的记录了开始，不算的话比例会不对
                onPluginLoadSucess(context, packageName, curIntent);

                // 表示后台加载，不需要处理该Intent
                continue;
            }

            if (DEBUG) {
                Log.d(TAG, "--- Launch target : " + targetClassName);
            }
            if (TextUtils.isEmpty(targetClassName)) {
                targetClassName = env.getTargetMapping().getDefaultActivityName();
                // 表示是启动activity
                curIntent.setAction(Intent.ACTION_MAIN);
            }


            // 处理启动的是service
            Class<?> targetClass;
            try {
                targetClass = env.dexClassLoader.loadClass(targetClassName);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                targetClass = Activity.class;
            }
            if (Service.class.isAssignableFrom(targetClass)) {
                env.remapStartServiceIntent(curIntent, targetClassName);
                context.startService(curIntent);

            } else if (BroadcastReceiver.class.isAssignableFrom(targetClass)) { //

                // try 解决：Bug 13039 【MTJ】7-5java.lang.NullPointerException: null result when primitive expected
                try {
                    if (curIntent.getAction().equals(ProxyEnvironment.ACTION_TARGET_LOADED)) {
                        // 做特殊处理。发一个内部用的动态广播
                        sendLoadResult(context, curIntent, packageName, true);
                    } else {
                        env.remapReceiverIntent(curIntent);
                        context.sendBroadcast(curIntent);
                    }
                } catch (NullPointerException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                    // For 统计
                    ReportManger.getInstance().onException(context, packageName, Util.getCallStack(e),
                            ExceptionConstants.TJ_78730002);
                }

            } else {
                Intent newIntent = new Intent(curIntent);
                newIntent.setClass(context, ActivityProxy.class);
                if (!(context instanceof Activity)) {
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                env.remapStartActivityIntent(newIntent, targetClassName);
                context.startActivity(newIntent);

                haveLaunchActivity = true;
            }
            // For 统计，没出异常就算插件启动成功
            onPluginLoadSucess(context, packageName, curIntent);
        }

        return haveLaunchActivity;
    }

    /**
     * 提前验证classLoader,有部分情况下，unable to open DEX file。
     * 出现ClassNotFoundException的异常。
     *
     * @param context Context
     * @param env     ProxyEnvironment
     * @param intent  Intent
     * @return 是否验证通过
     * @throws Exception 验证不通过会抛这个异常
     */
    private static boolean perVerifyClassLoader(Context context, ProxyEnvironment env, Intent intent)
            throws Exception {
        String className = intent.getComponent().getClassName();
        if (TextUtils.isEmpty(className) || TextUtils.equals(className, LOADTARGET_STUB_TARGET_CLASS)) {
            className = env.getTargetMapping().getDefaultActivityName();
        }
        if (DEBUG) {
            Log.d(TAG, "perVerifyClassLoader(): className=" + className);
        }
        if (!TextUtils.isEmpty(className)) {
            try {
                Class<?> clazz = Class.forName(className, true, env.getDexClassLoader());
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                throw (e);
            }
        }
        return true;
    }

    /**
     * 发送加载的结果
     *
     * @param context     Context
     * @param intent      Intent
     * @param packageName 包名
     * @param isSuccess   是否加载成功
     */
    private static void sendLoadResult(Context context, Intent intent, String packageName, boolean isSuccess) {
        if (context == null) {
            return;
        }
        if (intent == null) {
            return;
        }
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        if (!TextUtils.isEmpty(intent.getAction())
                && intent.getAction().equals(ProxyEnvironment.ACTION_TARGET_LOADED)) {
            // 做特殊处理。发一个内部用的动态广播
            Intent newIntent = new Intent(ProxyEnvironment.ACTION_TARGET_LOADED);
            newIntent.putExtra(EXTRA_TARGET_PACKAGNAME, packageName);
            newIntent.putExtra(EXTRA_TARGET_LOADED_RESULT, isSuccess);
            newIntent.setPackage(context.getPackageName());
            context.sendBroadcast(newIntent);
        }
        return;
    }

    /**
     * 注册动态广播代替静态广播
     */
    private void registerStaticBroadcasts() {
        Map<String, ArrayList<ActivityIntentInfo>> map = targetMapping.getRecvIntentFilters();
        if (map != null) {
            mRegisteredRecvs = new ArrayList<BroadcastReceiver>();
            for (String className : map.keySet()) {
                ActivityInfo recvInfo = targetMapping.getReceiverInfo(className);
                GPTPackageInfo gptPkgInfo = GPTPackageManager.getInstance(mHostAppContext).getPackageInfo(mPackageName);

                // 获取Receiver信息，获取不到则忽略
                if (recvInfo == null || gptPkgInfo == null) {
                    continue;
                }

                // 防止Receiver重复注册
                String processName = Util.getCurrentProcessName(mHostAppContext);
                if (!TextUtils.isEmpty(processName)) {

                    // 判断Receiver注册的进程,TODO 多进程处理需要关注。
                    String targetProcessSuffix = "";
                    switch (gptPkgInfo.extProcess) {
                        case Constants.GPT_PROCESS_DEFAULT:
                        default:
                            if (!gptPkgInfo.isUnionProcess) {
                                targetProcessSuffix = Constants.PROCESS_GPT_SUFFIX;
                            }
                            break;
                    }
                    // 进程不匹配，不注册
                    if (!TextUtils.equals(targetProcessSuffix, Util.getProcessNameSuffix(processName))) {
                        continue;
                    }

                } // else : 获取进程名的方法有兼容性问题，这时候不管了。重复注册也没办法了。

                ArrayList<ActivityIntentInfo> filters = map.get(className);

                // 容错
                if (filters == null || filters.size() == 0) {
                    continue;
                }

                try {
                    BroadcastReceiver recv = (BroadcastReceiver) dexClassLoader.loadClass(className).newInstance();
                    for (ActivityIntentInfo filter : filters) {
                        application.registerReceiver(recv, filter);
                    }
                    mRegisteredRecvs.add(recv);
                } catch (InstantiationException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                } catch (ClassCastException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 发注册静态广播
     */
    private void unregisterStaticBroadcasts() {
        if (mRegisteredRecvs == null || mRegisteredRecvs.size() == 0) {
            return;
        }

        for (BroadcastReceiver recv : mRegisteredRecvs) {
            try {
                application.unregisterReceiver(recv);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 替换系统的IWindowSession接口，主要是拦截WindowManager，不能替换WindowManagerImpl，
     * 系统有代码会进行强制转换，会导致crash
     *
     * @param hostContext 宿主context
     * @throws Exception 主体逻辑出错抛异常
     */
    public static void replaceWindowSession(final Context hostContext) {
        if (DEBUG) {
            Log.d(TAG, "--- replaceWindowSession");
        }
        if (sWindowSessionProxy == null) {
            if (DEBUG) {
                Log.i(TAG, "--- replaceWindowSession indeed!");
            }
            sProxyWs = new WindowSessionWorker();
            sProxyWs.setPackageName(hostContext.getPackageName());
            sProxyWs.setContext(hostContext);

            IWindowSession target = JavaCalls.callStaticMethod("android.view.WindowManagerGlobal", "getWindowSession",
                    new Object[]{});

            // 某些手机ROM，比如小辣椒手机，改成了peek这个方法，兼容试试
            if (target == null) {
                target = JavaCalls.callStaticMethod("android.view.WindowManagerGlobal", "peekWindowSession",
                        new Object[]{});
            }

            // 如果还是取不到，那就放弃。
            if (target == null) {
                if (DEBUG) {
                    Log.w(TAG, "--- replaceWindowSession failed!");
                }
                return;
            }
            // sWindowSessionProxy = sProxyWs.generateProxy(target);

            sProxyWs.mTarget = target;
            try {
                Class<?> iWindowSessionClazz = Class.forName(Constants.WINDOW_SESSION_CLASS);

                sWindowSessionProxy = Proxy.newProxyInstance(iWindowSessionClazz.getClassLoader(),
                        new Class<?>[]{iWindowSessionClazz}, new InvocationHandler() {

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                Object result = null;
                                try {
                                    MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(
                                            sProxyWs, Constants.WINDOW_SESSION_CLASS,
                                            method);
                                    if (methodInfo != null) {
                                        result = methodInfo.process(args);
                                    } else {
                                        result = method.invoke(sProxyWs.mTarget, args);
                                    }
                                } catch (Exception e) {
                                    if (DEBUG) {
                                        e.printStackTrace();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    String message = Util.printlnMethod("### Wm invoke : ", method, args);
                                    sb.append(Util.getCallStack(e));
                                    ReportManger.getInstance().onExceptionByLogService(hostContext, "", sb.toString(),
                                            ExceptionConstants.TJ_78730007);

                                    if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                        throw e;
                                    } else {
                                        throw new GPTProxyWmsException(message, e);
                                    }

                                }
                                return result;
                            }
                        });
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
            JavaCalls.setStaticField("android.view.WindowManagerGlobal", "sWindowSession", sWindowSessionProxy);
        }

    }

    /**
     * 替换系统NotificationManager相关接口
     *
     * @param hostContext 宿主context
     * @throws Exception 主体逻辑出错抛异常
     */
    public static void replaceNotificationManagerNative(final Context hostContext) {
        if (mNotificationManagerProxy == null) {
            mNotificationManagerNativeWorker = new NotificationManagerNativeWorker(hostContext);

            try {
                Class<?> iNotificationManagerClazz = Class.forName(Constants.NOTIFICATION_MANAGER_NATIVE_CLASS);

                mNotificationManagerProxy = Proxy.newProxyInstance(iNotificationManagerClazz.getClassLoader(),
                        new Class<?>[]{iNotificationManagerClazz}, new InvocationHandler() {

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                Object result = null;
                                try {
                                    MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(
                                            mNotificationManagerNativeWorker, Constants.NOTIFICATION_MANAGER_NATIVE_CLASS,
                                            method);
                                    if (methodInfo != null) {
                                        result = methodInfo.process(args);
                                    } else {
                                        result = method.invoke(mNotificationManagerNativeWorker.mTarget, args);
                                    }
                                } catch (Exception e) {
                                    if (DEBUG) {
                                        e.printStackTrace();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    String message = Util.printlnMethod("### nm invoke : ", method, args);
                                    sb.append(Util.getCallStack(e));
                                    ReportManger.getInstance().onExceptionByLogService(hostContext, "",
                                            sb.toString(), ExceptionConstants.TJ_78730016);

                                    if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                        throw e;
                                    } else {
                                        throw new GPTProxyNotificationException(message, e);
                                    }

                                }
                                return result;
                            }
                        });
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

            try {
                Field field = NotificationManager.class.getDeclaredField("sService");
                field.setAccessible(true);
                INotificationManager realService = JavaCalls.callStaticMethod(NotificationManager.class.getName(),
                        "getService");
                mNotificationManagerNativeWorker.mTarget = realService;
                field.set(NotificationManager.class, mNotificationManagerProxy);

                // 替换toast
                JavaCalls.setStaticField(Toast.class, "sService", mNotificationManagerProxy);

            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 替换系统NotificationManager相关接口
     *
     * @param context 宿主context
     * @throws Exception 主体逻辑出错抛异常
     */
    public static void replaceActivityManagerNative(final Context context) {
        if (DEBUG) {
            Log.d(TAG, "--- replaceActivityManagerNative");
        }

        if (mActivityManagerNativeProxy == null) {

            if (DEBUG) {
                Log.d(TAG, "--- replaceActivityManagerNative indeed!");
            }
            mActivityManagerNativeWorker = new ActivityManagerNativeWorker();
            mActivityManagerNativeWorker.setHostContext(context);
            try {
//                Class<?> iActivityManagerClazz = Class.forName(Constants.ACTIVE_MANAGER_NATIVE_CLASS);
                Class<?> iActivityManagerInterface = Class.forName(Constants.IACTIVE_MANAGER_CLASS);
                mActivityManagerNativeProxy = Proxy.newProxyInstance(iActivityManagerInterface.getClassLoader(),
                        new Class[]{iActivityManagerInterface}, new InvocationHandler() {

                            @Override
                            public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
                                if (method == null) {
                                    return null;
                                }
                                Object result = null;
                                try {
                                    MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(
                                            mActivityManagerNativeWorker, Constants.ACTIVE_MANAGER_NATIVE_CLASS, method);
                                    if (methodInfo != null) {
                                        result = methodInfo.process(args);
                                    } else {
                                        result = method.invoke(mActivityManagerNativeWorker.mTarget, args);
                                    }
                                } catch (Exception e) {
                                    if (DEBUG) {
                                        e.printStackTrace();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    String message = Util.printlnMethod("### Am invoke : ", method, args);
                                    // 如果在启动LogTraceService出现异常，为了防止死循环，直接抛出这个异常
                                    if (method.getName().equals("startService")
                                            && message.toString().contains
                                            ("com.baidu.android.gporter.stat.LogTraceService")) {

                                        if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                            throw e;
                                        } else {
                                            throw new GPTProxyAmsException(message, e);
                                        }
                                    }
                                    sb.append(Util.getCallStack(e));
                                    ReportManger.getInstance().onExceptionByLogService(context, "",
                                            sb.toString(), ExceptionConstants.TJ_78730005);

                                    if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                        throw e;
                                    } else {
                                        throw new GPTProxyAmsException(message, e);
                                    }
                                }
                                return result;
                            }
                        });
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    Util.printCallStack(e);
                }
            }

            try {
                // android O 引入的IActivityManagerSingleton。
                // 不用Build.VERSION.SDK_INT >25 ，是因为Android O的preview版本也是25，没有升级
                Singleton<IActivityManager> activityManagerRealSingleton =
                        (Singleton<IActivityManager>) JavaCalls.getStaticField
                                (Constants.ACTIVE_MANAGER_CLASS, "IActivityManagerSingleton");
                if (activityManagerRealSingleton != null) {
                    mActivityManagerNativeWorker.mTarget = activityManagerRealSingleton.get();
                    Singleton<IActivityManager> activityManagerProxySingleton = new Singleton<IActivityManager>() {
                        protected IActivityManager create() {
                            return (IActivityManager) mActivityManagerNativeProxy;
                        }
                    };
                    JavaCalls.setStaticField(Constants.ACTIVE_MANAGER_CLASS, "IActivityManagerSingleton",
                            activityManagerProxySingleton);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    Class<?> ActivityManagerNativeClass = Class.forName(Constants.ACTIVE_MANAGER_NATIVE_CLASS);
                    Field field = ActivityManagerNativeClass.getDeclaredField("gDefault");
                    field.setAccessible(true);
                    Singleton<IActivityManager> realService = (Singleton<IActivityManager>) field
                            .get(ActivityManagerNativeClass);
                    mActivityManagerNativeWorker.mTarget = realService.get();

                    Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
                        protected IActivityManager create() {
                            return (IActivityManager) mActivityManagerNativeProxy;
                        }
                    };

                    field.set(ActivityManagerNativeClass, gDefault);
                } else {
                    Class<?> ActivityManagerNativeClass = Class.forName(Constants.ACTIVE_MANAGER_NATIVE_CLASS);
                    Field field = ActivityManagerNativeClass.getDeclaredField("gDefault");
                    field.setAccessible(true);
                    IActivityManager realService = (IActivityManager) field.get(ActivityManagerNativeClass);
                    mActivityManagerNativeWorker.mTarget = realService;
                    field.set(ActivityManagerNativeClass, mActivityManagerNativeProxy);
                }

            } catch (Exception e) {
                if (DEBUG) {
                    Util.printCallStack(e);
                }
            }
        }
    }

    /**
     * 初始化插件的运行环境，如果已经初始化，则什么也不做
     *
     * @param context     application context
     * @param packageName 插件包名
     */
    public synchronized static boolean initProxyEnvironment(Context context, String packageName) {
        if (DEBUG) {
            Log.d(TAG, "--- initProxyEnvironment, pkg=" + packageName + ", process=" + android.os.Process.myPid());
        }

        long start = SystemClock.elapsedRealtime();
        if (sPluginsMap.containsKey(packageName)) {
            return true;
        }
        if (assertApkFile(context, packageName)) {
            ProxyEnvironment newEnv = new ProxyEnvironment(context, packageName);
            sPluginsMap.put(packageName, newEnv);
            long time = SystemClock.elapsedRealtime() - start;
            PluginTimeLine timeLine = pluginTimeLineMap.get(packageName);
            if (timeLine != null) {
                timeLine.initProxyEnvironmentTime = time;
            }
            ReportManger.getInstance().initProxyEnvironment(context, packageName, time);
            return true;
        } else {
            // 下次启动时尝试删除插件
            ApkInstaller.deletePackage(context, packageName, true);
            return false;
        }
    }

    /**
     * 退出插件
     *
     * @param packageName 插件包名
     * @return true退出成功，false退出失败
     */
    public static boolean exitProxy(String packageName) {
        return exitProxy(packageName, false);
    }

    /**
     * 退出插件，只是退出本进程的插件。
     * 如果不知道插件运行在哪个进程。可调用 {@link TargetActivator#unLoadTarget(String)}
     *
     * @param packageName 插件包名
     * @param force       是否强制退出
     * @return ture退出成功，false退出失败
     */
    public static boolean exitProxy(String packageName, boolean force) {
        if (DEBUG) {
            Log.i(TAG, "--- exitProxy, pkg=" + packageName + ", force=" + force);
        }
        if (packageName == null) {
            return true;
        }
        ProxyEnvironment env = null;
        synchronized (gLoadingMap) {
            if (!force && isLoadingOrLoaded(packageName)) {
                return false;
            }

            env = sPluginsMap.get(packageName);
            if (env == null || env.application == null) {
                return true;
            }

            if (env.bIsApplicationInit) {
                // 注销广播并调用onTerminate()
                env.unregisterStaticBroadcasts();
                env.application.onTerminate();
            }
            sPluginsMap.remove(packageName);
        }
        return true;
    }

    /**
     * 当前是否运行在插件代理模式
     *
     * @return true or false
     */
    public static boolean isProxyMode() {
        return sPluginsMap.size() > 0;
    }

    /**
     * 获取插件的classloader
     *
     * @return classloader
     */
    public ClassLoader getDexClassLoader() {
        return dexClassLoader;
    }

    /**
     * 获取插件资源
     *
     * @return 资源对象
     */
    public Resources getTargetResources() {
        return targetResources;
    }

    /**
     * 获取targetAssetManager
     *
     * @return targetAssetManager
     */
    public AssetManager getTargetAssetManager() {
        return targetAssetManager;
    }

    /**
     * 获取targetTheme
     *
     * @return targetTheme
     */
    public Theme getTargetTheme() {
        return targetTheme;
    }

    /**
     * 获取targetMapping
     *
     * @return targetMapping
     */
    public TargetMapping getTargetMapping() {
        return targetMapping;
    }

    /**
     * 获取TargetPackageName
     *
     * @return TargetPackageName
     */
    public String getTargetPackageName() {
        return targetMapping.getPackageName();
    }

    /**
     * remapShortCutCreatorIntent
     *
     * @param intent 启动组件的intent
     */
    public void remapShortCutCreatorIntent(Intent intent) {

        if (Constants.ACTION_INSTALL_SHORT_CUT.equals(intent.getAction())
                || Constants.ACTION_UNINSTALL_SHORT_CUT.equals(intent.getAction())) {
            Parcelable bitmap = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
            if (bitmap == null || !(bitmap instanceof Bitmap)) {
                Parcelable extra = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                if (extra != null && extra instanceof ShortcutIconResource) {
                    try {
                        ShortcutIconResource iconResource = (ShortcutIconResource) extra;
                        final int id = targetResources.getIdentifier(iconResource.resourceName, null, null);
                        Bitmap iconBitmap = BitmapFactory.decodeResource(targetResources, id);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, iconBitmap);
                        intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    } catch (Exception e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            Intent shortCutIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortCutIntent != null) {

                // 拷贝一份，因为有些插件可能用同一个intent进行install和uninstall
                shortCutIntent = new Intent(shortCutIntent);
                remapComponentIntent(shortCutIntent);

                // 把插件应用的组件信息从category取出，放到extra，去掉所有category是为了防止某些手机因为加了cate，无法启动快捷方式
                GPTComponentInfo info = GPTComponentInfo.parseFromIntent(shortCutIntent);
                if (info != null) {
                    shortCutIntent.removeCategory(info.toString());
                    shortCutIntent.putExtra(ShortcutActivityProxy.EXTRA_KEY_TARGET_INFO, info.toString());

                    // 保存原来category
                    if (shortCutIntent.getCategories() != null && shortCutIntent.getCategories().size() > 0) {
                        String[] cates = null;
                        cates = new String[shortCutIntent.getCategories().size()];
                        shortCutIntent.getCategories().toArray(cates);
                        shortCutIntent.putExtra(ShortcutActivityProxy.EXTRA_KEY_TARGET_CATE, cates);

                        for (String cate : cates) {
                            shortCutIntent.removeCategory(cate);
                        }
                    }
                }

                // 统一以shortcutactivityproxy为入口，避免在插件未加载的时候直接启动activityproxy会先一闪再启动
                shortCutIntent.setComponent(new ComponentName(mHostAppContext.getPackageName(),
                        ShortcutActivityProxy.class.getName()));

                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutIntent);
            }
        }
    }

    /**
     * 尝试重映射插件组件
     *
     * @param originIntent 启动组件的intent
     */
    public void remapComponentIntent(Intent originIntent) {
        remapStartActivityIntent(originIntent);
        remapStartServiceIntent(originIntent);
        remapReceiverIntent(originIntent);
    }

    /**
     * remapStartServiceIntent
     *
     * @param originIntent 启动组件的intent
     */
    public void remapStartServiceIntent(Intent originIntent) {
        RemapingUtil.remapServiceIntent(mHostAppContext, originIntent);
    }

    /**
     * remapStartServiceIntent
     *
     * @param intent        启动组件的intent
     * @param targetService 目标Service
     */
    public void remapStartServiceIntent(Intent intent, String targetService) {
        RemapingUtil.remapServiceIntent(mHostAppContext, intent, targetService);
    }

    /**
     * remapStartActivityIntent
     *
     * @param originIntent 启动组件的intent
     */
    public void remapStartActivityIntent(Intent originIntent) {
        RemapingUtil.remapActivityIntent(mHostAppContext, originIntent);
    }

    /**
     * remapStartActivityIntent
     *
     * @param intent         启动组件的intent
     * @param targetActivity 目标Activity
     */
    public void remapStartActivityIntent(Intent intent, String targetActivity) {
        RemapingUtil.remapActivityIntent(mHostAppContext, intent, mPackageName, targetActivity, true);
    }

    /**
     * remapReceiverIntent
     *
     * @param originIntent 启动组件的intent
     */
    public void remapReceiverIntent(Intent originIntent) {
        RemapingUtil.remapReceiverIntent(mHostAppContext, originIntent);
    }

    /**
     * 获取重映射之后的Activity类
     *
     * @param targetActivity 插件Activity类
     * @return 返回代理Activity类
     */
    public Class<?> getRemapedActivityClass(String targetActivity) {
        Class<?> clazz = ProxyActivityCounter.getInstance().getNextAvailableActivityClass(mHostAppContext,
                targetMapping.getActivityInfo(targetActivity).packageName,
                targetMapping.getActivityInfo(targetActivity), GPTPackageDataModule.getInstance(mHostAppContext)
                        .getPackageInfo(targetMapping.getActivityInfo(targetActivity).packageName));

        return clazz;
    }

    /**
     * 压入activity到栈
     *
     * @param activity 要压入的activity
     */
    public void pushActivityToStack(Activity activity) {
        synchronized (sActivityStack) {
            sActivityStack.addFirst(activity);
        }
    }

    /**
     * 从栈中弹出activity
     *
     * @param activity 要弹出的activity
     * @return true弹出成功, false弹出失败
     */
    public boolean popActivityFromStack(Activity activity) {
        synchronized (sActivityStack) {
            if (!sActivityStack.isEmpty()) {
                return sActivityStack.remove(activity);
            } else {
                return false;
            }
        }
    }

    /**
     * 我们自己处理launchMode
     *
     * @param hostCtx        宿主Context
     * @param intent         启动Activity的Intent
     * @param packageName    插件包名
     * @param targetActivity 目前Activity类名
     */
    public static void dealLaunchMode(Context hostCtx, Intent intent, String packageName, String targetActivity) {
        if (targetActivity == null || packageName == null) {
            if (DEBUG) {
                Log.d(TAG, "--- dealLaunchMode, param invalid : pkg=" + packageName + ", class=" + targetActivity);
            }
        }

        TargetMapping mapping = TargetManager.getInstance(hostCtx).getTargetMapping(packageName);
        if (mapping == null) {

            // 不是插件，不用处理
            return;
        }
        ActivityInfo info = mapping.getActivityInfo(targetActivity);
        if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {

            // 判断栈顶是否为需要启动的Activity
            Activity activity = null;
            synchronized (sActivityStack) {
                if (!sActivityStack.isEmpty()) {
                    activity = sActivityStack.getFirst();
                }
            }
            if (activity instanceof ActivityProxy) {
                ActivityProxy adp = (ActivityProxy) activity;
                Activity pt = adp.getCurrentActivity();
                if (pt != null && TextUtils.equals(targetActivity, pt.getClass().getName())) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                }
            }
        } else if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {

            Activity found = null;
            synchronized (sActivityStack) {
                Iterator<Activity> it = sActivityStack.iterator();
                while (it.hasNext()) {
                    Activity activity = it.next();
                    if (activity instanceof ActivityProxy) {
                        ActivityProxy adp = (ActivityProxy) activity;
                        Activity pt = adp.getCurrentActivity();
                        if (pt != null && TextUtils.equals(targetActivity, pt.getClass().getName())) {
                            found = activity;
                            break;
                        }
                    }
                }

                // 栈中已经有当前activity
                if (found != null) {
                    Iterator<Activity> iterator = sActivityStack.iterator();
                    while (iterator.hasNext()) {
                        Activity activity = iterator.next();
                        if (activity == found) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            break;
                        }
                        iterator.remove();
                        activity.finish();
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

    }

    /**
     * 判断apk文件是否存在
     *
     * @param context     Context
     * @param packageName 插件包名
     * @return 是否存在
     */
    private static boolean assertApkFile(final Context context, String packageName) {
        File apkFile = ApkInstaller.getInstalledApkFile(context, packageName);
        boolean isApk = apkFile != null && apkFile.isFile() && apkFile.getName().endsWith(ApkInstaller.APK_SUFFIX);

        if (!isApk) {
            String s = new String(apkFile.getPath() + " is not exist!!! on ProxyEnvironment.init");
            ReportManger.getInstance().onException(context, packageName, s, ExceptionConstants.TJ_78730011);
//			sendLoadResult(context, new Intent(ProxyEnvironment.ACTION_TARGET_LOADED), packageName, false);
            return false;
        }
        return true;
    }

    /**
     * 创建数据根路径
     */
    private void createDataRoot() {

        // TODO 这里需要考虑插件升级后MetaData的配置改变，data路径随之改变，是否需要保存数据
        targetDataRoot = getDataDir(mHostAppContext, mPackageName);
        targetDataRoot.mkdirs();
    }

    /**
     * 获取某个插件的 data 根目录。
     *
     * @param context     确保调用的context是host的，不然会出问题
     * @param packageName 插件包名
     * @return data目录的file对象
     */
    public static File getDataDir(Context context, String packageName) {
        GPTPackageInfo info = GPTPackageDataModule.getInstance(context).getPackageInfo(packageName);
        if (info != null && info.isUnionDataDir) {
            return new File(context.getFilesDir().getParent());
        } else {
            return new File(ApkInstaller.getGreedyPorterRootPath(context), packageName);
        }
    }

    /**
     * 创建ClassLoader， 需要在 createDataRoot之后调用
     */
    private void createClassLoader() {

        dexClassLoader = new GPTClassLoader(apkFile.getAbsolutePath(), new File(
                ApkInstaller.getGreedyPorterRootPath(mHostAppContext), mPackageName).getAbsolutePath(),
                getTargetLibPath(),
                super.getClass().getClassLoader().getParent(), super.getClass().getClassLoader());

        // 把 插件 classloader 注入到 host程序中，方便host app 能够找到 插件 中的class。因为 Intent put
        // serialize extra避免不开了！！！
        // TODO 暂时改为默认注入，后续再看如何处理
//        if (targetMapping.getMetaData() != null && targetMapping.getMetaData().getBoolean(META_KEY_CLASSINJECT)) {
//        ClassLoaderInjectHelper.inject(mHostAppContext.getClassLoader(), dexClassLoader, targetMapping.getPackageName()
//                + ".R");
//        }

        // 加载 GPT.dex 
/*        File gptClassesZip = new File(targetDataRoot, mPackageName + ApkInstaller.APK_SUFFIX
                 + DexExtractor.GPT_SUFFIX + DexExtractor.EXTRACTED_SUFFIX);
        
        if (gptClassesZip.exists()) {
            ClassLoader gptClassLoader = new DexClassLoader(gptClassesZip.getAbsolutePath(), 
                    targetDataRoot.getAbsolutePath(), getTargetLibPath(), dexClassLoader);
            
            dexClassLoader = gptClassLoader;
        } else {
            if (DEBUG) {
                Log.e(TAG, "### GPT ZIP not exist : " + gptClassesZip.getAbsolutePath());
            }
        }*/
        // 有应用会拿classloader强转成pathclassloader，兼容之
        dexClassLoader = new PathClassLoader("", dexClassLoader);

        if (DEBUG) {
            Log.d(TAG, "--- createClassLoader : " + dexClassLoader);
        }
    }

    /**
     * createTargetResource
     */
    private void createTargetResource() {
        try {
            AssetManager am = (AssetManager) AssetManager.class.newInstance();
            JavaCalls.callMethod(am, "addAssetPath", new Object[]{apkFile.getAbsolutePath()});
            targetAssetManager = am;
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        // 解决一个HTC ONE X上横竖屏会黑屏的问题
        Resources hostRes = mHostAppContext.getResources();
        Configuration config = new Configuration();
        config.setTo(hostRes.getConfiguration());
        config.orientation = Configuration.ORIENTATION_UNDEFINED;
        targetResources = new ResourcesProxy(targetAssetManager, hostRes.getDisplayMetrics(), config, hostRes);
        targetTheme = targetResources.newTheme();
        targetTheme.setTo(mHostAppContext.getTheme());
        targetTheme.applyStyle(targetMapping.getTheme(), true);
    }

    /**
     * 解析插件apk
     */
    private void createTargetMapping() {
        targetMapping = TargetManager.getInstance(mHostAppContext).getTargetMapping(mPackageName);

        // 容错，万一没创建成功
        if (targetMapping == null) {
            targetMapping = new ApkTargetMapping(mHostAppContext, apkFile);
        }
    }

    /**
     * addPermissions
     */
    private void addPermissions() {
        // TODO add permissions
    }

    /**
     * getParentPackagename
     *
     * @return the parentPackagename
     */
    public String getParentPackagename() {
        return parentPackagename;
    }

    /**
     * 获取插件Activity的ThemeResource
     *
     * @param activity Activity
     * @return ThemeResource
     */
    public int getTargetActivityThemeResource(String activity) {
        return targetMapping.getThemeResource(activity);
    }

    /**
     * 获取插件Activity的屏幕方向
     *
     * @param activity activity类名
     * @return 屏幕方向
     */
    public int getTargetActivityOrientation(String activity) {
        return targetMapping.getActivityInfo(activity).screenOrientation;
    }

    /**
     * 获取application
     *
     * @return the application
     */
    public Application getApplication() {
        return application;
    }

    /**
     * 获取HostResourcesId
     *
     * @param resourcesName resourcesName
     * @param resourceType  resourceType
     * @return ResourcesId
     */
    public int getHostResourcesId(String resourcesName, String resourceType) {
        if (mHostAppContext != null) {
            return mHostAppContext.getResources().getIdentifier(resourcesName, resourceType, mHostAppContext.getPackageName());
        }
        return 0;
    }

    /**
     * 获取HostPackageName
     *
     * @return parentPackagename
     */
    public String getHostPackageName() {
        return parentPackagename;
    }


    /**
     * 退出某个插件应用。不是卸载插件应用
     */
    public void quitApp() {
        synchronized (sActivityStack) {
            ArrayList<Activity> removed = new ArrayList<Activity>();
            for (Activity activity : sActivityStack) {
                if (activity.getPackageName().equals(mPackageName)) {
                    activity.finish();
                    removed.add(activity);
                }
            }
            sActivityStack.removeAll(removed);
        }
    }


    /**
     * bind service 获取的IBinder。先保存这，便于后续通信
     * 多个插件功能共用一个通道。只bind一次。不需要 unbind.s
     */
    public static IGPTBinder[] sGPTBinders = new IGPTBinder[Constants.GPT_PROCESS_NUM];


    /**
     * 加载插件, 此函数必须在主进程调用。
     *
     * @param context      application Context
     * @param launchIntent 启动的Intent
     * @param listenner    插件加载后的回调
     * @param cpExtProcess 额外进程
     */
    public static void initTargetAndLaunchIntent(final Context context, final Intent launchIntent,
                                                 final ITargetLoadListenner listenner, final int cpExtProcess) {
        if (DEBUG) {
            Log.d(TAG, "--- initTargetAndLaunchIntent, intent = " + launchIntent);
        }

        String launchPackageName = launchIntent.getComponent().getPackageName();
        GPTPackageInfo packageInfo = GPTPackageDataModule.getInstance(context).getPackageInfo(launchPackageName);
        // 已经启动了，则不走安装过程。
        if (isEnterProxy(launchPackageName) || hasInstanceInGPTProcess(launchPackageName)) {
            launchIntentAfterInstall(context, launchIntent, listenner, cpExtProcess, launchPackageName);
            return;
        }

        if (packageInfo != null && packageInfo.state == GPTPackageInfo.STATE_NEED_NEXT_SWITCH_INSTALL_FILE) {
            ApkInstaller.switchInstallDir(context, packageInfo.packageName, packageInfo.tempInstallDir,
                    packageInfo.tempApkPath, packageInfo.srcApkPath);
        }

        GPTPackageManager.getInstance(context).packageAction(launchPackageName, new IInstallCallBack() {

            @Override
            public void onPackageInstallFail(String packageName, String failReason) {
                clearLoadingIntent(packageName);
                if (listenner != null) {
                    listenner.onLoadFailed(packageName, failReason);
                }
            }

            @Override
            public void onPacakgeInstalled(final String packageName) {
                launchIntentAfterInstall(context, launchIntent, listenner, cpExtProcess, packageName);
            }
        });
    }

    /**
     * launchIntentAfterInstall
     *
     * @param context      application Context
     * @param launchIntent 启动的Intent
     * @param listenner    插件加载后的回调
     * @param cpExtProcess 额外进程 TODO 多进程启动需关注
     * @param packageName  包名
     */
    private static void launchIntentAfterInstall(final Context context, final Intent launchIntent,
                                                 final ITargetLoadListenner listenner, final int cpExtProcess, final String packageName) {
        final GPTPackageInfo info = GPTPackageManager.getInstance(context).getPackageInfo(packageName);
        final int extProcess = cpExtProcess != Constants.GPT_PROCESS_DEFAULT ? cpExtProcess : info.extProcess;
        if (info != null && (!info.isUnionProcess || extProcess != Constants.GPT_PROCESS_DEFAULT)) {
            final Context ctxWrapper = context.getApplicationContext();

            final Handler handler = new Handler(ctxWrapper.getMainLooper());
            final Runnable timeoutRunnable = new Runnable() {

                @Override
                public void run() {
                    clearLoadingIntent(packageName);
                    if (listenner != null) {
                        listenner.onLoadFailed(packageName, "");
                    }
                }
            };

            // 加载时间不超过1分钟
            handler.postDelayed(timeoutRunnable, Constants.MAX_LOADING_TARGET_TIME);

            if (sGPTBinders[extProcess] == null) {
                initBindGPTProcess(context, launchIntent, listenner, cpExtProcess, packageName, handler, timeoutRunnable);
            } else {
                launchIntentByPGTProcess(packageName, handler, launchIntent, listenner, null, extProcess);
                ReportManger.getInstance().startPluginGPTProcess(context, packageName);
            }
        } else {
            launchIntentByMainProcess(context, packageName, launchIntent, listenner);
        }

    }

    /**
     * initBindGPTProcess
     *
     * @param context         application Context
     * @param launchIntent    启动的Intent
     * @param listenner       插件加载后的回调
     * @param cpExtProcess    额外进程
     * @param packageName     包名
     * @param handler         Handler
     * @param timeoutRunnable timeoutRunnable
     */
    private static void initBindGPTProcess(final Context context, final Intent launchIntent,
                                           final ITargetLoadListenner listenner, final int cpExtProcess, final String packageName,
                                           final Handler handler, final Runnable timeoutRunnable) {
        final GPTPackageInfo info = GPTPackageManager.getInstance(context).getPackageInfo(packageName);
        final int extProcess = cpExtProcess != Constants.GPT_PROCESS_DEFAULT ? cpExtProcess : info.extProcess;
        final Context ctxWrapper = context.getApplicationContext();
        if (sGPTBinders[extProcess] == null) {


            ServiceConnection conn = new ServiceConnection() {

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    if (DEBUG) {
                        Log.d(TAG, "--- GPT service disconnected!");
                    }
                    // 防止 gpt 进程被意外杀死restart 重新自动bind，启动界面。同时避免restart 后
                    // onServiceDisconnected 两次
                    // 在这里 unbind 也可以防止 restart
                    // 捕获异常，Fix 11294 【Monkey】java.lang.IllegalArgumentException
                    try {
                        ctxWrapper.unbindService(this);
                    } catch (IllegalArgumentException e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }

                    sGPTBinders[extProcess] = null;

                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (DEBUG) {
                        Log.d(TAG, "--- GPT service connected!");
                    }
                    IGPTBinder binder = IGPTBinder.Stub.asInterface(service);
                    sGPTBinders[extProcess] = binder;

                    launchIntentByPGTProcess(packageName, handler, launchIntent, listenner, timeoutRunnable,
                            extProcess);
                    ReportManger.getInstance().startPluginGPTProcess(context, packageName);
                }
            };

            Class<?> gptProcessClass = GPTProcessService.class;
            switch (extProcess) {
                default:
                    gptProcessClass = GPTProcessService.class;
                    break;
            }
            ctxWrapper
                    .bindService(new Intent(context, gptProcessClass), conn,
                            Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * bind gpt process service 后，通过 gpt process service 启动该插件。
     *
     * @param packageName     包名
     * @param handler         Handler
     * @param launchIntent    Intent
     * @param listenner       ITargetLoadListenner
     * @param timeoutRunnable timeoutRunnable
     * @param extProcess      额外进程
     */
    private static void launchIntentByPGTProcess(String packageName, final Handler handler, Intent launchIntent,
                                                 final ITargetLoadListenner listenner, final Runnable timeoutRunnable, int extProcess) {

        if (DEBUG) {
            Log.d(TAG, "--- launchIntentByPGTProcess, intent = " + launchIntent);
        }

        Intent[] intents = null;
        List<Intent> itList = ProxyEnvironment.removeLoadingIntents(packageName);
        if (itList != null && itList.size() > 0) {
            intents = new Intent[itList.size()];
            itList.toArray(intents);
        }

        if (intents == null) {
            intents = new Intent[1];
            intents[0] = launchIntent;
        }

        try {

            // return不能往上移，需要把loadingmap给remove掉
            if (sGPTBinders[extProcess] == null) {
                return;
            }
            PluginTimeLine timeLine = pluginTimeLineMap.remove(packageName);
            long hotTimeStart = pluginHotStartTimeMap.get(packageName) == null ?
                    0 : pluginHotStartTimeMap.remove(packageName);
            sGPTBinders[extProcess].launchIntents(packageName, intents, timeLine, hotTimeStart,
                    new IGPTEnvCallBack.Stub() {

                        @Override
                        public void onTargetLoaded(final String pPackageName) throws RemoteException {
                            if (handler != null)
                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (timeoutRunnable != null) {
                                            handler.removeCallbacks(timeoutRunnable);
                                        }

                                        if (listenner != null) {
                                            listenner.onLoadFinished(pPackageName, false);
                                        }
                                    }
                                });
                        }

                    });
        } catch (RemoteException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            if (listenner != null) {
                listenner.onLoadFailed(packageName, "### GPT binder call failed!");
            }
        } catch (NullPointerException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            if (listenner != null) {
                listenner.onLoadFailed(packageName, "### GPT binder released!");
            }
        }
    }


    /**
     * 在主进程中启动插件
     *
     * @param context      Context
     * @param packageName  包名
     * @param launchIntent 启动的Intent
     * @param listenner    启动监听
     */
    private static void launchIntentByMainProcess(final Context context, String packageName,
                                                  final Intent launchIntent, final ITargetLoadListenner listenner) {
        // 在当前进程加载
        new AsyncTask<String, Integer, String>() {

            @Override
            protected String doInBackground(String... params) {
                String pPackageName = params[0];
                ProxyEnvironment.initProxyEnvironment(context, pPackageName);
                return pPackageName;
            }

            @Override
            protected void onPostExecute(String result) {
                launchIntent(context, launchIntent, null);
                if (listenner != null) {
                    listenner.onLoadFinished(result, true);
                }
                super.onPostExecute(result);
            }
        }.execute(packageName);
        ReportManger.getInstance().startPluginMainProcess(context, packageName);
    }
    
/*    public String getPackageName() {
        //为了拿到堆栈，制造一个异常
        try {
            throw new Exception();
        } catch (Exception e) {
            try{
                StackTraceElement[] stacks = e.getStackTrace();
                if(stacks != null){
                    for(int i = 0;i < stacks.length;i ++){
                        if(stacks[i].getClassName().equalsIgnoreCase("android.app.PendingIntent")){
                            return mHostAppContext.getPackageName();
                        }                            
                    }
                }                
            }
            catch (Exception e1){
            
            }
        }
        
      
        return mPackageName;
    }*/

    /**
     * getPackageManager
     *
     * @return PackageManager
     */
    public PackageManager getPackageManager() {
        return application.getPackageManager();
    }

    /**
     * replacePackageManager
     *
     * @param hostContext hostContext
     * @throws Exception 主体逻辑出错抛异常
     */
    public static synchronized void replacePackageManager(final Context hostContext) {
        if (sProxyPm == null) {
            sProxyPm = new PackageManagerWorker(hostContext);
            sProxyPm.setPackageName(hostContext.getPackageName());

            try {
                Class<?> iPackageManagerClass = Class.forName(Constants.PACKAGE_MANAGER_CLASS);

                Object packageManagerProxy = Proxy.newProxyInstance(iPackageManagerClass.getClassLoader(),
                        new Class<?>[]{iPackageManagerClass}, new InvocationHandler() {

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                Object result = null;
                                try {
                                    MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(
                                            sProxyPm, Constants.PACKAGE_MANAGER_CLASS,
                                            method);
                                    if (methodInfo != null) {
                                        result = methodInfo.process(args);
                                    } else {
                                        result = method.invoke(sProxyPm.mTarget, args);
                                    }
                                } catch (Throwable e) {
                                    if (DEBUG) {
                                        e.printStackTrace();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    String message = Util.printlnMethod("### Pm invoke : ", method, args);
                                    sb.append(Util.getCallStack(e));
                                    ReportManger.getInstance().onExceptionByLogService(hostContext, "",
                                            sb.toString(), ExceptionConstants.TJ_78730006);

                                    if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                        throw e;
                                    } else {
                                        throw new GPTProxyPmsException(message, e);
                                    }

                                }
                                return result;
                            }
                        });

                PackageManager pm = hostContext.getPackageManager();

                sProxyPm.mTarget = (IPackageManager) (JavaCalls.getField(pm, "mPM"));

                JavaCalls.setStaticField("android.app.ActivityThread", "sPackageManager", packageManagerProxy);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * MIUI的Android8.0中使用Setting的Provider获取值时不能及时生效的问题。
     * 插件无法有效兼容处理，所以在GPT框架中统一兼容。
     *
     * @param context Context
     */
    private static synchronized void getSettingProvider(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                Settings.Global.getInt(context.getContentResolver(),
                        android.provider.Settings.Global.AIRPLANE_MODE_ON);
        } catch (Throwable e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取mHostAppContext.getApplicationContext()。
     *
     * @return Application
     */
    public Application getApplicationProxy() {
        return (Application) mHostAppContext.getApplicationContext();
    }

    /**
     * GPT 1.0 版本接口。不能删除。1.0 版本的已安装插件会调用到此版本函数。
     *
     * @param name
     * @return SystemService
     */
    public Object getSystemService(String name) {
        return mHostAppContext.getSystemService(name);
    }


    /**
     * setTargetApplication。
     *
     * @param context Application
     */
    public void setTargetApplication(Application context) {
        application = context;
    }
    
/*    public Context createPackageContext(Context context) {
        try {
            ContextWrapper appContext = (ContextWrapper) context.getApplicationContext();
            Context base = (Context)JavaCalls.getField(appContext, "mBase");
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Constructor<?> constructor = contextImplClass.getConstructor(contextImplClass);
            Context newContext = (Context) constructor.newInstance(base);
            return newContext;
        } catch (Exception e) {         
            e.printStackTrace();
        } 
        return null;
    }*/

    /**
     * 插件包能显示的notification id白名单
     */
    private ArrayList<Integer> mNotificationWhitelist = new ArrayList<Integer>();

    /**
     * 检查notification id是否能显示
     *
     * @param notificationId 通知的ID
     */
    public boolean checkNotification(int notificationId) {
        synchronized (mNotificationWhitelist) {
            for (Integer id : mNotificationWhitelist) {
                if (id.intValue() == notificationId) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 添加插件包能显示的notification id到白名单
     *
     * @param notificationId 通知的ID
     * @return true添加成功, false添加失败
     */
    public boolean addNotificationId(int notificationId) {
        synchronized (mNotificationWhitelist) {
            for (Integer id : mNotificationWhitelist) {
                if (id.intValue() == notificationId) {
                    return true;
                }
            }
            return mNotificationWhitelist.add(notificationId);
        }
    }

    /**
     * 插件加载成功的时长统计
     *
     * @param context     Context
     * @param packageName 包名
     * @param intent      Intent
     */
    private static void onPluginLoadSucess(Context context, String packageName, Intent intent) {
        PluginTimeLine timeLine = pluginTimeLineMap.get(packageName);
        if (timeLine == null) {
            return;
        }
        long currentTme = SystemClock.elapsedRealtime();
        long startTime = timeLine.startElapsedRealTime;
        long coastTime = -1;
        if (startTime >= 0) {
            coastTime = currentTme - startTime;
        }
        timeLine.pluginLoadSucessTime = coastTime;
        // 此处用作时长性能统计上报，没有异常后就算为插件启动成功。
        ReportManger.getInstance().onPluginLoadSucess(context, packageName, intent, coastTime);
        ReportManger.getInstance().onPluginTimeLine(context, packageName, timeLine);
        pluginTimeLineMap.remove(packageName);
    }


}



