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


import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuhaitao
 * @since 2015-11-09
 */
public class ActivityManagerTest extends ListActivity {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ActivityManagerTest";


    /**
     * Id枚举
     */
    private enum Id {
        Broadcast, //registerReceiver , broadcastIntent
        PendingIntent, //getIntentSender
        Start_Stop_Service, //startService stopService
        Bind_Unbind_Service, // bindService , unbindService
        Provider, // getContentProvider
        CreateShortcut, // broadcastIntent
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use an existing ListAdapter that will map an array of strings to TextViews
        setListAdapter(new SimpleAdapter(this, getData(),
                android.R.layout.simple_list_item_1, new String[]{"title"},
                new int[]{android.R.id.text1}));
    }

    /**
     * getData
     *
     * @return List<Map<String, Object>>类型的数据
     */
    protected List<Map<String, Object>> getData() {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        Id[] ids = Id.values();

        for (Id id : ids) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("title", id.name());
            item.put("id", id);

            data.add(item);
        }

        return data;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, Object> item = (Map<String, Object>) l.getItemAtPosition(position);
        Id itemid = (Id) item.get("id");

        switch (itemid) {
            case Broadcast:
                testBroadcast();
                break;
            case PendingIntent:
                testGetIntentSender();
                break;
            case Start_Stop_Service:
                testService();
                break;
            case Bind_Unbind_Service:
                testBindService();
                break;
            case Provider:
                testProvider();
                break;

            case CreateShortcut:
                createShortCut();
                break;
        }
    }

    /**
     * toast
     *
     * @param content content
     */
    private void toast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    /**
     * 测试Broadcast
     */
    private void testBroadcast() {
        String action = "com.baidu.gpt.test.action.broadcast";

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);

        BroadcastReceiver receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (DEBUG) {
                    Log.d(TAG, "testBroadcast(): onReceive(Context context, Intent intent): context="
                            + context.toString() + "; intent=" + intent.toString());
                }
                toast("testBroadcast(): onReceive(Context context, Intent intent): context="
                        + context.toString() + "; intent=" + intent.toString());

                unregisterReceiver(this);
            }
        };

        registerReceiver(receiver, intentFilter);

        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * testGetIntentSender
     */
    private void testGetIntentSender() {
        Intent intent = new Intent();
        intent.setClass(this, AnimationAlphaActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 100, intent, 0);

        try {
            pi.send();
        } catch (CanceledException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * start
     */
    boolean start = true;

    /**
     * testService
     */
    private void testService() {
        // 测试前需要吧 bindservice 去掉。要不然 ondestroy 不会被回调。

        Intent intent = new Intent();
        intent.setClass(this, MyService.class);

        if (start) {
            ComponentName cn = getApplicationContext().startService(intent);
            if (cn == null) {
                toast("testService(): service not started");
            } else {
                toast("testService(): service started");
            }
        } else {
            boolean result = getApplicationContext().stopService(intent);
            if (result) {
                toast("testService(): service stoped");
            } else {
                toast("testService(): service stop failed !!!");
            }
        }

        start = !start;
    }

    /**
     * ServiceConnection
     */
    private ServiceConnection mConn = null;

    /**
     * 测试BindService
     */
    private void testBindService() {
        Intent sintent = new Intent(this, MyService.class);
        if (mConn == null) {
            mConn = new ServiceConnection() {

                public void onServiceDisconnected(ComponentName name) {
                    if (DEBUG) {
                        Log.d(TAG, "testBindService(): onServiceDisconnected(ComponentName name): name=" + name);
                    }
                }

                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (DEBUG) {
                        Log.d(TAG, "testBindService(): onServiceDisconnected(ComponentName name): name=" + name);
                    }

                    IMyAidl inter = IMyAidl.Stub.asInterface(service);
                    try {
                        inter.test();
                    } catch (RemoteException e) {
                        if (DEBUG) {
                            e.printStackTrace();
                        }
                    }

                }
            };
            boolean result = bindService(sintent, mConn, Context.BIND_AUTO_CREATE);
            if (result) {
                toast("testBindService(): onServiceDisconnected(ComponentName name): " + "bind service success");
            } else {
                toast("testBindService(): onServiceDisconnected(ComponentName name): " + "bind service failed!!!");
            }
        } else {
            unbindService(mConn);
            mConn = null;
        }
    }

    /**
     * 测试Provider
     */
    private void testProvider() {
        PackageManager pm = getPackageManager();

        try {
            String packageName = pm.getPackageInfo(getPackageName(), 0).packageName;
            if (DEBUG) {
                Log.d(TAG, "testProvider(): packageName = pm.getPackageInfo(getPackageName(), 0).packageName: packageName=" + packageName);
            }

        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }

        }


        ContentResolver cr = getContentResolver();
        Uri uri = Uri.parse("content://" + MyProvider.AUTH);
        String[] projection = {"name", "age"};

        Cursor c = cr.query(uri, projection, null, null, null);

        boolean success = false;
        while (c.moveToNext()) {
            int cc = c.getColumnCount();
            for (int i = 0; i < cc; i++) {
                if (DEBUG) {
                    Log.d(TAG, "testProvider(): c.getString(i)=" + c.getString(i) + "\n");
                }
            }
            success = true;
        }

        c.close();

        if (success) {
            toast("testProvider(): get provider success");
        } else {
            toast("testProvider(): get provider failed");
        }
    }

    /**
     * createShortCut
     */
    public void createShortCut() {
        //创建快捷方式的Intent                     
        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        //不允许重复创建                     
        shortcutintent.putExtra("duplicate", false);
        //需要现实的名称                     
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        //快捷图片                    
        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        //点击快捷图片，运行的程序主入口                     
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(getApplicationContext(), AnimationAlphaActivity.class));
        //发送广播
        sendBroadcast(shortcutintent);
    }
}

