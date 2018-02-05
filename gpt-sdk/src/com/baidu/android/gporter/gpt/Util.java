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
package com.baidu.android.gporter.gpt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.util.Constants;

/**
 * 工具类，主要处理intent
 *
 * @author liuhaitao
 * @since 2014年4月29日
 */
public class Util {

    /**
     * 退出某个插件应用，不是卸载插件
     *
     * @param packageName 包名
     */
    public static void quitApp(String packageName) {
        if (ProxyEnvironment.hasInstance(packageName)) {
            ProxyEnvironment.getInstance(packageName).quitApp();
        }
    }

    /**
     * 创建插件Activity的启动Intent
     *
     * @param ctx         host或者插件的application context
     * @param packageName 插件包名
     * @param className   启动的Activity类名
     * @return intent
     */
    public static Intent createActivityIntent(Context ctx, String packageName, String className) {
        Intent intent = new Intent();
        intent.setClassName(ctx.getPackageName(), className);
        if (ProxyEnvironment.hasInstance(packageName)) {
            ProxyEnvironment.getInstance(packageName).remapStartActivityIntent(intent, className);
        } else {
            intent.setClass(ctx, ActivityProxy.class);
            GPTComponentInfo info = new GPTComponentInfo();
            info.packageName = packageName;
            info.className = className;
            intent.addCategory(info.toString());
        }

        return intent;
    }

    /**
     * 检查Intent的extra是否合法，解决安全漏洞提示的 exported = true 的activity、receiver、service 的攻击而捕捉异常
     * 同时兼容卫士误将Intent序列化类传入到字符序列化Extra中引起的getStringExtra()反序列化时出错问题。
     *
     * @param intent intent
     * @return boolean true:合法；false：不合法
     */
    public static boolean hasValidIntentExtra(Intent intent) {
        if (intent == null) {
            return false;
        }
        try {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                bundle.containsKey(null);
            }
            return true;
        } catch (Exception e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
