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
package com.baidu.android.gporter.pm;

import android.content.pm.Signature;

/**
 * 校验插件的签名是否合法。
 * 在 host app 的 AndroidManifest.xml 中声明此类的具体实现子类，
 * <meta-data android:name="com.baidu.android.gporter.signatureverify.class" android:value="com.baidu.appsearch.XXXX" />
 * <p>
 * 也可以不声明，采用megapp框架默认实现：覆盖安装时如果签名不一致则安装失败，和android系统策略一致。
 *
 * @author liuhaitao
 * @since 2014年7月16日
 */
public interface ISignatureVerify {
    /**
     * 在matedata中声明的 key {@value}
     */
    public static final String MATA_DATA_VERIFY_CLASS = "com.baidu.android.gporter.signatureverify.class";

    /**
     * 校验签名是否合法，由主程序自己实现。
     *
     * @param packageName   插件packageName
     * @param isReplace     是否是覆盖安装
     * @param signatures    当前已经安装的插件签名
     * @param newSignatures 新版本的插件的签名
     * @return 验证通过返回true，不通过返回false。只有返回true才会继续安装。
     */
    boolean checkSignature(String packageName, boolean isReplace, Signature[] signatures, Signature[] newSignatures);
}
