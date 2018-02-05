#!/bin/bash
# export ANDROID_SDK_HOME=/Users/baidu/Library/Android/sdk
# export PROGUARD_HOME=/proguard

# 编译生成 gpt-sdk 的AAR相关文件到./output,同时上传到共享Maven服务器
# TODO 注意:执行本文件前先检查对应路径下的build.gradle的依赖配置。
# TODO 注意:对外开源同步代码时,可直接删改本文件。

rm -rf ./output/
chmod +x ./gradlew
 ./gradlew :gpt-sdk:buildSdk uploadArchives