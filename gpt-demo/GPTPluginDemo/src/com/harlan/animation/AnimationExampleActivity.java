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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.android.gporter.hostapi.HostUtil;
import com.baidu.android.gporter.rmi.Naming;
import com.baidu.gpt.hostdemo.lib.IHostLib;
import com.baidu.test.data.MySerialData;
import com.example.hellojni.HelloJni;

import java.io.File;
import java.util.List;

import static com.harlan.animation.R.layout.test;

/**
 * AnimationExampleActivity
 *
 * @author liuhaitao
 * @since 2018-01-15
 */
public class AnimationExampleActivity extends FragmentActivity implements OnClickListener {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = AnimationExampleActivity.class.getSimpleName();

    @Override
    public void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        Toast.makeText(getApplicationContext(), "home onNewIntent", Toast.LENGTH_SHORT).show();
    }

    private ServiceConnection mConn = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setUp();

        if (DEBUG) {
            Log.d(TAG, "onCreate(): begin encode");
        }

        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mReceiver, filter);

        String str = new HelloJni().stringFromJNI();
        if (DEBUG) {
            Log.d(TAG, "onCreate(): encoded: " + new String(str));
        }

        Intent sintent = new Intent(this, MyService.class);
        mConn = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
                if (DEBUG) {
                    Log.d(TAG, "ServiceConnection(): onServiceDisconnected(ComponentName name): name=" + name);
                }
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                if (DEBUG) {
                    Log.d(TAG, "ServiceConnection(): onServiceConnected(ComponentName name): name=" + name);
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
        bindService(sintent, mConn, Context.BIND_AUTO_CREATE);

    }

    /**
     * mReceiver
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Log.d(TAG, "mReceiver: onReceive(Context context, Intent intent): context=" + context + "; intent=" + intent.toString());
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            if (DEBUG) {
                Log.d(TAG, "unregisterReceiver");
            }
            mReceiver = null;
        }

        // Intent sintent = new Intent(this, MyService.class);
        // stopService(sintent);

        if (mConn != null) {
            unbindService(mConn);
        }

    }

    /**
     * 设置点击
     */
    private void setUp() {
        findViewById(R.id.btn_alpha).setOnClickListener(this);
        findViewById(R.id.btn_scale).setOnClickListener(this);
        findViewById(R.id.btn_translate).setOnClickListener(this);
        findViewById(R.id.btn_rotate).setOnClickListener(this);
        findViewById(R.id.btn_fragment).setOnClickListener(this);
        findViewById(R.id.btn_mode).setOnClickListener(this);
        findViewById(R.id.btn_dialog_activity).setOnClickListener(this);
        findViewById(R.id.btn_remote_call).setOnClickListener(this);
        findViewById(R.id.btn_notification).setOnClickListener(this);
        findViewById(R.id.btn_taskroot).setOnClickListener(this);
        findViewById(R.id.btn_wifi).setOnClickListener(this);
        findViewById(R.id.btn_wm).setOnClickListener(this);
        findViewById(R.id.btn_datadir).setOnClickListener(this);
        findViewById(R.id.btn_pkgmgr).setOnClickListener(this);
        findViewById(R.id.btn_activitymgr).setOnClickListener(this);
        findViewById(R.id.btn_alarm_send_intent).setOnClickListener(this);
        findViewById(R.id.btn_notification_remote_view).setOnClickListener(this);
        findViewById(R.id.btn_tab_activity).setOnClickListener(this);
        findViewById(R.id.btn_test_receiver).setOnClickListener(this);
        findViewById(R.id.btn_test_popup_window).setOnClickListener(this);
    }

    /**
     * 点击响应处理
     *
     * @param view View
     */
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.btn_alpha:
                intent = new Intent(AnimationExampleActivity.this, AnimationAlphaActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_scale:
                intent = new Intent(AnimationExampleActivity.this, AnimationScaleActivity.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.btn_translate:
                intent = new Intent(AnimationExampleActivity.this, AnimationTranslateActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_rotate:
                intent = new Intent(AnimationExampleActivity.this, AnimationRotateActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_fragment:
                intent = new Intent(AnimationExampleActivity.this, FragmentTestActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_mode:
                intent = new Intent(AnimationExampleActivity.this, LaunchModeTestActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_dialog_activity:
                intent = new Intent(AnimationExampleActivity.this, TestDialogActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_remote_call:
                test();
                break;
            case R.id.btn_notification:
                testNotification();
                break;
            case R.id.btn_taskroot:
                boolean isroot = isTaskRoot();
                System.out.println("xxx isroot " + isroot);
                break;
            case R.id.btn_wm:
                testWindowManager();
                break;
            case R.id.btn_wifi:
//            Toast.makeText(getApplicationContext(), "Context是插件的Toast", Toast.LENGTH_SHORT).show();
//            Activity act = AnimationExampleActivity.this.getParent();
//            if (act == null) {
//                act = AnimationExampleActivity.this;
//            }
//            Toast.makeText(act, "Context是宿主的Toast", Toast.LENGTH_SHORT).show();
//        	    getSystemService(PackageManager)
                PackageManager pkgManager = getPackageManager();
                int acessLocation = pkgManager.checkPermission("android.permission.ACCESS_FINE_LOCATION", getPackageName());
                if (DEBUG) {
                    Log.d(TAG, "onClick(View view): acessLocation:" + acessLocation);
                }
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
                break;
            case R.id.btn_datadir:
                testDataDir();
                break;
            case R.id.btn_pkgmgr:
                testPm();
                break;
            case R.id.btn_activitymgr:
                intent = new Intent();
                intent.setClass(this, ActivityManagerTest.class);
                startActivity(intent);
                break;
            case R.id.btn_alarm_send_intent:
                testAlarmSendIntent();
                break;
            case R.id.btn_notification_remote_view:
                testNotificationRemoteView();
                break;
            case R.id.btn_tab_activity:
                intent = new Intent(this, TestTabActvity.class);
                startActivity(intent);
                break;
            case R.id.btn_test_receiver:
                testReceiver();
//        	View view = findViewById(R.id.btn_test_receiver);
//        	view.requestFitSystemWindows();
//        	Choreographer.getInstance().postFrameCallback(new FrameCallback() {
//				
//				@Override
//				public void doFrame(long frameTimeNanos) {
//					Log.i(TAG, "doFrame",new Exception("doFrame"));
//				}
//			});
                break;
            case R.id.btn_test_popup_window:
                testPopupWindows(view);
                break;
            case R.id.btn_test_autofill:
                testAutoFillOnEditText();
                break;
            default:
                break;
        }

    }

    /**
     * 测试AutoFillManager的hook
     */
    private void testAutoFillOnEditText() {
        Intent intent = new Intent(this, AutoFillTestActivity.class);
        this.startActivity(intent);
    }

    /**
     * 测试Receiver
     */
    private void testReceiver() {
        IntentFilter filter = new IntentFilter(getPackageName() + ".test");
        TestReceiver testReceiver = new TestReceiver();
        registerReceiver(testReceiver, filter);
        Intent intent = new Intent(getPackageName() + ".test");
        sendBroadcast(intent);
//		unregisterReceiver(testReceiver);

        Intent intent2 = new Intent("com.baidu.host.demo.myreceiver");
        sendBroadcast(intent2);
    }

    /**
     * 测试WindowManager
     */
    private void testWindowManager() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        TextView tx = new TextView(getApplicationContext());
        View myView = getLayoutInflater().inflate(test, null);
        tx.setText(R.string.app_name);
        WindowManager.LayoutParams param = new WindowManager.LayoutParams();
        param.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        param.height = WindowManager.LayoutParams.WRAP_CONTENT;
        param.width = WindowManager.LayoutParams.MATCH_PARENT;
        param.x = 0;
        param.y = 0;
        param.packageName = getPackageName();
        wm.addView(myView, param);
    }

    /**
     * 测试
     */
    public void test() {

        IBinder binder = Naming.lookupHost("com.baidu.gpt.hostdemo.lib.HostLibImpl");

        IHostLib hostapi = IHostLib.Stub.asInterface(binder);

        String result = null;
        try {
            if (hostapi != null) {
                result = hostapi.test("hello");
            }
            if (DEBUG) {
                Log.d(TAG, "test(): result=" + result);
            }
            Toast.makeText(this, "result from host : " + result, Toast.LENGTH_LONG).show();
        } catch (RemoteException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 测试Notification
     */
    private void testNotification() {
        //Toast.makeText(this, R.string.app_name, Toast.LENGTH_LONG).show();

        //Intent sintent = new Intent(this, MyService.class);
        //startService(sintent);

//        Intent sintent = new Intent(this, MyService.class);
//        mConn = new ServiceConnection() {
//
//            public void onServiceDisconnected(ComponentName name) {
//                if (DEBUG) {
//                    Log.d(TAG, "testNotification(): onServiceDisconnected(ComponentName name): name=" + name);
//                }
//
//            }
//
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                if (DEBUG) {
//                    Log.d(TAG, "testNotification(): onServiceConnected(ComponentName name): name=" + name);
//                }
//            }
//        };
//        bindService(sintent, mConn, Context.BIND_AUTO_CREATE);
//
//        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        if (DEBUG) {
//            Log.d(TAG, "LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE): li=" + li.toString());
//        }
//
//        li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        if (DEBUG) {
//            Log.d(TAG, "li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE): li=" + li.toString());
//        }
//
////        createShortCut();
//
//        testNotificationImpl();

        // TODO 插件可以和宿主通过基于共同接口的Jar包引入,来通过宿主进行通知等特殊复杂功能实现,具体取决于不同产品需求,所以开源时注掉。
//        MARTImplsFactory.createNotificationImpl().sendNotification(
//                "com.harlan.animation", 6, 0, "test title", "test messgae", "test ticker",
//                new Intent(getApplicationContext(), AnimationAlphaActivity.class));
    }

    /**
     * 测试NotificationImpl
     */
    private void testNotificationImpl() {
        if (DEBUG) {
            Log.d(TAG, "testNotificationImpl()");
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification

        // choose the ticker text
        String tickerText = "this is ticker Text  hahahahahha  ticker ticker ticker.";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, tickerText,
                System.currentTimeMillis());

        Intent intent = new Intent();
        intent.setClass(this, AnimationAlphaActivity.class);
        MySerialData data = new MySerialData();
        data.key = "key";
        data.value = "value";
        intent.putExtra("serialtest", data);
        PendingIntent pintent = PendingIntent.getActivity(this, 10, intent, 0);

        // Set the info for the views that show in the notification panel.
//        notification.setLatestEventInfo(this, "Event title title",
//                tickerText, pintent);


        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.drawable.ic_launcher, notification);
        if (DEBUG) {
            Log.d(TAG, "nm.notify(R.drawable.ic_launcher, notification): notification=" + notification);
        }
    }

    /**
     * 测试DataDir
     */
    private void testDataDir() {
        File file = getExternalCacheDir();
        if (DEBUG) {
            Log.d(TAG, "testDataDir(): File file = getExternalCacheDir()=" + file);
        }

        file = getExternalFilesDir("picture");
        if (DEBUG) {
            Log.d(TAG, "testDataDir(): file = getExternalFilesDir(\"picture\")=" + file);
        }

        file = getApplicationContext().getExternalCacheDir();
        if (DEBUG) {
            Log.d(TAG, "testDataDir(): file = getApplicationContext().getExternalCacheDir()=" + file);
        }

        file = getApplicationContext().getExternalFilesDir("picture");
        if (DEBUG) {
            Log.d(TAG, "testDataDir(): file = getApplicationContext().getExternalFilesDir(\"picture\")=" + file);
        }

        StorageManager manager = (StorageManager) getSystemService(
                Context.STORAGE_SERVICE);
        Object[] result = null;
        Object o = JavaCalls.invokeMethod(manager, "getVolumeList", null, null);
        if (o != null) {
            result = (Object[]) o;
        }
        JavaCalls.invokeMethod(manager, "maybeTranslateEmulatedPathToInternal", new Class[]{java.lang.String.class},
                new Object[]{"/sdcard/baidu_plugin_test/"});
        if (DEBUG) {
            Log.d(TAG, "testDataDir(): result=" + result);
        }

    }


    /**
     * 测试Pm
     */
    private void testPm() {
        String pkgName = getPackageName();
        if (DEBUG) {
            Log.d(TAG, "testPm(): String pkgName = getPackageName()=" + pkgName);
        }

        // getPackageInfo
        PackageManager pm = getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(pkgName, 0);
            if (DEBUG) {
                Log.d(TAG, "testPm(): pi = pm.getPackageInfo(pkgName, 0)=" + pi);
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (pi == null || !pi.packageName.equals(pkgName)) {
            throw new RuntimeException("getPackageInfo failed");
        }

        // getApplicationInfo
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo(pkgName, 0);
            if (DEBUG) {
                Log.d(TAG, "testPm(): ai = pm.getApplicationInfo(pkgName, 0)=" + ai);
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (ai == null || !ai.packageName.equals(pkgName)) {
            throw new RuntimeException("getPackageInfo failed");
        }

        // getActivityInfo
        ActivityInfo activityInfo = null;
        try {
            activityInfo = pm.getActivityInfo(new ComponentName(this, AnimationAlphaActivity.class), 0);
            if (DEBUG) {
                Log.d(TAG, "testPm(): activityInfo = pm.getActivityInfo(new ComponentName(this, AnimationAlphaActivity.class), 0)=" + activityInfo);
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        if (activityInfo == null || !activityInfo.name.equals(AnimationAlphaActivity.class.getCanonicalName())) {
            throw new RuntimeException("getActivityInfo failed");
        }


        // getReceiverInfo
        ActivityInfo receiverInfo = null;
        try {
            receiverInfo = pm.getReceiverInfo(new ComponentName(this, MyReceiver.class), 0);
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (receiverInfo == null || !receiverInfo.name.equals(MyReceiver.class.getCanonicalName())) {
            throw new RuntimeException("getReceiverInfo failed");
        }

        // getServiceInfo
        ServiceInfo si = null;
        try {
            si = pm.getServiceInfo(new ComponentName(this, MyService.class), 0);
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (si == null || !si.name.equals(MyService.class.getCanonicalName())) {
            throw new RuntimeException("getServiceInfo failed");
        }

        // getProviderInfo
        ProviderInfo providerInfo = null;
        try {
            providerInfo = pm.getProviderInfo(new ComponentName(this, MyProvider.class), 0);
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        if (providerInfo == null || !providerInfo.name.equals(MyProvider.class.getCanonicalName())) {
            throw new RuntimeException("getProviderInfo failed");
        }


        // queryIntentActivities
        Intent intent = new Intent();
        intent.setComponent((new ComponentName(this, AnimationAlphaActivity.class)));
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        if (infos.size() == 0) {
            throw new RuntimeException("queryIntentActivities failed");
        } else {
            for (ResolveInfo ri : infos) {
                if (DEBUG) {
                    Log.d(TAG, "testPm(): for (ResolveInfo ri : infos): ri=" + ri);
                }
            }
        }

        Toast.makeText(this, "PackageManager 测试通过", Toast.LENGTH_LONG).show();
    }

    /**
     * 测试NotificationRemoteView
     */
    private void testNotificationRemoteView() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String tickerText = "this is ticker Text  hahahahahha  ticker ticker ticker.";
        Notification notification = new Notification(R.drawable.ic_launcher, tickerText,
                System.currentTimeMillis());
        int layoutId = HostUtil.getHostResourcesId(this, getPackageName(), "notification_view", "layout");
        RemoteViews mRemoteViews = new RemoteViews(getPackageName(), layoutId);
        notification.contentView = mRemoteViews;

        Intent intent = new Intent();
        intent.setClass(this, AnimationAlphaActivity.class);
        MySerialData data = new MySerialData();
        data.key = "key";
        data.value = "value";
        intent.putExtra("serialtest", data);
        PendingIntent pintent = PendingIntent.getActivity(this, 10, intent, 0);

//        notification.setLatestEventInfo(this, "Event title title",
//                tickerText, pintent);
        int viewId = HostUtil.getHostResourcesId(this, getPackageName(), "notification_view", "id");
        mRemoteViews.setOnClickPendingIntent(viewId, pintent);
        nm.notify(R.drawable.ic_launcher, notification);

    }

    /**
     * 测试AlarmSendIntent
     */
    private void testAlarmSendIntent() {
        Context context = getApplicationContext();
        context.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (DEBUG) {
                    Log.d(TAG, "onReceive(Context context, Intent intent): context=" + context + "; intent=" + intent);
                }
            }
        }, new IntentFilter("my.action"));
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("my.action");
//        intent.setPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            alarmManager.cancel(pendingIntent);
        } catch (Throwable e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3000, pendingIntent);
        } catch (Throwable e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String rs = "";
        if (data != null) {
            rs = data.getStringExtra("rs");
        }
        if (DEBUG) {
            Log.d(TAG, "onActivityResult(int requestCode, int resultCode, Intent data): requestCode=" + requestCode
                    + "; resultCode=" + resultCode + "; data=" + data + "\n new Exception(\"onActivityResult\")=" + new Exception("onActivityResult"));
        }
        Toast.makeText(this, "onActivityResult rs:" + rs, Toast.LENGTH_LONG).show();
    }


    /**
     * 测试PopupWindows
     *
     * @param v View
     */
    private void testPopupWindows(View v) {
        View view = LayoutInflater.from(this).inflate(R.layout.test_popup_window, null);
        final PopupWindow pop = new PopupWindow(view, getResources().getDimensionPixelSize(R.dimen.popwindow_width),
                getResources().getDimensionPixelSize(R.dimen.popwindow_height), true);
        pop.setOutsideTouchable(false);
        pop.setBackgroundDrawable(new ColorDrawable(0));
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (pop.isShowing()) {
                    pop.dismiss();
                }
            }
        });
        int[] xy = new int[2];
        v.getLocationInWindow(xy);
        pop.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        int popWindowAnchorY = xy[1] + pop.getContentView().getMeasuredHeight() / 2;
        pop.update((wm.getDefaultDisplay().getWidth() - getResources().getDimensionPixelSize(R.dimen.popwindow_offset))
                / 2, xy[1] - getResources().getDimensionPixelSize(R.dimen.popwindow_offset), -1, -1);

    }

}


