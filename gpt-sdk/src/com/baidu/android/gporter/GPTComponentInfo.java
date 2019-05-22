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

import android.content.Intent;

import com.baidu.android.gporter.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

/**
 * 插件组件信息
 *
 * 写这个是为了封装成一个json string 放到 category里。
 * 只所以不用extra是因为用extra的话，我们在Activity的super.onCreate之前就会去取extra， 导致calssloader取的不正确。
 *
 * @author liuhaitao
 * @since 2015-3-3
 */
public final class GPTComponentInfo {

    /**
     * DEBUG开关
     */
    private static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * 插件包名
     */
    public String packageName = null;
    /**
     * 组件类名
     */
    public String className = null;

    /**
     * Component对应的intent是初始化插件后，重新调度intent
     */
    public boolean reschedule = false;

    /**
     * 从Intent中解析组件信息
     *
     * @param intent Intent
     * @return 组件信息
     */
    public static GPTComponentInfo parseFromIntent(Intent intent) {
        GPTComponentInfo relt = null;

        if (intent == null) {
            return null;
        }

        Set<String> cates = intent.getCategories();
        if (cates == null || cates.size() == 0) {
            return null;
        }

        for (String cate : cates) {
            relt = parseFromString(cate);
            if (relt != null) {
                break;
            }
        }

        return relt;
    }

    /**
     * 从String里解析组件信息
     *
     * @param str String
     * @return 组件信息
     */
    public static GPTComponentInfo parseFromString(String str) {
        GPTComponentInfo relt = null;

        if (str != null && str.startsWith(ProxyEnvironment.TARGET_BEGIN_FLAG)) {
            try {
                GPTComponentInfo info = new GPTComponentInfo();

                JSONObject jo = new JSONObject(str.substring(ProxyEnvironment.TARGET_BEGIN_FLAG.length()));
                info.packageName = jo.getString(ProxyEnvironment.JKEY_TARGET_PACKAGNAME);
                info.className = jo.getString(ProxyEnvironment.JKEY_TARGET_ACTIVITY);
                info.reschedule = jo.getBoolean(ProxyEnvironment.JKEY_TARGET_RESCHEDULE);
                relt = info;
            } catch (JSONException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        return relt;
    }

    /**
     * 把component info添加到intent
     *
     * @param intent 目标intent
     */
    public void addSelf2Intent(Intent intent) {

        // 如果原来就包含了gpt的component信息，先去掉。因为发现支付插件会把intent透传给下一个activity，
        // 导致remap失效，还是上一次的component，出现循环启动同一个activity
        Set<String> cates = intent.getCategories();
        ArrayList<String> toRemove = new ArrayList<String>();
        if (cates != null && cates.size() > 0) {
            for (String cate : cates) {
                if (cate != null && cate.startsWith(ProxyEnvironment.TARGET_BEGIN_FLAG)) {
                    toRemove.add(cate);
                }
            }
        }

        for (String cate : toRemove) {
            intent.removeCategory(cate);
        }

        intent.addCategory(this.toString());
    }

    @Override
    public String toString() {
        JSONObject jo = new JSONObject();
        String format = null;
        try {
            jo.put(ProxyEnvironment.JKEY_TARGET_PACKAGNAME, packageName);
            jo.put(ProxyEnvironment.JKEY_TARGET_ACTIVITY, className);
            jo.put(ProxyEnvironment.JKEY_TARGET_RESCHEDULE, reschedule);
            format = ProxyEnvironment.TARGET_BEGIN_FLAG + jo.toString();
        } catch (JSONException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (format != null) {
            return format;
        } else {
            return super.toString();
        }
    }

}
