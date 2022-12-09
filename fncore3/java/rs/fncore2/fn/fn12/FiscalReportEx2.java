package rs.fncore2.fn.fn12;

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
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.FiscalReportExBase;
import rs.fncore2.fn.fn12.marking.FnMarkingStatus;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;

class FiscalReportEx2 extends FiscalReportExBase {

    protected FiscalReportEx2(Parcel p) {
        super(p);
    }

    public FiscalReportEx2(KKMInfo info) {
        super(info);
        if (info.getFFDProtocolVersion() != FFDVersionE.VER_10)
            add(FZ54Tag.T1209_FFD_VERSION, info.getFFDProtocolVersion().bVal);
    }

    public int write(Transaction transaction, OU operator) {
        if (mLocation.getAddress().isEmpty())
            getLocation().setAddress(mKKMInfo.getLocation().getAddress());
        if (getLocation().getPlace().isEmpty())
            getLocation().setPlace(mKKMInfo.getLocation().getPlace());
        
        mTotalCounters = FNManager.getInstance().readCounters(transaction,true);
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

            Calendar now = Calendar.getInstance();
            if (!transaction.write(FNCommandsE.START_FISCAL_REPORT, now).check())
                return transaction.getLastError();

            remove(FZ54Tag.T1209_FFD_VERSION);

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_FISCAL_REPORT);
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());
            Utils.readUint32LE(bb);
            Utils.readDate3(bb);

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		21,null);

            FnMarkingStatus info = new FnMarkingStatus(mKKMInfo);
            int res = info.read(transaction, bb);
            if (res != Errors.NO_ERROR) return res;
            mNumUnsentMarkNotify = info.mNumMarkingNotifyUnsent;

            return transaction.getLastError();

        } finally {
            BufferFactory.release(bb);
        }
    }


    public static final Creator<FiscalReportEx2> CREATOR = new Creator<FiscalReportEx2>() {

        @Override
        public FiscalReportEx2 createFromParcel(Parcel p) {
            p.readString();
            return new FiscalReportEx2(p);
        }

        @Override
        public FiscalReportEx2[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };

}
