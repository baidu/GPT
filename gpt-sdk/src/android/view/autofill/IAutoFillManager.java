package android.view.autofill;

import android.graphics.Rect;
import android.os.IBinder;

/**
 * author: BryantGui
 * date: 2018/4/18
 * desc: AutoFillManager binder接口
 */

public interface IAutoFillManager {
    int startSession(IBinder activityToken, IBinder appCallback, AutofillId autoFillId,
                     Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags,
                     String packageName);
}
