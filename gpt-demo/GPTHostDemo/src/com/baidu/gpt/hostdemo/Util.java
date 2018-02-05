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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * 工具方法封装类
 *
 * @author liuhaitao
 * @since 2017-09-01
 */
public final class Util {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & MainActivity.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "Util";

    /**
     * 写入文件，在文件后面追加写入
     *
     * @param filePath 文件路径
     * @param conent   写入内容
     */
    public static void writeFile(String filePath, String conent) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath, true)));
            out.write(conent + "\r\n");
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取文本文件内容
     *
     * @param file 文件
     * @return 文件内容
     */
    public static String readStringFile(File file) {

        String content = null;
        if (file != null && file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                StringBuffer strBuffer = new StringBuffer();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    strBuffer.append(line).append("\n");
                }
                content = strBuffer.toString();
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return content;
    }

    /**
     * 读取文本文件内容
     *
     * @param filePath 文件地址
     * @return 文件内容
     */
    public static String readStringFile(String filePath) {
        if (filePath != null) {
            return readStringFile(new File(filePath));
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param path 文件路径
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file == null) {
            return;
        }
        if (file.exists()) {
            file.delete();
        }
    }
}