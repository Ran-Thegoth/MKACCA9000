package rs.fncore2.fn.common;

import android.os.Parcel;
import rs.fncore.Const;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;


public class SellOrderExBase extends SellOrder  {

    protected KKMInfoExBase mKKMInfo;

    protected SellOrderExBase(Parcel p) {
        super();
        readFromParcel(p);
    }

    public SellOrderExBase(KKMInfoExBase info, SellOrder order) {
        super();
        Utils.readFromParcel(this, Utils.writeToParcel(order));
        mKKMInfo = info;
        mShiftNumber = mKKMInfo.getShift().getNumber();
        mFnsUrl = mKKMInfo.getFNSUrl();
        mSenderEmail = mKKMInfo.getSenderEmail();
    }


    public String getPF(String header, String item, String footer, String footerEx) {
        return getPF(header, item, footer, footerEx, null);
    }

    public String getPF(String header, String item, String footer, String footerEx,
                        KKMInfoExBase info) {
        if (info != null) mKKMInfo = info;

        header = PrintHelper.loadTemplate(header, "sale_header");
        item = PrintHelper.loadTemplate(item, "sale_item");
        footer = PrintHelper.loadTemplate(footer, "sale_footer");
        if(footerEx == null) footerEx = Const.EMPTY_STRING;
        footer = footer.replace("$footerEx$", footerEx);
        String s = PrintHelper.processTemplate(header, this);

        for (SellItem i : getItems())
            s += PrintHelper.processTemplate(item, i);
        s += PrintHelper.processTemplate(footer, this);
        return s;
    }


}
