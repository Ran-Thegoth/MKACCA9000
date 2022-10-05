package rs.fncore2.fn.fn1_05;

import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.OU;
import rs.fncore2.FNCore;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.common.ArchiveReportExBase;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;

class ArchiveReportEx extends ArchiveReportExBase {

    public ArchiveReportEx(Parcel p) {
        super(p);
    }

    public ArchiveReportEx(KKMInfoEx info) {
        super(info);
        add(FZ54Tag.T1048_OWNER_NAME, info.getOwner().getName());
        if (info.getFFDProtocolVersion() != FFDVersionE.VER_10)
            add(FZ54Tag.T1209_FFD_VERSION, info.getFFDProtocolVersion().bVal);
        add(FZ54Tag.T1038_SHIFT_NO, info.getShift().getNumber());
    }

    public int write(Transaction transaction, OU operator) {
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            add(FZ54Tag.T1009_TRANSACTION_ADDR, mLocation.getAddress());
            add(FZ54Tag.T1187_TRANSACTION_PLACE, mLocation.getPlace());

            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));

            if (getLocation().getAddress().isEmpty())
                getLocation().setAddress(mKKMInfo.getLocation().getAddress());
            if (getLocation().getPlace().isEmpty())
                getLocation().setPlace(mKKMInfo.getLocation().getPlace());

            if (!transaction.write(FNCommandsE.START_CLOSE_FISCAL_MODE).check())
                return transaction.getLastError();

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            String s = mKKMInfo.getKKMNumber();
            Calendar now = Calendar.getInstance();
            while (s.length() < 20) s = " " + s;
            transaction.write(FNCommandsE.COMMIT_CLOSE_FISCAL_MODE, now, s);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		6);
            return mKKMInfo.read(transaction);
        } finally {
            BufferFactory.release(bb);
        }
    }

    public static final Creator<ArchiveReportEx> CREATOR = new Creator<ArchiveReportEx>() {

        @Override
        public ArchiveReportEx createFromParcel(Parcel p) {
            p.readString();
            return new ArchiveReportEx(p);
        }

        @Override
        public ArchiveReportEx[] newArray(int arg0) {
            return null;
        }

    };
}
