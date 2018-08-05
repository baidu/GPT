package android.view.autofill;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * author: BryantGui
 * date: 2018/4/18
 * desc: android 8.0源码只保留声明
 */

public final class AutofillValue implements Parcelable {
    protected AutofillValue(Parcel in) {
    }

    public static final Creator<AutofillValue> CREATOR = new Creator<AutofillValue>() {
        @Override
        public AutofillValue createFromParcel(Parcel in) {
            return new AutofillValue(in);
        }

        @Override
        public AutofillValue[] newArray(int size) {
            return new AutofillValue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
//    private final @View.AutofillType int mType;
//    private final @NonNull Object mValue;
//
//    private AutofillValue(@View.AutofillType int type, @NonNull Object value) {
//        mType = type;
//        mValue = value;
//    }
//
//    /**
//     * Gets the value to autofill a text field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TEXT} for more info.</p>
//     *
//     * @throws IllegalStateException if the value is not a text value
//     */
//    @NonNull
//    public CharSequence getTextValue() {
//        Preconditions.checkState(isText(), "value must be a text value, not type=" + mType);
//        return (CharSequence) mValue;
//    }
//
//    /**
//     * Checks is this is a text value.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TEXT} for more info.</p>
//     */
//    public boolean isText() {
//        return mType == AUTOFILL_TYPE_TEXT;
//    }
//
//    /**
//     * Gets the value to autofill a toggable field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TOGGLE} for more info.</p>
//     *
//     * @throws IllegalStateException if the value is not a toggle value
//     */
//    public boolean getToggleValue() {
//        Preconditions.checkState(isToggle(), "value must be a toggle value, not type=" + mType);
//        return (Boolean) mValue;
//    }
//
//    /**
//     * Checks is this is a toggle value.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TOGGLE} for more info.</p>
//     */
//    public boolean isToggle() {
//        return mType == AUTOFILL_TYPE_TOGGLE;
//    }
//
//    /**
//     * Gets the value to autofill a selection list field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_LIST} for more info.</p>
//     *
//     * @throws IllegalStateException if the value is not a list value
//     */
//    public int getListValue() {
//        Preconditions.checkState(isList(), "value must be a list value, not type=" + mType);
//        return (Integer) mValue;
//    }
//
//    /**
//     * Checks is this is a list value.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_LIST} for more info.</p>
//     */
//    public boolean isList() {
//        return mType == AUTOFILL_TYPE_LIST;
//    }
//
//    /**
//     * Gets the value to autofill a date field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_DATE} for more info.</p>
//     *
//     * @throws IllegalStateException if the value is not a date value
//     */
//    public long getDateValue() {
//        Preconditions.checkState(isDate(), "value must be a date value, not type=" + mType);
//        return (Long) mValue;
//    }
//
//    /**
//     * Checks is this is a date value.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_DATE} for more info.</p>
//     */
//    public boolean isDate() {
//        return mType == AUTOFILL_TYPE_DATE;
//    }
//
//    /**
//     * Used to define whether a field is empty so it's not sent to service on save.
//     * <p>
//     * <p>Only applies to some types, like text.
//     *
//     * @hide
//     */
//    public boolean isEmpty() {
//        return isText() && ((CharSequence) mValue).length() == 0;
//    }
//
//    /////////////////////////////////////
//    //  Object "contract" methods. //
//    /////////////////////////////////////
//
//    @Override
//    public int hashCode() {
//        return mType + mValue.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (obj == null) return false;
//        if (getClass() != obj.getClass()) return false;
//        final AutofillValue other = (AutofillValue) obj;
//
//        if (mType != other.mType) return false;
//
//        if (isText()) {
//            return mValue.toString().equals(other.mValue.toString());
//        } else {
//            return Objects.equals(mValue, other.mValue);
//        }
//    }
//
//    @Override
//    public String toString() {
//        if (!sDebug) return super.toString();
//
//        final StringBuilder string = new StringBuilder()
//                .append("[type=").append(mType)
//                .append(", value=");
//        if (isText()) {
//            string.append(((CharSequence) mValue).length()).append("_chars");
//        } else {
//            string.append(mValue);
//        }
//        return string.append(']').toString();
//    }
//
//    /////////////////////////////////////
//    //  Parcelable "contract" methods. //
//    /////////////////////////////////////
//
//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel parcel, int flags) {
//        parcel.writeInt(mType);
//
//        switch (mType) {
//            case AUTOFILL_TYPE_TEXT:
//                parcel.writeCharSequence((CharSequence) mValue);
//                break;
//            case AUTOFILL_TYPE_TOGGLE:
//                parcel.writeInt((Boolean) mValue ? 1 : 0);
//                break;
//            case AUTOFILL_TYPE_LIST:
//                parcel.writeInt((Integer) mValue);
//                break;
//            case AUTOFILL_TYPE_DATE:
//                parcel.writeLong((Long) mValue);
//                break;
//        }
//    }
//
//    private AutofillValue(@NonNull Parcel parcel) {
//        mType = parcel.readInt();
//
//        switch (mType) {
//            case AUTOFILL_TYPE_TEXT:
//                mValue = parcel.readCharSequence();
//                break;
//            case AUTOFILL_TYPE_TOGGLE:
//                int rawValue = parcel.readInt();
//                mValue = rawValue != 0;
//                break;
//            case AUTOFILL_TYPE_LIST:
//                mValue = parcel.readInt();
//                break;
//            case AUTOFILL_TYPE_DATE:
//                mValue = parcel.readLong();
//                break;
//            default:
//                throw new IllegalArgumentException("type=" + mType + " not valid");
//        }
//    }
//
//    public static final Parcelable.Creator<AutofillValue> CREATOR =
//            new Parcelable.Creator<AutofillValue>() {
//                @Override
//                public AutofillValue createFromParcel(Parcel source) {
//                    return new AutofillValue(source);
//                }
//
//                @Override
//                public AutofillValue[] newArray(int size) {
//                    return new AutofillValue[size];
//                }
//            };
//
//    // Factory methods //
//    ////////////////////
//
//    /**
//     * Creates a new {@link AutofillValue} to autofill a {@link View} representing a text field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TEXT} for more info.
//     */
//    public static AutofillValue forText(@Nullable CharSequence value) {
//        return value == null ? null : new AutofillValue(AUTOFILL_TYPE_TEXT,
//                TextUtils.trimNoCopySpans(value));
//    }
//
//    /**
//     * Creates a new {@link AutofillValue} to autofill a {@link View} representing a toggable
//     * field.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_TOGGLE} for more info.
//     */
//    public static AutofillValue forToggle(boolean value) {
//        return new AutofillValue(AUTOFILL_TYPE_TOGGLE, value);
//    }
//
//    /**
//     * Creates a new {@link AutofillValue} to autofill a {@link View} representing a selection
//     * list.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_LIST} for more info.
//     */
//    public static AutofillValue forList(int value) {
//        return new AutofillValue(AUTOFILL_TYPE_LIST, value);
//    }
//
//    /**
//     * Creates a new {@link AutofillValue} to autofill a {@link View} representing a date.
//     * <p>
//     * <p>See {@link View#AUTOFILL_TYPE_DATE} for more info.
//     */
//    public static AutofillValue forDate(long value) {
//        return new AutofillValue(AUTOFILL_TYPE_DATE, value);
//    }
}
