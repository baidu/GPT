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

import android.app.IActivityManager.ContentProviderHolder;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.ProxyProviderCounter;
import com.baidu.android.gporter.gpt.ProviderProxyCursor;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 插件之间的 contentprovider调用，通过此类进行代理。
 *
 * @author liuhaitao
 * @since 2015年12月9日
 */
public class ContentProviderProxy extends ContentProvider {

    /**
     * AUTHORITY
     */
    public static String AUTHORITY = "com.baidu.android.gpt.ContentProviderProxy";
    /**
     * AUTHORITY_EXT
     */
    public static String AUTHORITY_EXT = "com.baidu.android.gpt.ContentProviderProxyExt";

    /**
     * 存储ContentProvider的SP文件
     */
    private static final String SP_FILENAME = "com.baidu.android.gpt.Providers";

    /**
     * mActivityThread
     */
    private Object mActivityThread;

    @Override
    public boolean onCreate() {

        String className = "android.app.ActivityThread";
        
        /*
         * 因为在 andorid 4.1 及以下系统中，在query函数中无法得到 currentActivityThread，因为那个是从ThreadLocal中获取的。
         * query函数是从 Binder_X 线程调过来的，所以获取不到。
         */
        try {
            Class clazz = Class.forName(className);
            Method m = clazz.getDeclaredMethod("currentActivityThread");
            mActivityThread = m.invoke(null);
        } catch (Exception e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        }

        checkAuthority();

        return true;
    }

    /**
     * 校验 authority 在 manifest声明是否正确。
     * 格式：packagename_authority, 比如 com.baidu.hostdemo_com.baidu.android.gpt.ContentProviderProxy
     */
    protected void checkAuthority() {
        Context context = getContext();
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);

            String authority = context.getPackageName() + "_" + AUTHORITY;
            String authorityExt = context.getPackageName() + "_" + AUTHORITY_EXT;

            boolean result = false;
            boolean resultExt = false;

            for (ProviderInfo provider : packageInfo.providers) {
                if (provider.name.equals(ContentProviderProxy.class.getName())) {
                    if (provider.authority.endsWith(authority)) {
                        result = true;
                    }
                }

                if (provider.name.equals(ContentProviderProxyExt.class.getName())) {
                    if (provider.authority.endsWith(authorityExt)) {
                        resultExt = true;
                    }
                }
            }

            if (!result) {
                throw new RuntimeException("ContentProviderProxy 's authority should be declared :" + authority);
            }

            if (!resultExt) {
                throw new RuntimeException("ContentProviderProxyExt 's authority should be declared :" + authorityExt);
            }

        } catch (NameNotFoundException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        /*
         * projection[0] 为 packagename ， projection[1] 为 authority
         */
        if (projection.length != 2) {
            return null;
        }

        String packageName = projection[0];
        String authority = projection[1];

        if (packageName == null || packageName.length() == 0 || authority == null || authority.length() == 0) {
            return null;
        }

        // 没有安装直接返回
        if (!GPTPackageManager.getInstance(getContext()).isPackageInstalled(packageName)) {
            return null;
        }

        // 没有初始化，需要初始化
        if (!ProxyEnvironment.isEnterProxy(packageName)) {
            ProxyEnvironment.initProxyEnvironment(getContext(), packageName);

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, ProxyEnvironment.LOADTARGET_STUB_TARGET_CLASS));
            ProxyEnvironment.launchIntent(getContext(), intent, mActivityThread);
        }

        if (!ProxyEnvironment.hasInstance(packageName)) {
            return null;
        }
        ProxyEnvironment pe = ProxyEnvironment.getInstance(packageName);
        ProviderInfo pi = ProxyProviderCounter.getInstance().getProviderInfo(authority);

        IContentProvider provider = null;
        if (pi != null) {
            ClassLoader cl = pe.getDexClassLoader();
            try {
                ContentProvider contentProvier = (ContentProvider) cl.loadClass(pi.name).newInstance();
                contentProvier.attachInfo(pe.getApplication(), pi);
                provider = (IContentProvider) JavaCalls.callMethod(contentProvier, "getIContentProvider");
            } catch (Exception e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        Bundle bundle = new Bundle();

        ContentProviderHolder holder = new ContentProviderHolder(pi);
        holder.provider = provider;
        holder.noReleaseNeeded = true;
        bundle.putParcelable("provider", holder);

        ProviderProxyCursor cursor = new ProviderProxyCursor(projection);
        cursor.setExtras(bundle);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * 安装阶段调用，把package对应的provider存入sp中。
     *
     * @param hostContext Context
     * @param providers   ProviderInfo[]
     */
    public static void addProviders(Context hostContext, ProviderInfo[] providers) {
        if (hostContext == null || providers == null || providers.length == 0) {
            return;
        }

        SharedPreferences sp = hostContext.getSharedPreferences(SP_FILENAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();

        for (ProviderInfo provider : providers) {
            String packageName = provider.packageName;
            String authority = provider.authority;

            if (sp.contains(authority)) {
                if (Constants.DEBUG) {
                    Log.e("ContentProviderProxy", packageName + "的provider: " + authority
                            + " 已经存在 , replace it.");
                }
            }

            editor.putString(authority, packageName);
        }

        editor.commit();
    }

    /**
     * removeProviders
     *
     * @param hostContext Context
     * @param packageName 包名
     */
    public static void removeProviders(Context hostContext, String packageName) {
        if (hostContext == null || packageName == null) {
            return;
        }

        SharedPreferences sp = hostContext.getSharedPreferences(SP_FILENAME, Context.MODE_MULTI_PROCESS);

        Map<String, String> providers = (Map<String, String>) sp.getAll();

        Editor editor = sp.edit();

        for (Map.Entry<String, String> entry : providers.entrySet()) {
            String authority = entry.getKey();
            String pkgName = entry.getValue();

            if (packageName.equals(pkgName)) {
                editor.remove(authority);
            }
        }

        editor.commit();
    }

    /**
     * 从sp中获取authroty对应的packagename
     *
     * @param hostContext Context
     * @param authority   authority
     */
    public static String getProviderPackageName(Context hostContext, String authority) {
        SharedPreferences sp = hostContext.getSharedPreferences(SP_FILENAME, Context.MODE_PRIVATE);
        return sp.getString(authority, null);
    }
}
