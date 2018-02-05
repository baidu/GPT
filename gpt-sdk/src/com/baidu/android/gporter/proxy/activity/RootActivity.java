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
package com.baidu.android.gporter.proxy.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.api.ILoadingViewCreator;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.ITargetLoadListenner;

/**
 * RootActivity
 *
 * @author liuhaitao
 * @since 2014-4-29
 */
public class RootActivity extends Activity {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "RootActivity";

    /**
     * loading的背景
     */
    private LinearLayout mRoot;

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "--- RootActivity onNewIntent");
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        if (DEBUG) {
            Log.d(TAG, "--- RootActivity onCreate");
        }

        overridePendingTransition(0, 0);
        super.onCreate(bundle);

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityCreated(this, bundle);
        }

        GPTComponentInfo info = GPTComponentInfo.parseFromIntent(getIntent());
        String packageName = info.packageName;

        ILoadingViewCreator creator = ProxyEnvironment.getLoadingViewCreator(packageName);

        mRoot = new LinearLayout(this);
        mRoot.setGravity(android.view.Gravity.CENTER);
        if (creator != null) {

            // 创建自定义loading
            mRoot.addView(creator.createLoadingView(getApplicationContext()));
            mRoot.setBackgroundResource(android.R.color.transparent);
        } else {

            // 创建默认loading
            ProgressBar bar = new ProgressBar(this);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            barParams.gravity = android.view.Gravity.CENTER;
            mRoot.addView(bar, barParams);
        }
        setContentView(mRoot);

        // 启动插件主界面或指定的界面
        Intent intent = new Intent(getIntent());
        String targetActivity = info.className;
        if (targetActivity == null) { // 防止空指针异常
            targetActivity = "";
        }
        intent.setComponent(new ComponentName(packageName, targetActivity));
        intent.removeCategory(info.toString());

        ProxyEnvironment.initTargetAndLaunchIntent(getApplicationContext(), intent, new ITargetLoadListenner() {

            @Override
            public void onLoadFinished(String packageName, boolean unionProcess) {
                if (DEBUG) {
                    Log.d(TAG, "--- RootActivity finish when load finished!");
                }

                finish();
            }

            @Override
            public void onLoadFailed(String packageName, String failReason) {
                if (DEBUG) {
                    Log.d(TAG, "--- RootActivity finish when load failed!");
                }

                finish();
            }
        }, Constants.GPT_PROCESS_DEFAULT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ProxyEnvironment.clearLoadingIntent(getIntent().getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME));

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityDestroyed(this);
        }
    }

    @Override
    public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
        if (paramKeyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(paramInt, paramKeyEvent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityPaused(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityResumed(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (ActivityProxy.sActivityLifecycleCallbacks != null) {
            ActivityProxy.sActivityLifecycleCallbacks.onActivityStopped(this);
        }
    }
}
