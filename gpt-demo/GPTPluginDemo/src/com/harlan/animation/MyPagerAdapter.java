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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

/**
 * MyPagerAdapter
 *
 * @author liuhaitao
 * @since 2014-03-20
 */
public class MyPagerAdapter extends FragmentPagerAdapter {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "MyPagerAdapter";

    public MyPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int arg0) {
        Fragment fragment = new FragmentTest();
        Bundle bundle = new Bundle();
        bundle.putInt("position", arg0);
        fragment.setArguments(bundle);
        if (DEBUG) {
            Log.d(TAG, "public Fragment getItem(int arg0): arg0=" + arg0 + "; fragment=" + fragment + "; bundle=" + bundle);
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 1;
    }

}