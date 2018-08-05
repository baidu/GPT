package com.baidu.android.gporter.stat;

/**
 * Created by bryantgui on 18/4/18.
 * 代理AutoFill时的异常
 */
public class GPTProxyAutoFillException extends RuntimeException {

    public GPTProxyAutoFillException() {
    }

    public GPTProxyAutoFillException(String detailMessage) {
        super(detailMessage);
    }

    public GPTProxyAutoFillException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
