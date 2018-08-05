# 项目Owner：刘海涛 

# 技术讨论
- Hi：dffd001
- QQ群：703661841

- CSDN：[http://blog.csdn.net/dffd001](http://blog.csdn.net/dffd001)
- 简书：[https://www.jianshu.com/u/2306ba8f1c59](https://www.jianshu.com/u/2306ba8f1c59)

# 开源代码 
- 登录GitHub帐号直接Fork后面项目地址：[https://github.com/baidu/GPT](https://github.com/baidu/GPT)

# 项目简介

## GreedyPorter(GPT)插件系统

- GPT插件系统是借鉴OSGI，AOP等技术实现的一个Android平台重量级插件系统。

- 目前已接入的百度产品包括:百度手机助手,百度网盘,百度卫士,度秘,拾相,91助手,安卓市场等。

- 目前仅百度手机助手已成功接入过的插件数目50+。

- 更多内容可同步关注http://blog.csdn.net/dffd001 和 https://www.jianshu.com/u/2306ba8f1c59 后续文章更新。

- 或参考源码工程"GPT接入必读"和相关代码注释说明。

# 主要特性

- 基于GPT的插件开发比较简单，就是一个普通的APK。

- 插件开发基于标准Android API，无需重新学习。

- 插件可以APK独立运行、方便调试测试，也可以插件形式运行、扩展宿主功能。

- 共用一套代码，无需单独开发维护多套代码，减少开发维护成本。

- 支持Android 四大组件。

- 支持Intent等标准调起方法。

- 支持数据库，Preference等数据存储。

- 支持未写死路径的第三方Jar包通用库。

- 支持多种插件和宿主的交互形式(取决于实际产品需求)。

- 插件默认独立进程安装,减少对主进程的影响。

- 插件默认运行在独立进程减少对主进程的影响，也可在插件的manifest中简单声明<meta-data android:name="gpt_union_process" android:value="true"/>以便插件和宿主运行在同进程。

- 插件运行时每个插件的classloader互相独立，避免类冲突和类兼容性问题;同时更可以根据需求在插件编包时依赖宿主接口或公告库,而在实际出包时简单配置排除,进而保证插件可以复用宿主的最新接口功能和公共库,极大的减小程序包大小和保证核心功能的宿主一致性。

# 快速开始

## Host接入GPT

- 方法一:AAR快速接入：
- (1)在Application项目的build.gradle文件中，添加如下代码：
- allprojects{
　　repositories{
　　　　maven{
　　　　　　url　"http://szwg-appsearch-dev.szwg01.baidu.com:8081/repository/maven-releases/"
　　　　}
　　}
}
- (2)在Module项目中的build.gradle文件中，添加如下代码
- dependencies {
    compile 'com.baidu.android.gporter:GreedyPorter:6.0'
}
 
- (3)【可选】如果需要使用统计功能，在Module项目中的build.gradle文件中，添加如下代码
dependencies {
    compile 'com.baidu.android.gporter:GreedyPorter-Statistics:6.0'
}

- 方法二:直接运行"GPTHostDemo"工程,并把"gpt-sdk"->"build"->"outputs"->"aar"路径下的"gpt-sdk-release.aar"加到对应项目工程里并添加对应依赖。

- 方法三:直接把"gpt-sdk"的Module引入到对应宿主工程模块中,并在对应项目的build.gradle文件中，添加如下依赖。
dependencies {
    compile project(':gpt-sdk')
}

- 方法四:根据实际产品需求直接拷贝"gpt-sdk"的工程源码到对应项目工程中。




## 插件校验

- 插件校验和Android系统比较相似，默认采用签名一致的校验方式;主程序需添加对应实现类声明并可自定义控制校验过程如下所示。

- 主程序写一个类继承自com.baidu.android.gporter.pm.ISignatureVerify并实现对应checkSignature方法,效验成功返回true后才能进行后续安装过程。
	public class SignatureVerifier implements ISignatureVerify{

	    @Override
	    public boolean checkSignature(String packageName, boolean isReplace, Signature[] signatures, Signature[] newSignatures) {
	        return true;
	    }

	}

- 然后在主程序的AndroidManifest中的application标签中添加如下配置声明使用对应的校验类即可。
	<!--插件的签名校验宿主自定义策略实现类,具体说明可参考SignatureVerifier类里的详细注释。-->
    <meta-data
        android:name="com.baidu.android.gporter.signatureverify.class"
        android:value="com.baidu.gpt.hostdemo.SignatureVerifier" />

## 混淆配置

- Host的混淆proguard配置可参考"GPT接入必读"中的proguard.cfg,为方便定位问题代码行号等,也可选择保持混淆后的行号或不混淆。

## 安装插件

- 如下所示直接传入对应插件APK的文件路径即可,安装成功失败会有广播通知,具体可参考"GPTHostDemo"并查看"com.baidu.android.gporter.pm.GPTPackageManager"类的相关方法和参数说明。
GPTPackageManager.getInstance(getApplicationContext()).installApkFile(filePath);

## 启动插件

- 支持插件包名和插件Intent组件,以及插件加载动画自定义和静默加载等多种不同形式的插件启动启动方法,具体可查看插件调用"com.baidu.android.gporter.api.TargetActivator"类的相关方法和参数说明。

- 启动插件的launcher activity
loadTargetAndRun(final Context context, String packageName)

- 启动intent对应的插件component
loadTargetAndRun(final Context context, final Intent intent)


# 开发测试


## 插件开发

- 基于GPT的插件开发比较简单，就是一个普通的apk，在排除特殊限制的情况下可以独立运行也可以插件方式运行，代码可以完全共用一套。

- 插件主体开发过程就是一个普通的Android App开发,极大地减少插件开发二次学习成本并有效降低插件和独立App的代码复用与问题修改同步成本。

- minSDK最低支持8，建议在10以上开发。

- 具体可参考本工程附带的完整Demo并根据产品实际需求详细阅读并开发修改TODO和注释逻辑内容。

- 注意:本开源工程上传代码时因"gpt-sdk"下的"libs"目录并无实际文件而git无法单独上传空文件夹,所以下载完本工程代码后只需要在"gpt-sdk"下新建一空的"libs"文件夹后即可编译运行不同目标工程。

- "gpt-demo"下的"GPTHostDemo"为宿主接入GPT插件系统的可运行Demo,同时提供了插件系统相关功能调试测试等附属功能,可根据实际需求增改。

- "gpt-demo"下的"GPTPluginDemo"为实际插件的可运行Demo,同时提供了插件相关功能调试测试等附属功能,并可根据实际需求增改。

- 为方便用户使用Demo,用户可直接运行安装"GPTHostDemo",本工程已默认内置安装对应的"GPTPluginDemo"以方便用户操作测试。

- 用户也可自行修改"GPTPluginDemo"运行出包后替换"GPTHostDemo"->"assets"下内置插件以包名命名的"com.harlan.animation.apk",内置插件需要以包名命名并放置在"assets"下。

- 内置插件的安装需要调用如下方法,可根据产品实际需求在Application的GPT初始化设置后或具体ActivityUI界面显示点击后调用如下方法以执行内置插件安装。
GPTPackageManager.getInstance(getActivity()).installBuildinApps();

- 用户也可以自行开发插件并随意命名成xxx.apk后放在手机的"/sdcard/baidu_plugin_test/"路径下并点击"GPTPluginDemo"的"扫描加载插件"功能进行新插件的独立安装和运行。

## 主要API

- 为了解决插件在独立进程中运行时，需要和主进程进行通信的需要，提供了跨进程通信的简单接口,具体功能需求时可兼容测试并扩展开发。

- 插件独立进程和主进程接口调用接口：com.baidu.android.gporter.rmi.Nameing，该接口是基于 aidl 方式通信， 具体可参考javadoc和代码注释说明以及"gpt-demo"中的"GPTHostDemo"和"GPTPluginDemo"对应使用实例。

- 插件默认运行在独立进程，如果插件想和主程序运行在一个进程，需要在插件的manifest中设置如下：

		<meta-data android:name="gpt_union_process" android:value="true"/>

- 插件安装、启动等主要API和自定义监听回调可参看"gpt-sdk"中"com.baidu.android.gporter.api"下的"TargetActivator"相关类和接口方法说明。

- 插件如需获取宿主的相关方法可参看"gpt-sdk"中"com.baidu.android.gporter.hostapi"下的"HostUtil"相关类和接口方法说明。

- 主要API相关类和接口方法说明也可用浏览器直接参看"GPT接入必读"中docs文件中的"index"和"index-all"等文档说明或代码注释。

- 更多详细文档可参考"GPT接入必读"和代码注释说明。

## 主要限制

- overridePendingTransition建议使用系统或宿主的
原因：此方法仅传给AMS两个int值，且由AMS层进行资源解析并实现动画效果，根本到不了客户端。
方案：支持系统或宿主动画资源，可将动画资源“预埋在主程序”并利用public.xml确保其ID固定，通过主程序动画ID传递给系统，实现相应效果。

- Notification的资源建议透传或使用系统、宿主的
原因：RemoteViews性质决定。
方案：支持图片资源以及文案等通过Drawable或String透传。
方案：也可在主程序添加相关资源，并共用主程序或系统资源。

- 插件不要静态写死数据路径
原因：插件数据路径和权限是依赖于宿主而生成的。
方案：使用标准方法context.getFilesDir()或getExternalFilesDir()等。

- 插件权限需宿主内声明
原因：权限是系统安装宿主程序时读取其AndroidManifest.xml中权限部分，并放入系统的package.xml中，除系统核心应用和Root外，外界无法修改。
此外：抛开技术，也建议插件权限走“宿主申请审核控制”，以保证宿主对插件权限的安全控制。
方案：插件接入联调时检查权限并在宿主中声明即可。

- 由于插件安装过程需要进行dex重新生成，需要较大的内存，所以插件Manifest中的Component建议不要过多，否则有可能出现OOM。

- 多个插件运行在同一个进程时，需要注意内存占用问题。

- GPT会把静态Broadcast转为动态方式进行支持。

- AccountManager、GMS接口、Class.getInputStream()等暂不支持。

## 主要风险

- GPT插件系统中的ActivityProxy和BroadcastReceiverProxy等设置了android:exported="true"的对外导出组件可能存在如下安全风险：
- 由于这类组件可接受外部输入参数：targetPackageName、targetClassName以便调起复杂可扩展的宿主插件具体组件，攻击者也可通过以上参数任意调用插件中的所有Activity、Service、Receiver等组件，并且能够传递intent参数。
- 因此建议GPT的宿主和插件接入方都根据实际产品业务需求和具体宿主插件组件逻辑，增加对应白名单安全配置机制，以便对内部宿主插件中允许被外部调用的组件等增加白名单配置匹配和安全检查处理机制。



## 高级通信

- 大部分插件通信都可以通过标准方法和指定插件包名,组件类名的Intent参数完成。例如:
    /**
     * 打开换机精灵插件通讯录整理页面
     */
    public static void openHuanjiTidyShowActivity(Context context) {
        Map<String, PlugInAppInfo> pluginAppMap = PluginAppManager.getInstance(context).getPlugAppMap();
        if (pluginAppMap == null || !pluginAppMap.containsKey("com.cx.huanjisdk")) {
            return;
        }
        PlugInAppInfo appInfo = pluginAppMap.get("com.cx.huanjisdk");
        Intent intent = new Intent();
        ComponentName localComponentName = new ComponentName("com.cx.huanjisdk", "com.cx.huanjisdk.tidy.contacts.TidyShowActivity);
        intent.setComponent(localComponentName);
        intent.setAction("android.intent.action.MAIN");
        PluginAppManager.getInstance(context).launchApp(appInfo, intent.toURI());
    }

- startActivityForResult等特殊需求使用前可先调用TargetActivator.remapActivityIntent(mContext, intent)。例如:
    TargetActivator.loadTarget(context, appInfo.getPkgName(), new ITargetLoadedCallBack() {

        @Override
        public void onTargetLoaded(String packageName, boolean isSucc) {
            try {
                if (isSucc) {
                    if (DEBUG) {
                        Log.d(TAG, packageName + " is loaded");
                    }
                    TargetActivator.remapActivityIntent(mContext, intent);
                    ((Activity) context).startActivityForResult(intent, requestCode);
                } else {
                    Toast.makeText(mContext, R.string.plugin_load_fail, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(mContext, R.string.plugin_load_fail, Toast.LENGTH_LONG).show();
            }

        }
    });

- 对于和宿主同进程的插件可以通过简单引入公开功能接口的Jar或第三方公共Jar引入保证插件编写通过,而在实际运行时排除对应Jar包来共享宿主公共库代码或匹配宿主实际功能实现。

- 简单的跨进程通信也可以通过BroadcastReceiver进行Action自定义协议处理,更可以通过宿主直接调用插件对应包名和组件类名直接启动对应组件。

- 插件独立进程和主进程的调用可参考接口com.baidu.android.gporter.rmi.Nameing并根据实际产品需求Log兼容测试，该接口是基于aidl方式通信。

- 以换机助手为例:插件定义需要开放给HOST主程序调用的接口，定义AIDL文件，放到相应src目录下，会在gen的相应目录生成对应的.java文件，例如：PhoneCheckedRemote.aidl
package com.cx.tools.remote;

import com.cx.tools.remote.IPhoneCheckedCallback;

interface PhoneCheckedRemote{
 	void startChecked();
 	void registerCallback(IPhoneCheckedCallback callBack);
 	void unRgisterCallback(IPhoneCheckedCallback callBack);
 }

- 插件定义需要回调HOST主程序的接口，定义AIDL文件，例如：IPhoneCheckedCallback.aidl
package com.cx.tools.remote;

interface IPhoneCheckedCallback{
    void notifyToUI(int score);
}

- 插件实现GPT的Remote接口，HOST主程序才能获得IBinder；Binder实现例如PhoneCheckedRemote.Stub。
如果从HOST主程序传入了回调接口，则可以使用RemoteCallbackList回调HOST主程序传入的回调接口PhoneCheckedRemoteImpl.java。
package com.cx.tools.remote;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.baidu.android.gporter.rmi.Remote;
import com.cx.base.CXApplication;
import com.cx.tools.check.IPhoneInfoListener;
import com.cx.tools.check.PhoneInfoChecked;
public class PhoneCheckedRemoteImpl implements Remote, IPhoneInfoListener {
    private RemoteCallbackList<IPhoneCheckedCallback> callbackList = new RemoteCallbackList<IPhoneCheckedCallback>();

    public PhoneCheckedRemoteImpl() {

    }

    @Override
    public IBinder getIBinder() {
        return mBinder;
    }

    private final PhoneCheckedRemote.Stub mBinder = new PhoneCheckedRemote.Stub() {

        @Override
        public void unRgisterCallback(IPhoneCheckedCallback callBack) throws RemoteException {
            if (callBack != null) {
                callbackList.unregister(callBack);
            }
        }

        @Override
        public void registerCallback(IPhoneCheckedCallback callBack) throws RemoteException {
            if (callBack != null) {
                callbackList.register(callBack);
            }
        }

        @Override
        public void startChecked() throws RemoteException {
            new PhoneInfoChecked(CXApplication.mAppContext, PhoneCheckedRemoteImpl.this).startChecked();
        }
    };

    @Override
    public void notifyToUI(int score) {
        int count = callbackList.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbackList.getBroadcastItem(i).notifyToUI(score);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        callbackList.finishBroadcast();

    }
}

- HOST主程序将插件定义需要开放出来的接口AIDL文件和插件定义需要回调HOST主程序的接口AIDL文件，放到相应src目录下，会在gen的相应目录生成对应的.java文件，例如PhoneCheckedRemote.aidl和IPhoneCheckedCallback.aidl

- HOST主程序实现插件定义的需要回调HOST主程序的接口，例如
public IPhoneCheckedCallbackImpl mIPhoneCheckedCallback = new IPhoneCheckedCallbackImpl();

public class IPhoneCheckedCallbackImpl extends IPhoneCheckedCallback.Stub {

    @Override
    public void notifyToUI(int score) throws RemoteException {
        Log.d(TAG, "IPhoneCheckedCallbackImpl:notifyToUI:score=" + score);
    }
}

- HOST主程序LOAD插件并调用插件接口，传入需要插件回调的接口，例如
TargetActivator.loadTarget(mContext, "com.cx.huanjisdk", new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName) {

                System.out.println(packageName + "is onTargetLoaded.");

                IBinder binderPhoneCheck = Naming.lookupPlugin("com.cx.huanjisdk",
                        "com.cx.tools.remote.PhoneCheckedRemoteImpl");
                PhoneCheckedRemote clientPhoneCheck = PhoneCheckedRemote.Stub.asInterface(binderPhoneCheck);

                if (clientPhoneCheck != null) {
                    try {
                        clientPhoneCheck.registerCallback(mIPhoneCheckedCallback);

                        clientPhoneCheck.startChecked();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

# 常见问题 #

## 宿主和插件在64位设备上加载so问题 ##

- Android的64位和32位运行分析可参考下述相关文章:
[http://blog.csdn.net/dffd001/article/details/79265028](http://blog.csdn.net/dffd001/article/details/79265028)或
[https://www.jianshu.com/p/393f806f1348](https://www.jianshu.com/p/393f806f1348)

- 在64位设备上，对于插件系统有一定的影响，主要是安装和加载。

- 注意:为了有效识别宿主和插件,宿主工程需要在"libs"目录下包含至少1个对应设备类型的so文件。

- 插件无法安装，插件系统报cpuabi不一致无法安装也是由于上述原因导致，比如Host没有so，插件只有armabi 32位的so，此时如果运行在64位设备上，则插件无法安装。

- 64位设备上运行策略如下:

- 如果APK存在lib/arm64-v8a,也存在lib/armabi，则系统运行主程序是则按照64位程序运行;
因为主程序存在64位代码则此时加载插件也需要64位代码，插件中必须包含lib/arm64-v8a的so，否则无法安装也无法运行。

- 如果APK中没有so目录，则系统按照默认配置64位加载主程序，此时按照上一条原则插件必须也是64位的。

- 如果APK存在lib/armabi目录的so，则系统以32位兼容方式加载主程序，此时运行插件也跟主程序一样以32位兼容方式运行，所以此时插件中必须包含armabi 32位so目录。


