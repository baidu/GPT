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
package com.baidu.android.gporter.util;

import android.content.pm.Signature;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * apk 签名解析
 *
 * @author liuhaitao
 * @since 2014年6月6日
 */
public final class SignatureParser {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "SignatureParser";

    /**
     * 缓存管理的锁
     */
    private static Object mSync = new Object();
    /**
     * 缓存池
     */
    private static WeakReference<byte[]> mReadBuffer;

    /**
     * 不能实例化
     */
    private SignatureParser() {

    }

    /**
     * 获取apk签名
     *
     * @param mArchiveSourcePath apk文件
     * @return 签名，null表示解析签名失败
     */
    public static Signature[] collectCertificates(String mArchiveSourcePath) {
        Signature[] signatures = null;

        WeakReference<byte[]> readBufferRef;
        byte[] readBuffer = null;
        synchronized (mSync) {
            readBufferRef = mReadBuffer;
            if (readBufferRef != null) {
                mReadBuffer = null;
                readBuffer = readBufferRef.get();
            }
            if (readBuffer == null) {
                readBuffer = new byte[8192];
                readBufferRef = new WeakReference<byte[]>(readBuffer);
            }
        }

        try {
            JarFile jarFile = new JarFile(mArchiveSourcePath);

            Certificate[] certs = null;

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry je = entries.nextElement();
                if (je.isDirectory())
                    continue;

                final String name = je.getName();

                if (name.startsWith("META-INF/"))
                    continue;

                final Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);

                if (localCerts == null) {
                    if (DEBUG) {
                        Log.e(TAG, "Package " + mArchiveSourcePath + " has no certificates at entry " + je.getName()
                                + "; ignoring!");
                    }
                    jarFile.close();
                    return null;
                } else if (certs == null) {
                    certs = localCerts;
                } else {
                    // Ensure all certificates match.
                    for (int i = 0; i < certs.length; i++) {
                        boolean found = false;
                        for (int j = 0; j < localCerts.length; j++) {
                            if (certs[i] != null && certs[i].equals(localCerts[j])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found || certs.length != localCerts.length) {
                            jarFile.close();
                            return null;
                        }
                    }
                }
            }
            jarFile.close();

            synchronized (mSync) {
                mReadBuffer = readBufferRef;
            }

            if (certs != null && certs.length > 0) {
                final int N = certs.length;
                signatures = new Signature[certs.length];
                for (int i = 0; i < N; i++) {
                    signatures[i] = new Signature(certs[i].getEncoded());
                }
            } else {
                if (DEBUG) {
                    Log.e(TAG, "Package " + mArchiveSourcePath + " has no certificates; ignoring!");
                }
                return null;
            }
        } catch (CertificateEncodingException e) {
            if (DEBUG) {
                Log.w(TAG, "Exception reading " + mArchiveSourcePath, e);
            }
            return null;
        } catch (IOException e) {
            if (DEBUG) {
                Log.w(TAG, "Exception reading " + mArchiveSourcePath, e);
            }
            return null;
        } catch (RuntimeException e) {
            if (DEBUG) {
                Log.w(TAG, "Exception reading " + mArchiveSourcePath, e);
            }
            return null;
        }

        return signatures;
    }

    /**
     * 加载签名文件
     *
     * @param jarFile    jar文件
     * @param je         Jar Entry
     * @param readBuffer 读取Buffer
     * @return 签名文件
     */
    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            // We must read the stream for the JarEntry to retrieve its certificates.
            InputStream is = new BufferedInputStream(jarFile.getInputStream(je));
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {
                // not using
            }
            is.close();
            return je != null ? je.getCertificates() : null;
        } catch (IOException e) {
            if (DEBUG) {
                Log.w(TAG, "Exception reading " + je.getName() + " in " + jarFile.getName(), e);
            }
        } catch (RuntimeException e) {
            if (DEBUG) {
                Log.w(TAG, "Exception reading " + je.getName() + " in " + jarFile.getName(), e);
            }
        }
        return null;
    }

}


