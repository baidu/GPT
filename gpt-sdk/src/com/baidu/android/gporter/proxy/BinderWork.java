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

import android.os.IBinder;
import android.os.IInterface;

/**
 * 代理android.os.IBinder
 *
 * @author liuhaitao
 * @since 2017-05-14
 */
public class BinderWork extends InterfaceProxy {

    /**
     * OlderBinder
     */
    public IBinder mOlderBinder;
    /**
     * NewBinder
     */
    public IInterface mNewBinder;

    /**
     * 构造方法
     */
    public BinderWork() {
        super(IBinder.class.getName());
    }

    /**
     * queryLocalInterface
     *
     * @param descriptor descriptor
     * @return IInterface
     */
    public IInterface queryLocalInterface(String descriptor) {
        mOlderBinder.queryLocalInterface(descriptor);
        return mNewBinder;
    }
}
