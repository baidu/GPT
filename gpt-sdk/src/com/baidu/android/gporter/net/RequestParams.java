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
package com.baidu.android.gporter.net;

import android.net.Uri;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * 请求数据
 *
 * @author liuhaitao
 * @since 2015-07-10
 */
public class RequestParams {

    /**
     * http 超时。
     */
    public static final int HTTP_TIMEOUT_MS = 30000;

    /**
     * Content-Type请求头
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 请求地址
     */
    private String mUrl;

    /**
     * 参数提交方式，默认Post提交
     */
    private RequestType mRequestType = RequestType.POST;

    /**
     * 请求参数
     */
    private List<NameValuePair> mParams;

    /**
     * 请求头数据
     */
    private HashMap<String, String> mHeader;

    /**
     * 请求参数
     */
    private byte[] mContent;

    /**
     * 参数编码
     */
    private String mParamsEncoding = "";

    /**
     * 上传文件
     */
    private File mFile;

    /**
     * 完整的请求地址
     */
    private String mFullUrl;

    /**
     * 设置请求地址
     *
     * @param url 请求地址
     */
    public void setUrl(String url) {
        mFullUrl = null;
        mUrl = url;
    }

    /**
     * 设置请求类型
     *
     * @param requestType 请求类型
     */
    public void setRequestType(RequestType requestType) {
        mRequestType = requestType;
    }

    /**
     * 设置请求参数
     *
     * @param params 请求参数
     */
    public void setParams(List<NameValuePair> params) {
        mFullUrl = null;
        mParams = params;
    }

    /**
     * 添加请求头信息
     *
     * @param key   请求键值
     * @param value 请求值
     */
    public void addHeader(String key, String value) {
        if (mHeader == null) {
            mHeader = new HashMap<String, String>(2);
        }
        mHeader.put(key, value);
    }

    /**
     * 设置参数
     *
     * @param content 请求参数
     */
    public void setContent(byte[] content) {
        mContent = content;
    }

    /**
     * 设置参数编码
     *
     * @param str 编码类型
     */
    public void setParamsEncoding(String str) {
        mParamsEncoding = new String(" charset=" + str);
    }

    /**
     * 设置上传文件
     *
     * @param file 上传文件
     */
    public void setUploadFile(File file) {
        mFile = file;
    }

    /**
     * 获取请求地址
     *
     * @return 请求地址
     */
    public String getUrl() {
        if (mFullUrl == null) {
            if (getParams() != null && getRequestType() == RequestType.GET) {
                String param = obtainGetParams(getParams());
                mFullUrl = mUrl + param;
            } else {
                mFullUrl = mUrl;
            }
        }

        return mFullUrl;
    }

    /**
     * 获取请求类型
     *
     * @return 请求类型
     */
    public RequestType getRequestType() {
        return mRequestType;
    }

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    private List<NameValuePair> getParams() {
        return mParams;
    }

    /**
     * 获取请求头信息
     *
     * @return 请求头信息
     */
    public HashMap<String, String> getHeader() {
        return mHeader;
    }

    /**
     * 获取参数
     *
     * @return 请求参数
     */
    public byte[] getBody() {
        if (mContent == null && getParams() != null && getRequestType() == RequestType.POST) {
            String value = obtainPostParams(getParams());
            mContent = value.getBytes();
        }
        return mContent;
    }

    /**
     * 获取内容类型
     *
     * @return 内容类型
     */
    public String getContentType() {
        return "application/x-www-form-urlencoded;" + getParamsEncoding();
    }

    /**
     * 获取参数编码
     *
     * @return 参数编码
     */
    public String getParamsEncoding() {
        if (mParamsEncoding == null) {
            return new String(" charset=UTF-8");
        } else {
            return mParamsEncoding;
        }
    }

    /**
     * 设置上传文件
     *
     * @return file 上传文件
     */
    public File getUploadFile() {
        return mFile;
    }

    /**
     * 获取请求参数
     *
     * @param params 请求参数
     * @return 字符形式参数
     */
    public String obtainPostParams(List<NameValuePair> params) {
        return obtainParams(params, true);
    }

    /**
     * 获取请求参数
     *
     * @param params 请求参数
     * @return 字符形式参数
     */
    private String obtainGetParams(List<NameValuePair> params) {
        return obtainParams(params, false);
    }

    /**
     * 获取参数
     *
     * @param params           请求参数
     * @param nameNeedToEncode 名字是否需要编码
     * @return 拼接参数的字符串
     */
    private String obtainParams(List<NameValuePair> params, boolean nameNeedToEncode) {
        StringBuffer paramsStr = new StringBuffer();
        for (NameValuePair param : params) {
            paramsStr.append('&');
            paramsStr.append(nameNeedToEncode ? Uri.encode(param.getName()) : param.getName());
            paramsStr.append('=');
            paramsStr.append(Uri.encode(param.getValue()));
        }
        return paramsStr.toString();
    }

}
