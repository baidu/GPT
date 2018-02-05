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
package com.baidu.android.pushservice.internal;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;

import java.io.Serializable;

/**
 * PushNotificationBuilder其
 * 注:Push相关测试,其它新产品可不关注。
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public abstract class PushNotificationBuilder implements Serializable {

    /**
     * NotificationDefaults
     */
    protected int mNotificationDefaults;
    /**
     * NotificationFlags
     */
    protected int mNotificationFlags;
    /**
     * NotificationText
     */
    protected String mNotificationText;
    /**
     * NotificationTitle
     */
    protected String mNotificationTitle;
    /**
     * Notificationsound
     */
    protected Uri mNotificationsound;
    /**
     * StatusbarIcon
     */
    protected int mStatusbarIcon;
    /**
     * VibratePattern
     */
    protected long[] mVibratePattern;

    /**
     * construct
     *
     * @param paramContext Context
     * @return Notification
     */
    public abstract Notification construct(Context paramContext);

    /**
     * setNotificationDefaults
     *
     * @param paramInt 需要设置的值
     */
    public void setNotificationDefaults(int paramInt) {
        this.mNotificationDefaults = paramInt;
    }

    /**
     * setNotificationFlags
     *
     * @param paramInt 需要设置的值
     */
    public void setNotificationFlags(int paramInt) {
        this.mNotificationFlags = paramInt;
    }

    /**
     * setNotificationSound
     *
     * @param paramUri Uri
     */
    public void setNotificationSound(Uri paramUri) {
        this.mNotificationsound = paramUri;
    }

    /**
     * setNotificationText
     *
     * @param paramString 需要设置的值
     */
    public void setNotificationText(String paramString) {
        this.mNotificationText = paramString;
    }

    /**
     * setNotificationTitle
     *
     * @param paramString 需要设置的值
     */
    public void setNotificationTitle(String paramString) {
        this.mNotificationTitle = paramString;
    }

    /**
     * setNotificationVibrate
     *
     * @param paramArrayOfLong 需要设置的long[]值
     */
    public void setNotificationVibrate(long[] paramArrayOfLong) {
        this.mVibratePattern = paramArrayOfLong;
    }

    /**
     * setStatusbarIcon
     *
     * @param paramInt 需要设置的值
     */
    public void setStatusbarIcon(int paramInt) {
        this.mStatusbarIcon = paramInt;
    }

}


