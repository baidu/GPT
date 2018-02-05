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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.install.ApkInstaller;
import com.baidu.android.gporter.install.ApkInstallerService;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * GPT插件数据的模块
 *
 * @author liuhaitao
 * @since 2017-12-29
 */
public class GPTPackageDataModule {

    /**
     * TAG
     */
    private static final String TAG = "GPTPackageDataModule";

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * 实例
     */
    private static GPTPackageDataModule mInstance;

    /**
     * Context
     */
    private Context mContext;

    /**
     * 存储在sharedpreference的安装列表
     */
    private static final String SP_APP_LIST = "packages";


    /**
     * 已安装列表。
     * !!!!!!! 注意:不要直接引用该变量。 因为该变量是 lazy init 方式，不需要的时候不进行初始化。
     * 使用 {@link #getInstalledPkgsInstance()} 获取该实例。
     */
    private Hashtable<String, GPTPackageInfo> mInstalledPkgs;

    /**
     * 获取GPTPackageDataModule的单例
     *
     * @param ctx Context
     * @return GPTPackageDataModule
     */
    public static synchronized GPTPackageDataModule getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new GPTPackageDataModule(ctx);
        }
        return mInstance;
    }

    /**
     * GPTPackageDataModule 构造方法
     *
     * @param ctx Context
     */
    private GPTPackageDataModule(Context ctx) {
        this.mContext = ctx.getApplicationContext();
    }

    /**
     * lazy init mInstalledPkgs 变量，没必要在构造函数中初始化该列表，减少hostapp每次初始化时的时间消耗。
     *
     * @return Hashtable
     */
    public Hashtable<String, GPTPackageInfo> getInstalledPkgsInstance() {
        initInstalledPackageListIfNeeded();
        return mInstalledPkgs;
    }

    /**
     * 初始化安装列表
     */
    private synchronized void initInstalledPackageListIfNeeded() {
        // 第一次初始化安装列表。
        if (mInstalledPkgs == null) {
            mInstalledPkgs = new Hashtable<String, GPTPackageInfo>();
            String jsonPkgs = null;
            boolean needReSave = false;
            SharedPreferences sp = mContext.getSharedPreferences(ApkInstaller.SHARED_PREFERENCE_NAME,
                    Context.MODE_PRIVATE);
            jsonPkgs = sp.getString(SP_APP_LIST, "");
            if (DEBUG) {
                Log.i(TAG, "initInstalledPackageListIfNeeded(): jsonPkgs=" + jsonPkgs);
            }
            HashMap<String, String> needSaveSignaturesMap = new HashMap<String, String>();

            if (jsonPkgs != null && jsonPkgs.length() > 0) {
                try {
                    JSONArray pkgs = new JSONArray(jsonPkgs);
                    int count = pkgs.length();
                    for (int i = 0; i < count; i++) {
                        JSONObject pkg = (JSONObject) pkgs.get(i);
                        GPTPackageInfo pkgInfo = new GPTPackageInfo();
                        pkgInfo.packageName = pkg.optString(GPTPackageInfo.TAG_PKG_NAME);
                        pkgInfo.srcApkPath = pkg.optString(GPTPackageInfo.TAG_APK_PATH);
                        pkgInfo.versionCode = pkg.optInt(GPTPackageInfo.TAG_PKG_VC, 0);
                        pkgInfo.versionName = pkg.optString(GPTPackageInfo.TAG_PKG_VN);
                        pkgInfo.isUnionProcess = pkg.optBoolean(GPTPackageInfo.TAG_PROCESS, false);
                        pkgInfo.isUnionDataDir = pkg.optBoolean(GPTPackageInfo.TAG_UNION_DATA, false);
                        pkgInfo.extProcess = pkg.optInt(GPTPackageInfo.TAG_EXT_PROCESS, Constants.GPT_PROCESS_DEFAULT);
//                        pkgInfo.extProcess = Constants.GPT_PROCESS_EXT_1; // 测试代码
                        pkgInfo.state = pkg.optInt(GPTPackageInfo.TAG_STATE, GPTPackageInfo.STATE_NORMAL);
                        pkgInfo.tempInstallDir = pkg.optString(GPTPackageInfo.TAG_TEMP_INSTALL_DIR);
                        pkgInfo.tempApkPath = pkg.optString(GPTPackageInfo.TAG_TEMP_APK_PATH);
                        pkgInfo.srcPathWithScheme = pkg.optString(GPTPackageInfo.TAG_SRC_PATH_WITH_SCHEME);

                        JSONArray signatures = null;
                        String signaturesFilePath = pkg.optString(GPTPackageInfo.TAG_SIGNATURES_FILE_PATH);
                        if (!TextUtils.isEmpty(signaturesFilePath)) {
                            String str = Util.readStringFile(new File(signaturesFilePath));
                            if (!TextUtils.isEmpty(str)) {
                                pkgInfo.signaturesFilePath = signaturesFilePath;
                                signatures = new JSONArray(str);
                                if (DEBUG) {
                                    Log.i(TAG, "signatures from file:" + str);
                                }
                            }
                        }
                        // 向下兼容。把签名写到文件中，是2.2.3才加的
                        if (signatures == null) {
                            signatures = pkg.optJSONArray(GPTPackageInfo.TAG_SIGNATURES);
                            // 重写签名文件
                            if (signatures != null) {
                                needReSave = true;
                                needSaveSignaturesMap.put(pkgInfo.packageName, signatures.toString());
                                pkgInfo.signaturesFilePath = ApkInstaller.getGreedyPorterRootPath(mContext)
                                        + File.separator + pkgInfo.packageName + File.separator
                                        + ApkInstallerService.FILE_SIGNATURE;
                            }
                        }
                        if (signatures != null) {
                            pkgInfo.signatures = new Signature[signatures.length()];
                            for (int j = 0; j < pkgInfo.signatures.length; j++) {
                                String temp = signatures.optString(j);
                                if (temp != null) {
                                    try {
                                        pkgInfo.signatures[j] = new Signature(temp);
                                    } catch (IllegalArgumentException e) {

                                        // For 统计
                                        StringWriter sw = new StringWriter();
                                        PrintWriter pw = new PrintWriter(sw);
                                        pw.append("### Read Signature Fail, pkg=").append(pkgInfo.packageName)
                                                .append("\n");
                                        if (DEBUG) {
                                            e.printStackTrace(pw);
                                        }
                                        ReportManger.getInstance().onException(mContext, "",
                                                sw.toString(), ExceptionConstants.TJ_78730004);
                                    }
                                }

                            }
                        }

                        if (pkgInfo.versionCode == 0 || TextUtils.isEmpty(pkgInfo.versionName)) {
                            // 这两个值是从2.0版本才有的，为了兼容，做下处理。

                            PackageManager pm = mContext.getPackageManager();
                            PackageInfo pi = pm.getPackageArchiveInfo(pkgInfo.srcApkPath, 0);
                            if (pi != null) {
                                pkgInfo.versionCode = pi.versionCode;
                                pkgInfo.versionName = pi.versionName;
                            }

                            needReSave = true;
                        }

                        mInstalledPkgs.put(pkgInfo.packageName, pkgInfo);
                    }

                    if (needReSave) { // 把兼容数据重新写回文件
                        saveInstalledPackageList();
                    }
                    // 覆盖更新2.2.2 以下的版本，通知ApkInstallerService，保存签名文件
                    if (!needSaveSignaturesMap.isEmpty()) {
                        if (DEBUG) {
                            Log.i(TAG, "startService save Signatures");
                        }
                        Intent intent = new Intent(ApkInstallerService.ACTION_SAVE_SIGNATURES_FILE);
                        intent.setClass(mContext, ApkInstallerService.class);
                        intent.putExtra(ApkInstallerService.EXTRA_SIGNATURES_MAP, needSaveSignaturesMap);
                        mContext.startService(intent);
                    }
                } catch (JSONException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 将安装列表写入文件存储。
     * 调用这个之前确保 mInstalledPkgs 已经初始化过了
     */
    private void saveInstalledPackageList() {
        if (mInstalledPkgs == null) {
            return;
        }
        JSONArray pkgs = new JSONArray();

        // 调用这个之前确保 mInstalledPkgs 已经初始化过了
        Enumeration<GPTPackageInfo> packages = mInstalledPkgs.elements();
        if (packages == null) {
            return;
        }
        while (packages.hasMoreElements()) {
            GPTPackageInfo pkg = packages.nextElement();

            JSONObject object = new JSONObject();
            try {
                object.put(GPTPackageInfo.TAG_PKG_NAME, pkg.packageName);
                object.put(GPTPackageInfo.TAG_APK_PATH, pkg.srcApkPath);
                object.put(GPTPackageInfo.TAG_PKG_VC, pkg.versionCode);
                object.put(GPTPackageInfo.TAG_PKG_VN, pkg.versionName);
                object.put(GPTPackageInfo.TAG_PROCESS, pkg.isUnionProcess);
                object.put(GPTPackageInfo.TAG_EXT_PROCESS, pkg.extProcess);
                object.put(GPTPackageInfo.TAG_UNION_DATA, pkg.isUnionDataDir);
                // SharedPreferences，只存签名的存储路径
                object.put(GPTPackageInfo.TAG_SIGNATURES_FILE_PATH, pkg.signaturesFilePath);
                object.put(GPTPackageInfo.TAG_STATE, pkg.state);
                // 上次安装的临时文件
                object.put(GPTPackageInfo.TAG_TEMP_INSTALL_DIR, pkg.tempInstallDir);
                object.put(GPTPackageInfo.TAG_TEMP_APK_PATH, pkg.tempApkPath);
                object.put(GPTPackageInfo.TAG_SRC_PATH_WITH_SCHEME, pkg.srcPathWithScheme);

                pkgs.put(object);
            } catch (JSONException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        SharedPreferences sp = mContext.getSharedPreferences(ApkInstaller.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        String value = pkgs.toString();
        Editor editor = sp.edit();
        if (DEBUG) {
            Log.d(TAG, "--- Save data to file : " + value);
        }
        editor.putString(SP_APP_LIST, value);
        editor.commit();
    }

    /**
     * 增加一个插件信息，默认不增加到安装文件中
     *
     * @param packageInfo GPTPackageInfo
     */
    public void addPackageInfo(GPTPackageInfo packageInfo) {
        addPackageInfo(packageInfo, false);
    }

    /**
     * 增加一个插件信息
     *
     * @param packageInfo 插件信息
     * @param saveToFile  是否保存到文件中
     */
    public void addPackageInfo(GPTPackageInfo packageInfo, boolean saveToFile) {
        initInstalledPackageListIfNeeded();
        mInstalledPkgs.put(packageInfo.packageName, packageInfo);
        if (saveToFile) {
            saveInstalledPackageList();
        }
    }

    /**
     * 增加一个插件信息 一般在插件安装的时候调用
     *
     * @param packageInfo        PackageInfo
     * @param destApkPath        插件apk的安装目录
     * @param isUnionProcess     是否跟主进程同一进程运行
     * @param isUnionDataDir     是否跟宿主使用相同的数据目录
     * @param signaturesFilePath 插件的签名文件路径
     * @param extProcess         插件运行的扩展进程
     */
    public void addPackageInfo(PackageInfo packageInfo, String destApkPath, boolean isUnionProcess,
                               boolean isUnionDataDir, String signaturesFilePath, int extProcess) {
        GPTPackageInfo gptPkgInfo = new GPTPackageInfo();
        gptPkgInfo.packageName = packageInfo.packageName;
        gptPkgInfo.srcApkPath = destApkPath;
        gptPkgInfo.versionCode = packageInfo.versionCode;
        gptPkgInfo.versionName = packageInfo.versionName;
        gptPkgInfo.signaturesFilePath = signaturesFilePath;
        gptPkgInfo.isUnionProcess = isUnionProcess;
        gptPkgInfo.isUnionDataDir = isUnionDataDir;
        gptPkgInfo.extProcess = extProcess;
        addPackageInfo(gptPkgInfo, true);
    }

    /**
     * 删除插件信息，默认不更新存储文件
     *
     * @param pkgName 插件包名
     */
    public void deletePackageInfo(String pkgName) {
        deletePackageInfo(pkgName, false);
    }

    /**
     * 删除插件信息 在插件卸载的时候调用
     *
     * @param pkgName      插件包名
     * @param updateToFile 是否更新到文件
     */
    public void deletePackageInfo(String pkgName, boolean updateToFile) {
        if (DEBUG) {
            Log.i(TAG, "deletePackageInfo :" + pkgName);
        }
        initInstalledPackageListIfNeeded();
        if (mInstalledPkgs.containsKey(pkgName)) {
            mInstalledPkgs.remove(pkgName);
            if (updateToFile) {
                saveInstalledPackageList();
            }
        }
    }

    /**
     * getPackageInfo
     *
     * @param pkgName 插件包名
     * @return GPTPackageInfo
     */
    public GPTPackageInfo getPackageInfo(String pkgName) {
        initInstalledPackageListIfNeeded();
        return mInstalledPkgs.get(pkgName);
    }

    /**
     * updatePackageList
     */
    public void updatePackageList() {
        saveInstalledPackageList();
    }

}

