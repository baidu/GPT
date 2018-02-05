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
package com.baidu.android.gporter.proxy.service;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.proxy.ProxyUtil;
import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Service代理，承载所有插件Service
 *
 * @author liuhaitao
 * @since 2014年12月3日
 */
public class ServiceProxy extends Service {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ServiceProxy";

    /**
     * 停止Service的方法
     */
    private static final String METHOD_STOPSERVICETOKEN = "stopServiceToken";
    /**
     * 改变Service前后台的方法
     */
    private static final String METHOD_SETSERVICEFOREGROUND = "setServiceForeground";
    /**
     * 存储Services的SP文件
     */
    private static final String SP_FILENAME = "com.baidu.android.gpt.Services.";
    /**
     * 存储Services的SP KEY
     */
    private static final String KEY_SERVICES = "runing_services";
    /**
     * Json字符串Key:包名
     */
    private static final String JKEY_PKG = "package_name";
    /**
     * Json字符串Key:类名
     */
    private static final String JKEY_CLASS_NAME = "class_name";
    /**
     * Json字符串Key:上一次Start的Intent
     */
    private static final String JKEY_LAST_INTENT = "last_intent";

    /**
     * ServiceProxy实例
     */
    protected static ServiceProxy sInstance = null;

    /**
     * 保存启动的Service的信息
     *
     * @author liuhaitao
     * @since 2014年12月3日
     */
    static class ServiceRecord {

        /**
         * Service对应的ComponentName
         */
        ComponentName name;
        /**
         * 运行的Service对象
         */
        Service service;
        /**
         * 是否前台进程运行
         */
        boolean isForeground; // is service currently in foreground mode?
        /**
         * 前台运行的通知
         */
        int foregroundId; // Notification ID of last foreground req.
        /**
         * 被杀死时是否停止
         */
        boolean stopIfKilled;
        /**
         * 最后一个start ID
         */
        int lastStartId;
        /**
         * 最后一个Intent
         */
        Intent lastIntent;
    }

    /**
     * 记录当前启动的Service的HashMap
     */
    private HashMap<String, ServiceRecord> mServices = new HashMap<String, ServiceProxy.ServiceRecord>();
    /**
     * IActivityManager的桩
     */
    private Object mIActivityManagerProxy = null;

    /**
     * Handler
     */
    private Handler mHandler;

    /**
     * 从Intent获取插件的Service信息
     *
     * @param intent intent
     * @return 插件
     */
    public static ComponentName getTargetComponent(Intent intent) {
        if (intent == null) {
            return null;
        }

        GPTComponentInfo info = GPTComponentInfo.parseFromIntent(intent);
        if (info == null) {
            return null;
        }

        String packageName = info.packageName;
        String targetService = info.className;

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(targetService)) {
            return null;
        }

        return new ComponentName(packageName, targetService);
    }

    /**
     * 获取参数中对应类型的参数
     *
     * @param <T> 参数类型
     * @author liuhaitao
     * @since 2014年12月3日
     */
    private static class ArgumentParser<T> {

        /**
         * 获取参数方法
         *
         * @param args   所有参数
         * @param prefer 参数所在的位置
         * @return 参数
         */
        @SuppressWarnings("unchecked")
        T get(Object[] args, int prefer) {
            if (args == null) {
                return null;
            }

            T ret = null;

            int pos = 0;
            for (Object arg : args) {
                try {
                    ret = (T) arg;
                    if (pos == prefer) {
                        break;
                    }
                } catch (ClassCastException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }

                }
                pos++;
            }

            return ret;

        }
    }

    /**
     * 从其他Context 来stopService
     *
     * @param cpName 插件Component
     * @return 参考系统1表示stop成功，-1表示没查记录，可能已经被stop了，0表示没找到service
     */
    public static int stopServiceExternal(ComponentName cpName) {
        if (DEBUG) {
            Log.i(TAG, "--- stopServiceExternal : " + cpName);
        }
        if (sInstance != null) {
            return sInstance.stopService(cpName);
        } else {
            if (DEBUG) {
                Log.i(TAG, "--- stopServiceExternal ret=0: " + cpName);
            }
            return 0;
        }
    }

    /**
     * stop插件Service
     *
     * @param cpName 插件ComponentName
     * @return 参考系统1表示stop成功，-1表示没查记录，可能已经被stop了
     */
    public int stopService(ComponentName cpName) {
        ServiceRecord sr = mServices.get(cpName.toString());
        if (sr == null) {
            if (DEBUG) {
                Log.d(TAG, "### Proxy stopService ret = 0, cp=" + cpName);
            }
            return 0;
        }

        sr.service.onDestroy();
        updateServicesToSp();

        mServices.remove(cpName.toString());
        if (mServices.isEmpty()) {
            stopSelf();
        }

        if (DEBUG) {
            Log.d(TAG, "### Proxy stopService ret = 1, cp=" + cpName);
        }

        return 1;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        if (DEBUG) {
            Log.i(TAG, "---  ServiceProxy start!!!");
        }

        // 初始化工具
        try {
            Class<?> iActivityManagerClazz = Class.forName("android.app.ActivityManagerNative");
            mIActivityManagerProxy = Proxy.newProxyInstance(iActivityManagerClazz.getClassLoader(),
                    iActivityManagerClazz.getInterfaces(),
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                            if (DEBUG) {
                                Log.d(TAG, "--- ServiceProxy am method called : " + method.toGenericString());
                            }

                            Object result = null;

                            String methodName = method.getName();
                            if (METHOD_STOPSERVICETOKEN.equals(methodName)) {
                                result = false;

                                Integer startId = 0;
                                final ComponentName cpName = new ArgumentParser<ComponentName>().get(args, 0);
                                if (DEBUG) {
                                    Log.d(TAG, "--- METHOD_STOPSERVICETOKEN : " + cpName.toString());
                                }
                                startId = new ArgumentParser<Integer>().get(args, 2); // SUPPRESS CHECKSTYLE
                                if (cpName != null && startId != null) {
                                    final ServiceRecord sr = mServices.get(cpName.toString());
                                    if (sr != null && (sr.lastStartId == startId.intValue() || startId == -1)) {
                                        result = true;
                                        mHandler.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                stopService(cpName);
                                            }
                                        });

                                    }
                                }
                            } else if (METHOD_SETSERVICEFOREGROUND.equals(methodName)) {
                                ComponentName cpName = null;
                                Integer id = null;
                                Notification notification = null;
                                Boolean removeNotification = null;
                                cpName = new ArgumentParser<ComponentName>().get(args, 0);
                                if (DEBUG) {
                                    Log.d(TAG, "--- METHOD_SETSERVICEFOREGROUND : " + cpName.toString());
                                }
                                id = new ArgumentParser<Integer>().get(args, 2);
                                notification = new ArgumentParser<Notification>().get(args, 3);
                                if (Build.VERSION.SDK_INT >= 24) {
                                    int removeNotificationFlag = new ArgumentParser<Integer>().get(args, 4);
                                    if ((removeNotificationFlag & 1) != 0) {
                                        removeNotification = true;
                                    }
                                } else {
                                    removeNotification = new ArgumentParser<Boolean>().get(args, 4);
                                }

                                if (cpName != null && id != null && removeNotification != null) {
                                    ServiceRecord sr = mServices.get(cpName.toString());

                                    if (sr != null) {

                                        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        if (id.intValue() != 0) {

                                            if (notification == null) {
                                                throw new IllegalArgumentException("null notification");
                                            }

                                            nm.notify(id.intValue(), notification);
                                            sr.foregroundId = id.intValue();
                                            sr.isForeground = true;
                                        } else {
                                            nm.cancel(sr.foregroundId);
                                            sr.foregroundId = 0;
                                            sr.isForeground = false;
                                        }
                                    }
                                }
                            }

                            return result;
                        }
                    });
        } catch (ClassNotFoundException e) {
            if (DEBUG) {
                Log.e(TAG, "### Get am failed, can not support service!!! msg=" + e.getMessage());
            }
        }

        if (mIActivityManagerProxy == null) {

            // 容错，不能支持Service了
            stopSelf();
            return;
        }

        mHandler = new Handler();

        // 从SharedPref读取原来运行的Service信息
        SharedPreferences sp = getSharedPreferences(SP_FILENAME + getClass().getSimpleName(), Context.MODE_PRIVATE);
        String jsonStr = sp.getString(KEY_SERVICES, null);

        if (jsonStr != null) {
            try {
                JSONArray jarr = new JSONArray(jsonStr);
                for (int i = 0; i < jarr.length(); i++) {
                    JSONObject jo = jarr.getJSONObject(i);

                    String packageName = jo.getString(JKEY_PKG);
                    String className = jo.getString(JKEY_CLASS_NAME);
                    Intent srvIntent = null;
                    if (jo.has(JKEY_LAST_INTENT)) {
                        try {
                            srvIntent = Intent.parseUri(jo.getString(JKEY_LAST_INTENT), 0);
                        } catch (URISyntaxException e) {
                            if (DEBUG) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 创建默认intent
                    if (srvIntent == null) {
                        srvIntent = new Intent();
                    }

                    srvIntent.setComponent(new ComponentName(packageName, className));
                    if (DEBUG) {
                        Log.i(TAG, "--- Schedule restart service by intent : " + srvIntent.toUri(0));
                        Log.i(TAG, "--- packageName=" + packageName + ", className=" + className + ", srvIntent="
                                + srvIntent);
                    }

                    ProxyEnvironment.enterProxy(getApplicationContext(), srvIntent, true, true);
                }
            } catch (JSONException e) {
                if (DEBUG) {
                    Log.w(TAG, "### Parse json fail : " + jsonStr);
                }
            }
        }

    }

    /**
     * 加载插件Service
     *
     * @param intent        传递给Service的Intent
     * @param componentName 插件Service对应的ComponentName
     * @return service信息
     */
    public ServiceRecord loadTarget(Intent intent, ComponentName componentName, boolean fromBind) {

        if (DEBUG) {
            Log.i(TAG, "--- Try to create service : " + componentName);
        }

        ServiceRecord sr = null;

        // 插件Service没有运行，创建插件Service
        if (!ProxyEnvironment.isEnterProxy(componentName.getPackageName())) {
            GPTComponentInfo info = GPTComponentInfo.parseFromIntent(intent);
            if (info != null && !info.reschedule) {
                Intent intentCpy = new Intent(intent);
                intentCpy.setComponent(componentName);
                ProxyEnvironment.enterProxy(this, intentCpy, true, true);
            }
        } else {
            Service service = null;
            try {
                service = (Service) ProxyEnvironment.getInstance(componentName.getPackageName()).getDexClassLoader()
                        .loadClass(componentName.getClassName()).newInstance();
                Application app = ProxyEnvironment.getInstance(componentName.getPackageName()).getApplication();
                JavaCalls.invokeMethod(service, "attach",
                        new Class<?>[]{Context.class, Class.forName("android.app.ActivityThread"), String.class,
                                IBinder.class, Application.class, Object.class},
                        new Object[]{app, null, componentName.getClassName(), null, app, mIActivityManagerProxy});

                // OpPackageName调用的地方一般都是aidl接口传递包名给system server，所以要用宿主的包名
                Object baseContext = service.getBaseContext();
                JavaCalls.setField(baseContext, "mOpPackageName", ProxyEnvironment
                        .getInstance(service.getPackageName())
                        .getHostPackageName());

                ProxyUtil.replaceSystemServices(app);
            } catch (InstantiationException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (SecurityException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (IllegalArgumentException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (RuntimeException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            } catch (java.lang.VerifyError e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                ReportManger.getInstance().onException(this, componentName.getPackageName(),
                        Util.getCallStack(e), ExceptionConstants.TJ_78730014);
            }

            if (service != null) {
                sr = new ServiceRecord();
                sr.service = service;
                sr.name = componentName;

                // 因为bind的方式如果放到队列中存储。ubind后，如果不从队列中删除。进程再次启动后会通过 startService 的方式启动。
                // 然后 如果此进程意外终止，系统会 schedule service 重启。
                if (!fromBind) {
                    mServices.put(componentName.toString(), sr);
                }
                service.onCreate();

                if (DEBUG) {
                    Log.i(TAG, "--- Create service success : " + componentName);
                }

            } else {
                if (DEBUG) {
                    Log.e(TAG, "### Create service fail! cp=" + componentName);
                }
            }
        }

        return sr;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mIActivityManagerProxy == null) {
            // 容错，不能支持Service了
            return null;
        }

        ComponentName target = getTargetComponent(intent);
        if (target == null) {
            if (mServices.isEmpty()) {
                stopSelf();
            }
            return null;
        }

        // 获取SR
        ServiceRecord sr = mServices.get(target.toString());
        if (sr == null) {
            sr = loadTarget(intent, target, true);
        }

        // SR还是空的，可能是load失败了
        if (sr == null) {
            if (mServices.isEmpty()) {
                stopSelf();
            }
            return null;
        }

        updateServicesToSp();

        return sr.service.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (DEBUG) {
            Log.d(TAG, "onStartCommand(Intent intent, int flags, int startId) : intent="
                    + ((intent == null) ? "null." : intent.toString())
                    + "; flags=" + flags + "; startId=" + startId);
        }

        if (mIActivityManagerProxy == null) {
            // 容错，不能支持Service了
            return 0;
        }

        if (intent == null) {
            return START_STICKY;
        }

        // 调用super，返回值用super的
        int ret = super.onStartCommand(intent, flags, startId);

        // 调用插件的onStartCommand方法
        int targetRet = 0;
        ComponentName target = getTargetComponent(intent);
        if (target == null) {
            if (mServices.isEmpty()) {
                stopSelf();
            }
            return ret;
        }

        // 插件SDK不能支持百度PushService，暂时先屏蔽了。
        if (TextUtils.equals(target.getClassName(), "com.baidu.android.pushservice.PushService")) {
            return ret;
        }

        // 获取SR
        ServiceRecord sr = mServices.get(target.toString());
        if (sr == null) {
            sr = loadTarget(intent, target, false);
        }

        // SR还是空的，可能是load失败了
        if (sr == null) {
            if (mServices.isEmpty()) {
                stopSelf();
            }
            return ret;
        }

        // 解决andorid 5.0 service get Serializable extra 找不到class的问题。
        intent.setExtrasClassLoader(ProxyEnvironment.getInstance(target.getPackageName()).getDexClassLoader());

        targetRet = sr.service.onStartCommand(intent, flags, startId);

        // 处理插件返回的ret
        switch (targetRet) {
            case Service.START_STICKY_COMPATIBILITY:
            case Service.START_STICKY: {
                sr.stopIfKilled = false;
                break;
            }
            case Service.START_NOT_STICKY: {
                if (sr.lastStartId == startId) {
                    sr.stopIfKilled = true;
                }
                break;
            }
            case Service.START_REDELIVER_INTENT: {
                sr.lastIntent = new Intent(intent);
                sr.stopIfKilled = false;

                // 更新Intent
                updateServicesToSp();
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown service start result: " + targetRet);
        }

        updateServicesToSp();

        return ret;
    }

    @Override
    public void onDestroy() {

        if (DEBUG) {
            Log.i(TAG, "--- ServiceProxy onDestroy()!");
        }
        sInstance = null;

        // 停止所有还未停止运行的插件Service
        for (ServiceRecord sr : mServices.values()) {
            sr.service.onDestroy();
        }
        mServices.clear();

        updateServicesToSp();
        super.onDestroy();

    }

    /**
     * 把当前运行的Service信息更新到SharedPreferences里。
     */
    public void updateServicesToSp() {
        SharedPreferences sp = getSharedPreferences(SP_FILENAME + getClass().getSimpleName(), Context.MODE_PRIVATE);
        Editor edit = sp.edit();
        if (mServices.isEmpty()) {
            edit.putString(KEY_SERVICES, null);
        } else {
            JSONArray jarr = new JSONArray();
            for (String name : mServices.keySet()) {
                ServiceRecord sr = mServices.get(name);
                if (sr.stopIfKilled) {

                    // service不用重启的话，用不着存下来
                    continue;
                }
                JSONObject jo = new JSONObject();
                try {
                    jo.put(JKEY_PKG, sr.name.getPackageName());
                    jo.put(JKEY_CLASS_NAME, sr.name.getClassName());
                    if (sr.lastIntent != null) {
                        jo.put(JKEY_LAST_INTENT, sr.lastIntent.toUri(0));
                    }
                    jarr.put(jo);
                } catch (JSONException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            edit.putString(KEY_SERVICES, jarr.toString());
        }
        edit.commit();
    }
}

