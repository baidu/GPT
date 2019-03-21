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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;

/**
 * PushManager
 * 注:Push相关测试,和具体产品功能相关,其它新产品根据实际情况可不关注。
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
@SuppressLint({"WorldReadableFiles"})
public class PushManager {
    private static final String a = "PushManager";
    private static int b = 0;
    private static final String c = "baidu/hybrid";
    private static final String d = "notimap";

    @SuppressLint({"UseSparseArrays"})
    private static HashMap e = new HashMap();


    public static void activityStarted(Activity paramActivity) {

    }

    public static void activityStoped(Activity paramActivity) {

    }

    public static void bind(Context paramContext, int paramInt) {

    }

    public static void bindGroup(Context paramContext, String paramString) {

    }

    public static Intent createMethodIntent(Context paramContext) {
        Intent localIntent;

        return null;
    }

    public static void delLappTags(Context paramContext, String paramString, List paramList) {

    }

    public static void delSDKTags(Context paramContext, String paramString, List paramList) {

    }

    public static void delTags(Context paramContext, List paramList) {
    }

    public static void deleteMessages(Context paramContext, String[] paramArrayOfString) {

    }

    public static void disableLbs(Context paramContext) {

    }

    public static void enableLbs(Context paramContext) {

    }

    public static void fetchGroupMessages(Context paramContext, String paramString, int paramInt1, int paramInt2) {

    }

    public static void fetchMessages(Context paramContext, int paramInt1, int paramInt2) {

    }

    public static HashMap getAppNotiMap() {
        try {
            File localFile1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "baidu/hybrid");
            if (!localFile1.exists())
                return null;
            File localFile2 = new File(localFile1, "notimap");
            if (!localFile2.exists())
                return null;
            FileInputStream localFileInputStream = new FileInputStream(localFile2);
            ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);
            HashMap localHashMap = (HashMap) localObjectInputStream.readObject();
            try {
                localObjectInputStream.close();
                localFileInputStream.close();
                return localHashMap;
            } catch (Exception localException2) {
                return localHashMap;
            }
        } catch (Exception localException1) {
        }
        return null;
    }

    public static void getGroupInfo(Context paramContext, String paramString) {

    }

    public static void getGroupList(Context paramContext) {

    }

    public static void getGroupMessageCounts(Context paramContext, String paramString) {

    }

    public static void getLappBindState(Context paramContext, String paramString) {

    }

    public static void getMessageCounts(Context paramContext) {

    }

    public static void init(Context paramContext, String paramString) {

    }

    public static void init(Context paramContext, String paramString1, String paramString2) {

    }

    public static void initFromAKSK(Context paramContext, String paramString) {

    }

    public static boolean isConnected(Context paramContext) {
        return false;
    }

    public static boolean isPushEnabled(Context paramContext) {
        return false;
    }

    public static void listLappTags(Context paramContext, String paramString) {

    }

    public static void listSDKTags(Context paramContext, String paramString) {

    }

    public static void listTags(Context paramContext) {

    }

    public static void resumeWork(Context paramContext) {

    }

    public static void saveAppNotiMap(HashMap paramHashMap) {

    }

    public static void sdkStartWork(Context paramContext, String paramString, int paramInt) {

    }

    public static void sdkUnbind(Context paramContext, String paramString) {

    }

    public static void sendMsgToUser(Context paramContext, String paramString1, String paramString2, String paramString3, String paramString4) {

    }

    public static void setAccessToken(Context paramContext, String paramString) {

    }

    public static void setApiKey(Context paramContext, String paramString) {

    }

    public static void setBduss(Context paramContext, String paramString) {

    }

    public static void setDefaultNotificationBuilder(Context paramContext, PushNotificationBuilder paramPushNotificationBuilder) {

    }

    public static void setLappTags(Context paramContext, String paramString, List paramList) {

    }

    public static void setMediaNotificationBuilder(Context paramContext, PushNotificationBuilder paramPushNotificationBuilder) {

    }

    public static void setNoDisturbMode(Context paramContext, int paramInt1, int paramInt2, int paramInt3, int paramInt4) {

    }

    public static void setNotificationBuilder(Context paramContext, int paramInt, PushNotificationBuilder paramPushNotificationBuilder) {

    }

    public static void setSDKTags(Context paramContext, String paramString, List paramList) {

    }

    public static void setTags(Context paramContext, List paramList) {

    }

    public static void startWork(Context paramContext, int paramInt, String paramString) {

    }

    public static void startWork(Context paramContext, String paramString1, String paramString2) {

    }

    public static void stopWork(Context paramContext) {

    }

    public static void tryConnect(Context paramContext) {

    }

    public static void unbind(Context paramContext) {

    }

    public static void unbindGroup(Context paramContext, String paramString) {

    }
}

