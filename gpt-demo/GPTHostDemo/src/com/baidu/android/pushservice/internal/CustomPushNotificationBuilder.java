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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * CustomPushNotificationBuilder
 * 注:Push相关测试,其它新产品可不关注。
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public class CustomPushNotificationBuilder extends PushNotificationBuilder {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public CustomPushNotificationBuilder(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
        this.a = paramInt1;
        this.b = paramInt2;
        this.c = paramInt3;
        this.d = paramInt4;
    }

    private void readObject(ObjectInputStream paramObjectInputStream) {

    }

    private void writeObject(ObjectOutputStream paramObjectOutputStream) {

    }

    public Notification construct(Context paramContext) {
        Notification localNotification = new Notification();

        return localNotification;
    }

    public void setLayoutDrawable(int paramInt) {
        this.e = paramInt;
    }

    public void setNotificationDefaults(int paramInt) {
        super.setNotificationDefaults(paramInt);
    }

    public void setNotificationFlags(int paramInt) {
        super.setNotificationFlags(paramInt);
    }

    public void setNotificationSound(Uri paramUri) {
        super.setNotificationSound(paramUri);
    }

    public void setNotificationText(String paramString) {
        super.setNotificationText(paramString);
    }

    public void setNotificationTitle(String paramString) {
        super.setNotificationTitle(paramString);
    }

    public void setNotificationVibrate(long[] paramArrayOfLong) {
        super.setNotificationVibrate(paramArrayOfLong);
    }

    public void setStatusbarIcon(int paramInt) {
        super.setStatusbarIcon(paramInt);
    }
}