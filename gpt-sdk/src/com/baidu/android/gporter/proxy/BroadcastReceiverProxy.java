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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.util.Constants;

/**
 * 广播代理，插件的广播被代理发送
 *
 * @author liuhaitao
 * @since 2014年4月29日
 */
public class BroadcastReceiverProxy extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (intent != null) {
            GPTComponentInfo info = GPTComponentInfo.parseFromIntent(intent);
            if (info == null) {
                return;
            }
            final String targetReceiver = info.className;
            final String targetPkgName = info.packageName;
            try {
                if (!ProxyEnvironment.isEnterProxy(targetPkgName)) { //插件没有启动的话，重新启动，且重新map Intent
                    Intent destIntent = new Intent(intent);
                    destIntent.setComponent(new ComponentName(targetPkgName, targetReceiver));
                    ProxyEnvironment.enterProxy(context, destIntent, true, true);
                } else {
                    BroadcastReceiver target = ((BroadcastReceiver) ProxyEnvironment.getInstance(targetPkgName)
                            .getDexClassLoader().loadClass(targetReceiver).asSubclass(BroadcastReceiver.class)
                            .newInstance());
                    target.onReceive(ProxyEnvironment.getInstance(targetPkgName).getApplication(), intent);
                }

            } catch (InstantiationException e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

}
