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
package android.app;

import java.util.List;

import android.content.ComponentName;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * Hook系统的对应接口类方法
 * System private API for talking with the activity manager service. This
 * provides calls from the application back to the activity manager.
 * <p>
 * {@hide}
 *
 * @author liuhaitao
 * @since 2018-01-09
 */
public interface IActivityManager {

    /**
     * 2.x 3.x
     */
    public Intent registerReceiver(IApplicationThread caller, String callerPackage, IIntentReceiver receiver,
                                   IntentFilter filter, String requiredPermission);

    /**
     * 4.x 5.x 6.x 7.X
     */
    public Intent registerReceiver(IApplicationThread caller, String callerPackage,
                                   IIntentReceiver receiver, IntentFilter filter,
                                   String requiredPermission, int userId);

    /**
     * 8.X
     */
    public Intent registerReceiver(IApplicationThread caller, String callerPackage,
                                   IIntentReceiver receiver, IntentFilter filter,
                                   String requiredPermission, int userId, int flags);

    /**
     * 2.x
     */
    public IIntentSender getIntentSender(int type,
                                         String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent intent, String resolvedType, int flags);

    /**
     * 4.0
     */
    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags) throws RemoteException;

    /**
     * 4.1
     */
    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String reesultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle options)
            throws RemoteException;

    /**
     * 4.4 5.x 6.0
     */
    public IIntentSender getIntentSender(int type, String packageName, IBinder token, String resultWho,
                                         int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle options, int userId)
            throws RemoteException;

    /**
     * getRunningAppProcesses
     */
    public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();

    /*
     * public List getTasks(int maxNum, int flags,
     * IThumbnailReceiver receiver) throws RemoteException;
     */

    /**
     * 2.x
     */
    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType);

    /**
     * 4.x 5.x
     */
    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType, int userId);

    /**
     * 6.0
     */
    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType, String callingPackage, int userId);

    /**
     * 7.X 三星
     */
    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType,
                                      String callingPackage, int userId, IBinder token, int l);

    /**
     * 8.X
     */
    public ComponentName startService(IApplicationThread caller, Intent service,
                                      String resolvedType, int i, Notification notification, String callingPackage, int userId);

    /**
     * 2.x
     */
    public int stopService(IApplicationThread caller, Intent service,
                           String resolvedType);

    /**
     * 4.x 5.x 6.0
     */
    public int stopService(IApplicationThread caller, Intent service,
                           String resolvedType, int userId);

    /**
     * 2.x
     */
    public int bindService(IApplicationThread caller, IBinder token,
                           Intent service, String resolvedType,
                           IServiceConnection connection, int flags);

    /**
     * 4.x 5.x
     */
    public int bindService(IApplicationThread caller, IBinder token,
                           Intent service, String resolvedType,
                           IServiceConnection connection, int flags, int userId);

    /**
     * 6.0
     */
    public int bindService(IApplicationThread caller, IBinder token, Intent service,
                           String resolvedType, IServiceConnection connection, int flags,
                           String callingPackage, int userId);

    /**
     * 2.x 4.x 5.x 6.x
     */
    public boolean unbindService(IServiceConnection connection);

    /**
     * android 2.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               boolean serialized, boolean sticky);

    /**
     * android 4.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               boolean serialized, boolean sticky, int userId);

    /**
     * android 5.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String requiredPermission,
                               int appOp, boolean serialized, boolean sticky, int userId);

    /**
     * android 6.x
     */
    public int broadcastIntent(IApplicationThread caller, Intent intent,
                               String resolvedType, IIntentReceiver resultTo, int resultCode,
                               String resultData, Bundle map, String[] requiredPermissions,
                               int appOp, Bundle options, boolean serialized, boolean sticky, int userId);

    /**
     * 2.3
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller,
                                                    String name);

    /**
     * 4.x
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller,
                                                    String name, boolean stable);

    /**
     * 5.x 6.x
     */
    public ContentProviderHolder getContentProvider(IApplicationThread caller,
                                                    String name, int userId, boolean stable);

    /**
     * ContentProviderHolder
     */
    public static class ContentProviderHolder implements Parcelable {
        public final ProviderInfo info;
        public IContentProvider provider;
        public IBinder connection;
        public boolean noReleaseNeeded;

        public ContentProviderHolder(ProviderInfo _info) {
            info = _info;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }

    /**
     * 2.3.x
     */
    public int startActivity(IApplicationThread caller,
                             Intent intent, String resolvedType, Uri[] grantedUriPermissions,
                             int grantedMode, IBinder resultTo, String resultWho, int requestCode,
                             boolean onlyIfNeeded, boolean debug);

    /**
     * 4.0.3
     */
    public int startActivity(IApplicationThread caller,
                             Intent intent, String resolvedType, Uri[] grantedUriPermissions,
                             int grantedMode, IBinder resultTo, String resultWho, int requestCode,
                             boolean onlyIfNeeded, boolean debug, String profileFile,
                             ParcelFileDescriptor profileFd,
                             boolean autoStopProfiler);

    /**
     * 4.1
     */
    public int startActivity(IApplicationThread caller,
                             Intent intent, String resolvedType, IBinder resultTo, String resultWho,
                             int requestCode, int flags, String profileFile,
                             ParcelFileDescriptor profileFd, Bundle options);

    /**
     * 4.4
     */
    public int startActivity(IApplicationThread caller, String callingPackage,
                             Intent intent, String resolvedType, IBinder resultTo, String resultWho,
                             int requestCode, int flags, String profileFile,
                             ParcelFileDescriptor profileFd, Bundle options);

    /**
     * 5.0, 5.1, 6.0
     */
    public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
                             String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags,
                             ProfilerInfo profilerInfo, Bundle options);

    /**
     * 2.3(没有此函数) 4.x, 5.x, 6.0
     */
    public void unstableProviderDied(IBinder connection);

    /**
     * getTaskForActivity
     */
    public int getTaskForActivity(IBinder token, boolean onlyRoot);

    /**
     * isTopOfTask
     */
    public boolean isTopOfTask(IBinder token);

}



