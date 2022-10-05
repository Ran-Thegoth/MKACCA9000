package rs.fncore2.fn.common;

import rs.fncore.data.OismStatistic;
import rs.utils.Utils;

public class OISMStatisticExBase extends OismStatistic {

    public OISMStatisticExBase() {
        super();
    }

    public OISMStatisticExBase (OismStatistic src){
        this();
        Utils.readFromParcel(this, Utils.writeToParcel(src));
    }

    public void cloneTo(OismStatistic dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public void setUnsentDocumentCount(int value) {
        mUnsentCount = value;
    }
}
