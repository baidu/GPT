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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.Log;
import android.view.IWindow;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.Surface;
import android.view.WindowManager;

import com.baidu.android.gporter.util.Constants;

/**
 * WindowSessionWorker
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class WindowSessionWorker extends InterfaceProxy {

    /**
     * 构造方法
     */
    public WindowSessionWorker() {
        super(Constants.WINDOW_SESSION_CLASS);
    }

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = false & Constants.DEBUG;
    /**
     * TAG
     */
    private static final String TAG = "WindowSessionWorker";

    /**
     * 系统真正的IWindowSession aidl接口
     */
    public IWindowSession mTarget;

    /**
     * 宿主包名
     */
    private String mHostPackageName = null;
    /**
     * Host Context
     */
    private Context mHostContext;

    /**
     * 设置宿主包名
     *
     * @param name 包名
     */
    public void setPackageName(String name) {
        mHostPackageName = name;
    }

    /**
     * 设置Context
     *
     * @param ctx Context
     */
    public void setContext(Context ctx) {
        mHostContext = ctx;
    }

    public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility,
                            int displayId, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {

        if (DEBUG) {
            Log.d(TAG, "--- addToDisplay, replace host pkg!");
        }

        // 包名要替换成宿主的，不然AppOpsService校验uid时，会因为找不到插件包对应的uid，出现校验失败
        attrs.packageName = mHostPackageName;

        return mTarget.addToDisplay(window, seq, attrs, viewVisibility, displayId, outContentInsets, outStableInsets,
                outInputChannel);
    }

    public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility,
                            int displayId, Rect outContentInsets, InputChannel outInputChannel) {

        if (DEBUG) {
            Log.d(TAG, "--- addToDisplay, replace host pkg!");
        }

        // 包名要替换成宿主的，不然AppOpsService校验uid时，会因为找不到插件包对应的uid，出现校验失败
        attrs.packageName = mHostPackageName;

        return mTarget.addToDisplay(window, seq, attrs, viewVisibility, displayId, outContentInsets,
                outInputChannel);
    }

    public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility,
                            int displayId, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, InputChannel outInputChannel) {

        if (DEBUG) {
            Log.d(TAG, "--- addToDisplay, replace host pkg!");
        }

        // 包名要替换成宿主的，不然AppOpsService校验uid时，会因为找不到插件包对应的uid，出现校验失败
        attrs.packageName = mHostPackageName;

        return mTarget.addToDisplay(window, seq, attrs, viewVisibility, displayId, outContentInsets, outStableInsets,
                outOutsets, outInputChannel);
    }

    // 7.X
    public int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs, int requestedWidth,
                        int requestedHeight, int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets,
                        Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame,
                        Configuration outConfig, Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags,
                outFrame, outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets,
                outOutsets, outBackdropFrame, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//			throw e;
//		}
//		return res;
    }

    // 6.X
    public int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs, int requestedWidth,
                        int requestedHeight, int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets,
                        Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets,
                        Configuration outConfig, Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags, outFrame,
                outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outOutsets, outConfig,
                outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

    // 5.X
    public int relayout(IWindow window, int seq, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                        int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                        android.graphics.Rect outOverscanInsets, android.graphics.Rect outContentInsets,
                        android.graphics.Rect outVisibleInsets, android.graphics.Rect outStableInsets,
                        android.content.res.Configuration outConfig, android.view.Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags, outFrame,
                outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

    // 4.4, 4.3
    public int relayout(IWindow window, int seq, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                        int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                        android.graphics.Rect outOverscanInsets, android.graphics.Rect outContentInsets,
                        android.graphics.Rect outVisibleInsets, android.content.res.Configuration outConfig,
                        android.view.Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags, outFrame,
                outOverscanInsets, outContentInsets, outVisibleInsets, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return  res;
    }

    // 4.2, 4.1
    public int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                        int requestedWidth, int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                        android.graphics.Rect outContentInsets, android.graphics.Rect outVisibleInsets,
                        android.content.res.Configuration outConfig, android.view.Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags, outFrame,
                outContentInsets, outVisibleInsets, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

    // 4.0
    public int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                        int requestedWidth, int requestedHeight, int viewVisibility, boolean insetsPending,
                        android.graphics.Rect outFrame, android.graphics.Rect outContentInsets,
                        android.graphics.Rect outVisibleInsets, android.content.res.Configuration outConfig,
                        android.view.Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, insetsPending,
                outFrame, outContentInsets, outVisibleInsets, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

    // 2.3, 2.2
    public int relayout(android.view.IWindow window, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                        int requestedHeight, int viewVisibility, boolean insetsPending, android.graphics.Rect outFrame,
                        android.graphics.Rect outContentInsets, android.graphics.Rect outVisibleInsets,
                        android.content.res.Configuration outConfig, android.view.Surface outSurface) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, attrs, requestedWidth, requestedHeight, viewVisibility, insetsPending, outFrame,
                outContentInsets, outVisibleInsets, outConfig, outSurface);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

    // sumsung_s3 4.3
    public int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                        int requestedWidth, int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                        android.graphics.Rect outContentInsets, android.graphics.Rect outVisibleInsets, android.graphics.Rect rect,
                        android.content.res.Configuration outConfig, android.view.Surface outSurface,
                        android.graphics.RectF rectF) {
        if (DEBUG) {
            Log.d(TAG, "--- relayout!");
        }
//		int res = 0;
//		try {
        return mTarget.relayout(window, seq, attrs, requestedWidth, requestedHeight, viewVisibility, flags, outFrame,
                outContentInsets, outVisibleInsets, rect, outConfig, outSurface, rectF);
//		} catch (Exception e) {
//			StatisticsUtil.onCrash(mHostContext, "", Util.getCallStack(e), StatisticsConstants.TJ_78730012);
//		}
//		return res;
    }

}
