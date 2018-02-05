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
package com.baidu.android.gporter.plug;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.Util;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;

/**
 * 管理解析后的插件信息
 *
 * @author liuhaitao
 * @since 2015-7-21
 */
public class TargetManager {

    /**
     * DEBUG 开关
     */
    private static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    private static final String TAG = "TargetManager";
    /**
     * 单例实例
     */
    private static TargetManager sInstance;
    /**
     * 宿主的application context
     */
    private Context mHostContext = null;
    /**
     * 插件信息的哈希表, TODO 后续可考虑是否使用软引用
     */
    private HashMap<String, SoftReference<TargetMapping>> mTargets = null;

    /**
     * GPT和统计不在同一子项目工程下,所以需要一个Action中转检查处理
     */
    public static final String ACTION_CHECK_UPLOAD_LOG = "com.baidu.android.gporter.stat.CHECK_UPLOAD_LOG";

    /**
     * 私有构造方法
     *
     * @param hostContext 宿主的application context
     */
    private TargetManager(Context hostContext) {
        if (DEBUG) {
            Log.d(TAG, "TargetManager(Context hostContext): hostContext="
                    + hostContext);
        }
        mHostContext = hostContext;
        mTargets = new HashMap<String, SoftReference<TargetMapping>>();
        checkUploadLog(mHostContext);
    }

    /**
     * 检查上报日志
     *
     * @param context Context
     */
    private void checkUploadLog(final Context context) {
        final Context hostContext = Util.getHostContext(context);
        try {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_CHECK_UPLOAD_LOG);
                    hostContext.sendBroadcast(intent);

                    if (DEBUG) {
                        Log.d(TAG, "checkUploadLog(final Context context): End: context="
                                + context + "; hostContext=" + hostContext + "; hostContext.sendBroadcast(intent);");
                    }
                }
            }, 3000); // SUPPRESS CHECKSTYLE
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, "checkUploadLog(final Context context): context="
                        + context + "; hostContext=" + hostContext + "hostContext.sendBroadcast(intent);"
                        + " e=" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取TargetManager的Instance
     *
     * @param hostContext Context
     * @return TargetManager
     */
    public static synchronized TargetManager getInstance(Context hostContext) {
        if (sInstance == null) {
            sInstance = new TargetManager(hostContext);
        }
        return sInstance;
    }

    /**
     * 获取插件信息
     *
     * @param packageName 插件包名
     * @return 插件信息对象
     */
    public TargetMapping getTargetMapping(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        SoftReference<TargetMapping> link = mTargets.get(packageName);
        if (link != null && link.get() != null) {
            return link.get();
        }

        GPTPackageInfo info = GPTPackageManager.getInstance(mHostContext).getPackageInfo(packageName);
        if (info != null) {
            TargetMapping target = new ApkTargetMapping(mHostContext, new File(info.srcApkPath));
            mTargets.put(packageName, new SoftReference<TargetMapping>(target));
            return target;
        }

        return null;

    }

}

