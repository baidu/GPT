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
package com.harlan.animation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * MyService
 *
 * @author liuhaitao
 * @since 2014-11-28
 */
public class MyService extends Service {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "MyService";

    /**
     * MyBinder
     */
    public static class MyBinder extends IMyAidl.Stub {

        @Override
        public void test() throws RemoteException {
            if (DEBUG) {
                Log.d(TAG, "public void test() throws RemoteException: aidl test() is called ");
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "public IBinder onBind(Intent intent):new MyBinder(): intent=" + intent);
        }
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d(TAG, "onCreate(): MyService start!");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            Log.d(TAG, "public int onStartCommand(Intent intent, int flags, int startId): intent="
                    + intent + "; flags=" + flags + "; startId=" + startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "onDestroy(): MyService onDestroy!");
        }

    }

}


