package rs.fncore2.fn.fn1_05;

import static rs.utils.Utils.readUint16LE;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.OU;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.ShiftExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;

class ShiftEx extends ShiftExBase {

    protected ShiftEx(Parcel p) {
        super(p);
    }

    public ShiftEx(KKMInfoEx info) {
        super(info);
    }

    public int writeOpenShift(Transaction transaction, OU operator) {
        mKKMInfo.getLocation().cloneTo(getLocation());

        if (mKKMInfo.getFFDProtocolVersion() != FFDVersionE.VER_10) {
            add(FZ54Tag.T1209_FFD_VERSION, mKKMInfo.getFFDProtocolVersion().bVal);
        }

        Calendar now = Calendar.getInstance();
        ByteBuffer bb = BufferFactory.allocateDocument();

        try {
            if (mKKMInfo.readBase(transaction, bb) != Errors.NO_ERROR)
                return transaction.getLastError();

            if(!isOpen()) {
            	mShiftCounters = FNManager.getInstance().readCounters(transaction, false);
            	mTotalCounters = FNManager.getInstance().readCounters(transaction, true);
            }
            transaction.write(FNCommandsE.START_OPEN_SHIFT, now);
            if (!transaction.check()) return transaction.getLastError();

            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            add(FZ54Tag.T1188_KKT_VERSION, KKMInfo.KKT_VERSION);
            add(FZ54Tag.T1189_KKT_FFD_VERSION, mKKMInfo.getFFDProtocolVersion().bVal);

            
            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_OPEN_SHIFT);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

            mShiftNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            transaction.write(FNCommandsE.GET_SHIFT_STATUS);
            if (transaction.read(bb) == Errors.NO_ERROR) read(bb);
            transaction.write(FNCommandsE.GET_OFD_STATUS);
            if (transaction.read(bb) == Errors.NO_ERROR) mOFDStat.update(bb);
            mWhenOpen = now.getTimeInMillis();
            Settings.getInstance().setWhenShiftOpen(mKKMInfo.getFNNumber(), mWhenOpen);
            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),2);
            
            return Errors.NO_ERROR;

        } finally {
            BufferFactory.release(bb);
        }
    }

    public int writeCloseShift(Transaction transaction, OU operator) {
        mKKMInfo.getLocation().cloneTo(getLocation());

        if (mKKMInfo.getFFDProtocolVersion() != FFDVersionE.VER_10) {
            add(FZ54Tag.T1209_FFD_VERSION, mKKMInfo.getFFDProtocolVersion().bVal);
        }

        Calendar now = Calendar.getInstance();
        ByteBuffer bb = BufferFactory.allocateDocument();

        try {
            if (mKKMInfo.readBase(transaction, bb) != Errors.NO_ERROR) {
                return transaction.getLastError();
            }
            {
                transaction.write(FNCommandsE.GET_OFD_STATUS);
                if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
                mOFDStat.update(bb);
            }


            if (mFnWarnings.isReplace30Days()) add(FZ54Tag.T1050_FN_EXPIRE_FLAG, true);
            if (mFnWarnings.isReplaceUrgent3Days()) add(FZ54Tag.T1051_FN_REPLACE_FLAG, true);
            if (mFnWarnings.isMemoryFull99()) add(FZ54Tag.T1052_FN_OVERFLOW_FLAG, true);
            if (mFnWarnings.isOFDTimeout()) mOfdTimeout=true;

            transaction.write(FNCommandsE.START_CLOSE_SHIFT, now);
            if (!transaction.check()) return transaction.getLastError();

            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            remove(FZ54Tag.T1188_KKT_VERSION);
            remove(FZ54Tag.T1189_KKT_FFD_VERSION);

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_CLOSE_SHIFT);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

            mShiftNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            transaction.write(FNCommandsE.GET_SHIFT_STATUS);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            read(bb);

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),5);
            return Errors.NO_ERROR;

        } finally {
            BufferFactory.release(bb);
        }
    }

    public void read(ByteBuffer bb) {
        mIsOpen = ((bb.get() & 0xFF) != 0);
        mShiftNumber = readUint16LE(bb);
        mLastDocNumber = readUint16LE(bb);
        mWhenOpen = Settings.getInstance().getWhenShiftOpen(mKKMInfo.getFNNumber());
    }

    public static final Creator<ShiftEx> CREATOR = new Creator<ShiftEx>() {
        @Override
        public ShiftEx createFromParcel(Parcel p) {
            p.readString();
            return new ShiftEx(p);
        }

        @Override
        public ShiftEx[] newArray(int arg0) {
            return null;
        }
    };
    @Override
    public String onKey(String key) {
    	return super.onKey(key);
    }
}
