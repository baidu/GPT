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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;

import com.baidu.android.gporter.stat.ExceptionConstants;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;
import com.baidu.android.gporter.util.Util;

/**
 * WindowCallbackWorker
 *
 * @author liuhaitao
 * @since 2015-10-28
 */
public class WindowCallbackWorker implements Window.Callback {

    /**
     * 系统原始的Window.Callback
     */
    public Window.Callback mTarget = null;
    /**
     * Activity
     */
    public Activity mActivity = null;

    /**
     * 构造方法
     */
    public WindowCallbackWorker() {

    }

    /**
     * initActionBar
     */
    private void initActionBar() {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;

        if (SDK_INT >= 11) {
            ActionBar actionBar = mActivity.getParent().getActionBar();
            JavaCalls.setField(mActivity, "mActionBar", actionBar);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        try {
            return mTarget.dispatchKeyEvent(event);
        } catch (IllegalStateException e) {
            // 4.x系统bug
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            // at android.app.FragmentManagerImpl.checkStateLoss(FragmentManager.java:1280)
            // at android.app.FragmentManagerImpl.popBackStackImmediate(FragmentManager.java:451)
            // at android.app.Activity.onBackPressed(Activity.java:2153)
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
            String pkgName = "";
            if (mActivity != null) {
                pkgName = mActivity.getPackageName();
            }
            ReportManger.getInstance().onException(mActivity, pkgName, Util.getCallStack(e),
                    ExceptionConstants.TJ_78730001);
            return false;
        }
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mTarget.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mTarget.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return mTarget.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return mTarget.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return mTarget.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        return mTarget.onCreatePanelView(featureId);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return mTarget.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return mTarget.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return mTarget.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return mTarget.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onWindowAttributesChanged(LayoutParams attrs) {
        mTarget.onWindowAttributesChanged(attrs);
    }

    @Override
    public void onContentChanged() {
        mTarget.onContentChanged();

        initActionBar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mTarget.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        mTarget.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        mTarget.onDetachedFromWindow();
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        initActionBar();

        mTarget.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public ActionMode onWindowStartingActionMode(Callback callback) {
        return mTarget.onWindowStartingActionMode(callback);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public ActionMode onWindowStartingActionMode(Callback callback, int type) {
        return mTarget.onWindowStartingActionMode(callback, type);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActionModeStarted(ActionMode mode) {
        mTarget.onActionModeStarted(mode);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onActionModeFinished(ActionMode mode) {
        mTarget.onActionModeFinished(mode);
    }

}
