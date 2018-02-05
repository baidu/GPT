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

import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;

import java.util.HashMap;
import java.util.Map;


/**
 * ProxyProviderCounter
 *
 * @author liuhaitao
 * @since 2017年12月26日
 */
public class ProxyProviderCounter {
    /**
     * ProxyProviderCounter
     */
    private static ProxyProviderCounter instance;

    /**
     * allProviders的Map
     */
    private Map<String, ProviderInfo> allProviders = new HashMap<String, ProviderInfo>();

    /**
     * 构造方法
     */
    private ProxyProviderCounter() {

    }

    /**
     * 获取ProxyProviderCounter的Instance
     */
    public static synchronized ProxyProviderCounter getInstance() {
        if (instance == null) {
            instance = new ProxyProviderCounter();
        }
        return instance;
    }

    /**
     * 获取ProviderInfo
     *
     * @param authority authority
     * @return ProviderInfo
     */
    public ProviderInfo getProviderInfo(String authority) {
        if (allProviders.containsKey(authority)) {
            return allProviders.get(authority);
        }
        return null;
    }

    /**
     * 添加Provider
     *
     * @param pkgInfo PackageInfo
     */
    public void addProviderMap(PackageInfo pkgInfo) {
        if (pkgInfo.providers != null) {
            for (int i = 0; i < pkgInfo.providers.length; i++) {
                allProviders.put(pkgInfo.providers[i].authority, pkgInfo.providers[i]);
            }
        }
    }
}
