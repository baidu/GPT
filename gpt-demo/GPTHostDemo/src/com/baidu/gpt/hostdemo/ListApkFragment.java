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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.android.gporter.api.TargetActivator;
import com.baidu.android.gporter.pm.GPTPackageInfo;
import com.baidu.android.gporter.pm.GPTPackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件APK列表显示的Fragment
 *
 * @author liuhaitao
 * @since 2017-02-21
 */
public class ListApkFragment extends ListFragment {
    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & MainActivity.DEBUG;

    /**
     * TAG
     */
    public static final String TAG = "ListApkFragment";
    /**
     * 数据
     */
    private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

    /**
     * 监听安装的BroadcastReceiver,用来处理数据提示更新
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String pluginPackage = intent.getStringExtra(GPTPackageManager.EXTRA_PKG_NAME);
            if (DEBUG) {
                Log.d(TAG, "onReceive(Context context, Intent intent): context=" + context.toString() + "\nintent=" + intent.toString()
                        + "\naction=" + action + "\n插件包名=" + pluginPackage);
            }

            if (GPTPackageManager.ACTION_PACKAGE_INSTALLED.equals(action)) {
                Toast.makeText(getActivity(), "已安装插件: " + pluginPackage, Toast.LENGTH_SHORT).show();
            }

            updateAppList();
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 注册插件安装、删除的相关广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(GPTPackageManager.ACTION_PACKAGE_INSTALLED);
        filter.addAction(GPTPackageManager.ACTION_PACKAGE_DELETED);

        getActivity().registerReceiver(receiver, filter);

        long startTimeMillis = System.currentTimeMillis();

        // 为方便HostDemo直接体验插件功能,默认先安装内置在 assets/greedyporter 目录下的内置插件APK(),
        // 内置APK必须以插件的 packageName 命名，比如 com.harlan.animation.apk
        // TODO 如不需要可删除对应路径、插件文件和下面这句代码。
        GPTPackageManager.getInstance(getActivity()).installBuildinApps();
        if (DEBUG) {
            Log.d(TAG, "onViewCreated(): GPTPackageManager.getInstance(getActivity()).installBuildinApps() coast TimeMillis = "
                    + String.valueOf(System.currentTimeMillis() - startTimeMillis));
        }

        updateAppList(); // 第一次可能为空，因为没有安装完。
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(receiver);
        super.onDestroyView();
    }

    /**
     * 更新插件App数据列表
     */
    private void updateAppList() {
        data.clear();
        List<GPTPackageInfo> installedApks = GPTPackageManager.getInstance(getActivity()).getInstalledApps();
        if (DEBUG) {
            Log.d(TAG, "updateAppList(): List<GPTPackageInfo> installedApks = GPTPackageManager.getInstance(getActivity()).getInstalledApps()\n"
                    + "installedApks.size()=" + installedApks.size() + "installedApks=" + installedApks.toString());
        }

        for (GPTPackageInfo info : installedApks) {
            AppInfo appInfo = parseApk(info.srcApkPath);
            if (appInfo == null) {
                continue;
            }
            addItem(info.packageName, info.srcApkPath, appInfo);
        }

        ApplistAdapter adapter = new ApplistAdapter(getActivity());
        setListAdapter(adapter);
    }

    /**
     * ViewHolder
     */
    public final class ViewHolder {
        /**
         * 显示插件ICON的ImageView
         */
        public ImageView iconView;
        /**
         * 显示插件fileName的TextView
         */
        public TextView fileNameTextView;
        /**
         * 显示插件appName的TextView
         */
        public TextView appNameTextView;
    }

    /**
     * ApplistAdapter
     */
    public class ApplistAdapter extends BaseAdapter {

        /**
         * LayoutInflater
         */
        private LayoutInflater mInflater;

        /**
         * 构造方法
         *
         * @param context Context
         */
        public ApplistAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int item) {
            return item;
        }

        @Override
        public long getItemId(int itemId) {
            return itemId;
        }

        @SuppressLint("NewApi")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.activity_main_plugin_item, null);
                holder.iconView = (ImageView) convertView.findViewById(R.id.img);
                holder.appNameTextView = (TextView) convertView.findViewById(R.id.app_name);
                holder.fileNameTextView = (TextView) convertView.findViewById(R.id.package_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.iconView.setBackgroundDrawable((Drawable) data.get(position).get("icon"));
            String appname = (String) data.get(position).get("appname");
            holder.appNameTextView.setText(appname);
            holder.fileNameTextView.setText((String) data.get(position).get("filename"));

            return convertView;
        }

    }

    /**
     * addItem
     *
     * @param filename 文件名
     * @param path     路径
     * @param appInfo  AppInfo
     */
    private void addItem(String filename, String path, AppInfo appInfo) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("filename", filename);
        map.put("path", path);
        map.put("appname", appInfo.appName);
        map.put("icon", appInfo.appIcon);
        map.put("packagename", appInfo.packageName);
        data.add(map);
        if (DEBUG) {
            Log.d(TAG, "addItem(String filename, String path, AppInfo appInfo): filename=" + filename
                    + "; path=" + path + "; appInfo=" + appInfo.toString()
                    + "\n data.add(map):map=" + map.toString());
        }
    }

    @Override
    public void onListItemClick(ListView l, final View v, int position, long id) {
        Map<String, Object> item = data.get(position);
        String packageName = (String) item.get("packagename");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, ""));

        // 启动插件的方法
        TargetActivator.loadTargetAndRun(getActivity(), intent);
    }

    /**
     * 简单定义的AppInfo
     */
    class AppInfo {
        /**
         * 插件App的Name
         */
        private String appName;
        /**
         * 插件App的Icon
         */
        private Drawable appIcon;
        /**
         * 插件App的包名
         */
        private String packageName;

        /**
         * 构造方法
         *
         * @param appName     插件App的Name
         * @param appIcon     插件App的Icon
         * @param packageName 插件App的包名
         */
        public AppInfo(String appName, Drawable appIcon, String packageName) {
            this.appName = appName;
            this.appIcon = appIcon;
            this.packageName = packageName;
        }

        /**
         * 获取插件App的Name
         *
         * @return 插件App的Name
         */
        public String getAppName() {
            return appName;
        }

        /**
         * 获取插件App的Icon
         *
         * @return 插件App的Icon
         */
        public Drawable getAppIcon() {
            return appIcon;
        }

        /**
         * 获取插件App的包名
         *
         * @return 插件App的包名
         */
        public String getPackageName() {
            return packageName;
        }
    }

    /**
     * 解析APK
     *
     * @param apkPath APK文件路径
     * @return AppInfo
     */
    private AppInfo parseApk(String apkPath) {
        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(apkPath, 0);
        if (pi == null) {
            return null;
        }
        pi.applicationInfo.sourceDir = apkPath;
        pi.applicationInfo.publicSourceDir = apkPath;

        Drawable appIcon = pi.applicationInfo.loadIcon(pm);
        String appName = (String) pi.applicationInfo.loadLabel(pm);
        return new AppInfo(appName, appIcon, pi.applicationInfo.packageName);
    }
}
