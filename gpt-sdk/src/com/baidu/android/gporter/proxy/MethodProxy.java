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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * MethodProxy
 *
 * @author liuhaitao
 * @since 2014-4-24
 */
public class MethodProxy {
    /**
     * 需要拦截的方法信息
     *
     * @author liuhaitao
     * @since 2015-10-13
     */
    public static class MethodInfo {
        /**
         * targetObject
         */
        Object targetObject;
        /**
         * targetMethod
         */
        Method targetMethod;

        /**
         * process
         *
         * @param args Object[]
         * @return Object
         */
        public Object process(Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Object obj = targetMethod.invoke(targetObject, args);
            return obj;
        }
    }

    /**
     * interfacesMap
     */
    static Map<String, InterfaceProxy.InterfaceInfo> interfacesMap = new HashMap<String, InterfaceProxy.InterfaceInfo>();

    /**
     * getMethodInfo
     *
     * @param targetObject targetObject
     * @param className    className
     * @param method       method
     * @return MethodInfo
     */
    public static MethodInfo getMethodInfo(Object targetObject, String className, Method method) {
        if (interfacesMap.containsKey(className)) {
            InterfaceProxy.InterfaceInfo info = interfacesMap.get(className);
            for (int i = 0; i < info.methods.size(); i++) {
                String oldName = method.getName();
                // oldName = oldName.substring(oldName.lastIndexOf("."));
                String newName = info.methods.get(i).getName();
                // newName = newName.substring(newName.lastIndexOf("."));
                if (oldName.equals(newName)) {
                    boolean same = checkSameMethod(method, info.methods.get(i));
                    if (same) {
                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.targetMethod = info.methods.get(i);
                        methodInfo.targetObject = targetObject;
                        return methodInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * checkSameMethod
     *
     * @param method      Method
     * @param proxyMethod Method
     * @return true or false
     */
    private static boolean checkSameMethod(Method method, Method proxyMethod) {
        if (!method.getReturnType().equals(proxyMethod.getReturnType())) {
            return false;
        }
        Class<?>[] classes = method.getParameterTypes();
        Class<?>[] proxyClasses = proxyMethod.getParameterTypes();
        if (classes.length != proxyClasses.length) {
            return false;
        }

        for (int i = 0; i < classes.length; i++) {
            if (!classes[i].equals(proxyClasses[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * registerInterface
     *
     * @param info InterfaceProxy.InterfaceInfo
     */
    public static void registerInterface(InterfaceProxy.InterfaceInfo info) {
        interfacesMap.put(info.name, info);
    }

}
