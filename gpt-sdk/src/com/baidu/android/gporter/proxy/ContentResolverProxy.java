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

import android.app.IActivityManager.ContentProviderHolder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;

import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

/**
 * 提供给host使用的ContentResolver封装，可以通过插件的authority直接操作插件的provider.
 *
 * @author liuhaitao
 * @since 2015年12月18日
 */
public class ContentResolverProxy extends ContentResolver {

    /**
     * ContentResolver
     */
    private ContentResolver mHostContentResolver;

    /**
     * 构造方法
     *
     * @param context         Context
     * @param contentResolver ContentResolver
     */
    public ContentResolverProxy(Context context, ContentResolver contentResolver) {
        super(context);

        this.mHostContentResolver = contentResolver;
    }

    /**
     * acquireProvider
     *
     * @param context Context
     * @param name    name
     * @return IContentProvider
     */
    protected IContentProvider acquireProvider(Context context, String name) {
        IContentProvider result = null;

        context = Util.getHostContext(context);
        ContentProviderHolder holder = ActivityManagerNativeWorker.getContentProviderHolder(context, name);
        if (holder != null && holder.provider != null) {
            result = holder.provider;
        }

        if (result == null) {
            result = JavaCalls.callMethod(mHostContentResolver, "acquireProvider", context, name);
        }
        return result;
    }

    /**
     * acquireUnstableProvider
     *
     * @param context Context
     * @param name    name
     * @return IContentProvider
     */
    protected IContentProvider acquireUnstableProvider(Context context, String name) {

        IContentProvider result = null;

        context = Util.getHostContext(context);
        ContentProviderHolder holder = ActivityManagerNativeWorker.getContentProviderHolder(context, name);
        if (holder != null && holder.provider != null) {
            result = holder.provider;
        }

        if (result == null) {
            result = JavaCalls.callMethod(mHostContentResolver, "acquireUnstableProvider", context, name);
        }
        return result;
    }

    /**
     * acquireExistingProvider
     *
     * @param context Context
     * @param name    name
     * @return IContentProvider
     */
    protected IContentProvider acquireExistingProvider(Context context, String name) {
        return JavaCalls.callMethod(mHostContentResolver, "acquireExistingProvider", context, name);
    }

    /**
     * releaseProvider
     *
     * @param provider IContentProvider
     * @return true or false
     */
    public boolean releaseProvider(IContentProvider provider) {
        return JavaCalls.callMethod(mHostContentResolver, "releaseProvider", provider);
    }

    /**
     * releaseUnstableProvider
     *
     * @param icp IContentProvider
     * @return true or false
     */
    public boolean releaseUnstableProvider(IContentProvider icp) {
        return JavaCalls.callMethod(mHostContentResolver, "releaseUnstableProvider", icp);
    }

    /**
     * unstableProviderDied
     *
     * @param icp IContentProvider
     */
    public void unstableProviderDied(IContentProvider icp) {
        JavaCalls.callMethod(mHostContentResolver, "unstableProviderDied", icp);
    }
}
