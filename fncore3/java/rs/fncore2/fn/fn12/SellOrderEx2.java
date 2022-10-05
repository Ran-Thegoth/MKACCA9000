package rs.fncore2.fn.fn12;

import static rs.fncore2.fn.fn12.MarkingCodeEx2.MARKING_EXTENDED_TAGS;
import static rs.fncore2.fn.fn12.MarkingCodeEx2.isAllMarkingItemsPositiveChecked;
import static rs.utils.Utils.readUint16LE;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Calendar;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.OU;
import rs.fncore.data.Payment;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.data.SignerEx;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.SellOrderExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

@SuppressLint({"SimpleDateFormat", "DefaultLocale"})
class SellOrderEx2 extends SellOrderExBase {

    protected SellOrderEx2(Parcel p) {
        super(p);
    }

    public SellOrderEx2(KKMInfoEx2 info, SellOrder orderSrc) {
        super(info, orderSrc);

        add(FZ54Tag.T1048_OWNER_NAME, info.getOwner().getName());
        add(FZ54Tag.T1060_FNS_URL, info.getFNSUrl());
        if (info.isAutomatedMode()) {
            String s = info.getAutomateNumber();
            while (s.length() < 20)
                s = " " + s;
            add(FZ54Tag.T1036_AUTOMAT_NO, s);
        }

        if (getLocation().getAddress().isEmpty())
            getLocation().setAddress(mKKMInfo.getLocation().getAddress());
        if (getLocation().getPlace().isEmpty())
            getLocation().setPlace(mKKMInfo.getLocation().getPlace());

        for (int i = 0; i< mItems.size(); i++) {
            SellItemEx2 newItem = new SellItemEx2(mKKMInfo, mItems.get(i));
            mItems.set(i, newItem);
        }
    }

    public void cloneTo(SellOrder dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public int write(Transaction transaction, OU operator, boolean cashControlFlag) {
        BigDecimal totalSum = getTotalSum();
        BigDecimal totalPayments = getTotalPayments();
        BigDecimal oneCent = new BigDecimal(0.01);
        Payment cash = getPaymentByType(PaymentTypeE.CASH);
        
        if (totalSum.subtract(totalPayments).compareTo(oneCent) == 1) {
            Logger.i("Paid sum too low: %s > %s", totalSum, totalPayments);
            return Errors.SUM_MISMATCH;
        }

        if (totalPayments.subtract(totalSum).compareTo(oneCent) == 1) {
            if (cash != null) {
                BigDecimal refund = getTotalPayments().subtract(getTotalSum());

                if (refund.compareTo(cash.getValue()) == 1) {
                    Logger.i("Refund more than cash received:  %s > %s", refund, cash.getValue());
                    return Errors.SUM_MISMATCH;

                } else {
                    cash.setValue(cash.getValue().subtract(refund));
                    setRefund(refund);
                }
            } else {
                Logger.i("Paid sum mismatch: %s != %s", totalSum, totalPayments);
                return Errors.SUM_MISMATCH;
            }
        }

        if (cashControlFlag && (cash != null)) {
            double cashBoxValue = Settings.getInstance().getCashRest(mKKMInfo.getFNNumber());
            double cashReturnValue = cash.getValue().doubleValue();
        	
            if ((mType == OrderTypeE.OUTCOME) || (mType == OrderTypeE.RETURN_INCOME)) {
                if (cashReturnValue > cashBoxValue) {
                    Logger.w("Outcome cash value: " + cashReturnValue + " not in cashbox: " + cashBoxValue);
                    return Errors.NO_CASH;
                }
                cashBoxValue -= cashReturnValue;
            }
            else 
            	cashBoxValue += cashReturnValue;
            Settings.getInstance().setCashRest(mKKMInfo.getFNNumber(), cashBoxValue);
        }

        Calendar now = Calendar.getInstance();
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            if (operator == null) operator = mKKMInfo.signature().operator();
            add(FZ54Tag.T1021_CASHIER_NAME, operator.getName());
            if (!OU.EMPTY_INN.equals(operator.getINN()))
                add(FZ54Tag.T1203_CASHIER_INN, operator.getINN(false));
            else remove(FZ54Tag.T1203_CASHIER_INN);

            if (haveMarkingItems()){
                add(FZ54Tag.T2107_MARKING_ITEM_CHECK_RES, isAllMarkingItemsPositiveChecked(mItems)?(byte)0:
                        (byte)1);
            }

            if (!transaction.write(FNCommandsE.START_BILL, now).check())
                return transaction.getLastError();

            byte[][] data = packToFN();
            for (byte[] b : data) {
                if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, b).check())
                    return transaction.getLastError();
            }

            for (SellItem item : mItems) {
                SellItemEx2 exItem=(SellItemEx2)item;
                if (exItem.write(transaction)!=Errors.NO_ERROR){
                    return transaction.getLastError();
                }
            }

            if (haveMarkingItems()) {
                byte[] extMarkingData = packToTlvList(MARKING_EXTENDED_TAGS);
                if (extMarkingData.length != 0) {
                    if (!transaction.write(FNCommandsE.MARKING_CODE_ADD_REQUEST_DATA,
                            MarkingCodeEx2.addRequestDataCommandParamE.ExtendedData.bVal,
                            extMarkingData).check()) {
                        return transaction.getLastError();
                    }
                }
            }

            transaction.write(FNCommandsE.COMMIT_BILL, now, getType().bVal, getTotalSum());
            if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

            mBillNumber = readUint16LE(bb);
            sign(bb, new SignerEx(mKKMInfo, getLocation()), operator, now.getTimeInMillis());

            FNCore.getInstance().getDB().storeDocument(mKKMInfo.getFNNumber(),signature().getFdNumber(),signature().signDate(),3);
            return transaction.getLastError();
        } finally {
            BufferFactory.release(bb);
        }
    }

    public static final Parcelable.Creator<SellOrderEx2> CREATOR = new Parcelable.Creator<SellOrderEx2>() {
        @Override
        public SellOrderEx2 createFromParcel(Parcel p) {
            p.readString();
            return new SellOrderEx2(p);
        }

        @Override
        public SellOrderEx2[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };
}
