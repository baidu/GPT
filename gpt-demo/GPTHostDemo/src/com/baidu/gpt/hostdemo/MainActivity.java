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
package com.baidu.gpt.hostdemo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.baidu.android.gporter.api.IGetContextCallBack;
import com.baidu.android.gporter.api.ITargetLoadedCallBack;
import com.baidu.android.gporter.api.TargetActivator;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.proxy.ActivityManagerNativeWorker;
import com.baidu.android.gporter.proxy.AlarmManagerWork;
import com.baidu.android.gporter.proxy.BinderWork;
import com.baidu.android.gporter.proxy.MountServiceWork;
import com.baidu.android.gporter.proxy.NotificationManagerNativeWorker;
import com.baidu.android.gporter.proxy.PackageManagerWorker;
import com.baidu.android.gporter.proxy.WifiManagerWorker;
import com.baidu.android.gporter.proxy.WindowSessionWorker;
import com.baidu.android.gporter.rmi.Naming;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;
import com.harlan.animation.rmi.IClient;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static com.baidu.gpt.hostdemo.Util.writeFile;

/**
 * GPTHostDemo的主Activity功能界面
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public class MainActivity extends FragmentActivity {

    /**
     * DEBUG 开关
     * GPTHostDemo目前独立类较少,所以先不单独加Constants类,
     * 同时为了和gpt-sdk的主Debug日志开关控制逻辑保持一致,所以直接采用DEBUG = true & Constants.DEBUG的形式。
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    private static final String TAG = "MainActivity";

    /**
     * 默认SD卡上的插件APK扫描加载测试路径,为了方便直接放在外部存储蓄路径的"/baidu_plugin_test/"下,
     * 默认设备对应全路径为"/sdcard/baidu_plugin_test/",
     * 高版本ROM上可能需要在Android系统设置中打开GPTHostDemo的应用读写存储设备的权限,
     * TODO 用户也可以自定义扫描加载其它内外部可识别路径,并将插件APK放置到对应路径中,
     * 更可打印查看Log详细路径信息。
     */
    private static final String PLUGIN_SCAN_PATH = "/baidu_plugin_test";

    /**
     * Activity的Root View
     */
    private ViewGroup mRootView = null;

    /**
     * 头部的View
     */
    private ViewGroup mHeaderView = null;

    /**
     * 正在安装插件标志
     */
    boolean isInstalling = false;

    Map<String, String> classMap = new ArrayMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate(Bundle savedInstanceState): savedInstanceState=" + savedInstanceState);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mRootView = (ViewGroup) findViewById(R.id.root);
        mHeaderView = (ViewGroup) findViewById(R.id.header);

        // 添加插件APK的显示的Fragment
        Fragment listApkFragment = new ListApkFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, listApkFragment);
        fragmentTransaction.commit();

        // "扫描加载插件"显示点击处理
        View scanLoadPlugin = mHeaderView.findViewById(R.id.scan_load_plugin);
        scanLoadPlugin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isInstalling) {
                    return;
                }

                isInstalling = true;

                File pluginScanLoadDir = new File(Environment.getExternalStorageDirectory(), PLUGIN_SCAN_PATH);
                if (DEBUG) {
                    Log.d(TAG, "onCreate(Bundle savedInstanceState): pluginScanLoadDir="
                            + pluginScanLoadDir.getAbsolutePath());
                }

                new AsyncTask<File, Integer, Void>() {

                    @Override
                    protected Void doInBackground(File... params) {

                        if (params[0] != null && params[0].isDirectory() && params[0].exists()) {
                            String[] files = params[0].list();
                            if (files != null) {
                                String filePath = "";
                                for (String file : files) {
                                    filePath = params[0].getAbsolutePath() + File.separator + file;
                                    GPTPackageManager.getInstance(getApplicationContext()).installApkFile(filePath);
                                    if (DEBUG) {
                                        Log.d(TAG, "AsyncTask doInBackground(File... params): "
                                                + "GPTPackageManager.getInstance(getApplicationContext()).installApkFile(filePath): "
                                                + "\n filePath=" + filePath);
                                    }
                                }
                            }
                        }

                        return null;
                    }

                    protected void onPostExecute(Void result) {
                        isInstalling = false;
                    }
                }.execute(pluginScanLoadDir);

            }
        });

        // TODO 以下为GPT插件框架部分功能测试入口,独立产品可参考使用或直接删除。
        if (DEBUG) {
            // "Remote Call"显示点击处理
            View remoteCall = mHeaderView.findViewById(R.id.remote_call);
            remoteCall.setVisibility(View.VISIBLE);
            remoteCall.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    TargetActivator.loadTarget(MainActivity.this, "com.harlan.animation", new ITargetLoadedCallBack() {

                        @Override
                        public void onTargetLoaded(String packageName, boolean isSucc) {
                            if (DEBUG) {
                                Log.d(TAG, "remoteCall: onTargetLoaded(String packageName, boolean isSucc)"
                                        + ": packageName=" + packageName + ": isSucc=" + isSucc);
                            }

                            try {
                                IBinder binlder = Naming.lookupPlugin("com.harlan.animation",
                                        "com.harlan.animation.rmi.ClientApiImpl");
                                IClient client = IClient.Stub.asInterface(binlder);
                                String result = client.test("test");
                                Toast.makeText(MainActivity.this, "result from client : " + result,
                                        Toast.LENGTH_LONG).show();
                                if (DEBUG) {
                                    Log.d(TAG, "IClient client = IClient.Stub.asInterface(binlder)"
                                            + ": client=" + client.toString() + ": result=" + result);
                                }

                            } catch (Exception e) {
                                if (DEBUG) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                }
            });

            // "Test Receiver"显示点击处理
            View testReceiver = mHeaderView.findViewById(R.id.test_receiver);
            testReceiver.setVisibility(View.VISIBLE);
            testReceiver.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent("com.baidu.gpt.hostdemo.test.receiver");
                        intent.setPackage(getPackageName());
                        PendingIntent pendingIntent = PendingIntent
                                .getBroadcast(getApplicationContext(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        long triggerAtMillis = SystemClock.elapsedRealtime() + 1;
                        AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                        am.cancel(pendingIntent);
                        am.set(AlarmManager.ELAPSED_REALTIME, triggerAtMillis, pendingIntent);
                    } catch (Exception e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // "Get Context"显示点击处理
            View getContext = mHeaderView.findViewById(R.id.get_context);
            getContext.setVisibility(View.VISIBLE);
            getContext.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        TargetActivator.loadAndApplicationContext(MainActivity.this,
                                "com.harlan.animation", new IGetContextCallBack() {
                                    @Override
                                    public void getTargetApplicationContext(Context context) {
                                        if (DEBUG) {
                                            Log.d(TAG, "getTargetApplicationContext: context=" + context.toString());
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }

                }
            });

            // TODO 主要Hook类对比方法的类名,后续如有新的类需要检查,直接在此按序添加即可。
            // 下面几个替换类一样,默认对比IACTIVE_MANAGER_CLASS就行。
            classMap.put(Constants.ACTIVE_MANAGER_NATIVE_CLASS, ActivityManagerNativeWorker.class.getName());
            classMap.put(Constants.IACTIVE_MANAGER_CLASS, ActivityManagerNativeWorker.class.getName());
            classMap.put(Constants.ACTIVE_MANAGER_CLASS, ActivityManagerNativeWorker.class.getName());

            classMap.put(Constants.NOTIFICATION_MANAGER_NATIVE_CLASS,
                    NotificationManagerNativeWorker.class.getName());
            classMap.put(Constants.PACKAGE_MANAGER_CLASS, PackageManagerWorker.class.getName());
            classMap.put(Constants.WINDOW_SESSION_CLASS, WindowSessionWorker.class.getName());
            classMap.put(Constants.WIFI_MANAGER_CLASS, WifiManagerWorker.class.getName());
            classMap.put(Constants.IALARM_MANAGERR_CLASS, AlarmManagerWork.class.getName());
            classMap.put(Constants.SERVICE_MANAGER_CLASS, BinderWork.class.getName());
            classMap.put(Constants.IMOUNT_SERVICE_CLASS, MountServiceWork.class.getName());

            // "Hook类方法对比"显示点击处理
            View hookCompare = mHeaderView.findViewById(R.id.hook_compare);
            hookCompare.setVisibility(View.VISIBLE);
            hookCompare.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        Context hostContext = Util.getHostContext(MainActivity.this);

                        // 结果输出文件
                        File androidClassMethodFile = new File(hostContext.getExternalFilesDir(null)
                                + Build.MODEL + "_" + Build.BRAND + "_" + Build.MANUFACTURER + "_"
                                + Build.VERSION.RELEASE + "_" + Build.VERSION.SDK_INT + "_"
                                + "AndroidClassMethodFile.txt");
                        File gptClassMethodFile = new File(hostContext.getExternalFilesDir(null)
                                + Build.MODEL + "_" + Build.BRAND + "_" + Build.MANUFACTURER + "_"
                                + Build.VERSION.RELEASE + "_" + Build.VERSION.SDK_INT + "_"
                                + "GptClassMethodFile.txt");

                        // 如果有旧文件则先删除,保证是最新设备结果。
                        if (androidClassMethodFile.exists()) {
                            androidClassMethodFile.delete();
                        }
                        if (gptClassMethodFile.exists()) {
                            gptClassMethodFile.delete();
                        }

                        if (DEBUG) {
                            Log.d(TAG, "public void onClick(View v): androidClassMethodFile = "
                                    + androidClassMethodFile + "; gptClassMethodFile = " + gptClassMethodFile);
                        }

                        for (String key : classMap.keySet()) {
                            if (DEBUG) {
                                Log.d(TAG, "public void onClick(View v): for (String key : calssMap.keySet()): key = "
                                        + key + "; className = " + classMap.get(key));
                            }
                            printMethodInfo(key, androidClassMethodFile);
                            printMethodInfo(classMap.get(key), gptClassMethodFile);
                        }

                        Toast.makeText(MainActivity.this, "主要类方法结果已保存到文件:"
                                        + "\n" + androidClassMethodFile.getAbsolutePath()
                                        + "\n" + gptClassMethodFile.getAbsolutePath(),
                                Toast.LENGTH_LONG).show();

                    } catch (Throwable e) {
                        if (DEBUG) {
                            Log.d(TAG, "public void onClick(View v): "
                                    + "catch (Throwable e): e = " + e.toString());
                            e.printStackTrace();
                        }
                    }
                }
            });

            // "Hook类方法对比"显示点击处理
            View androidPTest = mHeaderView.findViewById(R.id.androidp_test);
            androidPTest.setVisibility(View.VISIBLE);
            androidPTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (String key : classMap.keySet()) {
                        if (DEBUG) {
                            Log.d(TAG, "public void onClick(View v): for (String key : calssMap.keySet()): key = "
                                    + key + "; className = " + classMap.get(key));
                        }
                        testAndroidPMethod(key);
                        testAndroidPMethod(classMap.get(key));
                    }
                }
            });

        }

    }

    /**
     * 传入全路径类名输出对应类方法和参数类型
     *
     * @param className 全路径的类名
     * @param printFile 输出信息文件
     */
    @SuppressWarnings("rawtypes")
    private static void printMethodInfo(String className, File printFile) {
        StringBuilder contentString = new StringBuilder();
        try {
            if (DEBUG) {
                Log.d(TAG, "printMethodInfo(String className, File printFile): className = "
                        + className + "; printFile = " + printFile.getAbsolutePath());
            }

            Class clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();
            contentString.append("\n\n************************************\n");
            contentString.append("      printFile = ").append(printFile.getAbsolutePath()).append("\n");
            contentString.append("      className = ").append(className).append("\n");
            contentString.append("      methodNum = ").append(methods.length).append("\n");
            contentString.append("************************************\n");

            for (Method method : methods) {
                String methodName = method.getName();
                contentString.append("\n").append(methodName);

                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<?> parameterClass = null;
                for (int index = 0; index < parameterTypes.length; index++) {
                    parameterClass = parameterTypes[index];
                    String parameterName = parameterClass.getName();
                    if (index == 0) {
                        contentString.append(" ( ").append(parameterName);
                    } else if (index == parameterTypes.length) {
                        contentString.append(" , ").append(parameterName).append(" ) \n");
                    } else {
                        contentString.append(" , ").append(parameterName);
                    }
                }
            }

            writeFile(printFile.getPath(), contentString.toString());
            if (DEBUG) {
                Log.d(TAG, "printMethodInfo(String className, File printFile): className = "
                        + className + "; printFile = " + printFile.getAbsolutePath()
                        + "; contentString = " + contentString.toString());
            }

        } catch (Throwable e) {
            contentString.append("\n####################################\n");
            contentString.append("      catch (Throwable e): e = \n").append(Util.getCallStack(e)).append("\n");

            writeFile(printFile.getPath(), contentString.toString());

            if (DEBUG) {
                Log.d(TAG, "printMethodInfo(String className, File printFile): className = "
                        + className + "; printFile = " + printFile.getAbsolutePath()
                        + "; catch (Throwable e): e = " + e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始androidp方法调用测试
     * @param className 目标类
     */
    private static void testAndroidPMethod(String className) {
        try {
            if (DEBUG) {
                Log.d(TAG, "testAndroidPMethod(String className): className = "
                        + className);
            }
            Class clazz = Class.forName(className);
            Object newInstance = invokeClassInstance(clazz);
            invokeMethod(newInstance, clazz);
        } catch (Throwable throwable) {
            if (DEBUG) {
                Log.d(TAG, "It failed to instance the target class or interface!");
            }
        }
    }

    /**
     * 获取目标反射类实例
     * @param targetClass 目标类
     * @return 目标实例
     */
    private static Object invokeClassInstance(Class targetClass) {

        if (targetClass.isInterface()) {
            return Proxy.newProxyInstance(targetClass.getClassLoader(), new Class[]{targetClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return null;
                }
            });
        } else {
            Constructor[] constructrosArray = targetClass.getConstructors();
            Constructor targetConstructor = null;
            int curLength = Integer.MAX_VALUE;
            // 取出构造参数最少的构造函数
            for (Constructor tmpCon : constructrosArray) {
                if (curLength < tmpCon.getParameterTypes().length) {
                    continue;
                }
                curLength = tmpCon.getParameterTypes().length;
                targetConstructor = tmpCon;
            }
            Class<?>[] targetArgs = new Class[curLength];
            for (int i = 0; i < curLength; i++) {
                targetArgs[i] = null;
            }
            try {
                targetConstructor.setAccessible(true);
                return targetConstructor.newInstance(targetArgs);
            } catch (Throwable throwable) {
                Log.d(TAG, "It failed to construct the Instance!");
                return null;
            }
        }
    }

    /**
     * 反射方法
     * @param targetInstance 目标类实例
     * @param targetClass 目标类
     */
    private static void invokeMethod(Object targetInstance, Class targetClass) {
        Method[] methods = targetClass.getMethods();
        StringBuilder contentString = new StringBuilder();
        for (Method method : methods) {
            String methodName = method.getName();
            contentString.append("\n").append(methodName);
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> parameterClass = null;
            for (int index = 0; index < parameterTypes.length; index++) {
                parameterClass = parameterTypes[index];
                String parameterName = parameterClass.getName();
                if (index == 0) {
                    contentString.append(" ( ").append(parameterName);
                } else if (index == parameterTypes.length) {
                    contentString.append(" , ").append(parameterName).append(" ) \n");
                } else {
                    contentString.append(" , ").append(parameterName);
                }
            }
            try {
                method.invoke(targetInstance, parameterTypes);
            } catch (Throwable throwable) {
                // Do Nothing
            }
        }

    }

}