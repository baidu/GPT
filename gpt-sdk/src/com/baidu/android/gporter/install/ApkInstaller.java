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
package com.baidu.android.gporter.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.baidu.android.gporter.pm.GPTPackageDataModule;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.SimpleDateTime;
import com.baidu.android.gporter.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;


/**
 * app 安装接口。实际安装采用 {@link ApkInstallerService} 独立进程异步安装。<br>
 * <p>
 * 插件支持的后缀名为 {@value #APK_SUFFIX}, 内置在 assets/greedyporter 目录下，以{@value #APK_SUFFIX}后缀命名，安装完后缀名也是 {@value #APK_SUFFIX}。<br>
 * <p>
 * 由于android 2.2 及以下对asset文件的大小有1M限制。所以我们需要在编译脚本中对 aapt 增加一个 -0 {@value #APK_SUFFIX}参数，告诉aapt不要对{@value #APK_SUFFIX} 进行压缩处理。
 * 对于此问题的解释:http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/
 *
 * @author liuhaitao
 * @since 2014年5月8日
 */
public class ApkInstaller {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ApkInstaller";

    public static final String PLUGIN_PATH = "greedyporter";

    /**
     * 内置app的目录 assets/greedyporter
     */
    public static final String ASSETS_PATH = "greedyporter";

    /**
     * APK_SUFFIX
     */
    public static final String APK_SUFFIX = ".apk";

    /**
     * NATIVE_LIB_PATH
     */
    public static final String NATIVE_LIB_PATH = "lib";

    /**
     * 新版本 格式为 gptlib + timestamp 例如   gptlib1435904813558， 用于热插拔, so 只能被一个classloader加载。
     */
    public static final String NATIVE_LIB_PATH_GPT = "gptlib";


    /**
     * shared preference file name.
     */
    public static final String SHARED_PREFERENCE_NAME = "greedyporter";

    /**
     * HOST app 的 version code. 用于内置app的安装检查。
     */
    private static final String SP_HOSTAPP_VERSIONCODE_FOR_INSTALL = "host_version_code_for_install";

    /**
     * 安装/检查安装内置app是否结束。
     */
    private static boolean sInstallBuildinAppsFinished = false;

    /**
     * {@link #installBuildinApps(Context) 只能调用一次，再次调用，直接返回}
     */
    private static boolean sInstallBuildinAppsCalled = false;
    /**
     * receiver 只注册一次。
     */
    private static boolean sInstallerReceiverRegistered = false;

    /**
     * 安装列表，用于存储正在安装的 文件列表（packageName），安装完从中删除。
     */
    private static LinkedList<String> sInstallList = new LinkedList<String>();

    /**
     * 安装阶段本次需要安装的内置app列表（packageName）
     */
    private static ArrayList<String> sBuildinAppList = new ArrayList<String>();


    /**
     * 返回 greedyporter 的根目录 /data/data/hostapp/app_greedyporter
     *
     * @param context host的context
     * @return 插件跟路径
     */
    public static File getGreedyPorterRootPath(Context context) {
        File repoDir = context.getDir(PLUGIN_PATH, 0);

        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
        return repoDir;
    }


    /**
     * 安装内置在 assets/greedyporter 目录下的 apk
     *
     * @param context Context
     */
    public synchronized static void installBuildinApps(Context context) {
        if (sInstallBuildinAppsCalled) {
            return;
        }
        sInstallBuildinAppsCalled = true;

        registerInstallderReceiver(context);

        ApplicationInfo pi = context.getApplicationInfo(); // host app's packageinfo

        // debugable 状态下特殊对待，因为开发中每次都需要检查安装。
        // eclipse install的apk debugable = true, release 的为 false。
        boolean debugable = false; // host app debugable 是否为 true.
        if ((pi.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            debugable = true;
        }
        // 使用hostapp 默认 sharedpref，减少初始化开销。
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int savedHostVersionCode = sp.getInt(SP_HOSTAPP_VERSIONCODE_FOR_INSTALL, -1);
        int hostVersionCode = -1;
        try {
            hostVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e1) {
            if (DEBUG) {
                e1.printStackTrace();
            }
        }
        // 内置的app只需要安装一次，等下载hostapp升级，再次进行安装检查。debugable 模式除外。
        if (!debugable && hostVersionCode == savedHostVersionCode) {
            setInstallBuildinAppsFinished(context, false);
            return;
        }

        AssetManager am = context.getAssets();
        try {
            String files[] = am.list(ASSETS_PATH);
            boolean needInstall = false; //是否有文件需要安装或升级.
            for (String file : files) {
                if (!file.endsWith(APK_SUFFIX)) {
                    continue;
                }
                needInstall |= installBuildinApp(context, ASSETS_PATH + "/" + file);
            }

            if (!needInstall) { // 没有需要安装/升级的文件
                boolean writeVersioncide = savedHostVersionCode != hostVersionCode;
                setInstallBuildinAppsFinished(context, writeVersioncide);
            }

        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * installBuildinApp
     *
     * @param context    Context
     * @param assetsPath assetsPath
     * @return 需要安装 返回 true，不需要安装 返回 false.
     */
    public static synchronized boolean installBuildinApp(Context context, String assetsPath) {
        registerInstallderReceiver(context);

        // 先判断是否有安装文件存在，然后判断一下更新时间是否一致。
        // 注意:内置app必须以包名命名
        int start = assetsPath.lastIndexOf("/");
        int end = assetsPath.lastIndexOf(ApkInstaller.APK_SUFFIX);
        String mapPackagename = assetsPath.substring(start + 1, end);

        boolean isInstalling = isInstalling(mapPackagename);
        if (isInstalling && sBuildinAppList.contains(mapPackagename)) {
            // 有可能已经调用过 安装全部内置app，这里就不需要继续安装了
            return false;
        }

        GPTPackageInfo pkgInfo = GPTPackageDataModule.getInstance(context).getPackageInfo(mapPackagename);

        if (mapPackagename != null && mapPackagename.length() > 0
                && pkgInfo != null) {

            File installedFile = new File(pkgInfo.srcApkPath);
            if (installedFile.exists() && installedFile.isFile() && installedFile.length() > 0) {
                try {
                    // 如果已经存在了 ，则比较两个文件的最后修改日期，决定是否更新。
                    InputStream currentIs = new FileInputStream(installedFile);
                    SimpleDateTime currentDateTime = Util.readApkModifyTime(currentIs);
                    currentIs.close();

                    // 内置版本信息 asset 目录下的
                    InputStream buildinIs = context.getAssets().open(assetsPath);
                    SimpleDateTime buildinDateTime = Util.readApkModifyTime(buildinIs);
                    buildinIs.close();

                    // 如果当前安装的时间>= 内置app，则直接返回. 这种情况出现在 通过下载安装
                    if (currentDateTime.compareTo(buildinDateTime) >= 0) {
                        return false;
                    } // else 继续往下执行，进行安装
                } catch (Exception e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                    // 异常继续往后执行.
                    return false;
                }
            }
        }


        return startInstall(context, GPTPackageManager.SCHEME_ASSETS + assetsPath);
    }

    /**
     * 调用 {@link ApkInstallerService} 进行实际的安装过程。采用独立进程异步操作。
     *
     * @param context
     * @param filePath 支持两种scheme {@link ApkInstallerService#SCHEME_ASSETS} 和
     *                 {@link ApkInstallerService#SCHEME_FILE}
     * @return 是否成功调起安装服务
     */
    private static boolean startInstall(final Context context, String filePath) {
        return startInstall(context, filePath, Constants.GPT_PROCESS_DEFAULT);
    }

    /**
     * 调用 {@link ApkInstallerService} 进行实际的安装过程。采用独立进程异步操作。
     *
     * @param context
     * @param filePath   支持两种scheme {@link ApkInstallerService#SCHEME_ASSETS} 和
     *                   {@link ApkInstallerService#SCHEME_FILE}
     * @param extProcess 扩展进程id
     * @return 是否成功调起安装服务
     */
    private static boolean startInstall(final Context context, String filePath, int extProcess) {

        /*
         * 获取packagename
         * 1、内置app，要求必须以 packagename.apk 命名，处于效率考虑。
         * 2、外部文件的安装，直接从file中获取packagename, 消耗100ms级别，可以容忍。
         */
        String packageName = null;
        int versionCode = -1;

        boolean isBuildin = false;

        if (filePath.startsWith(GPTPackageManager.SCHEME_ASSETS)) {
            int start = filePath.lastIndexOf("/");
            int end = filePath.lastIndexOf(ApkInstaller.APK_SUFFIX);
            packageName = filePath.substring(start + 1, end);

            isBuildin = true;

        } else if (filePath.startsWith(GPTPackageManager.SCHEME_FILE)) {
            PackageManager pm = context.getPackageManager();
            String apkFilePath = filePath.substring(GPTPackageManager.SCHEME_FILE.length());
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkFilePath, 0);
            if (pkgInfo != null) {
                packageName = pkgInfo.packageName;
                versionCode = pkgInfo.versionCode;
            }
        }

        if (packageName != null) {
            ReportManger.getInstance().onInstallPluginStart(context, packageName, String.valueOf(versionCode));

            add2InstallList(packageName); // 添加到安装列表中
            if (isBuildin) {
                sBuildinAppList.add(packageName); // 添加到内置app安装列表中
            }

            Intent intent = new Intent(ApkInstallerService.ACTION_INSTALL);
            intent.setClass(context, ApkInstallerService.class);
            intent.putExtra(GPTPackageManager.EXTRA_SRC_FILE, filePath);
            intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, packageName);
            intent.putExtra(GPTPackageManager.EXTRA_EXT_PROCESS, extProcess);

            context.startService(intent);
            return true;
        } else {
            ReportManger.getInstance().onInstallPluginStart(context, filePath, ""); // 没有包名，统计传filePath
            ReportManger.getInstance().onInstallPluginFail(context,
                    TextUtils.isEmpty(packageName) ? filePath : packageName, "", GPTPackageManager.VALUE_PARSE_FAIL);
            ApkInstallerService.notifyInstallFailed(context, filePath, "", GPTPackageManager.VALUE_PARSE_FAIL);
            return false;
        }
    }


    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。 安装完会有
     * {@link #ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param context  Context
     * @param filePath apk 文件目录 比如 /sdcard/xxxx.apk
     * @return 是否成功调起安装服务
     */
    public static boolean installApkFile(Context context, String filePath) {
        return installApkFile(context, filePath, Constants.GPT_PROCESS_DEFAULT);
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。 安装完会有
     * {@link #ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param context    Context
     * @param filePath   apk 文件目录 比如 /sdcard/xxxx.apk
     * @param extProcess 插件运行的扩展进程id
     * @return 是否成功调起安装服务
     */
    public static boolean installApkFile(Context context, String filePath, int extProcess) {
        registerInstallderReceiver(context);
        return startInstall(context, GPTPackageManager.SCHEME_FILE + filePath, extProcess);
    }


    /**
     * 返回已安装的应用列表。临时函数。可能为空，安装内置app还没有执行完毕。需要监听安装broadcast来更新安装列表。
     *
     * @param context
     * @return 安装apk文件的 列表
     * @deprecated , use {@link GPTPackageManager#getInstalledApps()}
     */
    @Deprecated
    public static ArrayList<File> getInstalledApps(Context context) {
        Hashtable<String, GPTPackageInfo> pkgList = GPTPackageDataModule.getInstance(context)
                .getInstalledPkgsInstance();
        ArrayList<File> result = new ArrayList<File>();
        Set<String> keys = pkgList.keySet();
        for (String key : keys) {
            GPTPackageInfo pkg = pkgList.get(key);
            String filePath = pkg.srcApkPath;
            result.add(new File(filePath));
        }
        return result;
    }

    /**
     * 获取安装插件的apk文件目录
     *
     * @param context     host的application context
     * @param packageName 包名
     * @return File
     */
    public static File getInstalledApkFile(Context context, String packageName) {
        GPTPackageInfo info = GPTPackageDataModule.getInstance(context).getPackageInfo(packageName);

        if (info != null && info.srcApkPath != null && info.srcApkPath.length() > 0) {
            return new File(info.srcApkPath);
        } else {
            return null;
        }
    }

    /**
     * sApkInstallerReceiver
     */
    private static BroadcastReceiver sApkInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (GPTPackageManager.ACTION_PACKAGE_INSTALLED.equals(action)
                    || GPTPackageManager.ACTION_PACKAGE_INSTALLFAIL.equals(action)) {
                String pkgName = intent.getStringExtra(GPTPackageManager.EXTRA_PKG_NAME);

                handleApkInstalled(context, pkgName);
            }
        }

    };

    /**
     * 注册InstallderReceiver
     *
     * @param context Context
     */
    private static void registerInstallderReceiver(Context context) {
        if (sInstallerReceiverRegistered) {
            // 已经注册过就不再注册
            return;
        }
        sInstallerReceiverRegistered = true;
        Context appcontext = context.getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(GPTPackageManager.ACTION_PACKAGE_INSTALLED);
        filter.addAction(GPTPackageManager.ACTION_PACKAGE_INSTALLFAIL);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        appcontext.registerReceiver(sApkInstallerReceiver, filter);
    }

    /**
     * 增加到安装列表
     *
     * @param packagename 包名
     */
    private synchronized static void add2InstallList(String packagename) {
        if (sInstallList.contains(packagename)) {
            return;
        }

        sInstallList.add(packagename);
    }

    /**
     * 安装成功或者失败 都调用此方法。
     *
     * @param context     Context
     * @param packageName 包名
     */
    private synchronized static void handleApkInstalled(Context context, String packageName) {

        sInstallList.remove(packageName); // 从安装列表中删除

        // 检查内置app是否安装完成, 
        // 有可能是单个 apk 调用安装的，而不是调用全部安装，所以需要判断sInstallBuildinAppsCalled 来决定是否设置内置app安装完成
        if (sInstallBuildinAppsCalled && !sInstallBuildinAppsFinished) {
            if (sInstallList.isEmpty()) {
                setInstallBuildinAppsFinished(context, true);
            } else {
                boolean hasAssetsFileInstalling = false;
                for (String pkg : sInstallList) {
                    if (sBuildinAppList.contains(pkg)) {
                        hasAssetsFileInstalling = true;
                        break;
                    }
                }
                if (!hasAssetsFileInstalling) {
                    setInstallBuildinAppsFinished(context, true);
                }
            }
        }

        sBuildinAppList.remove(packageName);
    }


    /**
     * 内置app安装处理逻辑完成，有可能是检查不需要安装，有可能是实际安装完成。
     *
     * @param context          Context
     * @param writeVersionCode 是否是真的发生了实际安装，而不是检查完毕不需要安装，如果版本号不一致，也需要记录。
     */
    private static void setInstallBuildinAppsFinished(Context context, boolean writeVersionCode) {
        sInstallBuildinAppsFinished = true;

        if (writeVersionCode) {
            // 获取 hostapp verison code
            int hostVersionCode = -1;
            try {
                hostVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            } catch (NameNotFoundException e1) {
                if (DEBUG) {
                    e1.printStackTrace();
                }
            }
            // 保存当前的 hostapp verisoncode。 使用hostapp 默认 sharedpref，减少初始化开销。
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            //context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putInt(SP_HOSTAPP_VERSIONCODE_FOR_INSTALL, hostVersionCode);
            editor.commit();
        }
    }

    /**
     * 删除安装包的安装文件，apk，dex，so。
     *
     * @param context     app context
     * @param packageName 包名
     * @param nextDelete  是否需要下次卸载
     */
    public static void deletePackage(Context context, String packageName, boolean nextDelete) {
        // 统计
        ReportManger.getInstance().onUninstallPluginStart(context, packageName);
        Intent intent = new Intent(ApkInstallerService.ACTION_UNINSTALL);
        intent.setClass(context, ApkInstallerService.class);
        intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, packageName);
        intent.putExtra(ApkInstallerService.EXTRA_NEXT_DELETE, nextDelete);
        context.startService(intent);
    }


    /**
     * 查看某个app是否正在安装
     *
     * @param packageName 包名
     * @return true或false
     */
    public static synchronized boolean isInstalling(String packageName) {
        return sInstallList.contains(packageName);
    }

    /**
     * 第二次启动生效，切换安装目录
     *
     * @param context        Context
     * @param packageName    包名
     * @param tempInstallDir 临时安装目录
     * @param tempApkPath    临时安装文件
     * @param srcApkPath     源文件
     */
    public static void switchInstallDir(Context context, String packageName, String tempInstallDir, String tempApkPath,
                                        String srcApkPath) {
        registerInstallderReceiver(context);
        add2InstallList(packageName); // 添加到安装列表中
        Intent intent = new Intent(ApkInstallerService.ACTION_SWITCH_INSTALL_DIR);
        intent.setClass(context, ApkInstallerService.class);
        intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, packageName);
        intent.putExtra(GPTPackageManager.EXTRA_TEMP_INSTALL_DIR, tempInstallDir);
        intent.putExtra(GPTPackageManager.EXTRA_TEMP_APK_PATH, tempApkPath);
        intent.putExtra(GPTPackageManager.EXTRA_SRC_PATH_WITH_SCHEME, srcApkPath);

        context.startService(intent);
    }

    /**
     * 检查临时安装目录
     *
     * @param context Context
     */
    public static void checkInstallTempDir(Context context) {
        Intent intent = new Intent(ApkInstallerService.ACTION_CHECK_INSTALL_TEMP_DIR);
        intent.setClass(context, ApkInstallerService.class);
        context.startService(intent);
    }
}
