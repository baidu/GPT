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
package com.baidu.appsearch.invoke;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.baidu.gpt.hostdemo.MainActivity;

/**
 * AppSearchInvoker
 * 注:AppSearchInvoker相关测试,其它新产品可不关注。
 *
 * @author liuhaitao
 * @since 2014-11-26
 */
public final class AppSearchInvoker {

    /**
     * 工具类，不可实例化
     */
    private AppSearchInvoker() {

    }

    /**
     * 调起试玩包的完整版游戏
     *
     * @param ctx         Context
     * @param packageName 完整包名
     * @return 调起是否成功
     */
    public static boolean getFullGame(Context ctx, String packageName) {

        boolean isSuccess = false;
        try {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            Uri contentUrl = Uri.parse("http://m.baidu.com/app");
            intent.setData(contentUrl);
            ctx.startActivity(intent);
            isSuccess = true;
        } catch (Exception e) {
            isSuccess = false;
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        }

        return isSuccess;
    }

}

