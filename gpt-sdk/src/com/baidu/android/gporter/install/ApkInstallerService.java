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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.GPTClassLoader;
import com.baidu.android.gporter.IHostBinder;
import com.baidu.android.gporter.MainProcessService;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.install.ApkCompatLiteParser.ApkLiteInfo;
import com.baidu.android.gporter.plug.ApkTargetMapping;
import com.baidu.android.gporter.pm.GPTPackageDataModule;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.ContentProviderProxy;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.SignatureParser;
import com.baidu.android.gporter.util.Util;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * apk 安装service，从srcfile安装到destfile，并且安装so，以及dexopt。 因为android4.1
 * 以下系统dexopt会导致线程hang住无法返回，所以我们放到了一个独立进程，减小概率。
 * dexopt系统bug：http://code.google.com/p/android/issues/detail?id=14962
 *
 * @author liuhaitao
 * @since 2014年5月5日
 */
public class ApkInstallerService extends IntentService {

    /**
     * 权限 owner rwx
     */
    private static final int S_IRWXU = 00700;
    /**
     * 权限 group rwx
     */
    private static final int S_IRWXG = 00070;
    /**
     * 权限 other --x
     */
    private static final int S_IXOTH = 00001;

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ApkInstallerService";

    /**
     * 安装
     */
    public static final String ACTION_INSTALL = "com.baidu.android.porter.action.install";
    /**
     * 卸载
     */
    public static final String ACTION_UNINSTALL = "com.baidu.android.porter.action.uninstall";
    /**
     * 切换安装目录
     */
    public static final String ACTION_SWITCH_INSTALL_DIR = "com.baidu.android.porter.action.switch_install_dir";
    /**
     * 检查临时目录，删除无效的临时目录
     */
    public static final String ACTION_CHECK_INSTALL_TEMP_DIR = "com.baidu.android.porter.action.check_install_temp_dir";

    /**
     * 保存签名文件，主要在覆盖安装的时候调用。2.2.2以前的版本，签名没有写文件
     */
    public static final String ACTION_SAVE_SIGNATURES_FILE = "com.baidu.android.porter.action.save_signatures_file";

    /**
     * apk 中 lib 目录的前缀标示。比如 lib/x86/libshare_v2.so
     */
    public static String APK_LIB_DIR_PREFIX = "lib/";
    /**
     * lib中so后缀
     */
    public static final String APK_LIB_SUFFIX = ".so";
    /**
     * lib目录的 cpu abi 起始位置。比如 x86 的起始位置
     */
    public static int APK_LIB_CPUABI_OFFSITE = APK_LIB_DIR_PREFIX.length();

    /**
     * dex后缀
     */
    public static final String DEX_SUFFIX = ".dex";
    /**
     * 每个插件app的签名的存储文件
     */
    public static final String FILE_SIGNATURE = "Signature";
    /**
     * 需要保存的签名信息
     */
    public static final String EXTRA_SIGNATURES_MAP = "signatures_map";
    /**
     * 是否下次删除
     */
    public static final String EXTRA_NEXT_DELETE = "next_delete";
    /**
     * 临时安装文件的标记
     */
    public static final String GPT_TEMP_TEXT = "_gpt_temp_";

    /**
     * 构造方法
     */
    public ApkInstallerService() {
        super(ApkInstallerService.class.getSimpleName());
    }

    /**
     * GPTPackageDataModule
     */
    private GPTPackageDataModule mPackageDataModule;
    /**
     * bind 主进程的 ServiceConnection
     */
    private ServiceConnection mainConnection = null;
    /**
     * 主进程的IHostBinder
     */
    private IHostBinder mHostBinder = null;


    /**
     * @param name name
     */
    public ApkInstallerService(String name) {
        super(name);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        /*
         * 插件安装进程有可能在后台被系统杀死。安装中断，所以需要重启，并重新传递intent。
         */
        mPackageDataModule = GPTPackageDataModule.getInstance(getApplicationContext());
        setIntentRedelivery(true);
        bindMainProcess();
    }

    /**
     * bind 主进程
     */
    private void bindMainProcess() {
        Intent intent = new Intent();
        intent.setClass(this, MainProcessService.class);

        mainConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mHostBinder = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mHostBinder = IHostBinder.Stub.asInterface(service);
            }
        };

        getApplicationContext().bindService(intent, mainConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        /*
         * deleted by caohaitao 20150902
         * 由于需求不在使用这种setforeground的方式。因为产品上很难接受。
         * 所以service采用 deliver intent sticky 模式。被杀死后重启，然后继续完成任务。
         * 去host读取meta里面的接口，判断是否要前台显示
        IInstallerNotificationCreator creator = null;
        Object obj = Util.getHostMetaDataClassInstance(getApplicationContext(),
                IInstallerNotificationCreator.MATA_DATA_NOTI_CREATOR_CLASS);
        if (obj instanceof IInstallerNotificationCreator) {
            if (DEBUG) {
                Log.d(TAG, "host IInstallerNotificationCreator class : " + obj.getClass().getName());
            }
            creator = (IInstallerNotificationCreator) obj;
        }

        if (creator != null) {
            startForeground(IInstallerNotificationCreator.INSTALLER_NOTI_ID,
                    creator.createNotification(getApplicationContext()));
        }
        
        */

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unBind();
        super.onDestroy();
        // 退出时结束进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void unBind() {
        try {
            mHostBinder = null;
            getApplicationContext().unbindService(mainConnection);
        } catch (IllegalArgumentException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (action.equals(ACTION_INSTALL)) {
            String srcFile = intent.getStringExtra(GPTPackageManager.EXTRA_SRC_FILE);
            String pkgName = intent.getStringExtra(GPTPackageManager.EXTRA_PKG_NAME);
            int extProcess = intent.getIntExtra(GPTPackageManager.EXTRA_EXT_PROCESS, Constants.GPT_PROCESS_DEFAULT);
            if (!TextUtils.isEmpty(srcFile)) {
                handleInstall(srcFile, pkgName, extProcess);
            }
        } else if (action.equals(ACTION_SAVE_SIGNATURES_FILE)) {
            HashMap<String, String> signaturesMap = (HashMap<String, String>) intent
                    .getSerializableExtra(EXTRA_SIGNATURES_MAP);
            handleSaveSignaturesFile(signaturesMap);
        } else if (action.equals(ACTION_UNINSTALL)) {
            String uninstallPkgName = intent.getStringExtra(GPTPackageManager.EXTRA_PKG_NAME);
            boolean nextDelete = intent.getBooleanExtra(EXTRA_NEXT_DELETE, false);
            handleUninstall(uninstallPkgName, nextDelete);
        } else if (action.equals(ACTION_SWITCH_INSTALL_DIR)) {
            String packageName = intent.getStringExtra(GPTPackageManager.EXTRA_PKG_NAME);
            String tempInstallDir = intent.getStringExtra(GPTPackageManager.EXTRA_TEMP_INSTALL_DIR);
            String tempApkPath = intent.getStringExtra(GPTPackageManager.EXTRA_TEMP_APK_PATH);
            String srcApkPath = intent.getStringExtra(GPTPackageManager.EXTRA_SRC_PATH_WITH_SCHEME);
            handleSwitchInstallDir(packageName, tempInstallDir, tempApkPath, srcApkPath);
        } else if (action.equals(ACTION_CHECK_INSTALL_TEMP_DIR)) {
            handlleCheckInstallTempDir();
        }
    }

    /**
     * handleInstall
     */
    private void handleInstall(String srcFile, String pkgName, int extProcess) {
        ReportManger.getInstance().onInstallStart(this, pkgName);
        if (srcFile.startsWith(GPTPackageManager.SCHEME_ASSETS)) {
            installBuildinApk(srcFile, pkgName);
        } else if (srcFile.startsWith(GPTPackageManager.SCHEME_FILE)) {
            installAPKFile(srcFile, pkgName, extProcess);
        }
    }

    /**
     * 处理保存签名文件
     *
     * @param signaturesMap 签名信息
     */
    private void handleSaveSignaturesFile(HashMap<String, String> signaturesMap) {
        if (signaturesMap == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "handleSaveSignaturesFile");
        }
        Set<String> keys = signaturesMap.keySet();
        for (String packageName : keys) {
            File pkgDir = new File(ApkInstaller.getGreedyPorterRootPath(this), packageName);
            File signaturesFile = new File(pkgDir, ApkInstallerService.FILE_SIGNATURE);
            Util.writeToFile(signaturesMap.get(packageName), signaturesFile);
        }
    }

    /**
     * 安装内置的apk
     *
     * @param assetsPathWithScheme assets 目录
     * @param pkgName              包名
     */
    private void installBuildinApk(String assetsPathWithScheme, String pkgName) {

        String assetsPath = assetsPathWithScheme.substring(GPTPackageManager.SCHEME_ASSETS.length());
        // 先把 asset 拷贝到临时文件。
        InputStream is = null;
        try {
            is = this.getAssets().open(assetsPath);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            setInstallFail(assetsPathWithScheme, pkgName, GPTPackageManager.VALUE_READ_FAIL);
            return;
        }

        doInstall(is, assetsPathWithScheme, pkgName, Constants.GPT_PROCESS_DEFAULT);
        try {
            is.close();
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安装一个普通的文件 apk，用于外部或者下载后的apk安装。
     *
     * @param apkFilePathWithScheme 文件绝对目录
     * @param pkgName               包名
     * @param extProcess            进程标识
     */
    private void installAPKFile(String apkFilePathWithScheme, String pkgName, int extProcess) {

        String apkFilePath = apkFilePathWithScheme.substring(GPTPackageManager.SCHEME_FILE.length());

        File source = new File(apkFilePath);
        InputStream is = null;
        try {
            is = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        doInstall(is, apkFilePathWithScheme, pkgName, extProcess);

        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * doInstall。
     *
     * @param is                InputStream
     * @param srcPathWithScheme 文件路径
     * @param pkgName           包名
     * @param extProcess        进程标识
     */
    private String doInstall(InputStream is, String srcPathWithScheme, String pkgName, int extProcess) {
        if (DEBUG) {
            Log.d(TAG, "--- doInstall : srcPathWithScheme = " + srcPathWithScheme
                    + ", pkgName=" + pkgName + ", exProcess=" + extProcess);
        }

        if (is == null || srcPathWithScheme == null) {
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_INVALID_PATH);
            return null;
        }

        File tempApkFile = new File(ApkInstaller.getGreedyPorterRootPath(this),
                pkgName + GPT_TEMP_TEXT + System.currentTimeMillis() + ".apk");
        boolean result = Util.copyToFile(is, tempApkFile);

        if (!result) {
            tempApkFile.delete();
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_COPY_FAIL);
            return null;
        }

        PackageManager pm = this.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(tempApkFile.getAbsolutePath(),
                PackageManager.GET_PROVIDERS | PackageManager.GET_PERMISSIONS
                        | PackageManager.GET_META_DATA);
        if (pkgInfo == null) {
            tempApkFile.delete();
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_PARSE_FAIL);
            return null;
        }

        boolean permissionOk = verifyPermission(pkgInfo);
        if (DEBUG) {
            Log.d(TAG, "--- verifyPermission : " + permissionOk);
        }
        if (!permissionOk) {
            tempApkFile.delete();
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_PERMISSION_EXCEED);
            return null;
        }

        String packageName = pkgInfo.packageName;

        // 校验签名
        boolean isSignatureValid = verifySignature(packageName, tempApkFile.getAbsolutePath(), pkgInfo);
        if (DEBUG) {
            Log.d(TAG, "--- Signature checked : " + isSignatureValid);
        }

        if (!isSignatureValid) {
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_SIGNATURE_NOT_MATCH);
            return null;
        }

        // 如果是内置app，检查文件名是否以包名命名，处于效率原因，要求内置app必须以包名命名。
        if (srcPathWithScheme.startsWith(GPTPackageManager.SCHEME_ASSETS)) {
            int start = srcPathWithScheme.lastIndexOf("/");
            int end = srcPathWithScheme.lastIndexOf(ApkInstaller.APK_SUFFIX);
            String fileName = srcPathWithScheme.substring(start + 1, end);

            if (!packageName.equals(fileName)) {
                tempApkFile.delete();
                throw new RuntimeException(srcPathWithScheme + " must be named with it's package name : "
                        + packageName + ApkInstaller.APK_SUFFIX);
            }
        }

        // 先安装到temp目录下
        File tempPkgDir = new File(ApkInstaller.getGreedyPorterRootPath(this),
                packageName + GPT_TEMP_TEXT + System.currentTimeMillis());
        tempPkgDir.mkdir();
        if (DEBUG) {
            Log.i(TAG, "tempPkgDir:" + tempPkgDir);
        }
        try {
            JavaCalls.callStaticMethodOrThrow("android.os.FileUtils", "setPermissions", new Object[]{
                    tempPkgDir.getPath(), S_IRWXU | S_IRWXG | S_IXOTH, -1, -1});
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            ReportManger.getInstance().onException(getApplicationContext(),
                    packageName, Util.getCallStack(e),
                    ExceptionConstants.TJ_78730003);
        }

        File libDir = new File(tempPkgDir, ApkInstaller.NATIVE_LIB_PATH_GPT + System.currentTimeMillis());

        libDir.mkdirs();
        boolean libInstalled = installNativeLibrary(this, tempApkFile.getAbsolutePath(), libDir.getAbsolutePath());
        if (!libInstalled) {
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_INSTALL_LIBRARY_FAILED);
            return null;
        }


        // 存储签名
        String signaturesFilePath;
        try {
            signaturesFilePath = saveSignatures(tempPkgDir, pkgInfo);
        } catch (IllegalStateException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_SIGNATURE_ERROR);
            return null;
        }
        // 切换安装路径
        InstallFile installFile = switchInstallDir(tempPkgDir, tempApkFile, pkgInfo, srcPathWithScheme, extProcess);
        // 插件正在使用中，路径切换失败，下次切换
        if (installFile == null) {
            return null;
        }
        // 更新签名文件路径
        File signaturesFile = new File(installFile.destInstallDir, FILE_SIGNATURE);
        if (!signaturesFile.exists()) {
            setInstallFail(srcPathWithScheme, pkgName, GPTPackageManager.VALUE_SIGNATURE_ERROR);
            return null;
        }
        signaturesFilePath = signaturesFile.getAbsolutePath();

        // 存储 provider 信息
        ContentProviderProxy.removeProviders(this, packageName);
        ContentProviderProxy.addProviders(this, pkgInfo.providers);
        // 发送安装成功的消息
        sendInstallSucc(pkgInfo, installFile.destApkFile, srcPathWithScheme, signaturesFilePath, extProcess);
        ReportManger.getInstance().onInstallSuccess(this, pkgInfo.packageName);
        return packageName;
    }

    /**
     * 创建插件组件的派生类
     *
     * @param cp
     *            classpool
     * @param rootDir
     *            插件数据根目录
     * @param orignalClass
     *            原始的组件类名
     * @param in
     *            派生类源码文件流
     * @param customNewClass
     *            自定新的类类名
     * @return 创建成功的派生类文件
     * @throws IOException
     *             IO异常
     * @throws CannotCompileException
     *             派生类编译失败异常
     * @throws NotFoundException
     *             类找不到异常
     */
    /* public static File generateGptClassFile(ClassPool cp, File rootDir, String orignalClass, InputStream in,
            String customNewClass) {

        String ctClassName = null;
        if (!TextUtils.isEmpty(customNewClass)) {
            ctClassName = customNewClass;
        } else {
            ctClassName = getGptComponentName(orignalClass);
        }

        CtClass ctclass = null;
        try {
            if (in != null) {
                in.reset();
                ctclass = cp.makeClass(in);
                ctclass.setName(ctClassName);
            } else {
                ctclass = cp.makeClass(ctClassName);
            }
            ctclass.setSuperclass(cp.get(orignalClass));
            ctclass.writeFile(rootDir.getAbsolutePath());
            ctclass.detach(); // 修复 javassist 内存占用过大问题
            File classFile = new File(rootDir, ctClassName.replace(".", File.separator) + ".class");

            return classFile;
        } catch (IOException e) {
            Log.w(TAG, "### Get Gpt class fail(IOE), class = " + orignalClass);
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (CannotCompileException e) {
            Log.w(TAG, "### Get Gpt class fail(CCE), class = " + orignalClass);
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (NotFoundException e) {
            Log.w(TAG, "### Get Gpt class fail(NFE), class = " + orignalClass);
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return null;
    } */

    /**
     * 创建插件组件的派生类
     *
     * @param cp
     *            classpool
     * @param rootDir
     *            插件数据根目录
     * @param orignalClass
     *            原始的组件类名
     * @param in
     *            派生类源码文件流
     * @return 创建成功的派生类文件
     * @throws IOException
     *             IO异常
     * @throws CannotCompileException
     *             派生类编译失败异常
     * @throws NotFoundException
     *             类找不到异常
     */
    /*
    public static File generateGptClassFile(ClassPool cp, File rootDir, String orignalClass, InputStream in) {
        return generateGptClassFile(cp, rootDir, orignalClass, in, null);
    }*/


    /**
     * 发送安装失败的广播
     *
     * @param srcPathWithScheme 安装文件路径
     * @param pkgName           包名
     * @param failReason        失败原因
     */
    private void setInstallFail(String srcPathWithScheme, String pkgName, String failReason) {
        notifyInstallFailed(this, srcPathWithScheme, pkgName, failReason);
        ReportManger.getInstance().onInstallFail(this, pkgName, failReason);
    }

    /**
     * 发送安装失败广播
     *
     * @param context
     * @param srcPathWithScheme
     * @param pkgName
     * @param failReason
     */
    public static void notifyInstallFailed(Context context, String srcPathWithScheme, String pkgName,
                                           String failReason) {
        Intent intent = new Intent(GPTPackageManager.ACTION_PACKAGE_INSTALLFAIL);
        intent.setPackage(context.getPackageName());
        intent.putExtra(GPTPackageManager.EXTRA_SRC_FILE, srcPathWithScheme); // 同时返回安装前的安装文件目录。
        intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, pkgName);
        intent.putExtra(GPTPackageManager.EXTRA_FAIL_REASON, failReason);
        context.sendBroadcast(intent);
    }

    /**
     * 从 lib 目录下找到最匹配的 abi 目录
     */
    private static String findTargetAbi(Context context, String apkFilePath) {
        String targetAbi = "armeabi"; // 默认 abi

        String[] supportedAbis = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 5.0 以及以上
            supportedAbis = Build.SUPPORTED_ABIS;
        } else {
            supportedAbis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(apkFilePath);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        HashSet<String> abisHash = new HashSet<String>();

        // 找到包中lib 目录所支持的 abi
        if (zipFile != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ZipEntry entry;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (DEBUG) {
                    Log.d(TAG, "findTargetAbi(): entry = entries.nextElement() = " + entry.toString());
                }

                // 比如 lib/x86/libshare_v2.so
                String name = entry.getName();

                // 不是 lib 目录 继续
                if (!name.startsWith(APK_LIB_DIR_PREFIX) || !name.endsWith(APK_LIB_SUFFIX)) {
                    continue;
                }

                int lastSlash = name.lastIndexOf("/");
                String targetCupAbi = name.substring(APK_LIB_CPUABI_OFFSITE, lastSlash);
                if (DEBUG) {
                    Log.d(TAG, "targetCupAbi = " + targetCupAbi);
                }

                if (!abisHash.contains(targetCupAbi)) {
                    abisHash.add(targetCupAbi);
                }
            }
        }

        try {
            zipFile.close();
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        boolean isHost64Bit = Util.is64BitInstructionSet(Util.getPrimaryInstructionSet(context.getApplicationInfo()));
        // 按优先级查找。
        for (String abi : supportedAbis) {
            if (abisHash.contains(abi)) {
                boolean isTarget64bit = Util.is64BitInstructionSet(Util.getInstructionSet(abi));
                if (isHost64Bit != isTarget64bit) {
                    continue;
                }
                targetAbi = abi;
                break;
            }
        }

        if (DEBUG) {
            Log.d(TAG, "return: targetAbi = " + targetAbi);
        }
        return targetAbi;
    }

    /**
     * 安装apk中的so库。
     *
     * @param context     Context
     * @param apkFilePath 文件路径
     * @param libDir      lib目录
     * @return true or false
     */
    private static boolean installNativeLibrary(Context context, String apkFilePath, String libDir) {

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(apkFilePath);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (zipFile == null) {
            return false;
        }

        String targetAbi = findTargetAbi(context, apkFilePath);

        boolean isTarget64bit = Util.is64BitInstructionSet(Util.getInstructionSet(targetAbi));
        boolean isHost64Bit = Util.is64BitInstructionSet(Util.getPrimaryInstructionSet(context.getApplicationInfo()));

        // 注意细节:abi 不一样，运行时无法正确加载。所以直接安装失败。
        if (isTarget64bit != isHost64Bit) {
            if (DEBUG) {
                Log.e(TAG, "主程序和插件程序的cpuabi不一致，安装 so 失败。");
            }
            return false;
        }

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            if (DEBUG) {
                Log.d(TAG, "installNativeLibrary(): entry = entries.nextElement() = "
                        + entry.toString());
            }

            // 比如 lib/x86/libshare_v2.so
            String name = entry.getName();

            // 不是 lib 目录 继续
            if (!name.startsWith(APK_LIB_DIR_PREFIX) || !name.endsWith(APK_LIB_SUFFIX)) {
                continue;
            }

            int lastSlash = name.lastIndexOf("/");
            String abi = name.substring(APK_LIB_CPUABI_OFFSITE, lastSlash);
            if (DEBUG) {
                Log.d(TAG, "installNativeLibrary(): targetAbi = " + targetAbi + ", abi = " + abi);
            }

            if (!targetAbi.equals(abi)) {
                continue;
            }

            // 进行拷贝。
            try {
                InputStream entryIS = zipFile.getInputStream(entry);
                String soFileName = name.substring(lastSlash);
                Util.copyToFile(entryIS, new File(libDir, soFileName));
                entryIS.close();

            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        try {
            zipFile.close();
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return true;
    }


    /**
     * 初始化 dex，因为第一次loaddex，如果放hostapp进程，有可能会导致hang住(参考类的说明)。所以在安装阶段独立进程中执行。
     *
     * @param apkFile     APK文件
     * @param pkgDir      安装目录
     * @param packageName 插件包名
     * @param pkgInfo     插件信息
     * @return true or false
     */
    private boolean installDex(String apkFile, String pkgDir, String packageName, PackageInfo pkgInfo) {

        // odex存放的路径也固定在子目录，不可跟宿主的data使用一个目录
        // File dexDir = new File(ApkInstaller.getGreedyPorterRootPath(this), packageName);
        try {

            // 先删除已经存在的 packagename.dex
            File oldDexFile = new File(pkgDir, packageName + DEX_SUFFIX);
            if (oldDexFile.exists()) {
                oldDexFile.delete();
            }

            // android 2.3以及以上会执行dexopt，2.2以及下不会执行。需要额外主动load一次类才可以。
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                // 2.2 以及一下需要调用 dexopt
                dalvik.system.DexFile.loadDex(apkFile, oldDexFile.getAbsolutePath(), 0);
            } else {
                // 重新通过dexopt重新生成dex
                ClassLoader classloader = new GPTClassLoader(apkFile, pkgDir, null,
                        getClassLoader().getParent(), getClassLoader()); // 构造函数会执行loaddex底层函数。
            }

            return true;
        } catch (Exception e1) {
            if (DEBUG) {
                e1.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 切换安装路径，从临时路径切换到正式路径
     *
     * @param tempPkgDir        临时安装路径
     * @param tempApkFile       临时安装文件
     * @param pkgInfo           PackageInfo
     * @param srcPathWithScheme 原安装文件地址
     * @param extProcess        插件安装的扩展进程
     * @return 正式安装文件 or null
     */
    private InstallFile switchInstallDir(File tempPkgDir, File tempApkFile, PackageInfo pkgInfo,
                                         String srcPathWithScheme, int extProcess) {
        boolean isTargetLoaded = false;
        if (mHostBinder != null) {
            try {
                isTargetLoaded = mHostBinder.isTargetLoaded(pkgInfo.packageName);
            } catch (RemoteException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        if (isTargetLoaded) {
            GPTPackageInfo gptPackageInfo = mPackageDataModule.getInstalledPkgsInstance().get(pkgInfo.packageName);
            // 下次启动的时候切换文件夹，完成安装。保存到文件中。
            gptPackageInfo.state = GPTPackageInfo.STATE_NEED_NEXT_SWITCH_INSTALL_FILE;
            gptPackageInfo.tempInstallDir = tempPkgDir.getAbsolutePath();
            gptPackageInfo.tempApkPath = tempApkFile.getAbsolutePath();
            gptPackageInfo.srcPathWithScheme = srcPathWithScheme;
            gptPackageInfo.extProcess = extProcess;
            mPackageDataModule.updatePackageList();
            if (DEBUG) {
                Log.i(TAG, "switchInstallDir(): switchInstallDir next switch install file");
            }
            ReportManger.getInstance().onNextSwitchInstallDir(this, pkgInfo.packageName);

            return null;
        }

        File destFile = getPreferedInstallLocation(tempApkFile.getPath(), pkgInfo);
        if (destFile.exists()) {
            destFile.delete();
        }

        // 生成安装文件
        if (tempApkFile.getParent().equals(destFile.getParent())) {
            // 目标文件和临时文件在同一目录下
            tempApkFile.renameTo(destFile);
        } else {
            // 拷贝到其他目录，比如安装到 sdcard
            boolean tempResult = Util.copyToFile(tempApkFile, destFile);
            if (!tempResult) {
                switchInstallDirFail(tempPkgDir, tempApkFile, pkgInfo, srcPathWithScheme);
                return null;
            }
        }
        if (!destFile.exists()) {
            switchInstallDirFail(tempPkgDir, tempApkFile, pkgInfo, srcPathWithScheme);
            return null;
        }

        // 删除原来的文件。重命名文件夹。
        File pkgDir = new File(ApkInstaller.getGreedyPorterRootPath(this), pkgInfo.packageName);
        if (pkgDir.exists()) {
            try {
                // 先删除老的gptlib目录，再copy过去
                String oldLibPath = ProxyEnvironment.getTargetLibPath(this, pkgInfo.packageName);
                Util.deleteDirectory(new File(oldLibPath));
                Util.copyFolder(tempPkgDir.getPath(), pkgDir.getPath());
                // 删除temp目录
                Util.deleteDirectory(tempPkgDir);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                switchInstallDirFail(tempPkgDir, tempApkFile, pkgInfo, srcPathWithScheme);
                return null;
            }
        } else {
            tempPkgDir.renameTo(pkgDir);
        }
        if (!pkgDir.exists()) {
            switchInstallDirFail(tempPkgDir, tempApkFile, pkgInfo, srcPathWithScheme);
        }

        InstallFile installFile = new InstallFile();
        installFile.destApkFile = destFile;
        installFile.destInstallDir = pkgDir;

        // dexopt
        boolean dexResult = installDex(destFile.getAbsolutePath(), pkgDir.getAbsolutePath(), pkgInfo.packageName,
                pkgInfo);
        if (!dexResult) {
            setInstallFail(srcPathWithScheme, pkgInfo.packageName, GPTPackageManager.VALUE_COPY_FAIL);
            return null;
        }

        if (DEBUG) {
            Log.d(TAG, "switchInstallDir(): return: installFile = " + installFile);
        }

        return installFile;
    }


    /**
     * 文件夹目录切换失败的处理
     *
     * @param tempPkgDir        临时安装路径
     * @param tempApkFile       临时安装文件
     * @param pkgInfo           PackageInfo
     * @param srcPathWithScheme 原安装文件地址
     */
    private void switchInstallDirFail(File tempPkgDir, File tempApkFile, PackageInfo pkgInfo,
                                      String srcPathWithScheme) {
        try {
            Util.deleteDirectory(tempPkgDir);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        tempApkFile.delete();
        GPTPackageInfo gptPackageInfo = mPackageDataModule.getInstalledPkgsInstance().get(pkgInfo.packageName);
        if (gptPackageInfo != null) {
            // 把数据状态恢复到默认状态
            gptPackageInfo.state = GPTPackageInfo.STATE_NORMAL;
            gptPackageInfo.tempInstallDir = "";
            gptPackageInfo.tempApkPath = "";
            gptPackageInfo.srcPathWithScheme = srcPathWithScheme;
            mPackageDataModule.updatePackageList();
        }
        // 发送失败的消息
        setInstallFail(srcPathWithScheme, pkgInfo.packageName, GPTPackageManager.VALUE_COPY_FAIL);
        if (DEBUG) {
            Log.d(TAG, "switch install dir fail, delete temp file and reset pkgInfo data");
        }
    }

    /**
     * 获取安装路径，可能是外部 sdcard或者internal data dir
     *
     * @param apkPath apk路径
     * @param pkgInfo PackageInfo
     * @return 返回插件安装位置，非空
     */
    private File getPreferedInstallLocation(String apkPath, PackageInfo pkgInfo) {
        boolean preferExternal = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            ApkLiteInfo apkLiteInfo = new ApkCompatLiteParser().parsePackageLite(apkPath);
            int installLocation = ApkLiteInfo.INSTALL_LOCATION_INTERNAL_ONLY;
            if (apkLiteInfo != null) {
                installLocation = apkLiteInfo.installLocation;
            }
            if (DEBUG) {
                Log.d(TAG, "getPreferedInstallLocation(): installLocation=" + installLocation);
            }
            if (installLocation == ApkLiteInfo.INSTALL_LOCATION_PREFER_EXTERNAL) {
                preferExternal = true;
            }
        } else {
            // android 2.1 以及以下不支持安装到sdcard
            preferExternal = false;
        }

        // 查看外部存储器是否可用
        if (preferExternal) {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) { // 不可用
                preferExternal = false;
            }
        }

        File destFile = null;
        if (!preferExternal) {
            // 默认安装到 internal data dir
            destFile = new File(ApkInstaller.getGreedyPorterRootPath(this), pkgInfo.packageName
                    + ApkInstaller.APK_SUFFIX);
        } else {
            // 安装到外部存储器
            destFile = new File(getExternalFilesDir(ApkInstaller.PLUGIN_PATH), pkgInfo.packageName
                    + ApkInstaller.APK_SUFFIX);
        }
        if (DEBUG) {
            Log.d(TAG, "getPreferedInstallLocation(): destFile=" + destFile);
        }
        return destFile;
    }

    /**
     * 安装时进行签名的校验
     *
     * @param packageName 包名
     * @param newFilePath 文件路径
     * @param pkgInfo     PackageInfo
     * @return 校验成功返回true
     */
    private boolean verifySignature(String packageName, String newFilePath, PackageInfo pkgInfo) {
        // 校验签名
        // 新文件如果没有签名不能安装
        Signature[] newSignatures = SignatureParser.collectCertificates(newFilePath);
        if (newSignatures == null) {
            if (DEBUG) {
                Log.e(TAG, "*** install fail : no signature!!!");
            }
            return false;
        }
        pkgInfo.signatures = newSignatures;

        // 如果存在老的安装包。
        File oldApkFile = ApkInstaller.getInstalledApkFile(getApplicationContext(), packageName);

        // 判断是否覆盖安装，如果是，计算旧版本的签名
        boolean isReplace = false;
        Signature[] oldSignatures = null;
        if (oldApkFile != null && oldApkFile.exists()) {
            oldSignatures = SignatureParser.collectCertificates(oldApkFile.getAbsolutePath());
            isReplace = true;
        }

        if (mHostBinder != null) {
            if (DEBUG) {
                Log.d(TAG, "--- mHostSignCheck not null");
            }

            // 由主程序校验
            try {
                return mHostBinder.checkSignature(packageName, isReplace, oldSignatures, newSignatures);
            } catch (RemoteException e) {
                if (DEBUG) {
                    Util.printCallStack(e);
                    Log.e(TAG, "### install fail : Host signature check exp !!! , old=" + oldSignatures + ", new="
                            + newSignatures + ", replace=" + isReplace);
                }
                return false;
            }
        } else {
            if (oldSignatures == null // 老版本有可能没签名，这时直接允许安装，因为之前没有线上签名校验。
                    || Util.compareSignatures(oldSignatures, newSignatures) == PackageManager.SIGNATURE_MATCH) {
                return true;
            }
        }

        if (DEBUG) {
            Log.e(TAG, "### install fail : signature not match!!! , oldSignatures=" + oldSignatures + ", newSignatures="
                    + newSignatures + ", replace=" + isReplace);
        }

        return false;
    }

    /**
     * verifyPermission
     *
     * @param plugPkgInfo PackageInfo
     * @return true or false
     */
    private boolean verifyPermission(PackageInfo plugPkgInfo) {
        if (plugPkgInfo.requestedPermissions == null) {
            return true;
        }
        try {
            boolean hasAllPermission = true;
            String packageName = getPackageName();
            PackageInfo pkgInfo = getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_PERMISSIONS);
            String[] hostPermissions = pkgInfo.requestedPermissions;

            if (hostPermissions == null) {
                return false;
            }
            for (int i = 0; i < plugPkgInfo.requestedPermissions.length; i++) {
                boolean temp = false;

                for (int j = 0; j < hostPermissions.length; j++) {
                    if (hostPermissions[j].equalsIgnoreCase(plugPkgInfo.requestedPermissions[i])) {
                        temp = true;
                        break;
                    }
                }
                if (!temp) {
                    if (plugPkgInfo.permissions != null) { // 过滤一下自定义的权限
                        for (int j = 0; j < plugPkgInfo.permissions.length; j++) {
                            if (plugPkgInfo.permissions[j].name
                                    .equalsIgnoreCase(plugPkgInfo.requestedPermissions[i])) {
                                temp = true;
                                break;
                            }
                        }
                    }
                    if (temp) {
                        continue;
                    }
                    if (DEBUG) {
                        Log.w(TAG, "perission not exist:" + plugPkgInfo.requestedPermissions[i]);
                    }
                    /** 注意:暂时去掉权限校验，只输出警告信息 */
                    // hasAllPermission = false;
                }
            }
            return hasAllPermission;
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 保存插件签名，把签名信息写到文件
     *
     * @param pkgDir 插件的安装目录
     * @param pkg    PackageInfo
     * @return signaturesFile.getAbsolutePath()
     * @throws IllegalStateException
     */
    private String saveSignatures(File pkgDir, PackageInfo pkg) throws IllegalStateException {
        File signaturesFile = new File(pkgDir, FILE_SIGNATURE);
        JSONArray signatures = null;
        try {
            signatures = new JSONArray("[]");
            if (pkg.signatures != null) {
                for (int i = 0; i < pkg.signatures.length; i++) {
                    if (pkg.signatures[i] == null) {
                        continue;
                    }
                    String signatureStr = pkg.signatures[i].toCharsString();
                    if (signatureStr == null || (signatureStr.length() % 2 != 0)) {
                        throw new IllegalArgumentException("save signature error: packageName=" + pkg.packageName
                                + ", signatureStr=" + signatureStr + ", Build.MODEL=" + Build.MODEL);
                    }
                    signatures.put(pkg.signatures[i].toCharsString());
                }
            }

        } catch (JSONException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        // 把签名信息存到单独的文件中
        if (DEBUG) {
            Log.d(TAG, "signaturesFile :" + signaturesFile.getAbsolutePath());
        }
        if (signatures != null) {
            Util.writeToFile(signatures.toString(), signaturesFile);
        }
        return signaturesFile.getAbsolutePath();
    }

    /**
     * 卸载一个插件
     *
     * @param packageName 插件包名
     * @param nextDelete  下次启动的时候删除
     */
    private void handleUninstall(String packageName, boolean nextDelete) {

        ReportManger.getInstance().onUninstallStart(this, packageName);

        if (nextDelete) {
            GPTPackageInfo gptPackageInfo = mPackageDataModule.getInstalledPkgsInstance().get(packageName);
            // 下次启动的时候删除。保存到文件中。
            if (gptPackageInfo != null) {
                gptPackageInfo.state = GPTPackageInfo.STATE_NEED_NEXT_DELETE;
                mPackageDataModule.updatePackageList();
                if (DEBUG) {
                    Log.i(TAG, "handleUninstall(): handleUninstall next delete");
                }
                ReportManger.getInstance().onUninstallResult(this, packageName, 2);
            }
            return;
        }

        // 删除apk文件，因为有可能安装在sdcard，所以单独删除。
        File apkPath = ApkInstaller.getInstalledApkFile(this, packageName);
        if (apkPath != null) {
            apkPath.delete();
        }

        // 删除 dex,so 安装目录
        File dataDir = ProxyEnvironment.getDataDir(this, packageName);
        if (dataDir != null) {
            try {
                Util.deleteDirectory(dataDir);
            } catch (IOException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        ContentProviderProxy.removeProviders(this, packageName);

        mPackageDataModule.deletePackageInfo(packageName, true);
        // 发送广播
        Intent intent = new Intent(GPTPackageManager.ACTION_PACKAGE_DELETED);
        intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, packageName);
        sendBroadcast(intent);
        if (DEBUG) {
            Log.i(TAG, "handleUninstall(): handleUninstall delete success, packageName=" + packageName);
        }
        ReportManger.getInstance().onUninstallResult(this, packageName, 1);
    }

    /**
     * 发送安装成功的消息
     *
     * @param pkgInfo            PackageInfo
     * @param destFile           安装后的apk文件
     * @param srcPathWithScheme  安装前的安装文件目录。
     * @param signaturesFilePath 签名文件路径
     * @param extProcess         插件安装所在的扩展进程
     */
    private void sendInstallSucc(PackageInfo pkgInfo, File destFile, String srcPathWithScheme,
                                 String signaturesFilePath, int extProcess) {

        Intent intent = new Intent(GPTPackageManager.ACTION_PACKAGE_INSTALLED);
        intent.setPackage(getPackageName());

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0x110, new Intent(
                getApplicationContext(), ActivityProxy.class), 0);
        intent.putExtra(GPTPackageManager.EXTRA_PI, pi);

        intent.putExtra(GPTPackageManager.EXTRA_PKG_NAME, pkgInfo.packageName);
        intent.putExtra(GPTPackageManager.EXTRA_SRC_FILE, srcPathWithScheme); // 同时返回安装前的安装文件目录。
        intent.putExtra(GPTPackageManager.EXTRA_DEST_FILE, destFile.getAbsolutePath());
        intent.putExtra(GPTPackageManager.EXTRA_EXT_PROCESS, extProcess);
        intent.putExtra(GPTPackageManager.EXTRA_VERSION_CODE, pkgInfo.versionCode);
        intent.putExtra(GPTPackageManager.EXTRA_VERSION_NAME, pkgInfo.versionName);
        intent.putExtra(GPTPackageManager.EXTRA_SIGNATURES_PATH, signaturesFilePath);

        boolean unionProcess = false;
        boolean unionData = false;
        Bundle metaBundle = pkgInfo.applicationInfo.metaData;
        // 2.2读不到meta-data
        if (metaBundle == null) {
            metaBundle = ApkTargetMapping.getMeta(destFile);
        }
        if (metaBundle != null) {
            unionProcess = metaBundle.getBoolean(GPTPackageManager.META_KEY_UNION_PROCESS);
            unionData = metaBundle.getBoolean(GPTPackageManager.META_KEY_UNION_DATA);
        }

        if (DEBUG) {
            Log.i(TAG, "sendInstallSucc(): --- install pkg : " + pkgInfo.packageName
                    + ", unionProcess:" + unionProcess + ", unionData:"
                    + unionData + ", extProcess:" + extProcess);
        }

        intent.putExtra(GPTPackageManager.EXTRA_UNION_PROCESS, unionProcess);
        intent.putExtra(GPTPackageManager.EXTRA_UNION_DATA, unionData);
        // 安装成功，更新数据文件
        mPackageDataModule.addPackageInfo(pkgInfo, destFile.getAbsolutePath(), unionProcess, unionData,
                signaturesFilePath, extProcess);
        sendBroadcast(intent);
    }

    /**
     * 切换安装目录
     *
     * @param packageName    包名
     * @param tempInstallDir 临时安装目录
     * @param tempApkPath    临时安装文件
     * @param srcApkPath     源文件
     */
    private void handleSwitchInstallDir(String packageName, String tempInstallDir, String tempApkPath,
                                        String srcApkPath) {
        if (DEBUG) {
            Log.d(TAG, "handleSwitchInstallDir(): handle Switch Install Dir packageName :" + packageName);
        }
        ReportManger.getInstance().onStartSwitchInstallDir(this, packageName);
        File tempPkgDir = new File(tempInstallDir);
        File tempApkFile = new File(tempApkPath);
        if (!tempPkgDir.exists() || !tempApkFile.exists()) {
            setInstallFail(srcApkPath, packageName, GPTPackageManager.VALUE_SWTICH_INSALL_DIR_FILE_NOT_FOUND);
            return;
        }
        PackageManager pm = this.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(tempApkFile.getAbsolutePath(),
                PackageManager.GET_PROVIDERS | PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA);
        if (pkgInfo == null) {
            tempApkFile.delete();
            setInstallFail(srcApkPath, packageName, GPTPackageManager.VALUE_PARSE_FAIL);
            return;
        }

        GPTPackageInfo gptInfo = mPackageDataModule.getPackageInfo(packageName);
        int extProcess = gptInfo != null ? gptInfo.extProcess : Constants.GPT_PROCESS_DEFAULT;

        InstallFile installFile = switchInstallDir(tempPkgDir, tempApkFile, pkgInfo, srcApkPath, extProcess);
        if (installFile == null) {
            setInstallFail(srcApkPath, packageName, GPTPackageManager.VALUE_SWTICH_INSALL_DIR_FAIL);
            return;
        }
        // 更新签名文件路径
        File signaturesFile = new File(installFile.destInstallDir, FILE_SIGNATURE);
        if (!signaturesFile.exists()) {
            setInstallFail(srcApkPath, packageName, GPTPackageManager.VALUE_SIGNATURE_ERROR);
            return;
        }
        String signaturesFilePath = signaturesFile.getAbsolutePath();
        // 存储 provider 信息
        ContentProviderProxy.removeProviders(this, packageName);
        ContentProviderProxy.addProviders(this, pkgInfo.providers);
        // 发送安装成功的消息
        sendInstallSucc(pkgInfo, installFile.destApkFile, srcApkPath, signaturesFilePath, extProcess);

        ReportManger.getInstance().onStartSwitchDirSuccess(this, packageName);
        if (DEBUG) {
            Log.i(TAG, "handleSwitchInstallDir(): handle Switch Install Dir packageName :"
                    + packageName + " success ");
        }
    }

    /**
     * 检查删除临时目录
     */
    private void handlleCheckInstallTempDir() {
        Hashtable<String, GPTPackageInfo> pkgList = mPackageDataModule.getInstalledPkgsInstance();
        File rootFile = ApkInstaller.getGreedyPorterRootPath(this);
        String[] files = rootFile.list();
        if (files != null) {
            for (String f : files) {
                int index = f.lastIndexOf(GPT_TEMP_TEXT);
                if (index <= 0) {
                    continue;
                }
                String pkgName = f.substring(0, index);
                if (DEBUG) {
                    Log.i(TAG, "handlleCheckInstallTempDir(): check install temp dir : file="
                            + f.toString() + ",pkgName=" + pkgName);
                }

                if (TextUtils.isEmpty(pkgName)) {
                    continue;
                }
                boolean needDelete = false;
                GPTPackageInfo packageInfo = pkgList.get(pkgName);
                if (packageInfo == null) {
                    // GPTPackageInfo 为null。说明首次安装就没完成。
                    needDelete = true;
                } else {
                    if (packageInfo.state == GPTPackageInfo.STATE_NEED_NEXT_SWITCH_INSTALL_FILE) {
                        String path = rootFile.getAbsoluteFile() + File.separator + f;
                        if (path.equals(packageInfo.tempApkPath) || path.equals(packageInfo.tempInstallDir)) {
                            needDelete = false;
                        } else {
                            needDelete = true;
                        }
                    } else {
                        needDelete = true;
                    }
                }
                if (needDelete) {
                    File tempFile = new File(rootFile.getAbsoluteFile() + File.separator + f);
                    if (tempFile.isDirectory()) {
                        try {
                            Util.deleteDirectory(tempFile);
                            ReportManger.getInstance().onDeleteTempInstallDir(this);
                        } catch (IOException e) {
                            if (DEBUG) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        tempFile.delete();
                    }
                }
            }
        }
    }


    /**
     * 安装文件
     */
    private static class InstallFile {
        /**
         * 最终安装的APK文件
         */
        File destApkFile;
        /**
         * 最终安装的文件路径
         */
        File destInstallDir;
    }

}


