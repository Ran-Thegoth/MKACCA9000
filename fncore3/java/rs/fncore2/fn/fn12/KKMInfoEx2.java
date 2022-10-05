package rs.fncore2.fn.fn12;

import android.os.Parcel;

import static rs.fncore.data.KKMInfoEx2FiscalReason.getExtendedFiscalReasonChange;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.fncore.data.TaxModeE;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

class KKMInfoEx2 extends KKMInfoExBase {
    private static final byte MAX_REASON = 2;

    public static final String CLASS_UUID="fc2f1546-ebae-11eb-9a03-0242ac130003";

    @Override
    public String getClassUUID() {
        return CLASS_UUID;
    }

    String mFnVersion;
    boolean mFailedMarkingCode;
    boolean mForcedFailedMarkingCode;

    boolean mSupportOKP;
    long lastUpdateOKPTime = (new Date()).getTime();

    public KKMInfoEx2() {
        super();
        mShift = new ShiftEx2(this);
        add(FZ54Tag.T1013_KKT_SERIAL_NO, FNCore.getInstance().getDeviceSerial());
    }

    public KKMInfoEx2(KKMInfo src) {
        this();
        Utils.readFromParcel(this, Utils.writeToParcel(src));
        add(FZ54Tag.T1013_KKT_SERIAL_NO, FNCore.getInstance().getDeviceSerial());
    }


    public boolean isMGM() {
        return  mFnNumber != null &&  mFnNumber.startsWith("9999");
    }

    public boolean isOKPSupported() {
        return mSupportOKP;
    }

    @Override
    public void cloneTo(KKMInfo dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    private static Date getDateWithoutTimeUsingCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    @Override
    public int read(Transaction transaction) {

        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            FFDVersionInfo ver = getFNFFDVersion(transaction, bb);
            mSupppFFDVer = ver.supported;


            return update(transaction, bb);
        } finally {
            BufferFactory.release(bb);
        }
    }

    public int write(FiscalReasonE reason, Transaction transaction, OU operator, final KKMInfo oldInfo) {
        mRegistrationReason = reason;
        remove(FZ54Tag.T1190_FN_FFD_VESION);
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            byte reasonByte = reason.bVal > MAX_REASON ? MAX_REASON : reason.bVal;
            if(reason != FiscalReasonE.REGISTER )
            	mTotalCounters = FNManager.getInstance().readCounters(transaction,true);
            if (!transaction.write(FNCommandsE.START_FISCALIZATION_1_2, reasonByte,
                    getFFDProtocolVersion().bVal).check())
                return transaction.getLastError();

            if (!getOwner().getName().isEmpty())
                add(FZ54Tag.T1048_OWNER_NAME, getOwner().getName());

            String ofdInn = OU.EMPTY_INN_FULL;
            if (!isOfflineMode()) {
                ofdInn = ofd().getINN(true);
                add(FZ54Tag.T1046_OFD_NAME, ofd().getName());
            }

            if (operator == null) {
                operator = new OU();
                operator.setName("Администратор");
            }
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(true));
            else remove(FZ54Tag.T1203_CASHIER_INN);
            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }
            StringBuilder kkmNumber = new StringBuilder(getKKMNumber());
            while (kkmNumber.length() < 20) kkmNumber.append(" ");

            if (reason == FiscalReasonE.REGISTER)
                transaction.write(FNCommandsE.COMMIT_FISCALIZATION_1_2, Calendar.getInstance(),
                        getOwner().getINN(true), kkmNumber.toString(), TaxModeE.toByteArray(getTaxModes()),
                        WorkModeE.toByteArray(mWorkModes),
                        WorkModeExE.toByteArray(mWorkModesEx), ofdInn);
            else {
                mReRegistrationReason = getExtendedFiscalReasonChange(reason, oldInfo, this);
                transaction.write(FNCommandsE.COMMIT_FISCALIZATION_1_2, Calendar.getInstance(),
                        getOwner().getINN(true), kkmNumber.toString(), TaxModeE.toByteArray(getTaxModes()),
                        WorkModeE.toByteArray(mWorkModes), WorkModeExE.toByteArray(mWorkModesEx),
                        ofdInn, (int) mReRegistrationReason);
            }
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            sign(bb, new SignerEx(this, getLocation()), operator, System.currentTimeMillis());

            update(transaction, bb);
            FNCore.getInstance().getDB().storeDocument(getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		reason == FiscalReasonE.REGISTER ? 1 : 11);
            return Errors.NO_ERROR;
        } finally {
            BufferFactory.release(bb);
        }
    }

    public int restore(Transaction transaction, ByteBuffer bb) {
        int res = FN2.getFnVersion(transaction, bb, this);
        if (res != Errors.NO_ERROR) return res;

        if (!isFNActive()) return Errors.NO_ERROR;
        transaction.write(FNCommandsE.GET_FISCALIZATION_RESULT);
        if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

        long fTime = Utils.readDate5(bb);
        byte[] data = new byte[12];
        bb.get(data);
        String s = new String(data);
        mOwner.setINN(s);
        data = new byte[20];
        bb.get(data);
        mKkmNumber = new String(data).trim();
        mTaxModes = TaxModeE.fromByteArray(bb.get());
        mWorkModes = WorkModeE.fromByteArray(bb.get());
        mWorkModesEx = WorkModeExE.fromByteArray(bb.get());
        data = new byte[12];
        bb.get(data);
        s = new String(data);
        mOfd.setINN(s);
        if (bb.remaining() >= 12) {
            Utils.readUint32LE(bb);
        }
        Object tag = readTag(transaction, FZ54Tag.T1048_OWNER_NAME, null, String.class);
        if (tag != null)
            mOwner.setName(tag.toString());
        tag = readTag(transaction, FZ54Tag.T1009_TRANSACTION_ADDR, null, String.class);
        if (tag != null)
            getLocation().setAddress(tag.toString());
        tag = readTag(transaction, FZ54Tag.T1187_TRANSACTION_PLACE, null, String.class);
        if (tag != null)
            getLocation().setPlace(tag.toString());

        sign(bb, new SignerEx(this, getLocation()), new OU(), fTime);

        tag = readTag(transaction, FZ54Tag.T1046_OFD_NAME, bb, String.class);
        if (tag != null)
            mOfd.setName(tag.toString());
        tag = readTag(transaction, FZ54Tag.T1017_OFD_INN, bb, String.class);
        if (tag != null)
            mOfd.setINN(tag.toString());

        tag = readTag(transaction, FZ54Tag.T1021_CASHIER_NAME, bb, String.class);
        if (tag != null)
            signature().operator().setName(tag.toString());
        else
            signature().operator().setName("Администратор");
        tag = readTag(transaction, FZ54Tag.T1203_CASHIER_INN, bb, String.class);
        if (tag != null)
            signature().operator().setINN(tag.toString());

        tag = readTag(transaction, FZ54Tag.T1057_AGENT_FLAG, bb, byte.class);
        if (tag != null) {
            mAgentTypes = AgentTypeE.fromByteArray(((Number) tag).byteValue());
        }

        applyTag(transaction, FZ54Tag.T1060_FNS_URL, bb, String.class);
        applyTag(transaction, FZ54Tag.T1193_GAMBLING_FLAG, bb, boolean.class);
        applyTag(transaction, FZ54Tag.T1126_LOTTERY_FLAG, bb, boolean.class);
        applyTag(transaction, FZ54Tag.T1207_EXCISE_GOODS_FLAG, bb, boolean.class);
        applyTag(transaction, FZ54Tag.T1013_KKT_SERIAL_NO, bb, String.class);
        applyTag(transaction, FZ54Tag.T1209_FFD_VERSION, bb, byte.class);
        applyTag(transaction, FZ54Tag.T1189_KKT_FFD_VERSION, bb, byte.class);
        applyTag(transaction, FZ54Tag.T1117_SENDER_EMAIL, bb, String.class);
        applyTag(transaction, FZ54Tag.T1036_AUTOMAT_NO, bb, String.class);

        return update(transaction,bb);
    }

    @Override
    public ShiftEx2 getShift() {
        return (ShiftEx2) super.getShift();
    }

    private void checkLastUpdateOKPTime(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastUpdateOKPTime);
        if (cal.get(Calendar.YEAR)<2021) {
            Logger.w("wrong last update OKP time, resetting it %s", cal);
            lastUpdateOKPTime = new Date().getTime();
        }
    }


/*    private int updateOkpKeysDaysRemainin(Transaction transaction, ByteBuffer bb){
        CheckNeedUpdateKeysE res=FN2Commands.isNeedUpdateOkpKeys(transaction, bb, this);
        if (res==null) return transaction.getLastError();
        return Errors.NO_ERROR;
    } */

    @Override
    public int update(Transaction transaction, ByteBuffer buffer) {
        ByteBuffer bb = buffer;
        if (bb == null)
            bb = BufferFactory.allocateRecord();
        lastUpdateOKPTime = Settings.getInstance().getLastUpdateOKPTime(getFNNumber());
        try {
            checkLastUpdateOKPTime();
            updateFnRemainedDays(transaction,bb);
           /* if (updateOkpKeysDaysRemainin(transaction,bb)!= Errors.NO_ERROR)
                return transaction.getLastError(); */

            if (readBase(transaction, bb) != Errors.NO_ERROR)
                return transaction.getLastError();
            getShift().read(transaction, bb);
        } finally {
            if (buffer == null)
                BufferFactory.release(bb);
        }
        return transaction.getLastError();
    }

    @Override
    public byte[][] packToFN() {
        remove(FZ54Tag.T1057_AGENT_FLAG);
        remove(FZ54Tag.T1017_OFD_INN);
        remove(FZ54Tag.T1209_FFD_VERSION);
        remove(FZ54Tag.T1207_EXCISE_GOODS_FLAG);
        remove(FZ54Tag.T1193_GAMBLING_FLAG);
        remove(FZ54Tag.T1126_LOTTERY_FLAG);
        remove(FZ54Tag.T1221_AUTOMAT_FLAG);
        return super.packToFN();
    }

    @Override
    public void setFailedMarkingCode(boolean val) {
        mFailedMarkingCode = val;
    }

    @Override
    public void setForcedFailedMarkingCode(boolean val) {
        mForcedFailedMarkingCode = val;
    }

    @Override
    public boolean getFailedMarkingCode() {
        return mFailedMarkingCode;
    }

    @Override
    public boolean getForcedFailedMarkingCode() {
        return mForcedFailedMarkingCode;
    }

    @Override
    protected void writeToParcelSavedStatus(Parcel p, int flags) {
        super.writeToParcelSavedStatus(p, flags);
        p.writeLong(mShift.getWhenOpen());
        p.writeString(mFnVersion);
        p.writeInt(mFailedMarkingCode ? 1 : 0);
        p.writeInt(mForcedFailedMarkingCode ? 1 : 0);
        p.writeInt(mSupportOKP ? 1 : 0);
        p.writeLong(lastUpdateOKPTime);
        p.writeLong(mFnRemainedDays);
    }

    @Override
    protected void readFromParcelSavedStatus(Parcel p) {
        super.readFromParcelSavedStatus(p);
        if (p.dataAvail() == 0) return;
        getShift().setWhenOpen(p.readLong());
        mFnVersion = p.readString();
        mFailedMarkingCode=p.readInt() != 0;
        mForcedFailedMarkingCode=p.readInt() != 0;
        mSupportOKP = p.readInt() != 0;
        lastUpdateOKPTime = p.readLong();
        mFnRemainedDays = p.readLong();
    }

    public static final Creator<KKMInfoEx2> CREATOR = new Creator<KKMInfoEx2>() {
        @Override
        public KKMInfoEx2 createFromParcel(Parcel p) {
            p.readString();
            KKMInfoEx2 result = new KKMInfoEx2();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public KKMInfoEx2[] newArray(int arg0) {
            return null;
        }
    };


}
