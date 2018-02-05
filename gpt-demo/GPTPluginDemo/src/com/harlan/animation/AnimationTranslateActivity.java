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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * AnimationTranslateActivity
 *
 * @author liuhaitao
 * @since 2018-01-15
 */
public class AnimationTranslateActivity extends FragmentActivity {
    /**
     * ImageView
     */
    ImageView imgv0, imgv1, imgv2, imgv3, imgv4, imgv5;
    /**
     * Animation
     */
    Animation alphaAnimation0, alphaAnimation1, alphaAnimation2, alphaAnimation3, alphaAnimation4, alphaAnimation5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.an_translate);
        imgv0 = (ImageView) findViewById(R.id.img0);
        alphaAnimation0 = AnimationUtils.loadAnimation(this, R.anim.translate0);
        imgv0.startAnimation(alphaAnimation0);

        imgv1 = (ImageView) findViewById(R.id.img1);
        alphaAnimation1 = AnimationUtils.loadAnimation(this, R.anim.translate1);
        imgv1.startAnimation(alphaAnimation1);

        imgv2 = (ImageView) findViewById(R.id.img2);
        alphaAnimation2 = AnimationUtils.loadAnimation(this, R.anim.translate2);
        imgv2.startAnimation(alphaAnimation2);

        imgv3 = (ImageView) findViewById(R.id.img3);
        alphaAnimation3 = AnimationUtils.loadAnimation(this, R.anim.translate3);
        imgv3.startAnimation(alphaAnimation3);

        imgv4 = (ImageView) findViewById(R.id.img4);
        alphaAnimation4 = AnimationUtils.loadAnimation(this, R.anim.translate4);
        imgv4.startAnimation(alphaAnimation4);

        imgv5 = (ImageView) findViewById(R.id.img5);
        alphaAnimation5 = AnimationUtils.loadAnimation(this, R.anim.translate5);
        imgv5.startAnimation(alphaAnimation5);
    }
}
