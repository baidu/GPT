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
package com.baidu.android.gporter.util;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * JavaCalls
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class JavaCalls {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    private final static String TAG = "JavaCalls";
    /**
     * PRIMITIVE_MAP
     */
    private final static HashMap<Class<?>, Class<?>> PRIMITIVE_MAP = new HashMap<Class<?>, Class<?>>();

    static {
        PRIMITIVE_MAP.put(Boolean.class, boolean.class);
        PRIMITIVE_MAP.put(Byte.class, byte.class);
        PRIMITIVE_MAP.put(Character.class, char.class);
        PRIMITIVE_MAP.put(Short.class, short.class);
        PRIMITIVE_MAP.put(Integer.class, int.class);
        PRIMITIVE_MAP.put(Float.class, float.class);
        PRIMITIVE_MAP.put(Long.class, long.class);
        PRIMITIVE_MAP.put(Double.class, double.class);
        PRIMITIVE_MAP.put(boolean.class, boolean.class);
        PRIMITIVE_MAP.put(byte.class, byte.class);
        PRIMITIVE_MAP.put(char.class, char.class);
        PRIMITIVE_MAP.put(short.class, short.class);
        PRIMITIVE_MAP.put(int.class, int.class);
        PRIMITIVE_MAP.put(float.class, float.class);
        PRIMITIVE_MAP.put(long.class, long.class);
        PRIMITIVE_MAP.put(double.class, double.class);
    }

    /**
     * getDeclaredMethod
     *
     * @param clazz          类
     * @param name           方法名
     * @param parameterTypes 参数类型
     * @return Method
     */
    private static Method getDeclaredMethod(final Class<?> clazz, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException, SecurityException {

        // 包括公共、保护、默认（包）访问和私有方法，但不包括继承的方法。当然也包括它所实现接口的方法。
        Method[] methods = clazz.getDeclaredMethods();
        Method method = findMethodByName(methods, name, parameterTypes);

        if (method == null) {
            // 查找继承的方法
            method = clazz.getMethod(name, parameterTypes);
        }

        if (method == null) {
            throw new NoSuchMethodException();
        }

        return method;
    }

    /**
     * findMethodByName
     *
     * @param list           Method[]
     * @param name           方法名
     * @param parameterTypes 参数类型
     * @return Method
     */
    private static Method findMethodByName(Method[] list, String name, Class<?>[] parameterTypes) {
        if (name == null) {
            throw new NullPointerException("Method name must not be null.");
        }

        for (Method method : list) {
            if (method.getName().equals(name)
                    && compareClassLists(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }

        return null;
    }

    /**
     * compareClassLists
     *
     * @param a Class<?>[]
     * @param b Class<?>[]
     * @return true or false
     */
    private static boolean compareClassLists(Class<?>[] a, Class<?>[] b) {
        if (a == null) {
            return (b == null) || (b.length == 0);
        }

        int length = a.length;

        if (b == null) {
            return (length == 0);
        }

        if (length != b.length) {
            return false;
        }
        int count = 0;
        for (int i = 0; i <= length - 1; i++) {
            if ((null == b[i]) || a[i].isAssignableFrom(b[i])
                    || (PRIMITIVE_MAP.containsKey(a[i]) && PRIMITIVE_MAP.get(a[i]).equals(PRIMITIVE_MAP.get(b[i])))) {
                count++;
            }
        }
        if (count == length) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static class JavaParam<T> {
        public final Class<? extends T> clazz;
        public final T obj;

        public JavaParam(Class<? extends T> clazz, T obj) {
            super();
            this.clazz = clazz;
            this.obj = obj;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callMethod(Object targetInstance, String methodName, Object... args) {
        try {
            return callMethodOrThrow(targetInstance, methodName, args);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "Meet exception when call Method '" + methodName + "' in " + targetInstance);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callMethodOrThrow(Object targetInstance, String methodName, Object... args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        final Class<?> clazz = targetInstance.getClass();

        Method method = getDeclaredMethod(clazz, methodName, getParameterTypes(args));
        method.setAccessible(true);
        T result = (T) method.invoke(targetInstance, getParameters(args));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T callStaticMethod(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);
            return callStaticMethodOrThrow(clazz, methodName, args);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "Meet exception when call Method '" + methodName + "' in " + className);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T callStaticMethodOrThrow(final String className, String methodName, Object... args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        Method method = getDeclaredMethod(clazz, methodName, getParameterTypes(args));

        method.setAccessible(true);
        T result = (T) method.invoke(null, getParameters(args));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T callStaticMethodOrThrow(final Class<?> clazz, String methodName, Object... args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method method = getDeclaredMethod(clazz, methodName, getParameterTypes(args));

        method.setAccessible(true);
        T result = (T) method.invoke(null, getParameters(args));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<?> clazz, Object... args) {
        try {
            return getInstanceOrThrow(clazz, args);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "Meet exception when make instance as a " + clazz.getSimpleName());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstanceOrThrow(Class<?> clazz, Object... args) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Constructor<?> constructor = clazz.getConstructor(getParameterTypes(args));
        return (T) constructor.newInstance(getParameters(args));
    }

    /**
     * getInstance
     *
     * @param className 类名
     * @param args      参数
     * @return Object
     */
    public static Object getInstance(String className, Object... args) {
        try {
            return getInstanceOrThrow(className, args);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "Meet exception when make instance as a " + className);
            }
            return null;
        }
    }

    /**
     * getInstanceOrThrow
     *
     * @param className 类名
     * @param args      参数
     * @return Object
     */
    public static Object getInstanceOrThrow(String className, Object... args) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        return getInstanceOrThrow(Class.forName(className), getParameters(args));
    }

    /**
     * getParameterTypes
     *
     * @param args 参数
     * @return Class<?>[]
     */
    private static Class<?>[] getParameterTypes(Object... args) {
        Class<?>[] parameterTypes = null;

        if (args != null && args.length > 0) {
            parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                Object param = args[i];
                if (param != null && param instanceof JavaParam<?>) {
                    parameterTypes[i] = ((JavaParam<?>) param).clazz;
                } else {
                    parameterTypes[i] = param == null ? null : param.getClass();
                }
            }
        }
        return parameterTypes;
    }

    /**
     * getParameters
     *
     * @param args 参数
     * @return Object[]
     */
    private static Object[] getParameters(Object... args) {
        Object[] parameters = null;

        if (args != null && args.length > 0) {
            parameters = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object param = args[i];
                if (param != null && param instanceof JavaParam<?>) {
                    parameters[i] = ((JavaParam<?>) param).obj;
                } else {
                    parameters[i] = param;
                }
            }
        }
        return parameters;
    }

    /**
     * getDeclaredMethod
     *
     * @param object         Object
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return Method
     */
    public static Method getDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
        Method method = null;

        for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                return method;
            } catch (Exception e) {

            }
        }
        return null;
    }

    /**
     * getMethodFromClass
     *
     * @param clazz      Class<?>
     * @param methodName 方法名
     * @return Method
     */
    public static Method getMethodFromClass(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * getMethodToString
     *
     * @param method 方法
     * @return 转化后的方法描述字符串
     */
    public static String getMethodToString(Method method) {
        if (method == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Method Name:" + method.getName() + ",");
        Class<?>[] clazzList = method.getParameterTypes();
        if (clazzList != null) {
            stringBuilder.append(" Parameter:[");
            for (Class<?> clazz : clazzList) {
                stringBuilder.append(clazz.getName() + ",");
            }
            stringBuilder.append(" ]");
        }
        Class<?> returnClazz = method.getReturnType();
        if (returnClazz != null) {
            stringBuilder.append("return class :" + returnClazz);
        }
        return stringBuilder.toString();
    }

    /**
     * invokeMethod
     *
     * @param object         Object
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @param parameters     参数
     * @return Object
     */
    public static Object invokeMethod(Object object, String methodName, Class<?>[] parameterTypes,
                                      Object[] parameters) {
        Method method = getDeclaredMethod(object, methodName, parameterTypes);
        try {
            if (null != method) {
                method.setAccessible(true);
                return method.invoke(object, parameters);
            }
        } catch (IllegalArgumentException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (IllegalAccessException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (InvocationTargetException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * invokeMethodThrowException
     *
     * @param object         Object
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @param parameters     参数
     * @return Object
     */
    public static Object invokeMethodThrowException(Object object, String methodName,
                                                    Class<?>[] parameterTypes, Object[] parameters) throws MethodReflectException {
        Method method = getDeclaredMethod(object, methodName, parameterTypes);
        try {
            if (null != method) {
                method.setAccessible(true);
                return method.invoke(object, parameters);
            }
        } catch (IllegalArgumentException e) {
            throw new MethodReflectException("IllegalArgumentException " + printException(e));
        } catch (IllegalAccessException e) {
            throw new MethodReflectException("IllegalAccessException " + printException(e));
        } catch (InvocationTargetException e) {
            throw new MethodReflectException("InvocationTargetException " + printException(e));
        }
        return null;
    }

    /**
     * 打印异常的调用栈
     *
     * @param e Exception
     * @return 异常的调用栈
     */
    private static String printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with
     * the supplied <code>name</code>. Searches all superclasses up to
     * {@link Object}.
     *
     * @param object the class object to introspect
     * @param name   the name of the field
     * @return the corresponding Field object, or <code>null</code> if not found
     */
    public static Field findField(Object object, String name) {
        return findField(object, name, null);
    }

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with
     * the supplied <code>name</code> and/or {@link Class type}. Searches all
     * superclasses up to {@link Object}.
     *
     * @param object the class object to introspect
     * @param name   the name of the field (may be <code>null</code> if type is
     *               specified)
     * @param type   the type of the field (may be <code>null</code> if name is
     *               specified)
     * @return the corresponding Field object, or <code>null</code> if not found
     */
    public static Field findField(Object object, String name, Class<?> type) {
        Class<?> searchType = object.getClass();
        while (!Object.class.equals(searchType) && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName()))
                        && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * Set the field represented by the supplied {@link Field field object} on
     * the specified {@link Object target object} to the specified
     * <code>value</code>. In accordance with {@link Field#set(Object, Object)}
     * semantics, the new value is automatically unwrapped if the underlying
     * field has a primitive type.
     * <p>
     * Thrown exceptions are handled via a call to
     * {@link #handleReflectionException(Exception)}.
     *
     * @param field  the field to set
     * @param target the target object on which to set the field
     * @param value  the value to set; may be <code>null</code>
     */
    public static void setFieldOrThrow(Field field, Object target, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * setFieldOrThrow
     *
     * @param target the target object on which to set the field
     * @param name   name of the field
     * @param value  the value to set
     */
    public static void setFieldOrThrow(Object target, String name, Object value) throws IllegalAccessException,
            NoSuchFieldException {
        Field field = findField(target, name);
        if (field != null) {
            setFieldOrThrow(field, target, value);
        } else {

            StringBuilder sb = new StringBuilder();
            for (Method mtd : target.getClass().getDeclaredMethods()) {
                sb.append(mtd.toGenericString()).append("\n");
            }
            throw new NoSuchFieldException("Class=" + target.getClass().getName() + ", Method=" + name + ", Methods="
                    + sb.toString());
        }
    }

    /**
     * setField
     *
     * @param target the target object on which to set the field
     * @param name   name of the field
     * @param value  the value to set
     */
    public static void setField(Object target, String name, Object value) {
        Field field = findField(target, name);
        if (field != null) {
            try {
                setFieldOrThrow(field, target, value);
            } catch (IllegalAccessException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the field represented by the supplied {@link Field field object} on
     * the specified {@link Object target object}. In accordance with
     * {@link Field#get(Object)} semantics, the returned value is automatically
     * wrapped if the underlying field has a primitive type.
     * <p>
     * Thrown exceptions are handled via a call to
     * {@link #handleReflectionException(Exception)}.
     *
     * @param field  the field to get
     * @param target the target object from which to get the field
     * @return the field's current value
     */
    public static Object getField(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException ex) {
            if (DEBUG) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    /**
     * getField
     *
     * @param target the target object from which to get the field
     * @param name   the field to get
     * @return the field
     */
    public static Object getField(Object target, String name) {
        Field field = findField(target, name);
        if (field == null) {
            return null;
        }
        return getField(field, target);
    }

    /**
     * getStaticField
     *
     * @param className the class from which to get the field
     * @param filedName the field to get
     * @return the field
     */
    public static Object getStaticField(String className, String filedName) {
        Object result = null;
        try {
            Class<?> clazz = Class.forName(className);
            Field f = clazz.getDeclaredField(filedName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * setStaticField
     *
     * @param className the class from which to get the field
     * @param fieldName the field
     * @param value     值
     */
    public static void setStaticField(String className, String fieldName, Object value) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }

            return;
        }
        setStaticField(clazz, fieldName, value);
    }

    /**
     * setStaticField
     *
     * @param clazz     the class
     * @param fieldName the field
     * @param value     值
     */
    public static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            field.set(clazz, value);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }
}