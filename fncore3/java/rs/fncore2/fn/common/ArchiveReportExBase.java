package rs.fncore2.fn.common;

import android.os.Parcel;

import rs.fncore.data.ArchiveReport;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;


public class ArchiveReportExBase extends ArchiveReport  {
    protected KKMInfoExBase mKKMInfo;

    public void cloneTo(ArchiveReport dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public ArchiveReportExBase(Parcel p) {
        super();
        readFromParcel(p);
    }

    public ArchiveReportExBase(KKMInfoExBase info) {
        mKKMInfo = info;
        mIsAutomateMode = info.isAutomatedMode();
        mAutomateNumber = info.getAutomateNumber();
        mShiftNumber = info.getShift().getNumber();
        info.getLocation().cloneTo(mLocation);
    }

    public String getPF(String template, KKMInfoExBase info) {
        mKKMInfo = info;
        return getPF(template);
    }

    public String getPF(String template) {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(template, "archive"),
                this);
    }

}
