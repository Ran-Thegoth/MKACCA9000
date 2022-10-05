package rs.fncore2.fn.storage.virtual;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.fncore.data.Tag;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.log.Logger;

public class VStorage implements StorageI {

    static final byte FN_PREFIX = 0x04;
    static final byte NO_ERRORS = 0x00;

    VFnDB mDB;
    private static final String FNINFO_TAG = "FNInfo";
    private volatile Transaction mActive=null;

    class FNInfo {
        private static final String PROP_TAG="VFN";
        int mID;
        Tag mFiscalization;
        byte mState;
        boolean mShiftOpen;
        int mShiftNumber;
        Calendar mLastDocumentDate, mFTime;
        int mLastDocumentNo, mNCheck;
        byte[] mINN;
        byte[] mKkmNO;
        byte mWorkModes;
        byte mTaxModes;
        private SharedPreferences mSp;

        public FNInfo(Context ctx) {
            clear();

            mSp = ctx.getSharedPreferences(PROP_TAG, Context.MODE_PRIVATE);
            try {
                JSONObject obj = new JSONObject(mSp.getString(FNINFO_TAG, "{}"));
                mID = obj.getInt("Id");
                mFiscalization = mDB.getDocument(mID);
                mState = (byte) (obj.getInt("State") & 0xFF);
                mLastDocumentDate.setTimeInMillis(obj.getLong("LastDocumentDate"));
                mLastDocumentNo = obj.getInt("LastDocumentNo");
                mShiftOpen = obj.getBoolean("ShiftOpen");
                mShiftNumber = obj.getInt("ShiftNumber");
                mINN = Base64.decode(obj.getString("INN"), Base64.NO_WRAP);
                mKkmNO = Base64.decode(obj.getString("KKMNo"), Base64.NO_WRAP);
                mWorkModes = (byte) obj.getInt("WorkModes");
                mTaxModes = (byte) obj.getInt("TaxModes");
                mFTime.setTimeInMillis(obj.getLong("FiscalTime"));
                mNCheck = obj.getInt("NCheck");
            } catch (JSONException jse) {
                Logger.e(jse,"FNInfo create exc");
            }
        }

        public void store() {
            if (mSp==null) return;

            JSONObject obj = new JSONObject();
            try {
                obj.put("Id", mID);
                obj.put("State", mState);
                obj.put("LastDocumentDate", mLastDocumentDate.getTimeInMillis());
                obj.put("LastDocumentNo", mLastDocumentNo);
                obj.put("ShiftOpen", mShiftOpen);
                obj.put("ShiftNumber", mShiftNumber);
                obj.put("INN", Base64.encodeToString(mINN, Base64.NO_WRAP));
                obj.put("KKMNo", Base64.encodeToString(mKkmNO, Base64.NO_WRAP));
                obj.put("WorkModes", mWorkModes);
                obj.put("TaxModes", mTaxModes);
                obj.put("FiscalTime", mFTime.getTimeInMillis());
                obj.put("NCheck", mNCheck);
                mSp.edit().putString(FNINFO_TAG, obj.toString()).commit();

            } catch (JSONException jse) {
                Logger.e(jse,"FNInfo store exc");
            }
        }

        public void clear() {
            mID = 0;
            mState = KKMInfo.FNStateE.STAGE1.bVal;
            mINN = OU.EMPTY_INN.getBytes();
            mKkmNO = "0000000000000000    ".getBytes();
            mWorkModes = mTaxModes = 0;
            mShiftOpen = false;
            mNCheck = 0;
            mFiscalization = null;
            mShiftNumber = 0;
            mLastDocumentDate = Calendar.getInstance();
            mFTime = Calendar.getInstance();
            store();
        }
    }


    FNInfo mFnInfo;
    public static final String V_FN_NO = "9999999999999999";
    public static final byte[] VFN_SERIAL = V_FN_NO.getBytes();

    public VStorage(Context ctx) {
        mDB = new VFnDB(ctx);
        mFnInfo = new FNInfo(ctx);
    }

    @Override
    public Transaction open() {
        mActive=new VTransaction(this, this);
        return mActive;
    }

    public void openExisting(Transaction transaction) {
        mActive = transaction;
    }

    public static void clear(Context ctx) {
        (new VStorage(ctx)).clear();
    }

    public void clear() {
        mDB.clear();
        mFnInfo.clear();
    }

    @Override
    public void release(Transaction transaction) {
        if (mActive == transaction) {
            mActive = null;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isBusy(){
        return mActive!=null;
    }


    @Override
    public void waitReady() {

    }

    @Override
    public String toString() {
        return "VirtualStorage";
    }
}

