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
package com.harlan.animation;

import android.app.Application;
import android.util.Log;

/**
 * MyApplication
 *
 * @author liuhaitao
 * @since 2014-11-27
 */
public class MyApplication extends Application {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Object base = JavaCalls.getField(this, "mBase");

        if (DEBUG) {
            Log.d(TAG, "onCreate(): this=" + this + "; Object base = JavaCalls.getField(this, \"mBase\"): mBase=" + base);
        }

        testBoolean();

    }

    public void testBoolean() {
        Log.i(TAG, "getResources()" + getResources());
        boolean b = getResources().getBoolean(R.bool.test_boolean);
        if (DEBUG) {
            Log.d(TAG, "testBoolean(): this=" + this + "; getResources()=" + getResources()
                    + "; boolean b = getResources().getBoolean(R.bool.test_boolean): b=" + b);
        }

    }

}
