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

import android.util.Log;

import com.baidu.android.gporter.util.Constants;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/**
 * 接口拦截基类
 *
 * @author liuhaitao
 * @since 2015-10-13
 */
public class InterfaceProxy {

    /**
     * 拦截的接口信息
     */
    private InterfaceInfo mInterfaceInfo = null;
    /**
     * 拦截的目标实例
     */
    protected Object mTarget = null;

    /**
     * 需要拦截的接口信息
     *
     * @author liuhaitao
     * @since 2015-10-13
     */
    public static class InterfaceInfo {
        /**
         * 需要拦截的接口类名
         */
        public String name;
        /**
         * 需要拦截的方法描述
         */
        public ArrayList<Method> methods;
    }

    /**
     * 构造方法
     *
     * @param name 需要拦截的接口类名
     */
    public InterfaceProxy(String name) {
        InterfaceInfo info = initInterfaceInfo(name);
        if (info != null) {
            MethodProxy.registerInterface(info);
            mInterfaceInfo = info;
        }
    }

    /**
     * 初始化拦截信息
     *
     * @param name 接口类名
     * @return 拦截的接口信息
     */
    protected InterfaceInfo initInterfaceInfo(String name) {
        InterfaceInfo info = new InterfaceInfo();
        info.name = name;
        info.methods = new ArrayList<Method>();
        Method[] methods = this.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            info.methods.add(methods[i]);
        }
        return info;
    }

    /**
     * 产生拦截代理
     *
     * @param target 拦截目标实例
     * @return 代理实例
     */
    public Object generateProxy(Object target) {
        if (mInterfaceInfo == null || target == null) {
            if (Constants.DEBUG) {
                Log.e("InterfaceInfo", "### generateProxy fail, target = " + target);
            }
            return null;
        }
        mTarget = target;

        // 获取拦截的接口类
        ClassLoader cl = this.getClass().getClassLoader();
        Class<?> iTargetClazz = null;
        try {
            iTargetClazz = cl.loadClass(mInterfaceInfo.name);
        } catch (ClassNotFoundException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
            return null;
        }

        return Proxy.newProxyInstance(cl, iTargetClazz.getInterfaces(),
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;
                        MethodProxy.MethodInfo methodInfo = MethodProxy
                                .getMethodInfo(this, mInterfaceInfo.name, method);

                        if (methodInfo != null) {
                            result = methodInfo.process(args);
                        } else {
                            result = method.invoke(mTarget, args);
                        }
                        return result;
                    }
                });
    }
}


