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

import android.content.pm.PackageManager;

/**
 * PackageManagerTest
 *
 * @author liuhaitao
 * @since 2015-06-01
 */
public class PackageManagerTest {

    /**
     * 测试
     */
    public void test() {
        //PackageManager pm = getPackageManager();
        //try {
            
           /* pm.getApplicationEnabledSetting("com.baidu.appsearch");
            
            ApplicationInfo appinfo = pm.getApplicationInfo("com.baidu.appsearch", 0);
            System.out.println("xxx appinf " + appinfo.packageName);
            String label = (String) appinfo.loadLabel(pm);
            System.out.println("xxxxx label " + label);
            
            Drawable icon = appinfo.loadIcon(pm);
            System.out.println("xxxxx icon " + icon);
            btn_remote_call.setBackgroundDrawable(icon);
            
            
            appinfo = pm.getApplicationInfo("com.harlan.animation", 0);
            System.out.println("xxx appinf " + appinfo.packageName);
            label = (String) appinfo.loadLabel(pm);
            System.out.println("xxxxx label " + label);
            
            System.out.println("xxx -------- PackageInfo test");
            
            PackageInfo pi = pm.getPackageInfo("com.baidu.appsearch", 0);
            System.out.println("xxx pi " + pi.packageName);
            
            pi = pm.getPackageInfo("com.harlan.animation", 0);
            System.out.println("xxx pi " + pi.packageName);
            
            System.out.println("xxx -------- Activity info ");
            ComponentName cn = new ComponentName("com.baidu.appsearch", "com.baidu.appsearch.MainTabActivity");
            
            ActivityInfo ai = pm.getActivityInfo(cn, 0);
            System.out.println("xxx ai " + ai);
            
            cn = new ComponentName("com.harlan.animation", "com.harlan.animation.AnimationExampleActivity");
            ai = pm.getActivityInfo(cn, 0);
            System.out.println("xxx ai " + ai);*/
            
/*            ComponentName cn = new ComponentName("com.baidu.appsearch", "com.baidu.appsearch.youhua.common.PackageChangeReceiver");
            ActivityInfo ai = pm.getReceiverInfo(cn, 0);
            System.out.println("xxx " + ai);
            
            cn = new ComponentName("com.harlan.animation", "com.harlan.animation.MyReceiver");
            ai = pm.getReceiverInfo(cn, 0);
            System.out.println("xxx " + ai);
            
            System.out.println("xxxx ------        test service");
            
            cn = new ComponentName("com.baidu.appsearch", "com.baidu.appsearch.youhua.common.CommonIntentService");
            ServiceInfo si = pm.getServiceInfo(cn, 0);
            System.out.println("xxx " + si);
            
            cn = new ComponentName("com.harlan.animation", "com.harlan.animation.MyService");
            si = pm.getServiceInfo(cn, 0);
            System.out.println("xxx " + si);*/
            
            
            
/*            ComponentName cn = new ComponentName("com.baidu.appsearch", "com.baidu.appsearch.myapp.db.LocalAppsProvider");
            ProviderInfo pi = pm.getProviderInfo(cn, 0);
            System.out.println("xxx pi " + pi);
            
            cn = new ComponentName("com.harlan.animation", "com.harlan.animation.MyProvider");
            pi = pm.getProviderInfo(cn, 0);
            System.out.println("xxx pi " + pi);*/
            
            
/*            Intent intent = new Intent();
            intent.setClassName("com.baidu.appsearch", "com.baidu.appsearch.MainTabActivity");
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : activities) {
                System.out.println("xxxx " + ri);
            }
            
            
            intent.setClassName("com.harlan.animation", "com.harlan.animation.AnimationExampleActivity");
            activities = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : activities) {
                System.out.println("xxxx " + ri);
            }*/
            
/*            Drawable d = pm.getDrawable("com.harlan.animation", 0x7f020001, pm.getApplicationInfo("com.harlan.animation", 0));
            btn_remote_call.setBackgroundDrawable(d);*/
            
/*            Drawable d = pm.getApplicationLogo("com.baidu.gpt.hostdemo");
            btn_remote_call.setBackgroundDrawable(d);*/
            
/*            String text = (String) pm.getText("com.baidu.appsearch", 0x7f0a0000, pm.getApplicationInfo("com.baidu.appsearch", 0));
            System.out.println("xxx " + text);
            
            text = (String) pm.getText("com.harlan.animation", 0x7f060001, pm.getApplicationInfo("com.harlan.animation", 0));
            System.out.println("xxx " + text);*/
            
/*            ComponentName cn = new ComponentName("com.harlan.animation", "com.harlan.animation.AnimationExampleActivity");
            Drawable d = pm.getActivityIcon(cn);
            btn_remote_call.setBackgroundDrawable(d);*/
            
/*            pm.setInstallerPackageName("com.baidu.gpt.hostdemo", "com.baidu.gpt.hostdemo");
            pm.setInstallerPackageName("com.harlan.animation", "com.baidu.gpt.hostdemo");
            
            String t = pm.getInstallerPackageName("com.baidu.gpt.hostdemo");
            System.out.println("xxx " + t);
            t = pm.getInstallerPackageName("com.harlan.animation");
            System.out.println("xxx " + t);*/
/*            
            ComponentName cn = new ComponentName("com.baidu.gpt.hostdemo", "com.baidu.gpt.hostdemo.MainActivity");
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED , PackageManager.DONT_KILL_APP);
            System.out.println("xxx " + pm.getComponentEnabledSetting(cn));
            
            cn = new ComponentName("com.harlan.animation", "com.harlan.animation.AnimationExampleActivity");
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED , PackageManager.DONT_KILL_APP);
            System.out.println("xxx " + pm.getComponentEnabledSetting(cn));*/
    }

}


