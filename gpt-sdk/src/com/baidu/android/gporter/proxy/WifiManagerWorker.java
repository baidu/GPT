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

import android.content.Context;
import android.net.wifi.IWifiManager;

import com.baidu.android.gporter.util.Constants;

/**
 * WifiManagerWorker
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class WifiManagerWorker extends InterfaceProxy {

    /**
     * 构造方法
     */
    public WifiManagerWorker() {
        super(Constants.WIFI_MANAGER_CLASS);
    }

    /**
     * 系统原始的IWifiManager
     */
    public IWifiManager mTarget;

    /**
     * host Context
     */
    public Context mHostContext;

    /**
     * setWifiEnabled
     *
     * @param pkg     包名
     * @param enabled 是否可用
     * @return true or false
     */
    public boolean setWifiEnabled(String pkg, boolean enabled) {
        return mTarget.setWifiEnabled(mHostContext.getPackageName(), enabled);
    }

}

