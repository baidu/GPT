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
package com.baidu.android.gporter.gpt;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import com.baidu.android.gporter.ProxyEnvironment;

/**
 * GPTIntent
 *
 * @author liuhaitao
 * @since 2014年11月24日
 */
public class GPTIntent extends Intent {
    /**
     * PackageName
     */
    private String mPackageName;
    /**
     * ActivityClassName
     */
    private String mActivityClassName;

    /**
     * 构造方法
     */
    public GPTIntent(Intent o) {
        super(o);
    }

    @Override
    public Intent setClassName(String packageName, String className) {
        mPackageName = packageName;
        mActivityClassName = className;
        return super.setClassName(packageName, className);
    }

    @Override
    public ActivityInfo resolveActivityInfo(PackageManager pm, int flags) {
        ActivityInfo targetInfo = ProxyEnvironment.getInstance(mPackageName)
                .getTargetMapping().getActivityInfo(mActivityClassName);

        ActivityInfo info = new ActivityInfo(targetInfo);

        return info;
    }

}

