package rs.fncore.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import rs.fncore.Const;

/**
 * Настройки сервера ОФД или ОИСМ или ОКП
 *
 * @author amv
 */
public class DocServerSettings implements Parcelable {

    private String mServerAddress = Const.EMPTY_STRING;
    private int mServerPort = 7777;
    private int mServerTimeoutS = 60;
    private volatile boolean mSendImmediately;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(mServerAddress);
        p.writeInt(mServerPort);
        p.writeInt(mServerTimeoutS);
        p.writeInt(mSendImmediately ? 1 : 0);
    }

    public void readFromParcel(Parcel p) {
        mServerAddress = p.readString();
        mServerPort = p.readInt();
        mServerTimeoutS = p.readInt();
        mSendImmediately = p.readInt() != 0;
    }
    
    public JSONObject toJSON() throws JSONException {
    	JSONObject result = new JSONObject();
   		result.put("Address", mServerAddress);
   		result.put("Port",mServerPort);
   		result.put("Timeout",mServerTimeoutS);
   		result.put("Immediately",mSendImmediately);
    	return result;
    }
    public void fromJSON(JSONObject json) throws JSONException {
    	mServerAddress = json.getString("Address");
    	mServerPort = json.getInt("Port");
    	mServerTimeoutS = json.getInt("Timeout");
    	mSendImmediately = json.getBoolean("Immediately");
    }

    /**
     * Получить адрес сервера
     *
     * @return адрес сервера
     */
    public String getServerAddress() {
        return mServerAddress;
    }

    /**
     * Установить адрес сервера
     *
     * @param val адрес сервера
     */
    public void setServerAddress(String val) {
        if (val == null) val = Const.EMPTY_STRING;
        mServerAddress = val;
    }

    /**
     * Получить порт сервера
     *
     * @return порт сервера
     */
    public int getServerPort() {
        return mServerPort;
    }

    /**
     * Установить порт сервера
     *
     * @param port порт сервера
     */
    public void setServerPort(int port) {
        if (port > 65535) port = 65535;
        mServerPort = port;
    }

    /**
     * Получить время ожидания ответа от сервера
     *
     * @return время ожидания ответа от сервера
     */
    public int getServerTimeout() {
        return mServerTimeoutS;
    }

    /**
     * Установить время ожидания ответа от сервера
     *
     * @param value  время ожидания ответа от сервера
     */
    public void setServerTimeout(int value) {
        if (value <= 0) value = 60;
        mServerTimeoutS = value;
    }

    /**
     * Режим "отправка немедленно"
     *
     * @return Режим "отправка немедленно"
     */
    public boolean getImmediatelyMode() {
        return mSendImmediately;
    }

    /**
     * Установить режим "отправка немедленно"
     *
     * @param val режим "отправка немедленно"
     */
    public void setImmediatelyMode(boolean val) {
        mSendImmediately = val;
    }

    public static final Parcelable.Creator<DocServerSettings> CREATOR = new Parcelable.Creator<DocServerSettings>() {

        @Override
        public DocServerSettings createFromParcel(Parcel p) {
            DocServerSettings result = new DocServerSettings();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public DocServerSettings[] newArray(int size) {
            return new DocServerSettings[size];
        }

    };
}
