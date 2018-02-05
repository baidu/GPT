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
package com.baidu.android.gporter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.baidu.android.gporter.pm.ISignatureVerify;
import com.baidu.android.gporter.rmi.Remote;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;

/**
 * 运行在主进程中。用于 gpt 独立进程中有些任务需要从主进程发起的时候,通知此service代替其处理。
 * <p>
 * 另外一个重要功能是 gpt进程和 host进程之间通过 aidl 进行接口调用。
 *
 * @author liuhaitao
 * @since 2015年4月24日
 */
public class MainProcessService extends Service {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "MainProcessService";

    /**
     * 使用主进程调用    {@link ProxyEnvironment#enterProxy(android.content.Context, Intent, boolean, boolean)}
     */
    public static final String ACTION_ENTER_PROXY = "action_enter_proxy";
    /**
     * 字符串类型。使用 {@link #toUri} and {@link #parseUri} 进行intent转换
     */
    public static final String EXTRA_INTENT = "extra_intent";
    /**
     * key:extra_silent
     */
    public static final String EXTRA_IS_SILENT = "extra_silent";
    /**
     * key:extra_reboot
     */
    public static final String EXTRA_FOR_REBOOT = "extra_reboot";
    /**
     * key:extra_extprocess
     */
    public static final String EXTRA_EXT_PROCESS = "extra_extprocess";

    /**
     * ISignatureVerify
     */
    private ISignatureVerify mSignatureChecker = null;

    /**
     * ServiceHandler
     */
    private volatile ServiceHandler mServiceHandler;

    /**
     * ServiceHandler
     */
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }


    @Override
    public void onCreate() {

        super.onCreate();

        // 使用主线程进行处理
        mServiceHandler = new ServiceHandler(getMainLooper());
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new HostBinder();
    }

    /**
     * HostBinder
     */
    private class HostBinder extends IHostBinder.Stub {

        @Override
        public IBinder getInterface(String className) throws RemoteException {
            IBinder binder = getTargetInterface(className);

            return binder;
        }

        @Override
        public boolean isTargetLoaded(String pkgName) throws RemoteException {
            boolean isLoaded = ProxyEnvironment.isEnterProxy(pkgName);
            if (!isLoaded) {
                isLoaded = ProxyEnvironment.hasInstanceInGPTProcess(pkgName);
            }
            return isLoaded;
        }

        @Override
        public boolean checkSignature(String packageName, boolean isReplace, Signature[] oldSignatures,
                                      Signature[] newSignatures) throws RemoteException {
            if (mSignatureChecker == null) {
                mSignatureChecker = getHostSignatureVerifier(MainProcessService.this.getApplicationContext());
            }
            if (mSignatureChecker != null) {
                return mSignatureChecker.checkSignature(packageName, isReplace, oldSignatures, newSignatures);
            } else {
                return false;
            }
        }

    }

    /**
     * classname 为 {@link Remote} 的实现类。
     *
     * @param className
     * @return IBinder or null
     */
    public static IBinder getTargetInterface(String className) {
        IBinder binder = null;

        if (className != null && className.length() > 0) {
            try {
                Class clazz = Class.forName(className);

                Remote ri = (Remote) clazz.newInstance();

                binder = ri.getIBinder();

            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 使用主线程进行处理
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    private void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (ACTION_ENTER_PROXY.equals(action)) {
            Intent targetIntent = intent.getParcelableExtra(EXTRA_INTENT);
            boolean isSilent = intent.getBooleanExtra(EXTRA_IS_SILENT, false);
            boolean forReboot = intent.getBooleanExtra(EXTRA_FOR_REBOOT, false);
            int extProcess = intent.getIntExtra(EXTRA_EXT_PROCESS, Constants.GPT_PROCESS_DEFAULT);

            ProxyEnvironment.enterProxy(getApplicationContext(), targetIntent, isSilent, forReboot, true, extProcess);
        }
    }


    /**
     * 获取hostapp签名类的实例，如果存在，则使用hostapp的签名类进行签名校验。
     *
     * @param ctx application context
     * @return 签名校验器的实例 or null
     */
    private ISignatureVerify getHostSignatureVerifier(Context ctx) {

        Object obj = Util.getHostMetaDataClassInstance(ctx, ISignatureVerify.MATA_DATA_VERIFY_CLASS);
        if (obj instanceof ISignatureVerify) {
            if (DEBUG) {
                Log.d(TAG, "getHostSignatureVerifier(): host SignatureVerify class : " + obj.getClass().getName());
            }
            return (ISignatureVerify) obj;
        } else {
            return null;
        }
    }
}
