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
package com.baidu.android.pushservice.internal;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Notification;
import android.content.Context;

/**
 * BasicPushNotificationBuilder
 * 注:Push相关测试,其它新产品可不关注。
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public class BasicPushNotificationBuilder extends PushNotificationBuilder
{
  private void readObject(ObjectInputStream paramObjectInputStream)
  {
   
  }

  private void writeObject(ObjectOutputStream paramObjectOutputStream)
  {
    
  }

  public Notification construct(Context paramContext)
  {
    Notification localNotification = new Notification();
 
    return localNotification;
  }
}

