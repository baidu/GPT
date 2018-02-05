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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.baidu.test.data.MySerialData;

/**
 * AnimationAlphaActivity
 *
 * @author liuhaitao
 * @since 2018-01-15
 */
public class AnimationAlphaActivity extends FragmentActivity {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "AnimationAlphaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.an_alpha);
        ImageView imgv = (ImageView) findViewById(R.id.img);
        Animation alphaAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha);
        imgv.startAnimation(alphaAnimation);
        Object obj = getIntent().getSerializableExtra("serialtest");
        if (obj != null) {
            if (DEBUG) {
                Log.d(TAG, "get Class loader:" + obj.getClass().getClassLoader() + "\n"
                        + "current Class loader:" + MySerialData.class.getClassLoader());
            }

            if (obj instanceof MySerialData) {
                MySerialData data = (MySerialData) obj;
                if (DEBUG) {
                    Log.d(TAG, "data : key=" + data.key + ", value=" + data.value);
                }
            }
        }

        if (DEBUG) {
            Log.d(TAG, "AnimationAlphaActivity onCreate task id " + getTaskId());
        }

    }
}



