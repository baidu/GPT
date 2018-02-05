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
package com.baidu.android.gporter.install;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.baidu.android.gporter.util.Constants;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 自定义的apk解析器，简单解析应用。
 *
 * @author liuhaitao
 * @since 2015-3-27
 */
public class ApkCompatLiteParser {

    /**
     * Log debug 开关
     */
    private static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    private static final String TAG = "ApkCompatLiteParser";

    /**
     * File name in an APK for the Android manifest.
     */
    private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    /**
     * com.android.internal.R$styleable类名
     */
    private static final String R_INTERNAL_STYLEABLE = "com.android.internal.R$styleable";

    /**
     * 从apk中解析出来的简单应用信息
     *
     * @author liuhaitao
     * @since 2015-3-30
     */
    public static final class ApkLiteInfo {

        /**
         * 构造方法
         */
        public ApkLiteInfo() {

        }

        /**
         * 应用名称
         */
        public String name = "";
        /**
         * 包名
         */
        public String packageName = null;
        /**
         * version name
         */
        public String versionName = null;
        /**
         * version code
         */
        public int versionCode = 0;
        /**
         * 最低sdk要求
         */
        public int minSdk = 0;
        /**
         * Constant corresponding to <code>auto</code> in the
         * {@link android.R.attr#installLocation} attribute.
         */
        public static final int INSTALL_LOCATION_AUTO = 0;

        /**
         * Constant corresponding to <code>internalOnly</code> in the
         * {@link android.R.attr#installLocation} attribute.
         */
        public static final int INSTALL_LOCATION_INTERNAL_ONLY = 1;

        /**
         * Constant corresponding to <code>preferExternal</code> in the
         * {@link android.R.attr#installLocation} attribute.
         */
        public static final int INSTALL_LOCATION_PREFER_EXTERNAL = 2;

        /**
         * The install location requested by the package. From the
         * {@link android.R.attr#installLocation} attribute, one of
         * {@link #INSTALL_LOCATION_AUTO}, {@link #INSTALL_LOCATION_INTERNAL_ONLY},
         * {@link #INSTALL_LOCATION_PREFER_EXTERNAL}
         */
        public int installLocation = INSTALL_LOCATION_INTERNAL_ONLY;


        @Override
        public String toString() {
            return name + "@" + packageName + "@" + versionCode + "@" + versionName + "@" + minSdk;
        }

    }

    /**
     * 简单解析apk
     *
     * @param packageFilePath apk路径
     * @return apk信息
     */
    public ApkLiteInfo parsePackageLite(String packageFilePath) {
        ApkLiteInfo relt = new ApkLiteInfo();

        AssetManager assmgr = null;
        final XmlResourceParser parser;
        final Resources res;
        try {
            assmgr = (AssetManager) AssetManager.class.newInstance();

            Method mtd = AssetManager.class.getDeclaredMethod("addAssetPath", new Class<?>[]{String.class});
            int cookie = (Integer) mtd.invoke(assmgr, new Object[]{packageFilePath});
            if (cookie == 0) {
                if (DEBUG) {
                    Log.e(TAG, "### Parse asset fail!");
                }
                return null;
            }

            final DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            res = new Resources(assmgr, metrics, null);
            parser = assmgr.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
        } catch (Exception e) {
            if (assmgr != null) {
                assmgr.close();
            }
            if (DEBUG) {
                e.printStackTrace();
            }
            return null;
        }

        final AttributeSet attrs = parser;
        final String[] errors = new String[1];

        try {
            relt = parsePackageLite(res, parser, attrs, 0, errors);
        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (XmlPullParserException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
            if (assmgr != null) {
                assmgr.close();
            }
        }

        return relt;
    }

    /**
     * 解析Manifest
     *
     * @param res      apk资源
     * @param parser   xml解析器
     * @param attrs    属性集合
     * @param flags    解析flag
     * @param outError 错误信息
     * @return 解析的apk信息
     * @throws IOException            IO异常
     * @throws XmlPullParserException xml解析异常
     */
    public ApkLiteInfo parsePackageLite(Resources res, XmlPullParser parser, AttributeSet attrs, int flags,
                                        String[] outError) throws IOException, XmlPullParserException {

        ApkLiteInfo relt = new ApkLiteInfo();

        int type;
        int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.START_TAG // SUPPRESS CHECKSTYLE
                && type != XmlPullParser.END_DOCUMENT) {
            ; // SUPPRESS CHECKSTYLE
        }

        // TODO 增加一些对manifest的校验

        if (type != XmlPullParser.START_TAG) {
            outError[0] = "No start tag found";
            return null;
        }
        if (!parser.getName().equals("manifest")) {
            outError[0] = "No <manifest> tag";
            return null;
        }
        String pkgName = attrs.getAttributeValue(null, "package");
        if (pkgName == null || pkgName.length() == 0) {
            outError[0] = "<manifest> does not specify package";
            return null;
        }
        String nameError = validateName(pkgName, true);
        if (nameError != null && !"android".equals(pkgName)) {
            outError[0] = "<manifest> specifies bad package name \"" + pkgName + "\": " + nameError;
            return null;
        }
        relt.packageName = pkgName;

        TypedArray sa = res.obtainAttributes(attrs, (int[]) getStaticField(R_INTERNAL_STYLEABLE, "AndroidManifest"));

        int versionCode = sa.getInteger((Integer) getStaticField(R_INTERNAL_STYLEABLE,
                "AndroidManifest_versionCode"), -1);
        if (versionCode < 0) {
            outError[0] = "<manifest> does not specify version code";
            return null;
        }
        relt.versionCode = versionCode;
        try {
            relt.versionName = sa.getString((Integer) getStaticField(R_INTERNAL_STYLEABLE,
                    "AndroidManifest_versionName"));
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (relt.versionName == null) {
            outError[0] = "<manifest> does not specify version name";
            return null;
        }
        relt.installLocation = ApkLiteInfo.INSTALL_LOCATION_INTERNAL_ONLY;
        try {
            relt.installLocation = sa.getInteger(
                    (Integer) getStaticField(R_INTERNAL_STYLEABLE, "AndroidManifest_installLocation"),
                    ApkLiteInfo.INSTALL_LOCATION_INTERNAL_ONLY);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (DEBUG) {
            Log.i(TAG, "installLocation:" + relt.installLocation);
        }
        sa.recycle();

        // 解析minSDK和应用名称
        int found = 0;
        final int targetCount = 2;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT // SUPPRESS CHECKSTYLE
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            if (parser.getName().equals("uses-sdk")) {
                TypedArray saUsesSdk = res.obtainAttributes(attrs,
                        (int[]) getStaticField(R_INTERNAL_STYLEABLE, "AndroidManifestUsesSdk"));
                TypedValue val = saUsesSdk.peekValue((Integer) getStaticField(R_INTERNAL_STYLEABLE,
                        "AndroidManifestUsesSdk_minSdkVersion"));
                if (val != null) {
                    if (val.type == TypedValue.TYPE_STRING && val.string != null) {
                        relt.minSdk = Integer.parseInt(val.string.toString());
                    } else {
                        // If it's not a string, it's an integer.
                        relt.minSdk = val.data;
                    }
                }

                saUsesSdk.recycle();
                found++;
            } else if (parser.getName().equals("application")) {

                // 解析label
                TypedArray saApp = res.obtainAttributes(attrs,
                        (int[]) getStaticField(R_INTERNAL_STYLEABLE, "AndroidManifestApplication"));
                TypedValue v = saApp.peekValue((Integer) getStaticField(R_INTERNAL_STYLEABLE,
                        "AndroidManifestApplication_label"));
                if (v != null) {
                    if (v.resourceId == 0) {
                        relt.name = v.coerceToString().toString();
                    } else if (v.resourceId > 0) {
                        relt.name = res.getText(v.resourceId).toString();
                    }
                }

                saApp.recycle();
                found++;
            }

            if (found >= targetCount) {
                break;
            }
        }

        return relt;
    }

    /**
     * 校验包名是否合法
     *
     * @param name              包名
     * @param requiresSeparator 是否需要包含分隔符
     * @return 返回错误信息，null表示没有错误
     */
    private static String validateName(String name, boolean requiresSeparator) {
        final int n = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < n; i++) {
            final char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
                continue;
            }
            if (!front) {
                if ((c >= '0' && c <= '9') || c == '_') {
                    continue;
                }
            }
            if (c == '.') {
                hasSep = true;
                front = true;
                continue;
            }
            return "bad character '" + c + "'";
        }
        return hasSep || !requiresSeparator ? null : "must have at least one '.' separator";
    }

    /**
     * 反射获取Java静态成员
     *
     * @param className 类名
     * @param filedName 成员名
     * @return 静态成员
     */
    private static Object getStaticField(String className, String filedName) {
        Object result = null;
        try {
            Class<?> clazz = Class.forName(className);
            Field f = clazz.getDeclaredField(filedName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
