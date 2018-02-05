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

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.baidu.android.gporter.gpt.GPTIntent;
import com.baidu.android.gporter.proxy.activity.ActivityProxy;
import com.baidu.android.gporter.util.Constants;

/**
 * LocalActivityManagerProxy
 *
 * @author liuhaitao
 * @since 2018-01-08
 */
public class LocalActivityManagerProxy extends LocalActivityManager {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "LocalActivityManagerProxy";

    /**
     * TargetLocalActivityManager
     */
    LocalActivityManager mTargetLocalActivityManager;
    /**
     * TargetPackageName
     */
    private String mTargetPackageName;

    /**
     * 构造方法
     *
     * @param localActivityManager LocalActivityManager
     * @param packageName          包名
     */
    public LocalActivityManagerProxy(LocalActivityManager localActivityManager, String packageName) {
        super(null, false);
        mTargetLocalActivityManager = localActivityManager;
        mTargetPackageName = packageName;
    }

    /**
     * startActivity
     *
     * @param id     id
     * @param intent Intent
     * @return Window
     */
    public Window startActivity(String id, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "startActivity");
        }
//        ProxyEnvironment.getInstance(mTargetPackageName).remapStartActivityIntent(intent);
        GPTIntent newIntent = new GPTIntent(intent);
        newIntent.setClassName(mTargetPackageName, intent.getComponent().getClassName());
        return mTargetLocalActivityManager.startActivity(id, newIntent);
    }

    /**
     * destroyActivity
     *
     * @param id     id
     * @param finish true or false
     * @return Window
     */
    public Window destroyActivity(String id, boolean finish) {
        return mTargetLocalActivityManager.destroyActivity(id, finish);
    }

    /**
     * getCurrentActivity
     *
     * @return Activity
     */
    public Activity getCurrentActivity() {
        Activity activity = mTargetLocalActivityManager.getCurrentActivity();
        if (activity instanceof ActivityProxy) {
            return ((ActivityProxy) activity).getCurrentActivity();
        } else {
            return activity;
        }
    }

    /**
     * getCurrentId
     *
     * @return CurrentId
     */
    public String getCurrentId() {
        return mTargetLocalActivityManager.getCurrentId();
    }

    /**
     * getActivity
     *
     * @param id id
     * @return Activity
     */
    public Activity getActivity(String id) {
        Activity activity = mTargetLocalActivityManager.getActivity(id);
        if (activity instanceof ActivityProxy) {
            return ((ActivityProxy) activity).getCurrentActivity();
        } else {
            return activity;
        }
    }

    /**
     * dispatchCreate
     *
     * @param state Bundle
     */
    public void dispatchCreate(Bundle state) {
        mTargetLocalActivityManager.dispatchCreate(state);
    }

    /**
     * saveInstanceState
     *
     * @return Bundle
     */
    public Bundle saveInstanceState() {
        return mTargetLocalActivityManager.saveInstanceState();
    }

    /**
     * dispatchResume
     */
    public void dispatchResume() {
        mTargetLocalActivityManager.dispatchResume();
    }

    /**
     * dispatchPause
     *
     * @param finishing true or false
     */
    public void dispatchPause(boolean finishing) {
        mTargetLocalActivityManager.dispatchPause(finishing);
    }

    /**
     * dispatchStop
     */
    public void dispatchStop() {
        mTargetLocalActivityManager.dispatchStop();
    }

    /**
     * removeAllActivities
     */
    public void removeAllActivities() {
        mTargetLocalActivityManager.removeAllActivities();
    }

    /**
     * dispatchDestroy
     *
     * @param finishing true or false
     */
    public void dispatchDestroy(boolean finishing) {
        mTargetLocalActivityManager.dispatchDestroy(finishing);
    }

}
