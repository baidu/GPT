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
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

/**
 * TestTabActvity
 *
 * @author liuhaitao
 * @since 2014-05-16
 */
public class TestTabActvity extends TabActivity {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "TestTabActvity";

    /**
     * Tab的Indicator文本
     */
    private TabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_tab_activity);
        mTabHost = (TabHost) getTabHost();
        Intent intent1 = new Intent(this, AnimationExampleActivity.class);
        TabSpec tabSpec = mTabHost.newTabSpec(AnimationExampleActivity.class.toString()).setIndicator("Tab1")
                .setContent(intent1);
        mTabHost.addTab(tabSpec);
        Intent intent2 = new Intent(this, AnimationTranslateActivity.class);
        TabSpec tabSpec2 = mTabHost.newTabSpec(AnimationTranslateActivity.class.toString()).setIndicator("Tab2")
                .setContent(intent2);
        mTabHost.addTab(tabSpec2);

    }

    /**
     * dispatchActivityResult
     *
     * @param who         who
     * @param requestCode requestCode
     * @param resultCode  resultCode
     * @param data        data
     */
    void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data) {
        if (who != null) {
            Activity activity = getLocalActivityManager().getActivity(who);
            if (DEBUG) {
                Log.d(TAG, "void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data): who="
                        + who + "; requestCode=" + requestCode + "; resultCode=" + resultCode
                        + "; data=" + data + "; activity=" + activity);
            }

//            if (activity != null) {
//                activity.onActivityResult(requestCode, resultCode, data);
//                return;
//            }
        }
//        super.dispatchActivityResult(who, requestCode, resultCode, data);
    }
}
