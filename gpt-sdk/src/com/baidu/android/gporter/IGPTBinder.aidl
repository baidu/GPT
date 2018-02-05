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
import com.baidu.android.gporter.IGPTEnvCallBack;
import android.content.Intent;
import android.os.IBinder;
import com.baidu.android.gporter.stat.PluginTimeLine;

/**
* 主程序调用插件进程的接口
* @author liuhaitao
* @since 2017年12月26日
*/
interface IGPTBinder {
    void launchIntents(String packageName, in Intent[] intents, in PluginTimeLine timeLine,  long hotTimeStart, IGPTEnvCallBack back);
    boolean exitProxy(String packageName,boolean force);
    IBinder getInterface(String packageName, String className);
    boolean hasInstance(String packageName);
}