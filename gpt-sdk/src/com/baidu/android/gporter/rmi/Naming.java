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
package com.baidu.android.gporter.rmi;

import android.os.IBinder;
import android.os.RemoteException;

import com.baidu.android.gporter.GPTProcessService;
import com.baidu.android.gporter.IGPTBinder;
import com.baidu.android.gporter.IHostBinder;
import com.baidu.android.gporter.MainProcessService;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.util.Constants;

/**
 * 查找主进程或者 gpt进程 remote 接口的工具类。
 * 获取到 IBinder 后，需要使用 对应的aidl的 asInterface 获取到对应的接口。
 * <p>
 * <pre class="prettyprint">
 * IBinder binlder = Naming.lookupPlugin("com.harlan.animation", "com.harlan.animation.rmi.ClientApiImpl");
 * IClient client = IClient.Stub.asInterface(binlder);
 * client.test("param");
 * </pe>
 * <p>
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class Naming {

    /**
     * 查找主进程中的aidl接口实现。 对应的类需要是 {@link Remote}的子类。
     *
     * @param className aidl stub 实现类的类名（{@link Remote}）的子类)
     * @return 有可能为空。比如进程刚刚启动，bind还没有回调完成。这种情况可以进行延时重试。
     */
    public static IBinder lookupHost(String className) {
        IHostBinder hostBinder = GPTProcessService.sHostBinder;
        IBinder binder = null;

        // 有可能为空。比如主动杀死被 bind进程，等待重启的时间段内。这时候我们忽略。简单处理。很少发生。
        if (hostBinder != null) {
            try {
                binder = hostBinder.getInterface(className);
            } catch (RemoteException e) {
                if (Constants.DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        // 这种情况发生在，插件运行在主进程中。直接通过函数调用获得。
        if (binder == null) {
            binder = MainProcessService.getTargetInterface(className);
        }

        return binder;
    }

    /**
     * 查找 插件中的接口实现。对应的类需要是 {@link Remote}的子类。
     * <p>
     * 调用该函数之前，确定该插件已经被gpt框架加载到内存。
     *
     * @param packageName 包名
     * @param className   类名
     * @return 返回有可能为空。比如刚刚bind完，还没异步回调完成。这种情况可以进行延时重试。
     */
    public static IBinder lookupPlugin(String packageName, String className) {
        IGPTBinder[] gptBinders = ProxyEnvironment.sGPTBinders;
        IBinder binder = null;

        for (IGPTBinder gptBinder : gptBinders) {

            // 即便运行在这个进程，有可能为空。比如主动杀死被 bind进程，等待重启的时间段内。这时候我们忽略。简单处理。很少发生。
            if (gptBinder != null) {
                try {
                    binder = gptBinder.getInterface(packageName, className);
                } catch (RemoteException e) {
                    if (Constants.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }

            if (binder != null) {
                break;
            }
        }


        // 这种情况发生在，插件运行在主进程中。直接通过函数调用获得。
        if (binder == null) {
            binder = GPTProcessService.getTargetInterface(packageName, className);
        }

        return binder;
    }
}



