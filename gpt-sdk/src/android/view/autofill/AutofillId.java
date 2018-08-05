package android.view.autofill;

/**
 * author: BryantGui
 * date: 2018/4/18
 * desc: anroid 8.0源码 AutofillId类
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

/**
 * * A unique identifier for an autofill node inside an {@link android.app.Activity}.
 */
public final class AutofillId implements Parcelable {

    private final int mViewId;
    private final boolean mVirtual;
    private final int mVirtualId;

    /**
     * @hide
     */
    public AutofillId(int id) {
        mVirtual = false;
        mViewId = id;
        mVirtualId = View.NO_ID;
    }

    /**
     * @hide
     */
    public AutofillId(AutofillId parent, int virtualChildId) {
        mVirtual = true;
        mViewId = parent.mViewId;
        mVirtualId = virtualChildId;
    }

    /**
     * @hide
     */
    public AutofillId(int parentId, int virtualChildId) {
        mVirtual = true;
        mViewId = parentId;
        mVirtualId = virtualChildId;
    }

    /**
     * @hide
     */
    public int getViewId() {
        return mViewId;
    }

    /**
     * @hide
     */
    public int getVirtualChildId() {
        return mVirtualId;
    }

    /**
     * @hide
     */
    public boolean isVirtual() {
        return mVirtual;
    }

    /////////////////////////////////
    //  Object "contract" methods. //
    /////////////////////////////////

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mViewId;
        result = prime * result + mVirtualId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final AutofillId other = (AutofillId) obj;
        if (mViewId != other.mViewId) return false;
        if (mVirtualId != other.mVirtualId) return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder().append(mViewId);
        if (mVirtual) {
            builder.append(':').append(mVirtualId);
        }
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mViewId);
        parcel.writeInt(mVirtual ? 1 : 0);
        parcel.writeInt(mVirtualId);
    }

    private AutofillId(Parcel parcel) {
        mViewId = parcel.readInt();
        mVirtual = parcel.readInt() == 1;
        mVirtualId = parcel.readInt();
    }

    public static final Parcelable.Creator<AutofillId> CREATOR =
            new Parcelable.Creator<AutofillId>() {
                @Override
                public AutofillId createFromParcel(Parcel source) {
                    return new AutofillId(source);
                }

                @Override
                public AutofillId[] newArray(int size) {
                    return new AutofillId[size];
                }
            };
}
