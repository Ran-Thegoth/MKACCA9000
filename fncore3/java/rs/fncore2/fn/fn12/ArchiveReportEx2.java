package rs.fncore2.fn.fn12;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.OU;
import rs.fncore2.FNCore;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.ArchiveReportExBase;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;

class ArchiveReportEx2 extends ArchiveReportExBase {


    public ArchiveReportEx2(Parcel p) {
        super(p);
    }

    public ArchiveReportEx2(KKMInfoEx2 info) {
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

    public int write(Transaction transaction, OU operator) {
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            add(FZ54Tag.T1009_TRANSACTION_ADDR, mLocation.getAddress());
            add(FZ54Tag.T1187_TRANSACTION_PLACE, mLocation.getPlace());
            add(FZ54Tag.T1048_OWNER_NAME, mKKMInfo.getOwner().getName());
            mTotalCounters = FNManager.getInstance().readCounters(transaction,true);
            if (!OU.EMPTY_INN.equals(operator.getINN())) add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            if (!transaction.write(FNCommandsE.START_CLOSE_FISCAL_MODE).check()) return transaction.getLastError();

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check()) return transaction.getLastError();
            }

            String s = mKKMInfo.getKKMNumber();
            Calendar now = Calendar.getInstance();
            while (s.length() < 20) s = " " + s;
            transaction.write(FNCommandsE.COMMIT_CLOSE_FISCAL_MODE, now, s);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		6,null);
            return mKKMInfo.read(transaction);
        } finally {
            BufferFactory.release(bb);
        }
    }

    public static final Creator<ArchiveReportEx2> CREATOR = new Creator<ArchiveReportEx2>() {

        @Override
        public ArchiveReportEx2 createFromParcel(Parcel p) {
            p.readString();
            return new ArchiveReportEx2(p);
        }

        @Override
        public ArchiveReportEx2[] newArray(int arg0) {
            return null;
        }

    };
}
