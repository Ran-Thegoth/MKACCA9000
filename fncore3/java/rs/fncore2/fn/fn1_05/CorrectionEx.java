package rs.fncore2.fn.fn1_05;

import static rs.utils.Utils.readUint16LE;

import android.annotation.SuppressLint;
import android.os.Parcel;

import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.Correction;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.OU;
import rs.fncore.data.SellItem;
import rs.fncore2.FNCore;
import rs.fncore2.PrintHelper;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.common.CorrectionExBase;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;

@SuppressLint("DefaultLocale")
class CorrectionEx extends CorrectionExBase {

    public CorrectionEx(Parcel p) {
        super(p);
    }

    public CorrectionEx(KKMInfoEx info, Correction corrSrc) {
        super(info, corrSrc);

        add(FZ54Tag.T1048_OWNER_NAME, info.getOwner().getName());
        if (info.getFFDProtocolVersion() != FFDVersionE.VER_10) {
            add(FZ54Tag.T1209_FFD_VERSION, info.getFFDProtocolVersion().bVal);
        }
        add(FZ54Tag.T1060_FNS_URL, info.getFNSUrl());
    }

    public void cloneTo(Correction dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public int write(Transaction transaction, OU operator) {
        if (getLocation().getAddress().isEmpty())
            getLocation().setAddress(mKKMInfo.getLocation().getAddress());
        if (getLocation().getPlace().isEmpty())
            getLocation().setPlace(mKKMInfo.getLocation().getPlace());
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            Calendar now = Calendar.getInstance();

            if (!transaction.write(FNCommandsE.START_CORRECTION_BILL, now).check())
                return transaction.getLastError();

            byte[][] b = packToFN();
            for (byte[] e : b) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, e).check())
                    return transaction.getLastError();
            }

            transaction.write(FNCommandsE.COMMIT_BILL, now, getOrderType().bVal, getSum());
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();
            mBillNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());
            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),
            		31);
            return Errors.NO_ERROR;
        } finally {
            BufferFactory.release(bb);
        }
    }

    public String getPF(String header, String item, String footer, String footerEx) {
        return getPF(header, item, footer, footerEx, null);
    }

    public String getPF(String header, String item, String footer, String footerEx,
                        KKMInfoExBase info) {
        if (info != null) mKKMInfo = info;

        header = PrintHelper.loadTemplate(header, "correction2_header");
        item = PrintHelper.loadTemplate(item, "sale_item");
        footer = PrintHelper.loadTemplate(footer, "correction2_footer");

        StringBuilder s = new StringBuilder(PrintHelper.processTemplate(header, this));

        for (SellItem i : getItems()) {
            s.append(PrintHelper.processTemplate(item, i));
        }

        s.append(PrintHelper.processTemplate(footer, this));
        if (footerEx != null && !footerEx.isEmpty()) s.append(PrintHelper.processTemplate(footerEx, this));
        return s.toString();
    }
    
    public static final Creator<CorrectionEx> CREATOR = new Creator<CorrectionEx>() {
        @Override
        public CorrectionEx createFromParcel(Parcel p) {
            p.readString();
            return new CorrectionEx(p);
        }

        @Override
        public CorrectionEx[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };
}
