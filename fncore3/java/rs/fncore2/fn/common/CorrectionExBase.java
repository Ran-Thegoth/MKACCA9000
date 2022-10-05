package rs.fncore2.fn.common;

import android.os.Parcel;

import rs.fncore.data.Correction;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;


public class CorrectionExBase extends Correction  {

    protected KKMInfoExBase mKKMInfo;

    public CorrectionExBase(Parcel p) {
        super();
        readFromParcel(p);
    }

    public CorrectionExBase(KKMInfoExBase info, Correction src) {
        Utils.readFromParcel(this, Utils.writeToParcel(src));
        mKKMInfo = info;
        mShiftNumber = info.getShift().getNumber();
    }

    public String getPF(String template) {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(template, "correction"),
                this);
    }

    public String getPF(String template, KKMInfoExBase info) {
        mKKMInfo = info;
        return getPF(template);
    }

}
