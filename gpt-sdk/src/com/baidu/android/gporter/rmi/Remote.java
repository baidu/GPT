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

/**
 *
 * 具体接口需要继承该类。实现对应接口。
 *
 * <pre class="prettyprint">

IClient.aidl:

interface IClient {
    String test(String className);
}



public class ClientApiImpl implements Remote {

    public IBinder getIBinder() {

        IClient.Stub binder = new IClient.Stub() {

            public String test(String className) throws RemoteException {
                return className + "  from client";
            }
        };

        return binder;
    }
}
 * </pre>
 *
 * @author liuhaitao
 * @since 2018-01-08
 *
 */
public interface Remote {
    /**
     * 返回具体接口的 iBinder实例。
     * @return 返回的是aidl的 stub 的实现。
     */
    IBinder getIBinder();
}
