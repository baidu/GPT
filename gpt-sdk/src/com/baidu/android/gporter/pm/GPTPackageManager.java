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
package com.baidu.android.gporter.pm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.api.TargetActivator;
import com.baidu.android.gporter.install.ApkInstaller;
import com.baidu.android.gporter.install.IInstallCallBack;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * megapp package manager。
 * 负责安装卸载app，获取安装列表等工作。
 * 注意：内置app必须以package name 命名，比如 com.baidu.xxx.apk
 *
 * @author liuhaitao
 * @since 2014年5月10日
 */
public class GPTPackageManager {
    /**
     * TAG
     */
    private static final String TAG = "GPTPackageManager";

    /**
     * 安装完一个包会发送该broadcast，extra 为 {@link #EXTRA_PKG_NAME}
     * {@link#EXTRA_SRC_FILE} {@link #EXTRA_DEST_FILE}
     * {@link #EXTRA_VERSION_CODE} {@link #EXTRA_VERSION_NAME}
     */
    public static final String ACTION_PACKAGE_INSTALLED = "com.baidu.android.porter.installed";

    /**
     * 安装失败会发送一个broadcast
     */
    public static final String ACTION_PACKAGE_INSTALLFAIL = "com.baidu.android.porter.installfail";

    /**
     * 删除完一个包会发送该broadcast，extra 为 {@link #EXTRA_PKG_NAME}
     */
    public static final String ACTION_PACKAGE_DELETED = "com.baidu.android.porter.deleted";

    /**
     * 卸载失败，extra 为 {@link #EXTRA_PKG_NAME}
     */
    public static final String ACTION_PACKAGE_DELETE_FAIL = "com.baidu.android.porter.delete.fail";

    /**
     * 用于标识本应用
     */
    public static final String EXTRA_PI = "identity_pi";

    /**
     * ext_process
     */
    public static final String EXTRA_EXT_PROCESS = "ext_process";

    /**
     * 安装完的pkg的包名
     */
    public static final String EXTRA_PKG_NAME = "package_name";
    /**
     * 支持 assets:// 和 file:// 两种，对应内置和外部apk安装。
     * 比如 assets://megapp/xxxx.apk , 或者
     * file:///data/data/com.baidu.xxx/files/xxx.apk
     */
    public static final String EXTRA_SRC_FILE = "install_src_file";
    /**
     * 安装完的apk path，没有scheme 比如 /data/data/com.baidu.xxx/xxx.apk
     */
    public static final String EXTRA_DEST_FILE = "install_dest_file";

    /**
     * 安装完的pkg的 version code
     */
    public static final String EXTRA_VERSION_CODE = "version_code";
    /**
     * 安装完的pkg的 version name
     */
    public static final String EXTRA_VERSION_NAME = "version_name";
    /**
     * 安装完的pkg的 signature
     */
    public static final String EXTRA_SIGNATURES_PATH = "signatures_path";

    /**
     * 安装完的pkg是否跟主进程运行在一个进程
     */
    public static final String EXTRA_UNION_PROCESS = "union_process";
    /**
     * 安装完的pkg是否跟宿主使用相同data路径
     */
    public static final String EXTRA_UNION_DATA = "union_data";
    /**
     * 临时安装目录
     */
    public static final String EXTRA_TEMP_INSTALL_DIR = "temp_install_dir";
    /**
     * 临时安装的apk 文件
     */
    public static final String EXTRA_TEMP_APK_PATH = "temp_apk_path";
    /**
     * 安装的源文件
     */
    public static final String EXTRA_SRC_PATH_WITH_SCHEME = "src_path_with_scheme";

    /**
     * SCHEME_ASSETS
     */
    public static final String SCHEME_ASSETS = "assets://";
    /**
     * SCHEME_FILE
     */
    public static final String SCHEME_FILE = "file://";

    /**
     * 安装失败的原因
     */
    public static final String EXTRA_FAIL_REASON = "fail_reason";
    /**
     * 安装失败的原因:签名不一致
     */
    public static final String VALUE_SIGNATURE_NOT_MATCH = "signature_not_match";
    /**
     * 安装失败的原因:没有签名
     */
    public static final String VALUE_NO_SIGNATURE = "no_signature";
    /**
     * 安装失败的原因:apk解析失败
     */
    public static final String VALUE_PARSE_FAIL = "parse_fail";
    /**
     * 安装失败的原因:安装文件拷贝失败
     */
    public static final String VALUE_COPY_FAIL = "copy_fail";
    /**
     * 安装失败的原因:超时
     */
    public static final String VALUE_TIMEOUT = "time_out";
    /**
     * 安装失败的原因:权限超出
     */
    public static final String VALUE_PERMISSION_EXCEED = "permission_exceed";
    /**
     * 安装失败的原因:安装 so 失败，很可能是由于64bit机器上，主程序和插件程序的cpuabi不一致。
     */
    public static final String VALUE_INSTALL_LIBRARY_FAILED = "install_native_lib_failed";
    /**
     * 安装失败的原因:插件路径不合法
     */
    public static final String VALUE_INVALID_PATH = "invalid_path";
    /**
     * 安装失败的原因:插件读取失败
     */
    public static final String VALUE_READ_FAIL = "read_fail";
    /**
     * 安装失败的原因:第二次启动的时候，切换安装目录，但是文件找不到了
     */
    public static final String VALUE_SWTICH_INSALL_DIR_FILE_NOT_FOUND = "switch install dir,but file not found";
    /**
     * 安装失败的原因:第二次启动的时候，切换安装目录，失败
     */
    public static final String VALUE_SWTICH_INSALL_DIR_FAIL = "switch install dir fail";
    /**
     * 安装失败的原因:签名错误
     */
    public static final String VALUE_SIGNATURE_ERROR = "receive signature error";

    /**
     * meta-data key:是否跟主程序一个进程
     */
    public static final String META_KEY_UNION_PROCESS = "gpt_union_process";
    /**
     * meta-data key:是否跟宿主使用相同data路径
     */
    public static final String META_KEY_UNION_DATA = "gpt_union_data";

    /**
     * application context
     */
    private Context mContext;

    /**
     * GPTPackageManager
     */
    private static GPTPackageManager sInstance;

    /**
     * 已安装列表。 !!!!!!! 注意:不要直接引用该变量。
     * 因为该变量是 lazy init 方式，不需要的时候不进行初始化。
     * 使用 {@link #getInstalledPkgsInstance()} 获取该实例。
     */
//    private Hashtable<String, GPTPackageInfo> mInstalledPkgs;
    /**
     * GPTPackageDataModule
     */
    private GPTPackageDataModule mPackageDataModule;

    /**
     * 安装包任务队列。
     */
    private List<PackageAction> mPackageActions = new LinkedList<GPTPackageManager.PackageAction>();

    /**
     * 构造方法
     */
    private GPTPackageManager(Context context) {
        mContext = context.getApplicationContext();
        mPackageDataModule = GPTPackageDataModule.getInstance(mContext);
        boolean isMainProcess = Util.isHostProcess(context);
        if (isMainProcess) {
            checkInstallTempDir();
            checkNeedDelete();
        }
        registerInstallderReceiver();
    }


    /**
     * lazy init mInstalledPkgs 变量，没必要在构造函数中初始化该列表，减少hostapp每次初始化时的时间消耗。
     *
     * @return Hashtable<String, GPTPackageInfo>
     */
    private Hashtable<String, GPTPackageInfo> getInstalledPkgsInstance() {
        return mPackageDataModule.getInstalledPkgsInstance();
    }

    /**
     * 获取GPTPackageManager的单例。
     *
     * @return GPTPackageManager
     */
    public static synchronized GPTPackageManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GPTPackageManager(Util.getHostContext(context));
        }
        return sInstance;
    }

    /**
     * 检查是否有上次没卸载，等着这次卸载的插件。
     */
    private void checkNeedDelete() {
        if (Constants.DEBUG) {
            Log.i(TAG, "checkNeedDelete start");
        }
        Hashtable<String, GPTPackageInfo> pkgList = getInstalledPkgsInstance();
        Set<String> keys = pkgList.keySet();
        List<String> needDeleteKeys = new ArrayList<String>();
        for (String packageName : keys) {
            GPTPackageInfo packageInfo = pkgList.get(packageName);
            if (packageInfo.state == GPTPackageInfo.STATE_NEED_NEXT_DELETE) {
                if (Constants.DEBUG) {
                    Log.i(TAG, packageName + " need delete");
                }
                needDeleteKeys.add(packageName);
            }
        }
        for (String packageName : needDeleteKeys) {
            // 就怕在这之前，还有地方调用了getAppList
            // 这个时候先删数据。保证这个插件不会被加载。
            mPackageDataModule.deletePackageInfo(packageName);
            deletePackage(packageName);
        }
    }

    /**
     * 检查临时安装目录
     */
    private void checkInstallTempDir() {
        ApkInstaller.checkInstallTempDir(mContext);
    }

    /**
     * 获取安装列表。
     *
     * @return List<GPTPackageInfo>
     */
    public List<GPTPackageInfo> getInstalledApps() {
        Enumeration<GPTPackageInfo> packages = getInstalledPkgsInstance().elements();
        ArrayList<GPTPackageInfo> list = new ArrayList<GPTPackageInfo>();
        while (packages.hasMoreElements()) {
            GPTPackageInfo pkg = packages.nextElement();
            list.add(pkg);
        }

        return list;
    }

    /**
     * sApkInstallerReceiver
     */
    private BroadcastReceiver sApkInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 只在主进程统计，不然会统计两次
            boolean isMainProcess = Util.isHostProcess(context);
            if (ACTION_PACKAGE_INSTALLED.equals(action)) {

                // 非本应用发的广播不出来，防止第三方伪造信息
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                PendingIntent pi = (PendingIntent) intent.getParcelableExtra(EXTRA_PI);
                if (pi == null) {
                    if (Constants.DEBUG) {
                        Log.e(TAG, "### Install action identity miss!");
                    }

                    // 统计
                    if (isMainProcess) {
                        ReportManger.getInstance().onInstallPluginFail(context, pkgName, "", "install_unchecked");
                    }

                    return;
                }
                String createPackage = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    createPackage = pi.getCreatorPackage();
                } else {
                    createPackage = pi.getTargetPackage();
                }
                if (!TextUtils.equals(createPackage, context.getPackageName())) {
                    if (Constants.DEBUG) {
                        Log.e(TAG, "### Install action identity mismatch!");
                    }
                    // 统计
                    if (isMainProcess) {
                        ReportManger.getInstance().onInstallPluginFail(context, pkgName, "", "install_unchecked");
                    }

                    return;
                }

                String destApkPath = intent.getStringExtra(GPTPackageManager.EXTRA_DEST_FILE);
                String versionName = intent.getStringExtra(GPTPackageManager.EXTRA_VERSION_NAME);
                int versionCode = intent.getIntExtra(GPTPackageManager.EXTRA_VERSION_CODE, 0);
                boolean isUnionProcess = intent.getBooleanExtra(GPTPackageManager.EXTRA_UNION_PROCESS, false);
                boolean isUnionDataDir = intent.getBooleanExtra(GPTPackageManager.EXTRA_UNION_DATA, false);
                String signaturesFilePath = intent.getStringExtra(GPTPackageManager.EXTRA_SIGNATURES_PATH);
                int extProcess = intent.getIntExtra(GPTPackageManager.EXTRA_EXT_PROCESS, Constants.GPT_PROCESS_DEFAULT);

                GPTPackageInfo pkgInfo = new GPTPackageInfo();
                pkgInfo.packageName = pkgName;
                pkgInfo.srcApkPath = destApkPath;
                pkgInfo.versionCode = versionCode;
                pkgInfo.versionName = versionName;
                pkgInfo.isUnionProcess = isUnionProcess;
                pkgInfo.isUnionDataDir = isUnionDataDir;
                pkgInfo.signaturesFilePath = signaturesFilePath;
                pkgInfo.extProcess = extProcess;
                String str = Util.readStringFile(signaturesFilePath);
                if (!TextUtils.isEmpty(str)) {
                    try {
                        JSONArray signaturesJson = new JSONArray(str);
                        pkgInfo.signatures = new Signature[signaturesJson.length()];
                        for (int j = 0; j < pkgInfo.signatures.length; j++) {
                            String signatureStr = signaturesJson.optString(j);
                            if (signatureStr != null) {
                                pkgInfo.signatures[j] = new Signature(signatureStr);
                                if (signatureStr == null || (signatureStr.length() % 2 != 0)) {
                                    throw new IllegalArgumentException(
                                            "receive signature error: packageName=" + pkgInfo.packageName + ", signatureStr="
                                                    + signatureStr + ", Build.MODEL=" + Build.MODEL);
                                }
                            }
                            if (Constants.DEBUG) {
                                Log.i(TAG, "signatures:" + pkgInfo.signatures[j] + ",signatureStr:" + signatureStr);
                            }
                        }
                    } catch (JSONException e) {
                        if (Constants.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
                mPackageDataModule.addPackageInfo(pkgInfo);

                // 执行等待执行的action
                executePackageAction(pkgName, true, null);

                // 统计
                if (isMainProcess) {
                    ReportManger.getInstance().onInstallPluginSuccess(context, pkgName, versionCode + "");
                }
            } else if (ACTION_PACKAGE_INSTALLFAIL.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                String filePath = intent.getStringExtra(EXTRA_SRC_FILE);
                String failReason = intent.getStringExtra(EXTRA_FAIL_REASON);

                if (isMainProcess) {
                    ReportManger.getInstance().onInstallPluginFail(context,
                            TextUtils.isEmpty(pkgName) ? filePath : pkgName, "", GPTPackageManager.VALUE_PARSE_FAIL);
                }

                // install fail已经在service统计过了
                executePackageAction(pkgName, false, failReason);
            } else if (ACTION_PACKAGE_DELETED.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                if (Constants.DEBUG) {
                    Log.i(TAG, "delete pkg :" + pkgName);
                }
                if (isMainProcess) {
                    ReportManger.getInstance().onUninstallPluginResult(context, pkgName, 1);
                }
                mPackageDataModule.deletePackageInfo(pkgName);
            }
        }

    };

    /**
     * 监听安装列表变化.
     */
    private void registerInstallderReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PACKAGE_INSTALLED);
        filter.addAction(ACTION_PACKAGE_INSTALLFAIL);
        filter.addAction(ACTION_PACKAGE_DELETED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        mContext.registerReceiver(sApkInstallerReceiver, filter);
    }

    /**
     * 包依赖任务队列对象。
     */
    private class PackageAction {
        long timestamp;
        IInstallCallBack callBack;
        String packageName;
    }

    /**
     * 执行依赖于安装包的 runnable，如果该package已经安装，则立即执行。
     * 如果megapp正在初始化，或者该包正在安装，则放到任务队列中等待安装完毕执行。
     *
     * @param packageName 插件包名
     * @param callBack    插件安装回调
     */
    public void packageAction(String packageName, IInstallCallBack callBack) {

        boolean packageInstalled = isPackageInstalled(packageName);

        boolean installing = ApkInstaller.isInstalling(packageName);
        if (Constants.DEBUG) {
            Log.d(TAG, "packageAction , " + packageName + " installed : " + packageInstalled
                    + " installing: " + installing);
        }

        if (packageInstalled && (!installing)) { // 安装了，并且没有更新操作
            if (callBack != null) {
                callBack.onPacakgeInstalled(packageName);
            }

        } else {
            PackageAction action = new PackageAction();
            action.packageName = packageName;
            action.timestamp = System.currentTimeMillis();
            action.callBack = callBack;

            synchronized (this) {
                if (mPackageActions.size() < 1000) { // 防止溢出
                    mPackageActions.add(action);
                }
            }
        }

        clearExpiredPkgAction();
    }

    /**
     * 安装单个内置apk。此函数用在不需要调用 {@link #installBuildinApps()} 函数的时候，安装单个apk。
     *
     * @param packageName 包名
     * @return 如果内置的文件比较旧，不需要安装则返回false。如果进入安装过程，则返回true。如果安装过程中发生失败，则发送广播。
     */
    public boolean installBuildinApk(String packageName) {
        return ApkInstaller.installBuildinApp(mContext, ApkInstaller.ASSETS_PATH + File.separator + packageName
                + ApkInstaller.APK_SUFFIX);
    }

    /**
     * executePackageAction
     *
     * @param packageName 包名
     * @param isSuccess   true or false
     * @param failReason  失败原因
     * @return 如果内置的文件比较旧，不需要安装则返回false。如果进入安装过程，则返回true。如果安装过程中发生失败，则发送广播。
     */
    private void executePackageAction(String packageName, boolean isSuccess, String failReason) {
        ArrayList<PackageAction> executeList = new ArrayList<GPTPackageManager.PackageAction>();

        for (PackageAction action : mPackageActions) {
            if (packageName.equals(action.packageName)) {
                executeList.add(action);
            }
        }

        // 首先从总列表中删除
        synchronized (this) {
            for (PackageAction action : executeList) {
                mPackageActions.remove(action);
            }
        }

        // 挨个执行
        for (PackageAction action : executeList) {
            if (action.callBack != null) {
                if (isSuccess) {
                    action.callBack.onPacakgeInstalled(packageName);
                } else {
                    action.callBack.onPackageInstallFail(action.packageName, failReason);
                }
            }
        }
    }

    /**
     * 删除过期没有执行的 action，可能由于某种原因存在此问题。比如一个找不到package的任务。
     */
    private void clearExpiredPkgAction() {
        long currentTime = System.currentTimeMillis();

        ArrayList<PackageAction> deletedList = new ArrayList<PackageAction>();

        synchronized (this) {
            // 查找需要删除的
            for (PackageAction action : mPackageActions) {
                if (currentTime - action.timestamp >= 10 * 60 * 1000) {
                    deletedList.add(action);
                }
            }
            // 实际删除
            for (PackageAction action : deletedList) {
                mPackageActions.remove(action);
                action.callBack.onPackageInstallFail(action.packageName, VALUE_TIMEOUT);
            }
        }
    }

    /**
     * 判断一个package是否安装
     *
     * @param packageName 包名
     * @return 是否安装
     */
    public boolean isPackageInstalled(String packageName) {
        if (getInstalledPkgsInstance().containsKey(packageName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断app是否正在安装
     *
     * @param packageName packageName
     * @return 正在安装返回 true
     */
    public boolean isPackageInstalling(String packageName) {
        return ApkInstaller.isInstalling(packageName);
    }

    /**
     * 获取安装apk的信息
     *
     * @param packageName 包名
     * @return 插件信息，没有安装 返回null
     */
    public GPTPackageInfo getPackageInfo(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return null;
        }

        GPTPackageInfo info = getInstalledPkgsInstance().get(packageName);

        return info;
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 安装完会有 {@link #ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param filePath apk 文件目录 比如 /sdcard/xxxx.apk
     * @return 如果apk本身不是个合法的文件，直接返回false，不进行安装处理。同时也会发送安装失败广播。但是广播中不存在packagename,因为本身没有解析到。
     */
    public boolean installApkFile(String filePath) {
        return ApkInstaller.installApkFile(mContext, filePath);
    }

    /**
     * 安装内置在 assets/megapp 目录下的apk。 内置app必须以 packageName 命名，比如 com.baidu.xx.apk
     */
    public void installBuildinApps() {
        ApkInstaller.installBuildinApps(mContext);
    }

    /**
     * 删除安装包。
     *
     * @param packageName 需要删除的package 的 packageName
     */
    public void deletePackage(final String packageName) {
        deletePackage(packageName, false);
    }

    /**
     * 删除安装包。
     *
     * @param packageName 需要删除的package 的 packageName
     * @param force       是否强制卸载，false表示插件运行时不卸载
     */
    public void deletePackage(final String packageName, boolean force) {
        boolean nextDelete = false;
        if (!TargetActivator.unLoadTarget(packageName, force)) {
            nextDelete = true;
        }
        ApkInstaller.deletePackage(mContext, packageName, nextDelete);
    }

}


