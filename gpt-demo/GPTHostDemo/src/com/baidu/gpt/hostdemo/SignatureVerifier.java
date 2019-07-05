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
package com.baidu.gpt.hostdemo;

import android.content.pm.Signature;
import android.util.Log;
import com.baidu.android.gporter.pm.ISignatureVerify;

/**
 * SignatureVerifier
 * 插件的签名校验宿主自定义策略实现类
 * TODO 策略实现后同时在宿主AndroidManifest.xml中的 application 标签中添加如下对应meta-data,其中name不变、value为本类全路径名即可(可直接参考本Demo对应添加方法)
 * <meta-data android:name="com.baidu.android.gporter.signatureverify.class" android:value="com.baidu.gpt.hostdemo.SignatureVerifier" />
 *
 * @author liuhaitao
 * @since 2014-07-16
 */
public class SignatureVerifier implements ISignatureVerify {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & MainActivity.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "SignatureVerifier";

    @Override
    public boolean checkSignature(String packageName, boolean isReplace, Signature[] signatures, Signature[] newSignatures) {
        if (DEBUG) {
            Log.d(TAG, "checkSignature(String packageName, boolean isReplace, Signature[] signatures, Signature[] newSignatures):\n"
                    + "packageName=" + packageName + "; isReplace=" + isReplace
                    + "; signatures=" + signatures + "; newSignatures=" + newSignatures);
        }
        // 自定义签名策略，验证合法时需要返回true,验证不合法则需返回false。
        // 测试时可默认返回true，否则不能安装插件。
        return true;
    }

}


