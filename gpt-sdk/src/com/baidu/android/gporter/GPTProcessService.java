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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.baidu.android.gporter.stat.PluginTimeLine;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 插件运行的独立进程守护Service
 *
 * @author liuhaitao
 * @since 2014年12月22日
 */
public class GPTProcessService extends Service {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "GPTProcessService";

    /**
     * 主线程Handler
     */
    private Handler mHandler = new Handler();

    /**
     * 启动intent的线程，最大1个线程，缓存时间30s
     */
    private ExecutorService mLaunchIntentsThreadPool = null;


    /**
     * bind {@link MainProcessService} 后的接口。 用于调用主程序函数。这里主要用于插件和主程序的接口函数调用。rmi 部分。
     */
    public static IHostBinder sHostBinder = null;

    /**
     * ConnectBinder
     */
    private class ConnectBinder extends IGPTBinder.Stub {

        @Override
        public void launchIntents(final String packageName, final Intent[] intents, final PluginTimeLine timeLine,
                                  final long hotTimeStart, IGPTEnvCallBack back)
                throws RemoteException {

            final IGPTEnvCallBack binderCallback = IGPTEnvCallBack.Stub.asInterface(back.asBinder());

            mLaunchIntentsThreadPool.submit(new Runnable() {

                public void run() {
                    // 处理timeLine
                    if (timeLine != null) {
                        timeLine.process = Constants.GPT_PROCESS_DEFAULT;
                        ProxyEnvironment.pluginTimeLineMap.put(packageName, timeLine);
                    }
                    // 热启动时间记录
                    if (hotTimeStart > 0) {
                        ProxyEnvironment.pluginHotStartTimeMap.put(packageName, hotTimeStart);
                    }
                    ProxyEnvironment.initProxyEnvironment(getApplicationContext(), packageName);

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {

                            if (intents != null) {
                                for (Intent it : intents) {
                                    ProxyEnvironment.launchIntent(getApplicationContext(), it, null);
                                }
                            } else {
                                Intent stubIntent = new Intent();
                                stubIntent.setComponent(new ComponentName(packageName,
                                        ProxyEnvironment.LOADTARGET_STUB_TARGET_CLASS));
                                ProxyEnvironment.launchIntent(getApplicationContext(), stubIntent, null);
                            }
                            try {
                                binderCallback.onTargetLoaded(packageName);
                            } catch (RemoteException e) {
                                if (DEBUG) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }

            });

        }

        @Override
        public boolean exitProxy(String packageName, boolean force) throws RemoteException {
            if (force) { // 强制退出插件时，把进程杀了。
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            boolean b = ProxyEnvironment.exitProxy(packageName, force);
            return b;
        }

        @Override
        public IBinder getInterface(String packageName, String className) throws RemoteException {
            return getTargetInterface(packageName, className);
        }

        @Override
        public boolean hasInstance(String packageName) throws RemoteException {
            return ProxyEnvironment.hasInstance(packageName);
        }


    }

    /**
     * getTargetInterface
     *
     * @param packageName 包名
     * @param className   类名
     * @return IBinder
     */
    public static IBinder getTargetInterface(String packageName, String className) {

        if (DEBUG) {
            Log.i(TAG, "--- getTargetInterface, pkg=" + packageName + ", class=" + className);
        }
        // 是否已经被加载
        boolean loaded = ProxyEnvironment.hasInstance(packageName);

        if (!loaded) {
            // 如果没有加载，直接返回，因为初期是要求调用者先初始化插件。然后再调用插件接口。
            return null;
        }

        ProxyEnvironment env = ProxyEnvironment.getInstance(packageName);
        ClassLoader cl = env.getDexClassLoader(); // 使用插件classloader

        IBinder binder = null;

        if (className != null && className.length() > 0) {
            try {
                // 从classloader 中加载接口类
                Class clazz = cl.loadClass(className);

                Object ri = (Object) clazz.newInstance();
                // 这里不能直接进行类的强转。因为插件和host中都有remote类。会转换失败。所以反射调用。
                binder = JavaCalls.callMethod(ri, "getIBinder"); // ri.getIBinder();

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

        //  这个对bind方式不起作用
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ConnectBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLaunchIntentsThreadPool = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, // SUPPRESS CHECKSTYLE
                new LinkedBlockingQueue<Runnable>(), new com.baidu.android.gporter.util.NamingThreadFactory(
                "GPTProcessService-launchIntents-"));

        Intent intent = new Intent();
        intent.setClass(this, MainProcessService.class);

        ServiceConnection sc = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                sHostBinder = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                sHostBinder = IHostBinder.Stub.asInterface(service);
            }
        };

        getApplicationContext().bindService(intent, sc, Context.BIND_AUTO_CREATE);
    }
}
