package rs.fncore2.fn.fn12;

import static rs.utils.Utils.readUint16LE;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.CheckNeedUpdateKeysE;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.ShiftExBase;
import rs.fncore2.fn.fn12.marking.FnMarkingStatus;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;

class ShiftEx2 extends ShiftExBase {

    

    protected ShiftEx2(Parcel p) {
        super(p);
    }

    public ShiftEx2(KKMInfoEx2 info) {
        super(info);
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
    }

    public int writeOpenShift(Transaction transaction, OU operator, CheckNeedUpdateKeysE needUpdateOKPKeys) {
        mKKMInfo.getLocation().cloneTo(getLocation());

        Calendar now = Calendar.getInstance();
        ByteBuffer bb = BufferFactory.allocateDocument();

        try {
            if (mKKMInfo.update(transaction, bb) != Errors.NO_ERROR) {
                return transaction.getLastError();
            }
            mShiftCounters = mTotalCounters =  null;
            remove(FZ54Tag.T2112_MARKING_CODE_INCORRECT);
            transaction.write(FNCommandsE.START_OPEN_SHIFT, now);
            if (!transaction.check()) return transaction.getLastError();

            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            add(FZ54Tag.T1188_KKT_VERSION, KKMInfo.KKT_VERSION);
            add(FZ54Tag.T1189_KKT_FFD_VERSION, mKKMInfo.getFFDProtocolVersion().bVal);

/*            add(FZ54Tag.T1276_ADDITIONAL_REQUISIT_OOC, "");
            add(FZ54Tag.T1277_ADDITIONAL_DATA_OOC, new byte[]{}); */

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_OPEN_SHIFT);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

            mShiftNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            read(transaction, bb);
            transaction.write(FNCommandsE.GET_OFD_STATUS);
            if (transaction.read(bb) == Errors.NO_ERROR) mOFDStat.update(bb);

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),2);
            mWhenOpen = now.getTimeInMillis();
            Settings.getInstance().setWhenShiftOpen(mKKMInfo.getFNNumber(), mWhenOpen);
            return Errors.NO_ERROR;
        } finally {
            BufferFactory.release(bb);
        }
    }

    public int writeCloseShift(Transaction transaction, OU operator) {
        mKKMInfo.getLocation().cloneTo(getLocation());
        mOKPUpdateResult = "Обновление ключей не требуется";

        Calendar now = Calendar.getInstance();
        ByteBuffer bb = BufferFactory.allocateDocument();

        try {
            if (mKKMInfo.update(transaction, bb) != Errors.NO_ERROR) {
                return transaction.getLastError();
            }

            {
                transaction.write(FNCommandsE.GET_OFD_STATUS);
                if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
                mOFDStat.update(bb);
            }

            remove(FZ54Tag.T1188_KKT_VERSION);
            remove(FZ54Tag.T1189_KKT_FFD_VERSION);
            remove(FZ54Tag.T1276_ADDITIONAL_REQUISIT_OOC);
            remove(FZ54Tag.T1277_ADDITIONAL_DATA_OOC);

            mShiftCounters = FNManager.getInstance().readCounters(transaction,false);


            if (mFnWarnings.isReplace30Days()) add(FZ54Tag.T1050_FN_EXPIRE_FLAG, true);
            if (mFnWarnings.isReplaceUrgent3Days()) add(FZ54Tag.T1051_FN_REPLACE_FLAG, true);
            if (mFnWarnings.isMemoryFull99()) add(FZ54Tag.T1052_FN_OVERFLOW_FLAG, true);
            if (mFnWarnings.isOFDTimeout()) mOfdTimeout=true;

            {
                int tag_2112=0;
                if (mKKMInfo.getFailedMarkingCode()) tag_2112|=INCORRECT_MARKING_CODE_FLAG;
                if (mKKMInfo.getForcedFailedMarkingCode()) tag_2112|=INCORRECT_MARKING_CODE_IN_FISCAL_FLAG;
                if (tag_2112>0){
                    add(FZ54Tag.T2112_MARKING_CODE_INCORRECT, (byte)tag_2112);
                }
                mKKMInfo.setFailedMarkingCode(false);
                mKKMInfo.setForcedFailedMarkingCode(false);
            }

            transaction.write(FNCommandsE.START_CLOSE_SHIFT, now);
            if (!transaction.check()) return transaction.getLastError();

            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN())) {
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            }
            else remove(FZ54Tag.T1203_CASHIER_INN);

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_CLOSE_SHIFT);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

            mShiftNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());
            mTotalCounters = FNManager.getInstance().readCounters(transaction,true);

            FnMarkingStatus info = new FnMarkingStatus(mKKMInfo);
            int res=info.read(transaction, bb);
            if (res != Errors.NO_ERROR) return res;
            mNumUnsentMarkNotify = info.mNumMarkingNotifyUnsent;
            if (read(transaction, bb)!= Errors.NO_ERROR) return transaction.getLastError();

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),5);
            return Errors.NO_ERROR;

        } finally {
            BufferFactory.release(bb);
        }
    }

    public int read(Transaction transaction, ByteBuffer bb) {
        transaction.write(FNCommandsE.GET_SHIFT_STATUS);
        int res = transaction.read(bb);
        if (res != Errors.NO_ERROR) return res;

        mIsOpen = ((bb.get() & 0xFF) != 0);
        mShiftNumber = readUint16LE(bb);
        mLastDocNumber = readUint16LE(bb);
        mWhenOpen = Settings.getInstance().getWhenShiftOpen(mKKMInfo.getFNNumber());
        return res;
    }


    @Override
    public String onKey(String key) {
    	return super.onKey(key);
    }

    public static final Creator<ShiftEx2> CREATOR = new Creator<ShiftEx2>() {
        @Override
        public ShiftEx2 createFromParcel(Parcel p) {
            p.readString();
            return new ShiftEx2(p);
        }

        @Override
        public ShiftEx2[] newArray(int arg0) {
            return null;
        }
    };

	public void setUpdateOKPKeysResult(String value) {
		mOKPUpdateResult = value;
	}
    
}
