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

import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;


/**
 * NotificationManagerNativeWorker
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class NotificationManagerNativeWorker extends InterfaceProxy {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false & Constants.DEBUG;
    /**
     * TAG
     */
    private final static String TAG = "NotificationManagerNativeWorker";

    /**
     * 系统原始的INotificationManager
     */
    public INotificationManager mTarget;

    /**
     * host Context
     */
    public Context mHostContext;

    /**
     * 构造方法
     *
     * @param context hostContext
     */
    public NotificationManagerNativeWorker(Context context) {
        super(Constants.NOTIFICATION_MANAGER_NATIVE_CLASS);
        mHostContext = context;
    }

    /**
     * remapNotification
     *
     * @param notification Notification
     */
    private void remapNotification(Notification notification) {

        String pkgName = null;
        boolean getFromExtra = false;
        boolean getFromApplication = false;

        if (notification.contentView != null) {
            // android 4.x 以及以下。
            pkgName = (String) JavaCalls.getField(notification.contentView, "mPackage");
            if (pkgName == null) {
                ApplicationInfo appInfo = (ApplicationInfo) JavaCalls.getField(notification.contentView,
                        "mApplication");
                pkgName = appInfo.packageName;
                if (pkgName != null) {
                    getFromApplication = true;
                }
            }
        }

        String appInfoKey = "android.rebuild.applicationInfo";
        if (pkgName == null) {
            if (notification.extras != null) {
                // 先判断 android 5.0 不为空
                ApplicationInfo ai = notification.extras.getParcelable(appInfoKey);
                if (ai == null) { // android 7.0
                    ai = notification.extras.getParcelable("android.appInfo");
                    if (ai != null) {
                        appInfoKey = "android.appInfo";
                    }
                }
                if (ai != null) {
                    pkgName = ai.packageName;
                }
                getFromExtra = true;
            }
        }

        if (pkgName == null) {
            return;
        }

        boolean isPlugin = isPlugin(pkgName);
        if (!isPlugin) {
            // 不是插件直接返回。
            return;
        }

        /* 使用 host app的 icon 作为 notification 的 icon */
        notification.icon = mHostContext.getApplicationInfo().icon;
        if (getFromApplication) {
            notification.contentView.setImageViewResource(android.R.id.icon, notification.icon);
            JavaCalls.setField(notification.contentView, "mApplication", mHostContext.getApplicationInfo());
            Object icon = JavaCalls.getField(notification, "mSmallIcon");
            setSmallIcon(icon, notification.icon, mHostContext.getPackageName());
        } else if (getFromExtra) { // >= android 5.0
            Object icon = null;
            if (Build.VERSION.SDK_INT >= 24 /* Android N 7.0 */) {
                icon = JavaCalls.getField(notification, "mSmallIcon");
                setSmallIcon(icon, notification.icon, mHostContext.getPackageName());
            } else if (Build.VERSION.SDK_INT >= 23 /*Android m 6.0*/) {
                icon = notification.extras.get(Notification.EXTRA_SMALL_ICON);
                setSmallIcon(icon, notification.icon, mHostContext.getPackageName());
                if (icon != null) {
                    notification.extras.putParcelable(Notification.EXTRA_SMALL_ICON, (Parcelable) icon);
                }
            } else { // 5.x
                notification.extras.putInt(Notification.EXTRA_SMALL_ICON, notification.icon);
            }
            notification.extras.putParcelable(appInfoKey, mHostContext.getApplicationInfo());

        } else {
            // android 4.x 以及以下。
            JavaCalls.setField(notification.contentView, "mPackage", mHostContext.getPackageName());
            notification.contentView.setImageViewResource(android.R.id.icon, notification.icon);
        }
    }

    /**
     * 判断是否是插件。
     * <p>
     * 因为通过 PM 接口获取 pkg信息，我们优先返回插件的，不是插件再通过系统接口返回。
     *
     * @param packageName 包名
     * @return true or false
     */
    private boolean isPlugin(String packageName) {
        /*
         * 这里简单处理，如果插件没有被加载，也返回false。只有插件初始化了才认为是插件。同时也有效率方面的考虑。
         * 当然这个规则可以修改。
         * 
         * ！！！ 如果想改为根据插件有没有安装，那么在这里进行未初始化插件的初始化工作。
         * 
         */
        return ProxyEnvironment.hasInstance(packageName);
    }

    /**
     * 设置smallIcon
     *
     * @param icon    smallIcon
     * @param iconId  icon id
     * @param pkgName 包名
     * @return smallIcon
     */
    private Object setSmallIcon(Object icon, int iconId, String pkgName) {
        if (icon != null) {
            JavaCalls.setField(icon, "mInt1", iconId);
            JavaCalls.setField(icon, "mString1", pkgName);
        }
        return icon;
    }

    public void enqueueNotification(String pkg, int id, Notification notification, int[] idReceived) {
        remapNotification(notification);
        mTarget.enqueueNotification(mHostContext.getPackageName(), id, notification, idReceived);
    }

    public void enqueueNotificationWithTag(String pkg, String tag, int id, Notification notification, int[] idReceived,
                                           int userid) {
        remapNotification(notification);
        mTarget.enqueueNotificationWithTag(mHostContext.getPackageName(), tag, id, notification, idReceived, userid);
    }

    public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification,
                                           int[] idReceived, int userId) {
        remapNotification(notification);
        mTarget.enqueueNotificationWithTag(mHostContext.getPackageName(), opPkg, tag, id, notification, idReceived, userId);
    }

    public void enqueueNotificationWithTag(String pkg, String tag, int id, Notification notification, int[] idReceived) {
        remapNotification(notification);
        mTarget.enqueueNotificationWithTag(mHostContext.getPackageName(), tag, id, notification, idReceived);
    }

    public void enqueueToast(String pkg, ITransientNotification callback, int duration) {
        // android 5.0 需要替换为 host app 的 pkgname, 要不然 Ops manager 会禁止
        mTarget.enqueueToast(mHostContext.getPackageName(), callback, duration);
    }

    void enqueueToastUnrepeated(String pkg, ITransientNotification callback, CharSequence localObject, int duration) {
        // 兼容MX4 4.4.2的某个ROM,系统修改了这个方法名字，需要替换成host app的pkg, 要不然 Ops manager 会禁止
        mTarget.enqueueToastUnrepeated(mHostContext.getPackageName(), callback, localObject, duration);
    }

    public void cancelToast(String pkg, ITransientNotification callback) {
        // android 5.0 需要替换为 host app 的 pkgname, 要不然 Ops manager 会禁止
        mTarget.cancelToast(mHostContext.getPackageName(), callback);
    }

    public void cancelAllNotifications(String pkg) {
        mTarget.cancelAllNotifications(mHostContext.getPackageName());
    }

    public void cancelAllNotifications(String pkg, int userId) {
        mTarget.cancelAllNotifications(mHostContext.getPackageName(), userId);
    }

    public void cancelNotificationWithTag(String pkg, String tag, int id) {
        mTarget.cancelNotificationWithTag(mHostContext.getPackageName(), tag, id);
    }

    public void cancelNotificationWithTag(String pkg, String tag, int id, int userId) {
        mTarget.cancelNotificationWithTag(mHostContext.getPackageName(), tag, id, userId);
    }

    public void removeEdgeNotification(String pkg, int param1, Bundle param2, int param3) {

        // Sumsang S6 edge 5.1 的方法，需要替换包名
        mTarget.removeEdgeNotification(mHostContext.getPackageName(), param1, param2, param3);
    }
}

