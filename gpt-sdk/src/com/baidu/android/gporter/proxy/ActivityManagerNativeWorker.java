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
package com.baidu.android.gporter.proxy;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IActivityManager.ContentProviderHolder;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.RemapingUtil;
import com.baidu.android.gporter.gpt.GPTInstrumentation;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.service.ServiceProxy;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.util.List;

/**
 * ActivityManagerNativeWorker
 *
 * @author liuhaitao
 * @since 2018-01-04
 */
public class ActivityManagerNativeWorker extends InterfaceProxy {

    /**
     * 构造方法
     */
    public ActivityManagerNativeWorker() {
        super(Constants.ACTIVE_MANAGER_NATIVE_CLASS);
    }

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false & Constants.DEBUG;
    /**
     * TAG
     */
    private static final String TAG = "ActivityManagerNativeWorker";

    /**
     * 系统真正的IActivityManager aidl接口
     */
    public IActivityManager mTarget;

    /**
     * 宿主包名
     */
    private Context mHostContext = null;

    /**
     * INVALID_USER_ID
     */
    private static final int INVALID_USER_ID = -11111;

    /**
     * 设置宿主包名
     *
     * @param hostContext hostContext
     */
    public void setHostContext(Context hostContext) {
        mHostContext = hostContext;
    }

    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle options, int userId)
            throws RemoteException {
        remapIntents(mHostContext, intents, false);
        return mTarget.getIntentSender(type, mHostContext.getPackageName(), token, resultWho, requestCode, intents, resolvedTypes, flags,
                options, userId);
    }

    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags) throws RemoteException {
        remapIntents(mHostContext, intents, false);
        return mTarget.getIntentSender(type, mHostContext.getPackageName(), token, resultWho, requestCode, intents, resolvedTypes, flags);
    }

    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String reesultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bundle) throws RemoteException {
        remapIntents(mHostContext, intents, false);
        return mTarget.getIntentSender(type, mHostContext.getPackageName(), token, reesultWho, requestCode, intents, resolvedTypes, flags,
                bundle);
    }

    public IIntentSender getIntentSender(int type,
                                         String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent intent, String resolvedType, int flags) {
        Intent intents[] = {intent};
        remapIntents(mHostContext, intents, false);

        return mTarget.getIntentSender(type, mHostContext.getPackageName(), token, resultWho, requestCode, intent, resolvedType, flags);
    }

    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() {

        List<ActivityManager.RunningAppProcessInfo> apps = mTarget.getRunningAppProcesses();

        // TODO 临时加上捕获打出日志，这里有发现过crash
        try {
            String pluginPkg = Util.getCallingPulingPackage();

            if (pluginPkg != null) {
                int pid = android.os.Process.myPid();
                for (ActivityManager.RunningAppProcessInfo app : apps) {
                    if (app.pid == pid) {

                        // 在包名列表里加上插件的包名，小米的ROM在显示Toast之前，会获取RunningProcesses，遍历进程的pkgList，如果没有匹配到，
                        // 认为invisible to user，就不显示了。导致Toast不可用。
                        String[] pkgList = null;
                        String[] originalList = app.pkgList;
                        if (originalList != null) {
                            pkgList = new String[originalList.length + 1];
                            System.arraycopy(app.pkgList, 0, pkgList, 0, originalList.length);
                            pkgList[originalList.length] = pluginPkg;
                        }

                        app.processName = pluginPkg;
                        app.pkgList = pkgList;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return apps;
    }

    public Intent registerReceiver(IApplicationThread caller, String callerPackage, IIntentReceiver receiver,
                                   IntentFilter filter, String requiredPermission) {
        return mTarget.registerReceiver(caller, mHostContext.getPackageName(), receiver, filter, requiredPermission);
    }

    public Intent registerReceiver(IApplicationThread caller, String callerPackage, IIntentReceiver receiver,
                                   IntentFilter filter, String requiredPermission, int user) {
        return mTarget.registerReceiver(caller, mHostContext.getPackageName(), receiver, filter, requiredPermission, user);
    }

    /**
     * Android O 方法兼容适配。
     */
    public Intent registerReceiver(IApplicationThread caller, String callerPackage,
                                   IIntentReceiver receiver, IntentFilter filter,
                                   String requiredPermission, int userId, int flags) {
        return mTarget.registerReceiver(caller, mHostContext.getPackageName(), receiver,
                filter, requiredPermission, userId, flags);
    }

/*
    public List getTasks(int maxNum, int flags, IThumbnailReceiver receiver) throws RemoteException {
        com.baidu.android.gporter.gpt.Util.printClassStack();
        List list = target.getTasks(maxNum, flags, receiver);

        ProxyEnvironment instance = ProxyEnvironment.getInstance(mCurrentPackageName);
        if (instance != null) {
            List<Activity> stacks = instance.getActivityStack();
            if (stacks != null && stacks.size() > 0) {
                ActivityGroup activityGroup = (ActivityGroup) stacks.get(0);
                if (activityGroup.getCurrentActivity() == null) {
                    return list;
                }
                for (Object value : list) {
                    if (value instanceof RunningTaskInfo) {
                        RunningTaskInfo info = (RunningTaskInfo) value;
                        if (info.topActivity != null) {
                            if (info.topActivity.getClassName().equalsIgnoreCase(ActivityProxy.class.getName())) {
                                info.topActivity = activityGroup.getCurrentActivity().getComponentName();
                            }
                        }
                    }
                }
            }

        }

        return list;
    }
*/

    /**
     * remapIntents
     *
     * @param hostContext Context
     * @param intents     Intent[]
     * @param dealMode    true or false
     */
    private static void remapIntents(Context hostContext, Intent[] intents, boolean dealMode) {
        if (intents == null) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "--- remapIntents !");
        }
        for (int i = 0; i < intents.length; i++) {
            RemapingUtil.remapActivityIntent(hostContext, intents[i], dealMode);

            RemapingUtil.remapReceiverIntent(hostContext, intents[i]);

            RemapingUtil.remapServiceIntent(hostContext, intents[i]);
        }
    }

    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType) {

        RemapingUtil.remapServiceIntent(mHostContext, service);

        return mTarget.startService(caller, service, resolvedType);
    }

    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType, int userId) {

        RemapingUtil.remapServiceIntent(mHostContext, service);

        return mTarget.startService(caller, service, resolvedType, userId);
    }

    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType,
                                      String callingPackage, int userId) {

        RemapingUtil.remapServiceIntent(mHostContext, service);

        return mTarget.startService(caller, service, resolvedType, callingPackage, userId);
    }

    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType,
                                      String callingPackage, int userId, IBinder token, int l) {

        RemapingUtil.remapServiceIntent(mHostContext, service);

        return mTarget.startService(caller, service, resolvedType, callingPackage, userId, token, l);
    }


    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType, int i, Notification notification,
                                      String callingPackage, int userId) {
        RemapingUtil.remapServiceIntent(mHostContext, service);
        return mTarget.startService(caller, service, resolvedType, i, notification, callingPackage, userId);
    }

    public int stopService(IApplicationThread caller, Intent service,
                           String resolvedType) {

        if (service.getComponent() != null && ProxyEnvironment.hasInstance(service.getComponent().getPackageName())) {
            return ServiceProxy.stopServiceExternal(service.getComponent());
        } else {
            return mTarget.stopService(caller, service, resolvedType);
        }
    }

    public int stopService(IApplicationThread caller, Intent service,
                           String resolvedType, int userId) {

        if (service.getComponent() != null && ProxyEnvironment.hasInstance(service.getComponent().getPackageName())) {
            return ServiceProxy.stopServiceExternal(service.getComponent());
        } else {
            return mTarget.stopService(caller, service, resolvedType, userId);
        }
    }

    public int bindService(IApplicationThread caller, IBinder token,
                           Intent service, String resolvedType,
                           IServiceConnection connection, int flags) {

        return bindService(caller, token, service, resolvedType, connection, flags, INVALID_USER_ID);
    }

    public int bindService(IApplicationThread caller, IBinder token,
                           Intent service, String resolvedType,
                           IServiceConnection connection, int flags, int userId) {

        token = getActivityToken(token);

        RemapingUtil.remapServiceIntent(mHostContext, service);

        if (userId == INVALID_USER_ID) {
            return mTarget.bindService(caller, token, service, resolvedType, connection, flags);
        } else {
            return mTarget.bindService(caller, token, service, resolvedType, connection, flags, userId);
        }
    }

    /**
     * android 6.0
     */
    public int bindService(IApplicationThread caller, IBinder token, Intent service,
                           String resolvedType, IServiceConnection connection, int flags,
                           String callingPackage, int userId) {

        RemapingUtil.remapServiceIntent(mHostContext, service);

        return mTarget.bindService(caller, token, service, resolvedType, connection, flags, callingPackage, userId);
    }

    public boolean unbindService(IServiceConnection connection) {
        return mTarget.unbindService(connection);
    }

    /**
     * android 2.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               boolean serialized, boolean sticky) {

        remapBroadcastIntent(intent);

        return mTarget.broadcastIntent(caller, intent, resolvedType, resultTo, resultCode,
                resultData, map, requiredPermission, serialized, sticky);
    }

    /**
     * android 4.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               boolean serialized, boolean sticky, int userId) {

        remapBroadcastIntent(intent);

        return mTarget.broadcastIntent(caller, intent, resolvedType, resultTo, resultCode, resultData, map,
                requiredPermission, serialized, sticky, userId);
    }

    /**
     * android 5.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               int appOp, boolean serialized, boolean sticky, int userId) {

        remapBroadcastIntent(intent);

        return mTarget.broadcastIntent(caller, intent, resolvedType, resultTo, resultCode, resultData, map,
                requiredPermission, appOp, serialized, sticky, userId);
    }

    /**
     * android 6.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String[] requiredPermissions,
                               int appOp, Bundle options, boolean serialized, boolean sticky, int userId) {

        remapBroadcastIntent(intent);

        return mTarget.broadcastIntent(caller, intent, resolvedType, resultTo, resultCode, resultData, map,
                requiredPermissions, appOp, options, serialized, sticky, userId);

    }

    /**
     * 处理 创建和删除 桌面快捷方式 intent
     *
     * @param intent Intent
     * @return true or false
     */
    private boolean remapShortcutIntent(Intent intent) {
        boolean result = false;

        if (Constants.ACTION_INSTALL_SHORT_CUT.equals(intent.getAction()) || Constants.ACTION_UNINSTALL_SHORT_CUT.equals(intent.getAction())) {
            // 根据shortcut启动的component做判断。
            Intent shortcutIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

            ComponentName cn = shortcutIntent.getComponent();
            String packageName = cn.getPackageName();

            if (GPTInstrumentation.isPlugin(packageName) && ProxyEnvironment.hasInstance(packageName)) {
                ProxyEnvironment.getInstance(packageName).remapShortCutCreatorIntent(intent);
                result = true;
            }
        }

        return result;
    }

    /**
     * remapBroadcastIntent
     * <p>
     * 函数思路
     * 静态转动态应该还需要保留
     * 插件之间调用需要 remap 代理
     * Registter 不需要remap
     * Send的时候只需要把静态的remap
     *
     * @param intent Intent
     */
    private void remapBroadcastIntent(Intent intent) {
        boolean result = remapShortcutIntent(intent);
        if (result) {
            // 如果是 shortcut 的intent，直接返回不需要处理。
            return;
        }
        // 这里只remap插件manifest中声明的静态receiver。
        RemapingUtil.remapReceiverIntent(mHostContext, intent);
    }

    /**
     * 2.3
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller, String name) {
        ContentProviderHolder holder = getContentProviderHolder(mHostContext, name);
        if (holder != null) {
            return holder;
        }

        return mTarget.getContentProvider(caller, name);
    }

    /**
     * 4.x
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller, String name, boolean stable) {
        ContentProviderHolder holder = getContentProviderHolder(mHostContext, name);
        if (holder != null) {
            return holder;
        }

        return mTarget.getContentProvider(caller, name, stable);
    }

    /**
     * 5.x 6.0
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller, String name, int userId, boolean stable) {
        ContentProviderHolder holder = getContentProviderHolder(mHostContext, name);
        if (holder != null) {
            return holder;
        }

        return mTarget.getContentProvider(caller, name, userId, stable);
    }

    /**
     * getContentProviderHolder
     *
     * @param hostContext Context
     * @param authority   authority
     * @return ContentProviderHolder
     */
    public static ContentProviderHolder getContentProviderHolder(Context hostContext, String authority) {
        ContentProviderHolder holder = null;

        String packageName = ContentProviderProxy.getProviderPackageName(hostContext, authority);
        if (packageName == null || packageName.length() == 0) {
            return null;
        }

        GPTPackageInfo pkginfo = GPTPackageManager.getInstance(hostContext).getPackageInfo(packageName);
        if (pkginfo == null) {
            return null;
        }

        // 插件是否运行在主进程
        boolean runOnHostProcess = pkginfo.isUnionProcess;

        // 中转代理provider
        String proxyAuthority = hostContext.getPackageName() + "_" + ContentProviderProxy.AUTHORITY;
        if (!runOnHostProcess) {
            proxyAuthority = hostContext.getPackageName() + "_" + ContentProviderProxy.AUTHORITY_EXT;
        }

        Uri uri = Uri.parse("content://" + proxyAuthority);
        Cursor c = null;

        String[] projection = {packageName, authority};
        c = hostContext.getContentResolver().query(uri, projection, null, null, null);

        if (c != null) {
            Bundle bundle = c.getExtras();
            holder = bundle.getParcelable("provider");

            c.close();
        }

        return holder;
    }

    public int startActivity(IApplicationThread caller, Intent intent, String resolvedType,
                             Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode,
                             boolean onlyIfNeeded, boolean debug) {

        RemapingUtil.remapActivityIntent(mHostContext, intent);

        return mTarget.startActivity(caller, intent, resolvedType, grantedUriPermissions, grantedMode,
                resultTo, resultWho, requestCode, onlyIfNeeded, debug);
    }

    public int startActivity(IApplicationThread caller, Intent intent, String resolvedType, IBinder resultTo,
                             String resultWho, int requestCode, int flags, String profileFile, ParcelFileDescriptor profileFd,
                             Bundle options) {

        RemapingUtil.remapActivityIntent(mHostContext, intent);

        return mTarget.startActivity(caller, intent, resolvedType, resultTo, resultWho, requestCode,
                flags, profileFile, profileFd, options);
    }

    public int startActivity(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType,
                             IBinder resultTo, String resultWho, int requestCode, int flags, String profileFile,
                             ParcelFileDescriptor profileFd, Bundle options) {
        try {
            RemapingUtil.remapActivityIntent(mHostContext, intent);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return mTarget.startActivity(caller, callingPackage, intent, resolvedType, resultTo, resultWho,
                requestCode, flags, profileFile, profileFd, options);
    }

    public int startActivity(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType,
                             IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options) {

        RemapingUtil.remapActivityIntent(mHostContext, intent);

        return mTarget.startActivity(caller, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode,
                flags, profilerInfo, options);
    }

    public int startActivity(IApplicationThread caller, Intent intent, String resolvedType,
                             Uri[] grantedUriPermissions, int grantedMode, IBinder resultTo, String resultWho, int requestCode,
                             boolean onlyIfNeeded, boolean debug, String profileFile, ParcelFileDescriptor profileFd,
                             boolean autoStopProfiler) {

        RemapingUtil.remapActivityIntent(mHostContext, intent);

        return mTarget.startActivity(caller, intent, resolvedType, grantedUriPermissions, grantedMode, resultTo,
                resultWho, requestCode, onlyIfNeeded, debug, profileFile, profileFd, autoStopProfiler);
    }

    public void unstableProviderDied(IBinder connection) {
        if (connection == null) {
            // 我们自己的provider代理，此connection为null，如果传递给系统的ActivityManagerService会报空指针。
            return;
        }

        mTarget.unstableProviderDied(connection);
    }

    public int getTaskForActivity(IBinder token, boolean onlyRoot) {
        return mTarget.getTaskForActivity(getActivityToken(token), onlyRoot);
    }

    public boolean isTopOfTask(IBinder token) {
        return mTarget.isTopOfTask(getActivityToken(token));
    }

    /**
     * getActivityToken
     *
     * @param token IBinder
     * @return IBinder
     */
    private IBinder getActivityToken(IBinder token) {
        if (token != null) { // ContextImpl.getActivityToken
            // 是activity的话
            String className = "android.app.LocalActivityManager$LocalActivityRecord";
            Class clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

            if (clazz != null && token.getClass().equals(clazz)) {
                Activity activity = (Activity) JavaCalls.getField(token, "activity");

                if (activity != null) {
                    // token转为parent activity
                    token = (IBinder) JavaCalls.getField(activity.getParent(), "mToken");
                }
            }
        }
        return token;

    }

}
