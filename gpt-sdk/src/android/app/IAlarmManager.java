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
package android.app;

import android.os.WorkSource;

/**
 * Hook系统的对应接口类方法
 *
 * @author liuhaitao
 * @since 2017-05-14
 */
public interface IAlarmManager {
    void set(String callingPackage, int type, long triggerAtTime, long windowLength,
             long interval, int flags, PendingIntent operation, IAlarmListener listener,
             String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock);
}
