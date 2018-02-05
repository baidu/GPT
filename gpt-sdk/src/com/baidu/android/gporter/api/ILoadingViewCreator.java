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
package com.baidu.android.gporter.api;

import android.content.Context;
import android.view.View;

/**
 * 创建插件加载View的接口。插件加载过程中，会有个loading 界面。主程序可以定制此loading view。
 *
 * @author liuhaitao
 * @since 2014-5-20
 */
public interface ILoadingViewCreator {

    /**
     * 创建插件加载界面
     *
     * @param context Context
     * @return View view
     */
    View createLoadingView(Context context);

}
