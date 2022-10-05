package rs.fncore2.core;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.util.Base64;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import rs.fncore.data.DocServerSettings;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.PrintSettings;
import rs.fncore2.fn.common.FNBaseI;
import rs.log.Logger;

public class Settings {
    private static final String SETTINGS_TAG = "Settings";

    private static final int VERSION=320;

    public static final String OFD_CHANGED = "OFD changed";
    public static final String OISM_CHANGED = "OISM changed";

    private static final String XREPORT_TAG = "XReport2";
    private static String FN_TAG = "FN.";

    private DocServerSettings mOFDServer = new DocServerSettings();
    private DocServerSettings mOISMServer = new DocServerSettings();
    private PrintSettings mPrintSettings = new PrintSettings();
    private KKMInfo.FNConnectionModeE mConnMode = KKMInfo.FNConnectionModeE.USB;

    private boolean mKeepRestFlag = false;
    private boolean mCashControlFlag = true;
    private final SharedPreferences mSp;

    private final PropertyChangeSupport mOFDChanged;

    private static Settings SHARED_INSTANCE;  
    public static Settings getInstance() { return SHARED_INSTANCE; }
    
    public Settings(SharedPreferences sp) {
        mOFDChanged = new PropertyChangeSupport(this);
        SHARED_INSTANCE = this;
        this.mSp = sp;
        load();
    }

    @SuppressLint("ApplySharedPref")
    private void load(){
        String s = mSp.getString(SETTINGS_TAG, null);
        if (s != null && !s.isEmpty()) {
            try {
                byte[] b = Base64.decode(s, Base64.NO_WRAP);
                Parcel p = Parcel.obtain();
                p.unmarshall(b, 0, b.length);
                p.setDataPosition(0);
                int version = p.readInt();
                if (version==VERSION) {
                    mOFDServer.readFromParcel(p);
                    mOISMServer.readFromParcel(p);
                    mPrintSettings.readFromParcel(p);
                    mConnMode = KKMInfo.FNConnectionModeE.values()[p.readInt()];

                    if (mConnMode == KKMInfo.FNConnectionModeE.UNKNOWN){
                        mConnMode =KKMInfo.FNConnectionModeE.UART;
                    }

                    FN_TAG = p.readString();
                    mKeepRestFlag = p.readInt() != 0;
                    mCashControlFlag = p.readInt() != 0;
                    p.recycle();
                }
                else{
                    mSp.edit().putString(FN_TAG, "").commit();
                    mSp.edit().putString(XREPORT_TAG, "").commit();
                    Logger.e("settings version not match, provided: "+version+", expected: "+VERSION+", resetting settings");
                }

            } catch (Exception e) {
                Logger.e(e,"Ошибка восстановления настроек");
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    public void store() {
        Objects.requireNonNull(mSp);
        Parcel p = Parcel.obtain();
        p.writeInt(VERSION);
        mOFDServer.writeToParcel(p, 0);
        mOISMServer.writeToParcel(p, 0);
        mPrintSettings.writeToParcel(p, 0);
        p.writeInt(mConnMode.ordinal());
        p.writeString(FN_TAG);
        p.writeInt(mKeepRestFlag ? 1 : 0);
        p.writeInt(mCashControlFlag ? 1 : 0);

        p.setDataPosition(0);
        String s = Base64.encodeToString(p.marshall(), Base64.NO_WRAP);
        mSp.edit().putString(SETTINGS_TAG, s).commit();
        p.recycle();
    }

    protected KKMInfo.FNConnectionModeE getConnectionMode() {
        return mConnMode;
    }

    public PrintSettings getPrintSettings() {
        return mPrintSettings;
    }

    public DocServerSettings getOFDServer() {
        return mOFDServer;
    }

    public DocServerSettings getOISMServer() {
        return mOISMServer;
    }

    protected boolean isCashControlFlag() {
        return mCashControlFlag;
    }

    protected boolean isKeepRestFlag() {
        return mKeepRestFlag;
    }

    protected int getKKMInfoNumber(FNBaseI fn) {
        return mSp.getInt(FN_TAG+fn.getNumber(),0);
    }


    protected void setOFDServer(DocServerSettings server) {
        mOFDServer = server;
        store();
        mOFDChanged.firePropertyChange(OFD_CHANGED, null, mOFDServer);
    }

    protected void setOISMServer(DocServerSettings server) {
        mOISMServer = server;
        store();
        mOFDChanged.firePropertyChange(OISM_CHANGED, null, mOISMServer);
    }

    protected void setPrintSettings(PrintSettings print) {
        mPrintSettings = print;
        store();
    }

    protected void setConnectionMode(KKMInfo.FNConnectionModeE connectionMode) {
        mConnMode =connectionMode;
        store();
    }

    protected void setFnTAG(String fnTAG) {
        FN_TAG = fnTAG;
        store();
    }

    protected void setKeepRestFlag(boolean keepRestFlag) {
        mKeepRestFlag = keepRestFlag;
        store();
    }

    protected void setCashControlFlag(boolean cashControlFlag) {
        mCashControlFlag = cashControlFlag;
        store();
    }

    public long getWhenShiftOpen(String fnsn) {
    	return  mSp.getLong(FN_TAG+fnsn+".shift", 0); 
    }
    public void setWhenShiftOpen(String fnsn, long time) {
    	mSp.edit().putLong(FN_TAG+fnsn+".shift", time).commit();
    }


    public void addOFDChangedListener(PropertyChangeListener pcl) {
        mOFDChanged.addPropertyChangeListener(pcl);
    }

    public void removeOFDChangedListener(PropertyChangeListener pcl) {
        mOFDChanged.removePropertyChangeListener(pcl);
    }
    //endregion

	public void updateKKMInfo(int fdNumber, FNBaseI fn) {
		mSp.edit().putInt(FN_TAG+fn.getNumber(),fdNumber).commit();
		
	}

	public long getLastUpdateOKPTime(String fn) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -30);
		long r = mSp.getLong(FN_TAG+fn+".okp", 0);
		if(r == 0) {
			r = cal.getTimeInMillis();
			setLastUpdateOKPTime(fn,r);
		}
		return r;
	}
	public void setLastUpdateOKPTime(String fn, long time) {
		mSp.edit().putLong(FN_TAG+fn+".okp", time).commit();
	}
	
	public DocServerSettings getOKPServer() {
		DocServerSettings okp = new DocServerSettings();
		try {
			JSONObject json = new JSONObject(mSp.getString("OKP", "{}"));
			okp.fromJSON(json);
		} catch(JSONException jse) {}
		return okp;
	}
	public void setOKPServer(DocServerSettings okp) {
		Editor e = mSp.edit(); 
		try {
			 e.putString("OKP", okp.toJSON().toString()).commit();
		} catch(JSONException jse) { }
		e.commit();
	}
	
	public double getCashRest(String fn) {
		try {
			return Double.parseDouble(mSp.getString(FN_TAG+fn+".rest", "0.0"));
		} catch(NumberFormatException nfe) {
			return 0;
		}
	}
	public void setCashRest(String fn, double val) {
		mSp.edit().putString(FN_TAG+fn+".rest",String.format(Locale.ROOT, "%.2f",val)).commit();
	}
}
