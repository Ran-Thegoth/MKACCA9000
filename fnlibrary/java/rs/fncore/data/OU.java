package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;


import rs.fncore.Const;

/**
 * Данные о организации/сотруднике
 *
 * @author nick
 */
public class OU implements IReableFromParcel {

    private static final String NAME_TAG = "Name";
    private static final String INN_TAG = "INN";
    public static final String EMPTY_INN =      "0000000000";
    public static final String EMPTY_INN_FULL = "000000000000";
    private String mName = Const.EMPTY_STRING;
    private String mINN = EMPTY_INN;

    public OU(JSONObject json) throws JSONException {
        mName = json.getString(NAME_TAG);
        mINN = json.getString(INN_TAG);
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        OU newValue = (OU) o;
        // field comparison
        return mName.equals(newValue.mName)
                && mINN.equals(newValue.mINN);
    }

    @Override
    public int hashCode() {
        return 31*mName.hashCode()+mINN.hashCode();
    }

    public OU() {
    }

    /**
     * @param name - наименование
     */
    public OU(String name) {
        mName = name;
    }

    /**
     * @param name - наименование
     * @param inn  - ИНН
     */
    public OU(String name, String inn) {
        mName = name;
        mINN = inn;
    }

    /**
     * Получить наименование
     *
     * @return наименование
     */
    public String getName() {
        return mName;
    }

    /**
     * Установить наименование
     *
     * @param name наименование
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return Получить ИНН без дополнения нулями
     */
    public String getINN() {
        return getINN(false);
    }

    /**
     * @param padded - дополнять ли до 12 символов
     * @return Получить ИНН с/без дополнения нулями слева до 12 символов
     */
    public String getINN(boolean padded) {
        if (mINN == null || mINN.isEmpty())
            return EMPTY_INN;
        StringBuilder s = new StringBuilder(mINN);
        if (padded) {
            while (s.length() < 12) {
                s.append(" ");
            }
        }
        return s.toString();
    }

    /**
     * @return Получить ИНН с обрезанными лидирующими нулями
     */
    public String getINNtrimZ() {
        String s = getINN();
        while (s.endsWith(" ") && s.length() > 10) s = s.substring(0, s.length()-1);
        while (s.startsWith("0") && s.length() > 10) s = s.substring(1);
        if (EMPTY_INN.equals(s)) return Const.EMPTY_STRING;
        return s;
    }

    /**
     * Установить ИНН
     *
     * @param s ИНН
     */
    public void setINN(String s) {
        if (s != null && !s.trim().isEmpty())
            mINN = s.trim();
        else
            mINN = EMPTY_INN;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int arg1) {
        p.writeString(mINN);
        p.writeString(mName);
    }

    public void readFromParcel(Parcel p) {
        mINN = p.readString();
        mName = p.readString();
    }

    public void cloneTo(OU ou) {
        ou.mINN = mINN;
        ou.mName = mName;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();

        result.put(NAME_TAG, mName);
        result.put(INN_TAG, mINN);

        return result;
    }

    public static final Parcelable.Creator<OU> CREATOR = new Parcelable.Creator<OU>() {

        @Override
        public OU createFromParcel(Parcel p) {
            OU result = new OU();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public OU[] newArray(int size) {
            return new OU[size];
        }

    };
}
