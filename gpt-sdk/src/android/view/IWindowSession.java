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
package android.view;

import android.content.res.Configuration;
import android.graphics.Rect;

/**
 * Hook系统的对应接口类方法
 * 系统接口打桩，用于拦截系统方法
 *
 * @author liuhaitao
 * @since 2015-10-13
 */
public interface IWindowSession {

    /**
     * 5.0系统方法
     */
    int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
                     int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets,
                     InputChannel outInputChannel);

    int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
                     int viewVisibility, int displayId, Rect outContentInsets,
                     InputChannel outInputChannel);

    /**
     * 6.0 系统方法
     */
    int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs,
                     int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets,
                     Rect outOutsets, InputChannel outInputChannel);

    /**
     * 7.X
     */
    int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight,
                 int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
                 Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Rect outBackdropFrame,
                 Configuration outConfig, Surface outSurface);

    /**
     * 6.X
     */
    int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight,
                 int viewVisibility, int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets,
                 Rect outVisibleInsets, Rect outStableInsets, Rect outOutsets, Configuration outConfig, Surface outSurface);

    /**
     * 5.X
     */
    int relayout(IWindow window, int seq, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                 int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                 android.graphics.Rect outOverscanInsets, android.graphics.Rect outContentInsets,
                 android.graphics.Rect outVisibleInsets, android.graphics.Rect outStableInsets,
                 android.content.res.Configuration outConfig, android.view.Surface outSurface);

    /**
     * 4.4, 4.3
     */
    int relayout(IWindow window, int seq, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                 int requestedHeight, int viewVisibility, int flags, android.graphics.Rect outFrame,
                 android.graphics.Rect outOverscanInsets, android.graphics.Rect outContentInsets,
                 android.graphics.Rect outVisibleInsets, android.content.res.Configuration outConfig,
                 android.view.Surface outSurface);

    /**
     * 4.2, 4.1
     */
    int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                 int requestedWidth, int requestedHeight, int viewVisibility, int flags,
                 android.graphics.Rect outFrame,
                 android.graphics.Rect outContentInsets,
                 android.graphics.Rect outVisibleInsets,
                 android.content.res.Configuration outConfig, android.view.Surface outSurface);

    /**
     * 4.0
     */
    int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                 int requestedWidth, int requestedHeight, int viewVisibility, boolean insetsPending,
                 android.graphics.Rect outFrame, android.graphics.Rect outContentInsets,
                 android.graphics.Rect outVisibleInsets, android.content.res.Configuration outConfig,
                 android.view.Surface outSurface);

    /**
     * 2.3, 2.2
     */
    int relayout(android.view.IWindow window, android.view.WindowManager.LayoutParams attrs, int requestedWidth,
                 int requestedHeight, int viewVisibility, boolean insetsPending, android.graphics.Rect outFrame,
                 android.graphics.Rect outContentInsets, android.graphics.Rect outVisibleInsets,
                 android.content.res.Configuration outConfig, android.view.Surface outSurface);

    /**
     * sumsung_s3 4.3
     */
    int relayout(android.view.IWindow window, int seq, android.view.WindowManager.LayoutParams attrs,
                 int requestedWidth, int requestedHeight, int viewVisibility, int flags,
                 android.graphics.Rect outFrame,
                 android.graphics.Rect outContentInsets,
                 android.graphics.Rect outVisibleInsets,
                 android.graphics.Rect rect,
                 android.content.res.Configuration outConfig, android.view.Surface outSurface, android.graphics.RectF rectF);

}






