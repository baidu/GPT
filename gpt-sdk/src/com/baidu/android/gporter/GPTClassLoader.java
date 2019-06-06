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

        // 如果在插件的ClassLoader里没有找到类，则用宿主的去查找。
        if (clazz == null) {
            clazz = mHostClassLoader.loadClass(name);
        }

        return clazz;
    }
}

