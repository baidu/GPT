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
package com.baidu.android.gporter.stat;

import android.os.Parcel;
import android.os.Parcelable;

import com.baidu.android.gporter.util.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 插件启动过程中的时间记录
 *
 * @author liuhaitao
 * @since 2017-02-17
 */
public class PluginTimeLine implements Parcelable, Cloneable {
    /**
     * 包名
     */
    public String packageName = "";
    /**
     * 开始时间戳
     */
    public long startTimeStamp = 0;
    /**
     * 开始时间,ElapsedRealTime
     */
    public long startElapsedRealTime = 0;
    /**
     * initProxyEnvironment耗时
     */
    public long initProxyEnvironmentTime = 0;
    /**
     * 创建Application的耗时
     */
    public long createApplicationTime = 0;
    /**
     * 加载Application耗时
     */
    public long loadApplicationTime = 0;
    /**
     * 插件加载耗时
     */
    public long pluginLoadSucessTime = 0;
    /**
     * 插件运行在哪个进程 -1 主进程，0 GPT进程，1 Gpt_ext进程 TODO 多进程扩展管理时需注意
     */
    public int process = Constants.GPT_PROCESS_MAIN;

    /**
     * 构造方法
     */
    public PluginTimeLine() {

    }

    /**
     * 构造方法
     *
     * @param in in
     */
    public PluginTimeLine(Parcel in) {
        packageName = in.readString();
        startTimeStamp = in.readLong();
        startElapsedRealTime = in.readLong();
        initProxyEnvironmentTime = in.readLong();
        createApplicationTime = in.readLong();
        loadApplicationTime = in.readLong();
        pluginLoadSucessTime = in.readLong();
        process = in.readInt();
    }

    /**
     * Creator<PluginTimeLine>
     */
    public static final Creator<PluginTimeLine> CREATOR = new Creator<PluginTimeLine>() {
        @Override
        public PluginTimeLine createFromParcel(Parcel in) {
            return new PluginTimeLine(in);
        }

        @Override
        public PluginTimeLine[] newArray(int size) {
            return new PluginTimeLine[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeLong(startTimeStamp);
        dest.writeLong(startElapsedRealTime);
        dest.writeLong(initProxyEnvironmentTime);
        dest.writeLong(createApplicationTime);
        dest.writeLong(loadApplicationTime);
        dest.writeLong(pluginLoadSucessTime);
        dest.writeInt(process);
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultdate = new Date(startTimeStamp);
        String string = "packageName:" + packageName
                + ", startTimeStamp:" + sdf.format(resultdate)
                + ", startElapsedRealTime:" + startElapsedRealTime
                + ", process:" + process
                + ", initProxyEnvironmentTime:" + initProxyEnvironmentTime + "ms"
                + ", createApplicationTime:" + createApplicationTime + "ms"
                + ", loadApplicationTime:" + loadApplicationTime + "ms"
                + ", pluginLoadSucessTime:" + pluginLoadSucessTime + "ms";
        return string;
    }
}
