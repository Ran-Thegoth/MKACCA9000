package rs.fncore2.fn.common;

import android.os.Parcel;
import rs.fncore.data.Shift;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;


public class ShiftExBase extends Shift  {

    protected KKMInfoExBase mKKMInfo;
    protected boolean mOfdTimeout;

    protected ShiftExBase(Parcel p) {
        super();
        readFromParcel(p);
    }

    public ShiftExBase(KKMInfoExBase info) {
        mKKMInfo = info;
        mFnWarnings = new FnWarnings(info.getFNWarnings().iVal);
        mFnRemainedDays = info.getFnRemainedDays();
    }

    public void cloneTo(Shift dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    @Override
    public void setWhenOpen(long when) {
        mWhenOpen = when;
    }

    public String getPF(String template, KKMInfoExBase info) {
        mKKMInfo = info;
        return getPF(template);
    }

    public String getPF(String template) {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(template, "shift"), this);
    }



    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        mSignature.operator().writeToParcel(p, flags);
        p.writeInt(mOfdTimeout?1:0);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mSignature.operator().readFromParcel(p);
        mOfdTimeout = p.readInt() != 0;
    }
}
