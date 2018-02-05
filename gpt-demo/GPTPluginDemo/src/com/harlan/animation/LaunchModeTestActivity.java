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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * LaunchModeTestActivity
 *
 * @author liuhaitao
 * @since 2014-04-23
 */
public class LaunchModeTestActivity extends Activity {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "LaunchModeTestActivity";

    @Override
    public void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);

        Toast.makeText(getApplicationContext(), "onNewIntent(Intent paramIntent): " + paramIntent.toString(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);
        findViewById(R.id.test2).setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startActivity(new Intent(LaunchModeTestActivity.this, LaunchModeTestActivity.class));
            }
        });
        findViewById(R.id.home).setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startActivity(new Intent(LaunchModeTestActivity.this, AnimationExampleActivity.class));
            }
        });
        if (Constants.DEBUG) {
            Log.d(TAG, "onCreate(Bundle savedInstanceState): savedInstanceState" + savedInstanceState
                    + "; getWindow()= " + getWindow() + "; getWindow().getDecorView()= " + getWindow().getDecorView()
                    + "; CustomTextView.class.hashCode()= " + CustomTextView.class.hashCode()
                    + "; findViewById(R.id.custom).getClass().hashCode()= " + findViewById(R.id.custom).getClass().hashCode());
        }

        CustomTextView view = (CustomTextView) findViewById(R.id.custom);
        view.setText("我替换了自定义控件");

    }

}
