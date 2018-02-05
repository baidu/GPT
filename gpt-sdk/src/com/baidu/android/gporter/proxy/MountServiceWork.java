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
import android.os.storage.IMountService;

import com.baidu.android.gporter.util.Constants;

/**
 * MountServiceWork
 *
 * @author liuhaitao
 * @since 2017-05-14
 */
public class MountServiceWork extends InterfaceProxy {
    /**
     * 系统原始的IMountService
     */
    public IMountService mTarget;

    /**
     * host Context
     */
    public Context mHostContext;

    /**
     * 构造方法
     */
    public MountServiceWork() {
        super(Constants.IMOUNT_SERVICE_CLASS);
    }

    /**
     * mkdirs
     *
     * @param callingPkg 包名
     * @param path       路径
     * @return mTarget.mkdirs(mHostContext.getPackageName(), path)
     */
    public int mkdirs(String callingPkg, String path) {
        return mTarget.mkdirs(mHostContext.getPackageName(), path);
    }
}
