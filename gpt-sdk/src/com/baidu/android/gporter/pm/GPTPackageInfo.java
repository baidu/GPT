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

import android.content.pm.Signature;

/**
 * Megapp PackageInfo
 *
 * @author liuhaitao
 * @since 2014年5月10日
 */
public class GPTPackageInfo {
    /**
     * 包名
     */
    public String packageName;
    /**
     * 安装后的apk file path。
     */
    public String srcApkPath;
    /**
     * version code
     *
     * @since 2.0
     */
    public int versionCode;
    /**
     * version Name
     *
     * @since 2.0
     */
    public String versionName;
    /**
     * ext process:扩展进程号，当前只有1个，0表示不扩展
     */
    public int extProcess = 0x0;
    /**
     * 是否跟主进程同一进程运行
     */
    public boolean isUnionProcess;
    /**
     * 是否跟宿主使用相同的数据目录
     */
    public boolean isUnionDataDir;
    /**
     * 插件签名文件保存路径
     */
    public String signaturesFilePath;
    /**
     * 安装的临时目录
     */
    public String tempInstallDir;
    /**
     * 安装的临时APK文件
     */
    public String tempApkPath;
    /**
     * 安装的源文件
     */
    public String srcPathWithScheme;

    /**
     * 插件的状态
     */
    public int state = STATE_NORMAL;
    /**
     * 正常状态
     */
    public static final int STATE_NORMAL = 0x0000;
    /**
     * 需要下次初始化的时候卸载
     */
    public static final int STATE_NEED_NEXT_DELETE = 0x0001;

    /**
     * 需要下次初始化的时候，切换安装目录，完成安装
     */
    public static final int STATE_NEED_NEXT_SWITCH_INSTALL_FILE = 0x0002;

    /**
     * signature
     */
    public Signature[] signatures;
    /**
     * 存储在安装列表中的key:pkgName
     */
    static final String TAG_PKG_NAME = "pkgName";
    /**
     * 存储在安装列表中的key:srcApkPath
     */
    static final String TAG_APK_PATH = "srcApkPath";
    /**
     * 存储在安装列表中的key:versionCode
     */
    static final String TAG_PKG_VC = "versionCode";
    /**
     * 存储在安装列表中的key:versionName
     */
    static final String TAG_PKG_VN = "versionName";
    /**
     * 是否跟主进程同一进程运行的key
     */
    static final String TAG_PROCESS = "unionProcess";
    /**
     * 插件运行的扩展进程号的key
     */
    static final String TAG_EXT_PROCESS = "extProcess";
    /**
     * 存储在安装列表的key:签名
     */
    static final String TAG_SIGNATURES = "signatures";
    /**
     * 存储在安装列表的key:签名的存储文件
     */
    static final String TAG_SIGNATURES_FILE_PATH = "signatures_file_path";
    /**
     * 存储在安装列表的key:是否跟宿主使用相同的数据目录
     */
    static final String TAG_UNION_DATA = "unionData";
    /**
     * 存储在安装列表的key:插件状态
     */
    static final String TAG_STATE = "state";
    /**
     * 存储在安装列表的key:临时安装目录
     */
    static final String TAG_TEMP_INSTALL_DIR = "temp_install_dir";
    /**
     * 存储在安装列表的key:临时安装的apk文件
     */
    static final String TAG_TEMP_APK_PATH = "temp_apk_path";
    /**
     * 存储在安装列表的key:安装的源文件
     */
    static final String TAG_SRC_PATH_WITH_SCHEME = "src_path_with_scheme";


}



