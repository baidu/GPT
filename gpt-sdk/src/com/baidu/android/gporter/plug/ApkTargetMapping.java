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
package com.baidu.android.gporter.plug;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Activity;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.Provider;
import android.content.pm.PackageParser.Service;
import android.content.pm.PackageUserState;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.ProxyProviderCounter;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * ApkTargetMapping
 *
 * @author liuhaitao
 * @since 2017-12-29
 */
public class ApkTargetMapping implements TargetMapping {

    /**
     * Context
     */
    private final Context context;
    /**
     * APKFile
     */
    private final File apkFile;

    /**
     * versionName
     */
    private String versionName;
    /**
     * versionCode
     */
    private int versionCode;
    /**
     * 包名
     */
    private String packageName;
    /**
     * applicationClassName
     */
    private String applicationClassName;
    /**
     * defaultActivityName
     */
    private String defaultActivityName;
    /**
     * applicationName
     */
    private String applicationName = null;
    /**
     * PermissionInfo[]
     */
    private PermissionInfo[] permissions;
    /**
     * PackageInfo
     */
    private PackageInfo packageInfo;
    /**
     * AcitivtyMap
     */
    private HashMap<String, ActivityInfo> mAcitivtyMap = new HashMap<String, ActivityInfo>();
    /**
     * ServiceMap
     */
    private HashMap<String, ServiceInfo> mServiceMap = new HashMap<String, ServiceInfo>();
    /**
     * ReceiverMap
     */
    private HashMap<String, ActivityInfo> mReceiverMap = new HashMap<String, ActivityInfo>();

    /**
     * 存储插件静态广播的IntentFilter
     */
    private HashMap<String, ArrayList<ActivityIntentInfo>> mRecvIntentFilters = null;

    /**
     * IntentInfos
     */
    private Vector<IntentInfo> mIntentInfos = new Vector<ApkTargetMapping.IntentInfo>();

    /**
     * 构造方法
     *
     * @param context Context
     * @param apkFile APK文件
     */
    public ApkTargetMapping(Context context, File apkFile) {
        this.context = context;
        this.apkFile = apkFile;
        init();
    }

    /**
     * getApkPath
     *
     * @return APK文件的Path
     */
    public String getApkPath() {
        return apkFile.getAbsolutePath();
    }

    /**
     * init方法
     */
    private void init() {
        // apk文件被删除，则下次删除这个apk
        if (!apkFile.exists()) {
            String s = new String(apkFile.getPath() + " is not exist!!! on ApkTargetMapping.init()");
            ReportManger.getInstance().onException(context, "", s, ExceptionConstants.TJ_78730011);
            return;
        }
        android.content.pm.PackageParser parser = newPackageParser();
        PackageParser.Package pkg = getPackage(parser, apkFile);
        PackageInfo pkgInfo = null;
        try {
            int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA
                    | PackageManager.GET_SERVICES | PackageManager.GET_CONFIGURATIONS | PackageManager.GET_GIDS
                    | PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS;
            pkgInfo = getPackageArchiveInfo(parser, pkg, apkFile.getAbsolutePath(), flags);
            packageName = pkgInfo.packageName;

            // 改变application
            if (pkgInfo.applicationInfo.icon == 0) {
                pkgInfo.applicationInfo.icon = android.R.drawable.sym_def_app_icon;
            }
            pkgInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
            pkgInfo.applicationInfo.sourceDir = apkFile.getAbsolutePath();
            pkgInfo.applicationInfo.processName = pkgInfo.packageName;
            pkgInfo.applicationInfo.uid = context.getApplicationInfo().uid;
            pkgInfo.applicationInfo.dataDir = ProxyEnvironment.getDataDir(context, packageName)
                    .getAbsolutePath();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                String libdir = ProxyEnvironment.getTargetLibPath(context, pkgInfo.packageName);
                pkgInfo.applicationInfo.nativeLibraryDir = libdir;
            }
            pkgInfo.applicationInfo.flags |= ApplicationInfo.FLAG_HAS_CODE;
            pkgInfo.applicationInfo.flags |= ApplicationInfo.FLAG_SUPPORTS_SCREEN_DENSITIES;
            pkgInfo.signatures = GPTPackageManager.getInstance(context).getPackageInfo(pkgInfo.packageName).signatures;
//            if (pkgInfo.applicationInfo.className != null) {
//                pkgInfo.applicationInfo.className += Constants.GPT_SUFFIX;
//            } else {
//                pkgInfo.applicationInfo.className = packageName + "."
//                        + Constants.DEFAULT_APPLICATION_CLASS_NAME + Constants.GPT_SUFFIX;
//            }

            applicationClassName = pkgInfo.applicationInfo.className;

            permissions = pkgInfo.permissions;
            versionCode = pkgInfo.versionCode;
            versionName = pkgInfo.versionName;
            packageInfo = pkgInfo;

            queryDefaultActivityAndSpecialInfo(pkg);

            ActivityInfo infos[] = pkgInfo.activities;
            ServiceInfo serviceInfo[] = pkgInfo.services;
            ProviderInfo providerInfo[] = pkgInfo.providers;
            ActivityInfo receiverInfo[] = pkgInfo.receivers;
            // pkgInfo.activities可能为null，为null就重新生成一遍。
            if (infos == null || infos.length <= 0) {
                generateActivityInfo(pkg, pkgInfo, flags);
                infos = pkgInfo.activities;
            }

            if (defaultActivityName == null && pkgInfo.activities != null) {
                defaultActivityName = pkgInfo.activities[0].name;
            }

            if (serviceInfo == null || serviceInfo.length <= 0) {
                generateServiceInfo(pkg, pkgInfo, flags);
                serviceInfo = pkgInfo.services;
            }
            if (providerInfo == null || providerInfo.length <= 0) {
                generateProviderInfo(pkg, pkgInfo, flags);
                providerInfo = pkgInfo.providers;
            }
            if (receiverInfo == null || receiverInfo.length <= 0) {
                generateReceiverInfo(pkg, pkgInfo, flags);
                receiverInfo = pkgInfo.receivers;
            }
            if (infos != null && infos.length > 0) {
                for (ActivityInfo info : infos) {
                    info.applicationInfo = pkgInfo.applicationInfo;
                    mAcitivtyMap.put(info.name, info);
                    //info.name += Constants.GPT_SUFFIX;
                }
            }
            if (serviceInfo != null && serviceInfo.length > 0) {
                for (ServiceInfo info : serviceInfo) {
                    info.applicationInfo = pkgInfo.applicationInfo;
                    mServiceMap.put(info.name, info);
                }
            }
            if (receiverInfo != null && receiverInfo.length > 0) {
                for (ActivityInfo info : receiverInfo) {
                    info.applicationInfo = pkgInfo.applicationInfo;
                    mReceiverMap.put(info.name, info);
                }
            }

            ProxyProviderCounter.getInstance().addProviderMap(pkgInfo);
        } catch (Throwable e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            if (pkg == null) {
                sb.append("pkg is null \n`");
            }
            if (pkgInfo == null) {
                sb.append("pkgInfo is null \n`");
            }
            sb.append("apk path:" + apkFile.getAbsolutePath() + "\n`");
            sb.append(Util.getCallStack(e));
            ReportManger.getInstance().onException(context, packageName, sb.toString(),
                    ExceptionConstants.TJ_78730008);
            return;
        }
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getApplicationClassName() {
        return applicationClassName;
    }

    @Override
    public String getDefaultActivityName() {
        return defaultActivityName;
    }

    @Override
    public String getApplicationName() {
        if (applicationName == null) {
            applicationName = (String) context.getPackageManager().getApplicationLabel(
                    packageInfo.applicationInfo);
        }
        return applicationName;
    }

    /**
     * getPermissions
     *
     * @return PermissionInfo[]
     */
    public PermissionInfo[] getPermissions() {
        return permissions;
    }

    @Override
    public String getVersionName() {
        return versionName;
    }

    @Override
    public int getVersionCode() {
        return versionCode;
    }

    @Override
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    @Override
    public int getThemeResource(String activity) {
        if (activity == null) {
            return android.R.style.Theme;
        }
        ActivityInfo info = mAcitivtyMap.get(activity);

        /**
         * 指定默认theme为android.R.style.Theme
         * 有些OPPO手机上，把theme设置成0，其实会把Theme设置成holo主题，带ActionBar，导致插件黑屏，目前插件SDK不支持ActionBar。
         *
         * ### 以上注释为 megapp 框架，新框架已经没有问题，gpt框架支持actionbar。
         */
        if (info == null || info.getThemeResource() == 0) {
            return 0; // 使用系统默认
        }
        return info.getThemeResource();
    }

    @Override
    public ActivityInfo getActivityInfo(String activity) {
        if (activity == null) {
            return null;
        }
        return mAcitivtyMap.get(activity);
    }

    @Override
    public ServiceInfo getServiceInfo(String service) {
        if (service == null) {
            return null;
        }
        return mServiceMap.get(service);
    }

    /**
     * @return the metaData
     */
    public Bundle getMetaData() {
        return packageInfo.applicationInfo.metaData;
    }

    @Override
    public int getTheme() {

        // application的theme取launcher的theme
        return getThemeResource(defaultActivityName);
    }

    /**
     * 获取IntentInfos
     */
    public Vector<IntentInfo> getIntentInfos() {
        return mIntentInfos;
    }

    /**
     * 获取启动的Activity和Application的MetaData,Receiver intent等信息
     */
    private void queryDefaultActivityAndSpecialInfo(PackageParser.Package pkg) {

        // 2.2系统的解析没有赋值meta-data，所以自己取一下
        if (pkg.mAppMetaData != null) {
            packageInfo.applicationInfo.metaData = new Bundle(pkg.mAppMetaData);
        }

        for (int i = 0; i < pkg.activities.size(); i++) {
            android.content.pm.PackageParser.Activity activity = pkg.activities.get(i);
            ArrayList<android.content.pm.PackageParser.ActivityIntentInfo> intentFilters = activity.intents;
            IntentInfo intentInfo = new IntentInfo();
            intentInfo.type = "activity";
            intentInfo.className = activity.className;
            intentInfo.categories = new ArrayList<String>();
            intentInfo.actions = new ArrayList<String>();
            for (int j = 0; j < intentFilters.size(); j++) {
                boolean hasAction = false;
                boolean hasCategory = false;

                for (int k = 0; k < intentFilters.get(j).countCategories(); k++) {
                    intentInfo.categories.add(intentFilters.get(j).getCategory(k));
                    if (intentFilters.get(j).getCategory(k)
                            .equalsIgnoreCase("android.intent.category.LAUNCHER")
                            && pkg.activities.get(i).info.enabled) {
                        hasCategory = true;
                    }
                }

                for (int k = 0; k < intentFilters.get(j).countActions(); k++) {
                    intentInfo.actions.add(intentFilters.get(j).getAction(k));
                    if (intentFilters.get(j).getAction(k).equalsIgnoreCase("android.intent.action.MAIN")
                            && pkg.activities.get(i).info.enabled) {
                        hasAction = true;
                    }
                }
                if (hasAction && hasCategory) {
                    android.content.pm.PackageParser.ActivityIntentInfo activityInfo = intentFilters.get(j);
                    defaultActivityName = activityInfo.activity.className;
                }
            }
            mIntentInfos.add(intentInfo);
        }

        // 有receiver，处理一下receiver
        if (pkg.receivers != null && pkg.receivers.size() > 0) {
            mRecvIntentFilters = new HashMap<String, ArrayList<ActivityIntentInfo>>();
            for (Activity receiver : pkg.receivers) {
                mRecvIntentFilters.put(receiver.className, (ArrayList<ActivityIntentInfo>) receiver.intents);
            }
        }
    }

    /**
     * newPackageParser
     *
     * @return android.content.pm.PackageParser
     */
    private static android.content.pm.PackageParser newPackageParser() {
        android.content.pm.PackageParser parser = null;

        // 适配5.0
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            parser = new android.content.pm.PackageParser();
        } else {
            parser = new android.content.pm.PackageParser(null);
        }

        return parser;
    }

    /**
     * getPackage
     *
     * @param parser  android.content.pm.PackageParser
     * @param apkFile APK文件
     * @return PackageParser.Package
     */
    private static PackageParser.Package getPackage(android.content.pm.PackageParser parser, File apkFile) {

        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        final File sourceFile = new File(apkFile.getAbsolutePath());
        android.content.pm.PackageParser.Package pkg = null;

        // 适配5.0
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            Method mtd = null;
            try {
                mtd = android.content.pm.PackageParser.class.getDeclaredMethod("parsePackage", File.class,
                        int.class);
            } catch (NoSuchMethodException e1) {
                if (Constants.DEBUG) {
                    e1.printStackTrace();
                }

            }

            if (mtd != null) {
                try {
                    pkg = parser
                            .parsePackage(apkFile, PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RECEIVERS);
                } catch (PackageParserException e) {
                    if (Constants.DEBUG) {
                        e.printStackTrace();
                    }
                }
            } else { // L的情况
                pkg = parser.parsePackage(sourceFile, apkFile.getAbsolutePath(), metrics,
                        PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RECEIVERS);
            }
        } else {
            pkg = parser.parsePackage(sourceFile, apkFile.getAbsolutePath(), metrics, PackageManager.GET_INTENT_FILTERS
                    | PackageManager.GET_RECEIVERS);
        }
        return pkg;
    }

    /**
     * getMeta
     *
     * @param apkFile APK文件
     * @return Bundle
     */
    public static Bundle getMeta(File apkFile) {
        PackageParser.Package pkg = getPackage(newPackageParser(), apkFile);
        // 2.2系统的解析没有赋值meta-data，所以自己取一下
        Bundle bundle = null;
        try {
            if (pkg != null && pkg.mAppMetaData != null) {
                bundle = new Bundle(pkg.mAppMetaData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bundle;
    }

    @Override
    public ActivityInfo getReceiverInfo(String receiver) {
        if (receiver == null) {
            return null;
        }
        return mReceiverMap.get(receiver);
    }

    @Override
    public ProviderInfo getProviderInfo(String provider) {
        return null;
    }

    @Override
    public Map<String, ArrayList<ActivityIntentInfo>> getRecvIntentFilters() {
        return mRecvIntentFilters;
    }

    /**
     * getPackageArchiveInfo
     *
     * @param parser          android.content.pm.PackageParser
     * @param pkg             PackageParser.Package
     * @param archiveFilePath archiveFilePath
     * @param flags           flags
     * @return PackageInfo
     */
    public PackageInfo getPackageArchiveInfo(android.content.pm.PackageParser parser, PackageParser.Package pkg,
                                             String archiveFilePath, int flags) {
        PackageInfo packageInfo = null;
        if ((flags & PackageManager.GET_SIGNATURES) != 0) {
            // 2.3 4.0 4.1 4.2 4.4
            parser.collectCertificates(pkg, 0);
            // 5.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                parser.collectManifestDigest(pkg);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 4.2 4.3 4.4 5.0 6.0
            PackageUserState state = new PackageUserState();
            //packageInfo = PackageParser.generatePackageInfo(pkg, null, flags, 0, 0, null, state);
            try {
                // 这里使用反射。因为 permission 参数 在 5.0 5.1 6.0 上参数不同，但是我们传入的为 null
                packageInfo = JavaCalls.callStaticMethodOrThrow(PackageParser.class, "generatePackageInfo",
                        pkg, null, flags, 0L, 0L, null, state);
            } catch (Exception e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
                String methodInfo = JavaCalls
                        .getMethodToString(JavaCalls.getMethodFromClass(PackageParser.class, "generatePackageInfo"));
                if (methodInfo == null) {
                    methodInfo = "";
                }
                throw new RuntimeException("generatePackageInfo get exception, " + methodInfo, e);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // 4.1
            packageInfo = PackageParser.generatePackageInfo(pkg, null, flags, 0, 0, null, false, 0);
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            //  2.3 4.0
            packageInfo = PackageParser.generatePackageInfo(pkg, null, flags, 0, 0);
        }

        return packageInfo;
    }

    /**
     * generateActivityInfo
     *
     * @param p     PackageParser.Package
     * @param pi    PackageInfo
     * @param flags flags
     */
    private void generateActivityInfo(PackageParser.Package p, PackageInfo pi, int flags) {
        PackageUserState state = new PackageUserState();

        if ((flags & PackageManager.GET_ACTIVITIES) != 0) {
            int N = p.activities.size();
            if (N > 0) {
                if ((flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                    pi.activities = new ActivityInfo[N];
                } else {
                    int num = 0;
                    for (int i = 0; i < N; i++) {
                        if (p.activities.get(i).info.enabled) num++;
                    }
                    pi.activities = new ActivityInfo[num];
                }
                for (int i = 0, j = 0; i < N; i++) {
                    final Activity activity = p.activities.get(i);
                    if (activity.info.enabled
                            || (flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                        pi.activities[j++] = PackageParser.generateActivityInfo(activity, flags, state, 0);
//                        try {
//                            pi.activities[j++] = JavaCalls.callStaticMethodOrThrow(PackageParser.class, "generateActivityInfo",
//                                    activity, flags, state, 0);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } 
                    }
                }
            }
        }
    }

    /**
     * generateServiceInfo
     *
     * @param p     PackageParser.Package
     * @param pi    PackageInfo
     * @param flags flags
     */
    private void generateServiceInfo(PackageParser.Package p, PackageInfo pi, int flags) {
        PackageUserState state = new PackageUserState();
        if ((flags & PackageManager.GET_SERVICES) != 0) {
            int N = p.services.size();
            if (N > 0) {
                if ((flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                    pi.services = new ServiceInfo[N];
                } else {
                    int num = 0;
                    for (int i = 0; i < N; i++) {
                        if (p.services.get(i).info.enabled) num++;
                    }
                    pi.services = new ServiceInfo[num];
                }
                for (int i = 0, j = 0; i < N; i++) {
                    final Service service = p.services.get(i);
                    if (service.info.enabled
                            || (flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                        pi.services[j++] = PackageParser.generateServiceInfo(p.services.get(i), flags,
                                state, 0);
//                        try {
//                            pi.services[j++] = JavaCalls.callStaticMethodOrThrow(PackageParser.class, "generateServiceInfo",
//                                    p.services.get(i), flags, state, 0);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } 
                    }
                }
            }
        }
    }

    /**
     * generateProviderInfo
     *
     * @param p     PackageParser.Package
     * @param pi    PackageInfo
     * @param flags flags
     */
    private void generateProviderInfo(PackageParser.Package p, PackageInfo pi, int flags) {
        PackageUserState state = new PackageUserState();
        if ((flags & PackageManager.GET_PROVIDERS) != 0) {
            int N = p.providers.size();
            if (N > 0) {
                if ((flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                    pi.providers = new ProviderInfo[N];
                } else {
                    int num = 0;
                    for (int i = 0; i < N; i++) {
                        if (p.providers.get(i).info.enabled) num++;
                    }
                    pi.providers = new ProviderInfo[num];
                }
                for (int i = 0, j = 0; i < N; i++) {
                    final Provider provider = p.providers.get(i);
                    if (provider.info.enabled
                            || (flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                        pi.providers[j++] = PackageParser.generateProviderInfo(p.providers.get(i), flags,
                                state, 0);
//                        try {
//                            pi.providers[j++] = JavaCalls.callStaticMethodOrThrow(PackageParser.class, "generateProviderInfo",
//                                    p.providers.get(i), flags, state, 0);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } 
                    }
                }
            }
        }


    }

    /**
     * generateReceiverInfo
     *
     * @param p     PackageParser.Package
     * @param pi    PackageInfo
     * @param flags flags
     */
    private void generateReceiverInfo(PackageParser.Package p, PackageInfo pi, int flags) {
        PackageUserState state = new PackageUserState();
        if ((flags & PackageManager.GET_RECEIVERS) != 0) {
            int N = p.receivers.size();
            if (N > 0) {
                if ((flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                    pi.receivers = new ActivityInfo[N];
                } else {
                    int num = 0;
                    for (int i = 0; i < N; i++) {
                        if (p.receivers.get(i).info.enabled) num++;
                    }
                    pi.receivers = new ActivityInfo[num];
                }
                for (int i = 0, j = 0; i < N; i++) {
                    final Activity activity = p.receivers.get(i);
                    if (activity.info.enabled
                            || (flags & PackageManager.GET_DISABLED_COMPONENTS) != 0) {
                        pi.receivers[j++] = PackageParser.generateActivityInfo(p.receivers.get(i), flags, state, 0);
//                        try {
//                            pi.receivers[j++] = JavaCalls.callStaticMethodOrThrow(PackageParser.class,
//                                    "generateActivityInfo", p.receivers.get(i), flags, state, 0);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                    }
                }
            }
        }
    }
}

