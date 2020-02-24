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

import com.baidu.android.gporter.util.Constants;

import dalvik.system.DexClassLoader;

/**
 * @author liuhaitao
 * @since 2015-3-2
 */
public class GPTClassLoader extends DexClassLoader {

    /**
     * 宿主的ClassLoader
     */
    private ClassLoader mHostClassLoader;

    /**
     * 构造方法
     *
     * @param dexPath            dexPath
     * @param optimizedDirectory optimizedDirectory
     * @param libraryPath        libraryPath
     * @param parent             ParentClassLoader
     * @param hostLoader         HostClassLoader
     */
    public GPTClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent,
                          ClassLoader hostLoader) {
        super(dexPath, optimizedDirectory, libraryPath, parent);

        mHostClassLoader = hostLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        Class<?> clazz = null;
        try {
            clazz = super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        }

        // 如果在插件的ClassLoader里没有找到类，则用宿主mHostClassLoader的去查找。
        // 插件可以采用编译时依赖，但真正打包时去除的方式，来实现如下相关功能：
        // (1)插件复用宿主版本的代码逻辑；
        // (2)插件的功能逻辑保持和宿主对应版本一致，而不再出现同一宿主中不同插件功能逻辑不一致的情况；
        // (3)插件、宿主都引入复用的公共第三方库，也可通过类似方法，实现只在宿主中添加一份库文件,
        //    而无需所有插件都重复引入库文件,从而实现公共库复用依赖的同时，更能有效缩减冗余代码和保持宿主、插件功能逻辑一致性。
        // (4)同步用户反馈问题，如想作到插件类ClassLoader的完全安全隔离，则只需注掉下面的HostClassLoader复用查找机制即可。
        if (clazz == null) {
            clazz = mHostClassLoader.loadClass(name);
        }

        return clazz;
    }
}

