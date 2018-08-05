package com.baidu.android.gporter.proxy;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManager;

import com.baidu.android.gporter.util.Constants;

/**
 * author: BryantGui
 * date: 2018/4/18
 * desc: AutoFillManager hooker
 */

public class AutoFillManagerWorker extends InterfaceProxy {
    /**
     * 构造方法
     */
    public AutoFillManagerWorker() {
        super(Constants.IAUTO_FILL_MANAGER_CLASS);
    }

    /**
     * 系统原始的IAutoFillManager
     */
    public IAutoFillManager mTarget;

    /**
     * host Context
     */
    public Context mHostContext;

    int startSession(IBinder activityToken, IBinder appCallback, AutofillId autoFillId,
                     Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags,
                     String packageName) {
        String hostPackageName = mHostContext.getPackageName();
        return mTarget.startSession(activityToken, appCallback,
                autoFillId, bounds, value, userId,
                hasCallback, flags, hostPackageName);
    }
}
