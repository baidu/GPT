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
package android.net.wifi;

/**
 * Hook系统的对应接口类方法
 *
 * @author liuhaitao
 * @since 2018-01-10
 */
public interface IWifiManager {

    /**
     * setWifiEnabled
     * 兼容特殊机型GT-I9508，4.4ROM，发现setWifiEnabled方法多了一个参数
     *
     * @param pkg     包名
     * @param enabled 是否可用
     * @return true or false
     */
    boolean setWifiEnabled(String pkg, boolean enabled);
}
