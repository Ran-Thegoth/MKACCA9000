package rs.fncore2.fn.fn1_05;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;

import rs.fncore.FZ54Tag;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.Tag;
import rs.fncore.data.VatE;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.SellItemExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.utils.Utils;
import rs.log.Logger;

class SellItemEx extends SellItemExBase {

    public SellItemEx() {
    }

    public SellItemEx(SellItem src) {
        this();
        Utils.readFromParcel(this, Utils.writeToParcel(src));
    }

    public SellItemEx(String name, BigDecimal qtty, BigDecimal price, VatE vat) {
        super(name, qtty, price, vat);
    }

    public SellItemEx(String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price,
                          VatE vat) {
        super(name, qtty, measure, price, vat);
    }

    public SellItemEx(SellItemTypeE type, ItemPaymentTypeE paymentType,
                          String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price, VatE vat) {
        super(type, paymentType, name, qtty, measure, price, vat);
    }

    public int write(Transaction transaction) {
        if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, formatTag()).check()) {
            Logger.e("error write transaction %s", transaction.getLastError());
        }
        return transaction.getLastError();
    }

    @Override
    public Tag formatTag() {
        add(FZ54Tag.T1197_ITEM_UNIT_NAME, mMeasure.pName);
        if(!getMarkingCode().isEmpty()) {
        	
        }
        return super.formatTag();
    }

    public static final Parcelable.Creator<SellItemEx> CREATOR = new Parcelable.Creator<SellItemEx>() {
        @Override
        public SellItemEx createFromParcel(Parcel p) {
            return null;
        }

        @Override
        public SellItemEx[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };
}
