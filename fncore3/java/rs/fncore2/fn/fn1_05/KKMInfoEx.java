package rs.fncore2.fn.fn1_05;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.fncore.data.TaxModeE;
import rs.fncore2.FNCore;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.common.ShiftExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;

class KKMInfoEx extends KKMInfoExBase {
    private static final byte MAX_REASON=2;

    public static final String CLASS_UUID="134a4714-ebaf-11eb-9a03-0242ac130003";

    @Override
    public String getClassUUID() {
        return CLASS_UUID;
    }

    public KKMInfoEx() {
        super();
        mSupppFFDVer = FFDVersionE.VER_11;
		add(FZ54Tag.T1209_FFD_VERSION, FFDVersionE.VER_11.bVal);
		add(FZ54Tag.T1189_KKT_FFD_VERSION, FFDVersionE.VER_12.bVal);
        mShift = new ShiftEx(this);
    }

    public KKMInfoEx(KKMInfo src) {
        super(src);
        Utils.readFromParcel(this, Utils.writeToParcel(src));
        mSupppFFDVer = FFDVersionE.VER_11;
		add(FZ54Tag.T1209_FFD_VERSION, FFDVersionE.VER_11.bVal);
		add(FZ54Tag.T1189_KKT_FFD_VERSION, FFDVersionE.VER_12.bVal);
        mShift = new ShiftEx(this);
    }

/*    public int read(Transaction transaction) {
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            mSupppFFDVer = FFDVersionE.VER_105;
            return update(transaction, bb);
        } finally {
            BufferFactory.release(bb);
        }
    } */

    public int write(FiscalReasonE reason, Transaction transaction, OU operator) {
        mRegistrationReason = reason;
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            byte reasonByte = reason.bVal>MAX_REASON?MAX_REASON:reason.bVal;
            if (!transaction.write(FNCommandsE.START_FISCALIZATION, reasonByte).check())
                return transaction.getLastError();

            if (!getOwner().getName().isEmpty()) add(FZ54Tag.T1048_OWNER_NAME, getOwner().getName());
            if (isOfflineMode()) {
                add(FZ54Tag.T1017_OFD_INN, OU.EMPTY_INN_FULL);
            } else {
                add(FZ54Tag.T1017_OFD_INN, ofd().getINN(true));
                add(FZ54Tag.T1046_OFD_NAME, ofd().getName());
            }
            if (getFFDProtocolVersion() == FFDVersionE.VER_10) remove(FZ54Tag.T1209_FFD_VERSION);
            if (operator == null) {
                operator = new OU();
                operator.setName("Администратор");
            }
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN())) add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(true));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }
            StringBuilder kkmNumber = new StringBuilder(getKKMNumber());
            while (kkmNumber.length() < 20) kkmNumber.append(" ");

            if (reason.bVal < MAX_REASON)
                transaction.write(FNCommandsE.COMMIT_FISCALIZATION, Calendar.getInstance(),
                        getOwner().getINN(true), kkmNumber.toString(),
                        TaxModeE.toByteArray(getTaxModes()), WorkModeE.toByteArray(mWorkModes));
            else {
                transaction.write(FNCommandsE.COMMIT_FISCALIZATION, Calendar.getInstance(),
                        getOwner().getINN(true), kkmNumber.toString(),
                        TaxModeE.toByteArray(getTaxModes()), WorkModeE.toByteArray(mWorkModes),
                        reason.bVal);
            }
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            sign(bb, new SignerEx(this, getLocation()), operator, System.currentTimeMillis());

            readBase(transaction, bb);
            FNCore.getInstance().getDB().storeDocument(getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		reason == FiscalReasonE.REGISTER ? 1 : 11);
            return Errors.NO_ERROR;
        } finally {
            BufferFactory.release(bb);
        }
    }

    public void restore(Transaction transaction, ByteBuffer bb) {

        if (!isFNActive()) return;
        transaction.write(FNCommandsE.GET_FISCALIZATION_RESULT);
        if (transaction.read(bb) != Errors.NO_ERROR) return;
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
        update(transaction, bb);
    }

    @Override
    public ShiftEx getShift() {
    	ShiftExBase shift = super.getShift();
    	if(!(shift instanceof ShiftEx)) {
    		mShift = new ShiftEx(this);
    		Utils.deserialize(Utils.serialize(shift), mShift);
    	}
        return (ShiftEx) super.getShift();
    }

    public int update(Transaction transaction, ByteBuffer buffer) {

        ByteBuffer bb = buffer;
        if (bb == null)
            bb = BufferFactory.allocateRecord();
        try {
            if (readBase(transaction, bb) != Errors.NO_ERROR)
                return transaction.getLastError();
            transaction.write(FNCommandsE.GET_SHIFT_STATUS);
            if (transaction.read(bb) == Errors.NO_ERROR) {
                getShift().read(bb);
            }
        } finally {
            if (buffer == null)
                BufferFactory.release(bb);
        }
        return transaction.getLastError();
    }

    @Override
    public byte[][] packToFN() {
        add(FZ54Tag.T1057_AGENT_FLAG, AgentTypeE.toByteArray(mAgentTypes));
        return super.packToFN();
    }

    @Override
    protected void writeToParcelSavedStatus(Parcel p, int flags) {
        super.writeToParcelSavedStatus(p, flags);
        p.writeLong(mShift.getWhenOpen());
    }

    @Override
    protected void readFromParcelSavedStatus(Parcel p) {
        super.readFromParcelSavedStatus(p);
        if (p.dataAvail() == 0) return;
        getShift().setWhenOpen(p.readLong());
    }

    public static final Creator<KKMInfoEx> CREATOR = new Creator<KKMInfoEx>() {
        @Override
        public KKMInfoEx createFromParcel(Parcel p) {
            p.readString();
            KKMInfoEx result = new KKMInfoEx();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public KKMInfoEx[] newArray(int arg0) {
            return null;
        }
    };
}
