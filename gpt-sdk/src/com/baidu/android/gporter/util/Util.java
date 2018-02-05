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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.ProxyEnvironment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Util
 *
 * @author liuhaitao
 * @since 2013-5-22
 */
public final class Util {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "Util";

    /**
     * utility class private constructor
     */
    private Util() {

    }

    /**
     * 读取 apk 文件的最后修改时间（生成时间），通过编译命令编译出来的apk第一个 entry 为
     * META-INF/MANIFEST.MF  所以我们只读取此文件的修改时间可以。
     * <p>
     * 对于 eclipse 插件打包的 apk 不适用。文件 entry顺序不确定。
     *
     * @param fis InputStream
     * @return 返回 {@link SimpleDateTime}
     * @throws IOException
     */
    public static SimpleDateTime readApkModifyTime(InputStream fis) throws IOException {

        int LOCHDR = 30; //header 部分信息截止字节 // SUPPRESS CHECKSTYLE
        int LOCVER = 4; //排除掉magic number 后的第四个字节，version部分 // SUPPRESS CHECKSTYLE
        int LOCTIM = 10; //最后修改时间 第10个字节。 // SUPPRESS CHECKSTYLE

        byte[] hdrBuf = new byte[LOCHDR - LOCVER];

        // Read the local file header.
        byte[] magicNumer = new byte[4]; // SUPPRESS CHECKSTYLE magic number
        fis.read(magicNumer);
        fis.read(hdrBuf, 0, hdrBuf.length);

        int time = peekShort(hdrBuf, LOCTIM - LOCVER);
        int modDate = peekShort(hdrBuf, LOCTIM - LOCVER + 2);

        SimpleDateTime cal = new SimpleDateTime();
        /*
         * zip中的日期格式为 dos 格式，从 1980年开始计时。
         */
        cal.set(1980 + ((modDate >> 9) & 0x7f), ((modDate >> 5) & 0xf), // SUPPRESS CHECKSTYLE magic number
                modDate & 0x1f, (time >> 11) & 0x1f, (time >> 5) & 0x3f, // SUPPRESS CHECKSTYLE magic number
                (time & 0x1f) << 1);  // SUPPRESS CHECKSTYLE magic number

        fis.skip(0);

        return cal;
    }

    /**
     * 从buffer数组中读取一个 short。
     *
     * @param buffer buffer数组
     * @param offset 偏移量，从这个位置读取一个short。
     * @return short值
     */
    private static int peekShort(byte[] buffer, int offset) {
        short result = (short) ((buffer[offset + 1] << 8) | (buffer[offset] & 0xff)); // SUPPRESS CHECKSTYLE

        return result & 0xffff; // SUPPRESS CHECKSTYLE magic number
    }

    /**
     * copy 文件
     *
     * @param srcFile  源文件
     * @param destFile 目标文件
     * @return 是否成功
     */
    public static boolean copyToFile(File srcFile, File destFile) {
        if (srcFile == null || destFile == null) {
            return false;
        }
        InputStream tempIs = null;
        boolean b = false;
        try {
            tempIs = new FileInputStream(srcFile);
            b = Util.copyToFile(tempIs, destFile);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (tempIs != null) {
                try {
                    tempIs.close();
                } catch (IOException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return b;
    }

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     *
     * @param inputStream source file inputstream
     * @param destFile    destFile
     * @return success return true
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {

        if (inputStream == null || destFile == null) {
            return false;
        }
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096]; // SUPPRESS CHECKSTYLE
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 把文本内容保存到文件中
     *
     * @param src      文本内容
     * @param destFile destFile
     * @return success return true
     */
    public static boolean writeToFile(String src, File destFile) {

        if (TextUtils.isEmpty(src) || destFile == null) {
            return false;
        }
        PrintStream writer = null;
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            writer = new PrintStream(destFile);
            writer.print(src);
            return true;
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            return false;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


    /**
     * 读取文本文件内容
     *
     * @param file 文件
     * @return 文件内容
     */
    public static String readStringFile(File file) {

        String content = null;
        if (file != null && file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                StringBuffer strBuffer = new StringBuffer();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    strBuffer.append(line);
                }
                content = strBuffer.toString();
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return content;
    }

    /**
     * 读取文本文件内容
     *
     * @param filePath 文件地址
     * @return 文件内容
     */
    public static String readStringFile(String filePath) {
        if (filePath != null) {
            return readStringFile(new File(filePath));
        }
        return null;
    }


    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
                if (DEBUG) {
                    ioe.printStackTrace();
                }
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException  if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }


    /**
     * Returns a byte[] containing the remainder of 'in', closing it when done.
     *
     * @param in InputStream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] readFully(InputStream in) throws IOException {
        try {
            return readFullyNoClose(in);
        } finally {
            in.close();
        }
    }

    /**
     * Returns a byte[] containing the remainder of 'in'.
     *
     * @param in InputStream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] readFullyNoClose(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    /**
     * 比较两个签名是否相同
     *
     * @param s1 Signature[]
     * @param s2 Signature[]
     * @return 比较结果
     */
    public static int compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null) {
            return s2 == null ? PackageManager.SIGNATURE_NEITHER_SIGNED : PackageManager.SIGNATURE_FIRST_NOT_SIGNED;
        }
        if (s2 == null) {
            return PackageManager.SIGNATURE_SECOND_NOT_SIGNED;
        }
        HashSet<Signature> set1 = new HashSet<Signature>();
        for (Signature sig : s1) {
            set1.add(sig);
        }
        HashSet<Signature> set2 = new HashSet<Signature>();
        for (Signature sig : s2) {
            set2.add(sig);
        }
        // Make sure s2 contains all signatures in s1.
        if (set1.equals(set2)) {
            return PackageManager.SIGNATURE_MATCH;
        }
        return PackageManager.SIGNATURE_NO_MATCH;
    }

    /**
     * 获取host在MetaData里面声明的类实例
     * warning 这个方法慎重使用。2.2.2--2.2.5版中。由于NotificationManagerNativeWorker 初始化调用这个方法
     * 导致大量的Package manager has died
     *
     * @param ctx Context
     * @param key key
     * @return 类实例
     */
    public static Object getHostMetaDataClassInstance(Context ctx, String key) {
        Object object = null;
        ApplicationInfo hostAppInfo = null;
        try {
            hostAppInfo = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e1) {
            if (DEBUG) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (hostAppInfo != null && hostAppInfo.metaData != null) {
            String clazz = hostAppInfo.metaData.getString(key);
            if (clazz != null && clazz.length() > 0) {
                try {
                    object = Class.forName(clazz).newInstance();
                } catch (InstantiationException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return object;
    }

    /**
     * 获取调用过来的插件包名
     *
     * @return 插件包名，null表示非插件调用，从宿主调用过来
     */
    public static String getCallingPulingPackage() {
        String currentPkg = null;
        ArrayList<String> classes = new ArrayList<String>();
        try {
            throw new Exception();
        } catch (Exception e) {
            try {
                StackTraceElement[] stacks = e.getStackTrace();
                if (stacks != null) {
                    for (int i = 0; i < stacks.length; i++) {
                        String className = stacks[i].getClassName();
                        int index = className.indexOf("$"); // 内部类
                        if (index > 0) {
                            // 因为我们比较的是component里边的类，所以需要去掉内部类，取之前主类。
                            className = className.substring(0, index);
                        }
                        classes.add(className);
                    }
                }
            } catch (Exception e1) {
                if (DEBUG) {
                    e1.printStackTrace();
                }
            }
        }

        Map<String, ProxyEnvironment> pluginsMap = ProxyEnvironment.getPluginsMap();
        if (pluginsMap != null) {
            for (Map.Entry<String, ProxyEnvironment> entry : pluginsMap.entrySet()) {
                currentPkg = entry.getKey();
                ProxyEnvironment instance = entry.getValue();
                PackageInfo packageInfo = instance.getTargetMapping().getPackageInfo();
                for (String className : classes) {
                    String classGptName = className /*+ Constants.GPT_SUFFIX */;
                    if (classGptName.equalsIgnoreCase(packageInfo.applicationInfo.className)) {
                        return currentPkg;
                    }

                    if (packageInfo.activities != null) {
                        for (ActivityInfo activity : packageInfo.activities) {
                            if (classGptName.equalsIgnoreCase(activity.name)) {
                                return currentPkg;
                            }
                        }
                    }

                    if (packageInfo.services != null) {
                        for (ServiceInfo service : packageInfo.services) {
                            if (classGptName.equalsIgnoreCase(service.name)) {
                                return currentPkg;
                            }
                        }
                    }

                    if (packageInfo.providers != null) {
                        for (ProviderInfo provider : packageInfo.providers) {
                            if (classGptName.equalsIgnoreCase(provider.name)) {
                                return currentPkg;
                            }
                        }
                    }

                    if (packageInfo.receivers != null) {
                        for (ActivityInfo receiver : packageInfo.receivers) {
                            if (classGptName.equalsIgnoreCase(receiver.name)) {
                                return currentPkg;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Append path segments to given base path, returning result.
     *
     * @param base     base
     * @param segments segments
     * @return File
     */
    public static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    /**
     * ABI_TO_INSTRUCTION_SET_MAP
     */
    private static final Map<String, String> ABI_TO_INSTRUCTION_SET_MAP = new HashMap<String, String>();

    static {
        ABI_TO_INSTRUCTION_SET_MAP.put("armeabi", "arm");
        ABI_TO_INSTRUCTION_SET_MAP.put("armeabi-v7a", "arm");
        ABI_TO_INSTRUCTION_SET_MAP.put("mips", "mips");
        ABI_TO_INSTRUCTION_SET_MAP.put("mips64", "mips64");
        ABI_TO_INSTRUCTION_SET_MAP.put("x86", "x86");
        ABI_TO_INSTRUCTION_SET_MAP.put("x86_64", "x86_64");
        ABI_TO_INSTRUCTION_SET_MAP.put("arm64-v8a", "arm64");
    }

    /**
     * Returns the runtime instruction set corresponding to a given ABI. Multiple
     * compatible ABIs might map to the same instruction set. For example
     * {@code armeabi-v7a} and {@code armeabi} might map to the instruction set {@code arm}.
     * <p>
     * This influences the compilation of the applications classes.
     *
     * @param abi abi
     * @return instructionSet
     */
    public static String getInstructionSet(String abi) {
        final String instructionSet = ABI_TO_INSTRUCTION_SET_MAP.get(abi);
        if (instructionSet == null) {
            throw new IllegalArgumentException("Unsupported ABI: " + abi);
        }

        return instructionSet;
    }

    /**
     * sPreferredInstructionSet
     */
    private static String sPreferredInstructionSet;

    /**
     * getPreferredInstructionSet
     *
     * @return PreferredInstructionSet
     */
    private static String getPreferredInstructionSet() {
        if (sPreferredInstructionSet == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sPreferredInstructionSet = getInstructionSet(Build.SUPPORTED_ABIS[0]);
            } else {
                sPreferredInstructionSet = getInstructionSet(Build.CPU_ABI);
            }
        }

        return sPreferredInstructionSet;
    }

    /**
     * getPrimaryInstructionSet
     *
     * @param info ApplicationInfo
     * @return PrimaryInstructionSet
     */
    public static String getPrimaryInstructionSet(ApplicationInfo info) {
        String primaryCpuAbi = (String) JavaCalls.getField(info, "primaryCpuAbi");

        if (primaryCpuAbi == null) {
            return getPreferredInstructionSet();
        }

        return getInstructionSet(primaryCpuAbi);
    }

    /**
     * is64BitInstructionSet
     *
     * @param instructionSet instructionSet
     * @return true or false
     */
    public static boolean is64BitInstructionSet(String instructionSet) {
        return "arm64".equals(instructionSet)
                || "x86_64".equals(instructionSet)
                || "mips64".equals(instructionSet);
    }


    /**
     * 是不是主进程。
     * <p>
     * gpt框架分为两个进程，一个是主进程，跟主程序一个进程。另外一个是 :gpt进程。
     *
     * @param context Context
     * @return 是否为主进程
     */
    public static boolean isHostProcess(Context context) {
        Context hostontext = getHostContext(context);

        String hostPackageName = hostontext.getPackageName();

        while (hostontext instanceof ContextWrapper) {
            hostontext = ((ContextWrapper) hostontext).getBaseContext();
        }

/*        int myPid = Process.myPid();
        int hostPid = 0;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.processName.equals(hostPackageName)) {
                hostPid = processInfo.pid;
                break;
            }
        }
        
        if (myPid == hostPid) {
            return true;
        } else {
            return false;
        }*/

        // 获取ActivityThread
        Object curActivityThread = null;

        curActivityThread = JavaCalls.getField(hostontext, "mMainThread");

        if (curActivityThread == null) {
            return false;
        }

        String processName = JavaCalls.callMethod(curActivityThread, "getProcessName");

        if (processName == null) {
            return false;
        }

        if (hostPackageName.equals(processName)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * 获取当前进程的名称
     *
     * @param context
     * @return 进程名
     */
    public static String getCurrentProcessName(Context context) {
        String processName = "";
        Context hostontext = getHostContext(context);

        while (hostontext instanceof ContextWrapper) {
            hostontext = ((ContextWrapper) hostontext).getBaseContext();
        }

        // 获取ActivityThread
        Object curActivityThread = null;

        curActivityThread = JavaCalls.getField(hostontext, "mMainThread");

        if (curActivityThread == null) {
            return processName;
        }

        processName = JavaCalls.callMethod(curActivityThread, "getProcessName");

        return processName;
    }

    /**
     * 取进程名后缀部分，如果不是以a:b的形式出现，就认为是主进程了
     *
     * @param processName 进程名
     * @return 进程名的后缀
     */
    public static String getProcessNameSuffix(String processName) {
        if (TextUtils.isEmpty(processName)) {
            return "";
        }

        String[] parts = processName.split(":");
        if (parts == null || parts.length != 2) {
            return "";
        }

        return parts[1];
    }

    /**
     * 把Exception的调用栈输出成string
     *
     * @param e Throwable
     * @return 调用栈字符串
     */
    public static String getCallStack(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 自定义方式打印异常调用栈
     *
     * @param e 异常
     */
    public static void printCallStack(Throwable e) {
        if (DEBUG) {
            Log.e("GPT_EXP", "### Exception stack : " + getCallStack(e));
        }
    }

    /**
     * 获取context对应的host context，因为context有可能为插件的context
     *
     * @param context Context
     * @return Context
     */
    public static Context getHostContext(Context context) {
        String packageName = context.getPackageName();

        // 首先判断此函数是不是插件调过来的, 这种情况也存在
        if (ProxyEnvironment.hasInstance(packageName)) {
            context = ProxyEnvironment.getInstance(packageName).getApplicationProxy();
        }

        return context;
    }

    /**
     * 测试反射代码时使用
     *
     * @param className 类名
     */
    public static void testClass(String className) {
        if (DEBUG) {
            Log.d(TAG, "class name " + className);
        }

        try {
            Class<?> clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            Field[] fields = clazz.getDeclaredFields();
            Method[] methods = clazz.getMethods();

            if (DEBUG) {
                Log.d(TAG, "fields.length：" + fields.length + "; methods.length：" + methods.length);
            }

            for (Field f : fields) {
                if (DEBUG) {
                    Log.d(TAG, "f.name：" + f.getName() + ",f.class:" + f.getType().getName());
                }
            }
            for (Method m : methods) {
                if (DEBUG) {
                    Log.d(TAG, "m.name：" + m.getName());
                }
            }

            Class classes[] = clazz.getDeclaredClasses();
            if (DEBUG) {
                Log.d(TAG, "classes.length：" + classes.length);
            }
            for (Class c : classes) { // 对成员内部类进行反射
                if (DEBUG) {
                    Log.d(TAG, "c.name：" + c.getName());
                }
            }
        } catch (ClassNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打印方法
     *
     * @param tag    tag
     * @param method 方法
     * @param args   参数
     * @return 拼接后的显示字符串
     */
    public static String printlnMethod(String tag, Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();
        Class<?>[] types = method.getParameterTypes();
        int i = 0;
        sb.append(tag);
        sb.append(method.getName()).append(", params : ");
        if (args != null) {
            for (Object arg : args) {
                sb.append(types[i] + ":");
                if (arg != null) {
                    sb.append(arg.toString());
                } else {
                    sb.append("null");
                }
                sb.append(",");
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 复制整个文件夹内容
     *
     * @param oldPath String  原文件路径
     * @param newPath String  复制后路径
     * @throws Exception
     */
    public static void copyFolder(String oldPath, String newPath) throws Exception {

        File oldFolder = new File(oldPath);
        String[] file = oldFolder.list();
        File temp = null;
        for (int i = 0; i < file.length; i++) {
            if (oldPath.endsWith(File.separator)) {
                temp = new File(oldPath + file[i]);
            } else {
                temp = new File(oldPath + File.separator + file[i]);
            }
            if (temp.isFile()) {
                File destFile = new File(newPath + "/" + temp.getName().toString());
                if (destFile.exists()) {
                    destFile.delete();
                }
                boolean b = copyToFile(temp, destFile);
                if (!b) {
                    throw new RuntimeException("copy file fail");
                }
            }
            if (temp.isDirectory()) { // 如果是子文件夹
                String newPath2 = newPath + "/" + file[i];
                new File(newPath2).mkdirs();
                copyFolder(oldPath + "/" + file[i], newPath2);
            }
        }
    }
}
