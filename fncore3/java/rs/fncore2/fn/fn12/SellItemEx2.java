package rs.fncore2.fn.fn12;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.util.List;

import rs.fncore.FZ54Tag;
import rs.fncore.data.MarkingCode.ItemCheckResult2106;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.Tag;
import rs.fncore.data.VatE;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.common.SellItemExBase;
import rs.fncore2.fn.fn12.marking.MarkingCodeParam;
import rs.fncore2.fn.storage.Transaction;
import rs.utils.Utils;

class SellItemEx2 extends SellItemExBase {

    public SellItemEx2(KKMInfoExBase info, SellItem src) {
        Utils.readFromParcel(this, Utils.writeToParcel(src));
        mMarkingCode=new MarkingCodeEx2(info, mMarkingCode, this);
    }

    public SellItemEx2(String name, BigDecimal qtty, BigDecimal price, VatE vat) {
        super(name, qtty, price, vat);
    }

    public SellItemEx2(String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price,
                      VatE vat) {
        super(name, qtty, measure, price, vat);
    }

    public SellItemEx2(SellItemTypeE type, ItemPaymentTypeE paymentType,
                      String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price, VatE vat) {
        super(type, paymentType, name, qtty, measure, price, vat);
    }

    public void setMarkResult(ItemCheckResult2106 result) {
    	mMarkResult = result;
    }
    @Override
    public MarkingCodeEx2 getMarkingCode() {
        return (MarkingCodeEx2)mMarkingCode;
    }

    public void cloneTo(SellItem dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public int write(Transaction transaction) {
        if (getMarkingCode().isEmpty()) {
            Tag currTag= formatTag();
            if (!transaction.write(FNCommandsE.ADD_DOCUMENT_DATA, currTag).check()) {
                return transaction.getLastError();
            }
        }
        else{
            Tag currTag1=getTags(FZ54Tag.T2007_MARKING_ITEM_DATA, MarkingCodeEx2.MARK_REQUEST_TAGS_2007);
            if (!transaction.write(FNCommandsE.MARKING_CODE_ADD_REQUEST_DATA,
                    MarkingCodeEx2.addRequestDataCommandParamE.Notification.bVal,
                    currTag1).check()) {
                return transaction.getLastError();
            }

            Tag currTag2 = formatTag();
            if (!transaction.write(FNCommandsE.MARKING_CODE_ADD_REQUEST_DATA,
                    MarkingCodeEx2.addRequestDataCommandParamE.Data.bVal,
                    currTag2.pack(true)).check()) {
                return transaction.getLastError();
            }
        }
        return transaction.getLastError();
    }

    private Tag getTags(int resultTag, int [] requestedTags){
        Tag res = new Tag(resultTag);
        List<Tag> sellItemTags = formatTag().getChilds(requestedTags);
        List<Tag> markingTags = mMarkingCode.getChilds(requestedTags);
        res.add(markingTags);
        res.add(sellItemTags);
        return res;
    }

    @Override
    public Tag formatTag() {
        remove(FZ54Tag.T1197_ITEM_UNIT_NAME);
        remove(FZ54Tag.T1162_COMMODITY_NUMBER);
        add(FZ54Tag.T2108_MEASURE_AMOUNT_ITEM, mMeasure.bVal);
        if (!getMarkingCode().isEmpty()){
            Tag tlv = new Tag(FZ54Tag.T1163_GOODS_CODE);
            MarkingCodeParam.CodeTypesParamE param=getMarkingCode().getCodeTypeParam();
            tlv.add(param.tag, getMarkingCode().getItemIdentificator());
            add(tlv);
        }
        return super.formatTag();
    }

    public static final Parcelable.Creator<SellItemEx2> CREATOR = new Parcelable.Creator<SellItemEx2>() {
        @Override
        public SellItemEx2 createFromParcel(Parcel p) {
            return null;
        }

        @Override
        public SellItemEx2[] newArray(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    };
}
