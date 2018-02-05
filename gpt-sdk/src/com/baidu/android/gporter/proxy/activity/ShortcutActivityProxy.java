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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.baidu.android.gporter.GPTComponentInfo;
import com.baidu.android.gporter.ProxyEnvironment;
import com.baidu.android.gporter.gpt.Util;
import com.baidu.android.gporter.pm.GPTPackageManager;
import com.baidu.android.gporter.stat.ReportManger;
import com.baidu.android.gporter.util.Constants;

/**
 * ShortcutActivityProxy
 *
 * @author liuhaitao
 * @since 2018-01-03
 */
public class ShortcutActivityProxy extends Activity {

    /**
     * 插件目标组件信息
     */
    public static final String EXTRA_KEY_TARGET_INFO = "gpt_shortcut_target_info";
    /**
     * 插件快捷方式界面的Category
     */
    public static final String EXTRA_KEY_TARGET_CATE = "gpt_shortcut_target_category";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GPTComponentInfo info;

        if (!Util.hasValidIntentExtra(getIntent())
                || (info = GPTComponentInfo.parseFromString(getIntent().getStringExtra(EXTRA_KEY_TARGET_INFO))) == null
                || GPTPackageManager.getInstance(getApplicationContext()).getPackageInfo(info.packageName) == null) {

            // 提示用户，插件不存在
            Toast.makeText(getApplicationContext(), Constants.HINT_TARGET_NOT_INSTALLED, Toast.LENGTH_SHORT).show();

            finish();
            return;
        }

        Intent newIntent = new Intent(getIntent());
        newIntent.setComponent(new ComponentName(info.packageName, info.className));
        // 统计
        ReportManger.getInstance().onPluginStartByShortcut(getApplicationContext(), info.packageName);

        // 恢复category
        String[] cates = getIntent().getStringArrayExtra(EXTRA_KEY_TARGET_CATE);
        if (cates != null && cates.length > 0) {
            for (String cate : cates) {
                newIntent.addCategory(cate);
            }
        }

        ProxyEnvironment.enterProxy(getApplicationContext(), newIntent, true, false);
        finish();
    }

}



