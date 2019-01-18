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
package com.baidu.android.gporter.proxy.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.ProxyPhoneLayoutInflater;
import com.baidu.android.gporter.gpt.GPTIntent;
import com.baidu.android.gporter.proxy.LocalActivityManagerProxy;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;
import com.baidu.android.gporter.util.JavaCalls;

import java.lang.reflect.Field;


/**
 * 因为要重写dispatchActivityResult，所以我们必须继承ActivityGroup，那个函数无法自己重写。
 *
 * @author liuhaitao
 * @since 2014年12月1日
 */
public class ActivityProxy extends ActivityGroup {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "ActivityProxy";

    /**
     * LocalActivityManager
     */
    private LocalActivityManager mLAM;

    /**
     * TargetPackageName
     */
    private String mTargetPackageName;
    /**
     * TargetClassName
     */
    private String mTargetClassName;
    /**
     * TargetActivity
     */
    private Activity mTargetActivity;
    /**
     * LocalActivityManagerProxy
     */
    private LocalActivityManagerProxy mProxyLocalActivityManager;


    /**
     * 参考{@link ActivityLifecycleCallbacks}
     */
    static ActivityLifecycleCallbacks sActivityLifecycleCallbacks;

    /**
     * activity生命周期回调。目前只有一个地方需要，就是hostapp需要activity的生命周期回调，来进行 activity task
     * 上的计数，用于判断是否还有activity存在。因为hostapp中的actiivty大都继承于同一个BaseActivity，我们的插件Activity
     * 则不是，对hostapp依赖于这样的计数，无法解决。所以提供此回调。
     * 通过 {@link GPTActivityBak#setActivityLifecycleCallbacks(ActivityLifecycleCallbacks)} 进行设置。
     */
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);

        void onActivityStarted(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityStopped(Activity activity);

        void onActivitySaveInstanceState(Activity activity, Bundle outState);

        void onActivityDestroyed(Activity activity);
    }

    /**
     * 参考 {@link ActivityLifecycleCallbacks}
     *
     * @param callback ActivityLifecycleCallbacks
     */
    public static void setActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        sActivityLifecycleCallbacks = callback;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // 暂时先屏蔽Restore的功能，防止crash。
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        // 暂时先屏蔽Restore的功能，防止crash。
    }

    /**
     * onProvideAssistData被调用过
     */
    private boolean mOnProvideAssistDataCalledFlag = false;

    @Override
    public void onProvideAssistData(Bundle data) {
        super.onProvideAssistData(data);
        mOnProvideAssistDataCalledFlag = true;
    }

    @Override
    public Intent getIntent() {
        // TODO 粗暴点先解了，后续再想办法：6.0上长按home激活google now，会调用ActivityManagerService.getAssistContextExtras
        // 通过ApplicationThread的requestAssistContextExtras方法获取当前Activity的extra，导致serializable解析失败。
        if (mOnProvideAssistDataCalledFlag) {
            try {
                throw new Exception();
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                StackTraceElement[] st = e.getStackTrace();
                if (st.length > 1 && "android.app.ActivityThread".equals(st[1].getClassName())
                        && "handleRequestAssistContextExtras".equals(st[1].getMethodName())) {
                    return null;
                }
            }
        }

        return super.getIntent();
    }

    /**
     * 初始化
     * 包括设置theme，android2.3需要在super.oncreate之前调用settheme
     *
     * @param bundle Bundle
     */
    public boolean initTargetActivity(Bundle bundle) {
        Intent curIntent = super.getIntent();
        if (curIntent == null) {
            finish();
            return false;
        }

        GPTComponentInfo info = GPTComponentInfo.parseFromIntent(curIntent);
        if (info == null) {
            finish();
            return false;
        }

        String targetClassName = info.className;
        String targetPackageName = info.packageName;

        mTargetPackageName = targetPackageName;
        mTargetClassName = targetClassName;

        if (!ProxyEnvironment.isEnterProxy(targetPackageName)
                || ProxyEnvironment.getInstance(targetPackageName).getRemapedActivityClass(targetClassName) != this
                .getClass()) {

            if (targetClassName == null) {
                targetClassName = "";
            }

            if (!TextUtils.isEmpty(targetPackageName)) {

                if (!info.reschedule) {
                    Intent intent = new Intent(super.getIntent());
                    intent.setComponent(new ComponentName(targetPackageName, targetClassName));
                    ProxyEnvironment.enterProxy(super.getApplicationContext(), intent, true, true);
                }
            }
            finish();
            return false;
        }

        // 设置屏幕方向
        int orientation = ProxyEnvironment.getInstance(mTargetPackageName).getTargetActivityOrientation(
                mTargetClassName);
        if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(orientation);
        }

        // 设置主题
        setTheme(ProxyEnvironment.getInstance(targetPackageName).getTargetActivityThemeResource(targetClassName));

        return true;
    }

    /**
     * loadTargetActivity
     *
     * @param bundle Bundle
     */
    public void loadTargetActivity(Bundle bundle) {

        // greedy porter create--------
        if (mLAM == null) {
            mLAM = getLocalActivityManager();
        }

        ActivityInfo targetInfo = ProxyEnvironment.getInstance(mTargetPackageName)
                .getTargetMapping().getActivityInfo(mTargetClassName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setUiOptions(targetInfo.uiOptions);
        }

        GPTIntent lamIntent = new GPTIntent(super.getIntent());
        lamIntent.setClassName(mTargetPackageName, mTargetClassName);
        Window window = mLAM.startActivity(mTargetClassName, lamIntent);

        //window.setCallback(getCurrentActivity());
        // 和 GPTActivity.getWindow 相对应，都是使用proxy的 window。
        //JavaCalls.setField(getCurrentActivity(), "mWindow", getWindow());
        //setContentView(window.getDecorView());

        /**
         * 替换icon和logo为目标插件
         */
        replaceActivityInfo(targetInfo);

        //int SDK_INT = android.os.Build.VERSION.SDK_INT;

        /*
         * android 4.4 以及以上由于没有setcontentview初始化actionbar，导致后来初始化调用hide，空指针
         * 所以提前初始化一下
         */
        //if (SDK_INT >= 11) {
        //ActionBar actionbar = getActionBar();
        //JavaCalls.setField(getCurrentActivity(), "mActionBar", actionbar);
        //}
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Activity target = getCurrentActivity();
        if (target != null) {
            target.onWindowFocusChanged(hasFocus);
        }
    }


    @Override
    protected void onCreate(Bundle bundle) {

        boolean inited = initTargetActivity(bundle);

        super.onCreate(bundle);

        if (!inited) {
            return;
        }

        loadTargetActivity(bundle);

        if (!ProxyEnvironment.isEnterProxy(mTargetPackageName)) {
            return;
        }

        // ActvityGroup不入栈，否则通过LaunchMode找Activity栈的时候会有问题。
        if (this.getParent() == null) {
            ProxyEnvironment.getInstance(mTargetPackageName).pushActivityToStack(this);
        }

        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityCreated(this, bundle);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityDestroyed(this);
        }

        // 为了确保出栈，在finish和onDestroy各调用一次，destroy回调比较晚，可能会影响栈管理。
        if (!ProxyEnvironment.isEnterProxy(mTargetPackageName) || this.getParent() != null) {
            return;
        }

        ProxyEnvironment.getInstance(mTargetPackageName).popActivityFromStack(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mLAM != null) {
            GPTIntent newIntent = new GPTIntent(intent);
            newIntent.setClassName(mTargetPackageName, mTargetClassName);
            mLAM.startActivity(mTargetClassName, newIntent);
        }
    }

    /**
     * activity显示开始的时间
     */
    protected long timeStart = SystemClock.elapsedRealtime();

    @Override
    protected void onPause() {
        super.onPause();

        // 统计使用时长
        if (!TextUtils.isEmpty(mTargetPackageName) && !TextUtils.isEmpty(mTargetClassName)) {
            ReportManger.getInstance().activityElapsedTime(getApplicationContext(), mTargetPackageName,
                    mTargetClassName, (SystemClock.elapsedRealtime() - timeStart));
        }
        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityPaused(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 统计使用时长
        if (!TextUtils.isEmpty(mTargetPackageName) && !TextUtils.isEmpty(mTargetClassName)) {
            timeStart = SystemClock.elapsedRealtime();
        }

        // 首次启动设置titile为子activity的title
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            setTitle(currentActivity.getTitle());
        }
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        // 设置actionbar的icon为插件的icon
        if (SDK_INT >= 11) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null && currentActivity != null) {
                ActivityInfo targetInfo = ProxyEnvironment.getInstance(mTargetPackageName)
                        .getTargetMapping().getActivityInfo(mTargetClassName);
                int icon = targetInfo.icon != 0 ? targetInfo.icon : targetInfo.applicationInfo.icon;
                actionBar.setIcon(icon);
            }
        }

        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityResumed(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle paramBundle) {
        super.onSaveInstanceState(paramBundle);
        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivitySaveInstanceState(this, paramBundle);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityStopped(this);
        }
    }


/*
    public void startActivityFromChild(Activity paramActivity, Intent paramIntent, int paramInt) {
        ProxyEnvironment.getInstance(mTargetPackageName).remapStartActivityIntent(paramIntent);
        super.startActivityFromChild(paramActivity, paramIntent, paramInt);
    }
*/

    @Override
    protected void onApplyThemeResource(Resources.Theme paramTheme, int paramInt, boolean paramBoolean) {
        super.onApplyThemeResource(paramTheme, paramInt, paramBoolean);
    }

    @Override
    protected void onChildTitleChanged(Activity paramActivity, CharSequence paramCharSequence) {
        setTitle(paramCharSequence); // 插件title 变化，需要设置代理title
    }


    @Override
    public Resources getResources() {
        boolean installed = ProxyEnvironment.hasInstance(mTargetPackageName);

        if (installed) {
            return ProxyEnvironment.getInstance(mTargetPackageName).getTargetResources();
        } else {
            return super.getResources();
        }
    }

    @Override
    public Context getApplicationContext() {
        boolean installed = ProxyEnvironment.hasInstance(mTargetPackageName);

        if (installed) {
            return ProxyEnvironment.getInstance(mTargetPackageName).getApplication();
        } else {
            return super.getApplicationContext();
        }
    }

    @Override
    public AssetManager getAssets() {
        boolean installed = ProxyEnvironment.hasInstance(mTargetPackageName);

        if (installed) {
            return ProxyEnvironment.getInstance(mTargetPackageName).getTargetAssetManager();
        } else {
            return super.getAssets();
        }
    }

    /**
     * 自定义一个theme, 否则在holo主题会找不到internal资源
     */
    private Resources.Theme mTargetTheme;

    @Override
    public Theme getTheme() {
        boolean installed = ProxyEnvironment.hasInstance(mTargetPackageName);

        if (installed) {
            if (mTargetTheme == null) {
                mTargetTheme = ProxyEnvironment.getInstance(mTargetPackageName).getTargetResources().newTheme();
                mTargetTheme.setTo(ProxyEnvironment.getInstance(mTargetPackageName).getTargetTheme());
            }
            return mTargetTheme;
        } else {
            return super.getTheme();
        }
    }

    @Override
    public void setTheme(int resid) {
        getTheme().applyStyle(resid, true);
    }

    @Override
    public ClassLoader getClassLoader() {
        boolean installed = ProxyEnvironment.hasInstance(mTargetPackageName);

        if (installed) {
            return ProxyEnvironment.getInstance(mTargetPackageName).getDexClassLoader();
        } else {
            return super.getClassLoader();
        }
    }

    /**
     * onBeforeCreate
     *
     * @param context Context
     */
    public void onBeforeCreate(Context context) {
        Activity activity = (Activity) context;
        mTargetActivity = activity;

        changeInflatorContext(context);
        changeLocalActivityManager(context);
    }

    /**
     * 替换layoutinflator,参考{@link ProxyPhoneLayoutInflater}
     *
     * @param context GPTActivity
     */
    private void changeInflatorContext(Context context) {
        Window window = getWindow(); 
         
        /*
         * GPTActivity getWindow 公用的是 ActivityProxy的window。
         * 
         * 这里需要提前替换layoutinflator。返回我们自己的ProxyPhoneLayoutInflator。
         */
        Object layoutInflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        JavaCalls.setField(window, "mLayoutInflater", layoutInflator);
    }

    /**
     * changeLocalActivityManager
     *
     * @param context Context
     */
    private void changeLocalActivityManager(Context context) {
        if (mProxyLocalActivityManager == null && !(mTargetActivity instanceof ActivityProxy)) {
            try {
                ActivityGroup tabActivity = (ActivityGroup) mTargetActivity;
                mProxyLocalActivityManager = new LocalActivityManagerProxy(tabActivity.getLocalActivityManager(),
                        mTargetPackageName);
                JavaCalls.setField(tabActivity, "mLocalActivityManager", mProxyLocalActivityManager);
//                Field field = ActivityGroup.class.getDeclaredField("mLocalActivityManager");
//                field.setAccessible(true);
//                field.set(tabActivity, mProxyLocalActivityManager);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Activity target = getCurrentActivity();
        if (target != null) {
            return target.onKeyDown(keyCode, event);
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Activity target = getCurrentActivity();
        if (target != null) {
            return target.onKeyUp(keyCode, event);
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void finish() {
        if (mTargetActivity != null) {
            int resultCode = (Integer) JavaCalls.getField(mTargetActivity, "mResultCode");
            Intent data = (Intent) JavaCalls.getField(mTargetActivity, "mResultData");
            setResult(resultCode, data);
        }
        super.finish();

        if (!ProxyEnvironment.isEnterProxy(mTargetPackageName) || this.getParent() != null) {
            return;
        }

        ProxyEnvironment.getInstance(mTargetPackageName).popActivityFromStack(this);
    }

    @Override
    public void finishAffinity() {
        super.finishAffinity();
    }

    @Override
    public Object getSystemService(String name) {

        return super.getSystemService(name);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Activity target = getCurrentActivity();
        if (target != null) {
            return target.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public void onContentChanged() {
        Activity target = getCurrentActivity();
        if (mTargetActivity != null) {
            mTargetActivity.onContentChanged();
        } else {
            super.onContentChanged();
        }

    }

    /**
     * 替换mActivityInfo中的icon和logo
     *
     * @param info 插件的activityinfo
     */
    private void replaceActivityInfo(ActivityInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Field field = Activity.class.getDeclaredField("mActivityInfo");
                field.setAccessible(true);
                Object obj = field.get(this);
                if (obj instanceof ActivityInfo) {
                    ActivityInfo hostInfo = (ActivityInfo) obj;
                    ActivityInfo newInfo = new ActivityInfo(hostInfo);
                    newInfo.applicationInfo = new ApplicationInfo(
                            hostInfo.applicationInfo);
                    newInfo.applicationInfo.icon = info.icon;
                    newInfo.applicationInfo.logo = info.logo;
                    field.set(this, newInfo);
                }
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

}
