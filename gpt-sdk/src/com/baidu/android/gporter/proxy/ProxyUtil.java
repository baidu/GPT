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
package com.baidu.android.gporter.proxy;

import android.app.AlarmManager;
import android.app.IAlarmManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.location.LocationManager;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.storage.IMountService;
import android.text.TextUtils;
import android.util.Log;
import android.view.autofill.IAutoFillManager;

import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.GPTProxyAlarmException;
import com.baidu.android.gporter.stat.GPTProxyAutoFillException;
import com.baidu.android.gporter.stat.GPTProxyWifiException;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.GPTProxyAlarmException;
import com.baidu.android.gporter.stat.GPTProxyWifiException;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * 处理代理相关功能的工具类
 *
 * @author liuhaitao
 * @since 2016-2-24
 */
public final class ProxyUtil {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ProxyUtil";

    /**
     * IWifiManager的代理类
     */
    private static Object sWifiManagerProxy = null;
    /**
     * IWifiManager的拦截处理器
     */
    private static WifiManagerWorker sWifiManagerWorker = null;
    /**
     * IAlarmManager的拦截处理器
     */
    private static AlarmManagerWork sAlarmManagerWork = null;

    /**
     * IAlarmManager的代理类
     */
    private static HashMap<String, Object> sAlarmManagerProxyMap = new HashMap<String, Object>();

    /**
     * IMountService 的拦截处理器
     */
    private static MountServiceWork mountServiceWork = null;

    /**
     * 工具类，不实例化
     */
    private ProxyUtil() {

    }

    /**
     * 拦截IWifiManager
     *
     * @param context 插件上需要拦截的context
     */
    public static void replaceWifiManager(final Context context) {

        // 替换IWifiManager
        WifiManager wfm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // 魅族MX4的机型了自己写了一个IMsapWifiManager，如果有这个WiFiManager，则需要把ContextImpl的Outer设置成宿主的
        if (context instanceof ContextWrapper) {
            try {

                Class.forName("android.net.wifi.IMsapWifiManager");
                JavaCalls.setField(((ContextWrapper) context).getBaseContext(), "mBasePackageName",
                        Util.getHostContext(context).getPackageName());
            } catch (ClassNotFoundException e1) {
                if (DEBUG) {
                    e1.printStackTrace();
                }
            }
        }

        // GT-I9508 这款手机setWifiEnable方法不一样，所以需要拦截
        if (TextUtils.equals(Build.MODEL, "GT-I9508")) {
            if (sWifiManagerProxy == null) {
                sWifiManagerWorker = new WifiManagerWorker();
                sWifiManagerWorker.mTarget = (IWifiManager) JavaCalls.getField(wfm, "mService");
                sWifiManagerWorker.mHostContext = Util.getHostContext(context);
                try {
                    Class<?> iWifiManagerClazz = Class.forName(Constants.WIFI_MANAGER_CLASS);
                    sWifiManagerProxy = Proxy.newProxyInstance(iWifiManagerClazz.getClassLoader(),
                            new Class<?>[]{iWifiManagerClazz}, new InvocationHandler() {

                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    Object result = null;
                                    try {
                                        MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(sWifiManagerWorker,
                                                Constants.WIFI_MANAGER_CLASS, method);

                                        if (methodInfo != null) {
                                            result = methodInfo.process(args);
                                        } else {
                                            result = method.invoke(sWifiManagerWorker.mTarget, args);
                                        }
                                    } catch (Exception e) {
                                        StringBuilder sb = new StringBuilder();
                                        String message = Util.printlnMethod("### WifiM invoke : ", method, args);
                                        sb.append(Util.getCallStack(e));
                                        if (DEBUG) {
                                            e.printStackTrace();
                                        }

                                        ReportManger.getInstance().onExceptionByLogService(context, "", sb.toString(),
                                                ExceptionConstants.TJ_78730013);

                                        if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                            throw e;
                                        } else {
                                            throw new GPTProxyWifiException(message, e);
                                        }

                                    }
                                    return result;
                                }
                            });
                } catch (ClassNotFoundException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
            JavaCalls.setField(wfm, "mService", sWifiManagerProxy);
        }

        Object mAppOps = JavaCalls.getField(wfm, "mAppOps");
        if (mAppOps != null) {

            // 特殊机型适配：GiONEE GN9004 - 4.3，这个手机在调用setWifiEnable前会调用AppOpsManager判断uid是否合法
            // 将context替换成host的，调用setWifiEnable时，校验uid才能通过
            JavaCalls.setField(mAppOps, "mContext", Util.getHostContext(context));
        }
    }

    /**
     * 拦截ClipboardManager
     *
     * @param context 插件上需要拦截的context
     */
    public static void replaceClipboardManager(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            JavaCalls.setField(cm, "mContext", Util.getHostContext(context));
        }
    }

    /**
     * 拦截LocationManager
     *
     * @param context 插件上需要拦截的context
     */
    public static void replaceLocationManager(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        JavaCalls.setField(lm, "mContext", Util.getHostContext(context));
    }

    /**
     * 拦截IAutoFillManager
     *
     * @param context
     */
    public static void replaceAutoFillManager(final Context context) {
        if (Build.VERSION.SDK_INT < 26) { // 8.0之前什么都不做
            return;
        }

        try {
            final AutoFillManagerWorker autoFillManagerWork = new AutoFillManagerWorker();
            autoFillManagerWork.mHostContext = Util.getHostContext(context);
            Object autofill = context.getSystemService("autofill");
            if (autofill == null) {
                return;
            }
            autoFillManagerWork.mTarget = (IAutoFillManager) JavaCalls.getField(autofill, "mService");
            if (autoFillManagerWork.mTarget == null) {
                return;
            }

            Class[] interfaces = new Class<?>[]{Class.forName(Constants.IAUTO_FILL_MANAGER_CLASS)};
            Object autoFillManagerProxy = Proxy.newProxyInstance(context.getClassLoader(),
                    interfaces, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = null;
                            try {
                                MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(autoFillManagerWork,
                                        Constants.IAUTO_FILL_MANAGER_CLASS, method);

                                if (methodInfo != null) {
                                    result = methodInfo.process(args);
                                } else {
                                    result = method.invoke(autoFillManagerWork.mTarget, args);
                                }
                            } catch (Exception e) {
                                StringBuilder sb = new StringBuilder();
                                String message = Util.printlnMethod("### IAutoFillManager invoke : ", method, args);
                                sb.append(Util.getCallStack(e));

                                ReportManger.getInstance().onExceptionByLogService(context, "", sb.toString(),
                                        ExceptionConstants.TJ_78730017);

                                if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                    throw e;
                                } else {
                                    throw new GPTProxyAutoFillException(message, e);
                                }

                            }
                            return result;
                        }
                    });

            JavaCalls.setField(autofill, "mService", autoFillManagerProxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拦截IAlarmManager
     *
     * @param context 插件上需要拦截的context
     */
    public static void replaceAlarmManager(final Context context) {
        String pluginPkgName = context.getPackageName();
        Object sAlarmManagerProxy = sAlarmManagerProxyMap.get(pluginPkgName);

        if (sAlarmManagerProxy != null) {
            return;
        }
        try {
            sAlarmManagerWork = new AlarmManagerWork();
            sAlarmManagerWork.mHostContext = Util.getHostContext(context);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            sAlarmManagerWork.mTarget = (IAlarmManager) JavaCalls.getField(am, "mService");
            if (sAlarmManagerWork.mTarget == null) {
                return;
            }
            Class[] interfaces = new Class<?>[]{Class.forName(Constants.IALARM_MANAGERR_CLASS)};
            sAlarmManagerProxy = Proxy.newProxyInstance(context.getClassLoader(),
                    interfaces, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = null;
                            try {
                                MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(sAlarmManagerWork,
                                        Constants.IALARM_MANAGERR_CLASS, method);
                                if (methodInfo != null) {
                                    result = methodInfo.process(args);
                                } else {
                                    result = method.invoke(sAlarmManagerWork.mTarget, args);
                                }
                            } catch (Exception e) {
                                StringBuilder sb = new StringBuilder();
                                String message = Util.printlnMethod("### IAlarmManager invoke : ", method, args);
                                sb.append(Util.getCallStack(e));
                                if (DEBUG) {
                                    e.printStackTrace();
                                }

                                ReportManger.getInstance().onExceptionByLogService(context, "", sb.toString(),
                                        ExceptionConstants.TJ_78730015);

                                if (e instanceof RemoteException) { // 为便于开者捕获处理RemoteException，直接抛出。
                                    throw e;
                                } else {
                                    throw new GPTProxyAlarmException(message, e);
                                }

                            }
                            return result;
                        }
                    });

            JavaCalls.setField(am, "mService", sAlarmManagerProxy);
            sAlarmManagerProxyMap.put(pluginPkgName, sAlarmManagerProxy);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 替换ServiceManager中sCache里面的IBinder
     * 适合没有单例缓冲的IBinder，直接从 ServiceManager.getService 中取IBinder
     * <p>
     * 例如
     * IMountService.Stub.asInterface(ServiceManager.getService("mount"));
     *
     * @param ctx         插件上需要拦截的context
     * @param serviceName service的名称
     */
    public static BinderWork replaceServiceManagerBinder(final Context ctx, String serviceName) {
        Context context = Util.getHostContext(ctx);
        final BinderWork binderWork = new BinderWork();
        try {
            final IBinder binder = JavaCalls.callStaticMethod(Constants.SERVICE_MANAGER_CLASS,
                    "getService", serviceName);
            binderWork.mOlderBinder = binder;
            IBinder binderProxy = (IBinder) Proxy.newProxyInstance(context.getClassLoader(),
                    new Class[]{IBinder.class}, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = null;
                            try {
                                MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(binderWork,
                                        IBinder.class.getName(), method);
                                if (DEBUG) {
                                    Log.i(TAG, Util.printlnMethod("IBinder:", method, args));
                                }
                                if (methodInfo != null) {
                                    result = methodInfo.process(args);
                                } else {
                                    result = method.invoke(binder, args);
                                }
                            } catch (Exception e) {
                                if (DEBUG) {
                                    e.printStackTrace();
                                }
                                throw e;
                            }
                            return result;
                        }
                    });
            HashMap<String, IBinder> sCache = (HashMap<String, IBinder>)
                    JavaCalls.getStaticField(Constants.SERVICE_MANAGER_CLASS, "sCache");
            sCache.remove(serviceName);
            sCache.put(serviceName, (IBinder) binderProxy);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        return binderWork;
    }

    /**
     * 拦截IMountService
     *
     * @param ctx 插件上需要拦截的context
     */
    public static void replaceMountService(final Context ctx) {
        if (mountServiceWork != null) {
            return;
        }
        Context context = Util.getHostContext(ctx);
        final BinderWork binderWork = replaceServiceManagerBinder(context, "mount");
        mountServiceWork = new MountServiceWork();
        mountServiceWork.mHostContext = context;
        mountServiceWork.mTarget = JavaCalls.callStaticMethod(Constants.IMOUNT_SERVICE_CLASS + "$Stub",
                "asInterface", binderWork.mOlderBinder);
        try {
            IMountService mountServiceProxy = (IMountService) Proxy.newProxyInstance(context.getClassLoader(),
                    new Class[]{Class.forName(Constants.IMOUNT_SERVICE_CLASS),
                            android.os.IInterface.class}, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = null;
                            try {
                                MethodProxy.MethodInfo methodInfo = MethodProxy.getMethodInfo(mountServiceWork,
                                        Constants.IMOUNT_SERVICE_CLASS, method);
                                if (DEBUG) {
                                    Log.i(TAG, Util.printlnMethod("MountService:", method, args));
                                }
                                if (methodInfo != null) {
                                    result = methodInfo.process(args);
                                } else {
                                    result = method.invoke(mountServiceWork.mTarget, args);
                                }
                            } catch (Exception e) {
                                if (DEBUG) {
                                    e.printStackTrace();
                                }
                                throw e;
                            }
                            return result;
                        }
                    });
            binderWork.mNewBinder = (android.os.IInterface) mountServiceProxy;
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 插件启动之后，替换系统的一些Service
     *
     * @param context Context
     */
    public static void replaceSystemServices(Context context) {
        ProxyUtil.replaceWifiManager(context);
        ProxyUtil.replaceLocationManager(context);
        ProxyUtil.replaceClipboardManager(context);
        // 7.0 以上的手机才需要hook AlarmManger，增加条件判断，优化插件启动时间
        if (Build.VERSION.SDK_INT >= 24) {
            ProxyUtil.replaceAlarmManager(context);
        }

        if (Build.VERSION.SDK_INT >= 26) {
            ProxyUtil.replaceAutoFillManager(context);
        }
//        ProxyUtil.replaceMountService(context);
    }

}


