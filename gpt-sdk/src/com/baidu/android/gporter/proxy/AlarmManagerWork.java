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
package com.baidu.android.gporter.proxy;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.app.IAlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.WorkSource;

import com.baidu.android.gporter.util.Constants;

/**
 * hook AlarmManager
 *
 * @author liuhaitao
 * @since 2017-05-14
 */

public class AlarmManagerWork extends InterfaceProxy {
    /**
     * 构造方法
     */
    public AlarmManagerWork() {
        super(Constants.IALARM_MANAGERR_CLASS);
    }

    /**
     * 系统原始的IAlarmManager
     */
    public IAlarmManager mTarget;

    /**
     * host Context
     */
    public Context mHostContext;

    public void set(String callingPackage, int type, long triggerAtTime, long windowLength,
                    long interval, int flags, PendingIntent operation, IAlarmListener listener,
                    String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) {
        mTarget.set(mHostContext.getPackageName(), type, triggerAtTime, windowLength,
                interval, flags, operation, listener, listenerTag, workSource, alarmClock);
    }
}
