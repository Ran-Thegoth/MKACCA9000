package rs.fncore2.fn.fn1_05;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.OU;
import rs.fncore2.FNCore;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.FiscalReportExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;

class FiscalReportEx extends FiscalReportExBase {

    protected FiscalReportEx(Parcel p) {
        super(p);
    }

    public FiscalReportEx(KKMInfo info) {
        super(info);
        if (info.getFFDProtocolVersion() != FFDVersionE.VER_10)
            add(FZ54Tag.T1209_FFD_VERSION, info.getFFDProtocolVersion().bVal);
        if (info.isOfflineMode())
            add(FZ54Tag.T1002_AUTONOMOUS_MODE, info.isOfflineMode());
    }

    public int write(Transaction transaction, OU operator) {
        if (getLocation().getAddress().isEmpty())
            getLocation().setAddress(mKKMInfo.getLocation().getAddress());
        if (getLocation().getPlace().isEmpty())
            getLocation().setPlace(mKKMInfo.getLocation().getPlace());

        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            transaction.write(FNCommandsE.GET_OFD_STATUS);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            mOfdStatistic.update(bb);
            if (operator == null)
                operator = mKKMInfo.signature().operator();
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            transaction.write(FNCommandsE.GET_SHIFT_STATUS);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            if (mIsShiftOpen)
                add(FZ54Tag.T1038_SHIFT_NO, mShiftNumber);

            Calendar now = Calendar.getInstance();
            if (!transaction.write(FNCommandsE.START_FISCAL_REPORT, now).check())
                return transaction.getLastError();
            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_FISCAL_REPORT);
            if (transaction.read(bb) == Errors.NO_ERROR) {
                sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());
                FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),
                		21);
            }
            return transaction.getLastError();

        } finally {
            BufferFactory.release(bb);
        }
    }

    public static final Creator<FiscalReportEx> CREATOR = new Creator<FiscalReportEx>() {

        @Override
        public FiscalReportEx createFromParcel(Parcel p) {
            p.readString();
            return new FiscalReportEx(p);
        }

        @Override
        public FiscalReportEx[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };
}
