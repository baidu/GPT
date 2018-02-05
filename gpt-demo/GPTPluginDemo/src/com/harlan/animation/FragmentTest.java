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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * FragmentTest
 *
 * @author liuhaitao
 * @since 2014-03-20
 */
public class FragmentTest extends Fragment {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "FragmentTest";

    @Override
    public void onAttach(Activity activity) {

        if (DEBUG) {
            Log.d(TAG, "onAttach(Activity activity): activity=" + activity);
        }
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreate(Bundle savedInstanceState): savedInstanceState=" + savedInstanceState);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (DEBUG) {
            Log.d(TAG, "onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState): inflater=" + inflater
                    + "; container=" + container + "; savedInstanceState=" + savedInstanceState);
        }
        Bundle bundle = getArguments();
        int position = bundle.getInt("position");
        android.util.Log.e("CYK", "onCreateView = " + this);
        TextView text = new TextView(getActivity());
        text.setText("This is view : " + position);
        return text;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onActivityCreated(Bundle savedInstanceState): savedInstanceState=" + savedInstanceState);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        if (DEBUG) {
            Log.d(TAG, "onStart(): " + this);
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume(): " + this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause(): " + this);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) {
            Log.d(TAG, "onStop(): " + this);
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) {
            Log.d(TAG, "onDestroyView(): " + this);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "onDestroy(): " + this);
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if (DEBUG) {
            Log.d(TAG, "onDetach(): " + this);
        }
        super.onDetach();
    }

}

