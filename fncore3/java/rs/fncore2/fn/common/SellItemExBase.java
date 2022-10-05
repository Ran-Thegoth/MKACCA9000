package rs.fncore2.fn.common;

import java.math.BigDecimal;

import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.VatE;
import rs.utils.Utils;

public class SellItemExBase extends SellItem {


    public SellItemExBase() {
    }

    public SellItemExBase(SellItem src) {
        this();
        Utils.readFromParcel(this, Utils.writeToParcel(src));
    }

    public SellItemExBase(String name, BigDecimal qtty, BigDecimal price, VatE vat) {
        super(name, qtty, price, vat);
    }

    public SellItemExBase(String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price,
                          VatE vat) {
        super(name, qtty, measure, price, vat);
    }

    public SellItemExBase(SellItemTypeE type, ItemPaymentTypeE paymentType,
                          String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price, VatE vat) {
        super(type, paymentType, name, qtty, measure, price, vat);
    }

}
