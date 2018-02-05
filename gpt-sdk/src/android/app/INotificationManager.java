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

import android.os.Bundle;

/**
 * Hook系统的对应接口类方法
 * /device/java/android/android/app/INotificationManager.aidl
 *
 * @author liuhaitao
 * @since 2018-01-10
 */
public interface INotificationManager {
    /**
     * 2.x   @deprecated use {@link #enqueueNotificationWithTag} instead
     */
    void enqueueNotification(String pkg, int id, Notification notification, int[] idReceived);

    /**
     * 2.x 4.0 4.1
     */
    void enqueueNotificationWithTag(String pkg, String tag, int id, Notification notification, int[] idReceived);

    /**
     * 4.2
     */
    void enqueueNotificationWithTag(String pkg, String tag, int id, Notification notification, int[] idReceived,
                                    int userid);

    /**
     * 4.4 5.0 5.1 6.0
     */
    void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification,
                                    int[] idReceived, int userId);

    /**
     * 2.x 4.x 5.x 6.0
     */
    void enqueueToast(String pkg, ITransientNotification callback, int duration);

    /**
     * 兼容MX4 4.4.2的某个ROM,系统修改了这个方法名字
     */
    void enqueueToastUnrepeated(String pkg, ITransientNotification callback, CharSequence localObject, int duration);

    /**
     * 2.x 4.x 5.x 6.0
     */
    void cancelToast(String pkg, ITransientNotification callback);

    /**
     * 2.x
     */
    void cancelAllNotifications(String pkg);

    /**
     * 4.x 5.x 6.0
     */
    void cancelAllNotifications(String pkg, int userId);

    /**
     * 2.x
     */
    void cancelNotificationWithTag(String pkg, String tag, int id);

    /**
     * 4.x 5.x 6.0
     */
    void cancelNotificationWithTag(String pkg, String tag, int id, int userId);

    /**
     * Sumsang S6 edge 5.1
     */
    void removeEdgeNotification(String pkg, int param1, Bundle param2, int param3);
}

