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
package com.baidu.gpt.hostdemo;

import android.app.Application;

//import com.baidu.android.gporter.stat.DefaultInstallReport;
//import com.baidu.android.gporter.stat.DefaultLoadReport;
//import com.baidu.android.gporter.stat.DefaultReportImpl;
//import com.baidu.android.gporter.stat.ReportManger;

/**
 * HostDemo的Application
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public class HostApplication extends Application {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & MainActivity.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "HostApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 设置GPT统计上报具体逻辑实现类,不影响GPT插件框架的主体功能
        // TODO 不同产品接入使用时可直接注掉、或者参照下面方法实现并设置GPT对应统计数据接口自定义处理策略类即可。
//         ReportManger.getInstance().setInstallReport(new DefaultInstallReport());
//         ReportManger.getInstance().setLoadReprot(new DefaultLoadReport());
//         ReportManger.getInstance().setPluginReport(new DefaultReportImpl());
//
//         ReportManger.getInstance().setWritePluginTimeLineToFile(DEBUG);

    }
}
