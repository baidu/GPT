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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;

import com.baidu.android.gporter.plug.TargetManager;
import com.baidu.android.gporter.plug.TargetMapping;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.proxy.activity.ActivityProxyExt;
import com.baidu.android.gporter.proxy.activity.ActivityProxyTranslucent;
import com.baidu.android.gporter.proxy.activity.ActivityProxyTranslucentExt;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;

/**
 * Activity映射的管理，并且做Activity代理的计数
 *
 * @author liuhaitao
 * @since 2014年7月30日
 */
public class ProxyActivityCounter {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ProxyActivityCounter";

    /**
     * ProxyActivityCounter
     */
    private static ProxyActivityCounter instance;

    /**
     * 构造方法
     */
    private ProxyActivityCounter() {

    }

    /**
     * 获取ProxyActivityCounter的Instance
     */
    public static synchronized ProxyActivityCounter getInstance() {
        if (instance == null) {
            instance = new ProxyActivityCounter();
        }
        return instance;
    }

    /**
     * 返回插件activity对应的代理Activity
     *
     * @param hostCtx     宿主context
     * @param packageName 插件包名
     * @param actInfo     插件activity info
     * @param pkgInfo     自定义的插件信息
     * @return 代理Activity类对象
     */
    public Class<?> getNextAvailableActivityClass(Context hostCtx, String packageName, ActivityInfo actInfo,
                                                  GPTPackageInfo pkgInfo) {

        Class<?> proxyClass = ActivityProxy.class;

        boolean useTranslucentTheme = useTranslucentTheme(hostCtx, packageName, actInfo.theme);

        switch (pkgInfo.extProcess) {
            case Constants.GPT_PROCESS_DEFAULT:
            default:
                if (useTranslucentTheme) {
                    if (pkgInfo.isUnionProcess) {
                        proxyClass = ActivityProxyTranslucent.class;
                    } else {
                        proxyClass = ActivityProxyTranslucentExt.class;
                    }
                } else {
                    if (pkgInfo.isUnionProcess) {
                        proxyClass = ActivityProxy.class;
                    } else {
                        proxyClass = ActivityProxyExt.class;
                    }
                }
                break;
        }
        return proxyClass;

    }

    /**
     * 判断 activity是否使用 透明背景。 因为透明背景不能动态切换，所以我们使用一个独立的透明activity。
     *
     * @param hostContext     插件包名
     * @param packageName     插件包名
     * @param resourceThemeId 主题资源id
     * @return 主题是否透明
     */
    public boolean useTranslucentTheme(Context hostContext, String packageName, int resourceThemeId) {
        boolean useTranslucentTheme = true; // 默认用透明的，万一错了，activityproxy会先finish，再重新启动，这样不会闪黑一下
        try {
            synchronized (this) {
                TypedArray windowStyle = null;
                int[] windowStyleIndex = (int[]) JavaCalls.getStaticField("com.android.internal.R$styleable", "Window");

                Resources targetRes = null;
                if (!ProxyEnvironment.hasInstance(packageName)) {
                    TargetMapping mapping = TargetManager.getInstance(hostContext).getTargetMapping(packageName);
                    if (mapping == null) {
                        return true; // 容错，查不到插件，就默认透明
                    }

                    try {
                        AssetManager am = (AssetManager) AssetManager.class.newInstance();
                        JavaCalls.callMethod(am, "addAssetPath", new Object[]{mapping.getApkPath()});
                        targetRes = new Resources(am, hostContext.getResources().getDisplayMetrics(), hostContext
                                .getResources().getConfiguration());
                    } catch (Exception e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                        return true; // 容错，默认透明
                    }
                } else {
                    targetRes = ProxyEnvironment.getInstance(packageName).getTargetResources();
                }

                Theme theme = targetRes.newTheme();
                theme.applyStyle(resourceThemeId, true);

                windowStyle = theme.obtainStyledAttributes(windowStyleIndex);

                // 检查 Window_windowIsFloating， dialog 用这种方式
                int index = (Integer) JavaCalls.getStaticField("com.android.internal.R$styleable", "Window_windowIsFloating");
                useTranslucentTheme = windowStyle.getBoolean(index, false);

                // 检查 Window_windowIsTranslucent， Theme.Translucent 用这种方式
                if (!useTranslucentTheme) {
                    index = (Integer) JavaCalls.getStaticField("com.android.internal.R$styleable", "Window_windowIsTranslucent");
                    useTranslucentTheme = windowStyle.getBoolean(index, false);
                }

                if (windowStyle != null) {
                    windowStyle.recycle();
                }
            }
        } catch (Exception e) {
            // 因为用到了内部类，所以做catch处理，避免找不到时crash
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return useTranslucentTheme;
    }
}
