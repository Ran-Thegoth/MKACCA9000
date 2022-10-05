package rs.fncore2.data;

import rs.fncore.data.KKMInfo;
import rs.fncore.data.Location;
import rs.fncore.data.Signer;

public class SignerEx extends Signer {
    public SignerEx(KKMInfo info, Location loc) {
        mFnNumber = info.getFNNumber();
        mKkmNumber = info.getKKMNumber();
        mDeviceSerial = info.getKKMSerial();
        mFFDVersion = info.getFFDProtocolVersion();
        loc.cloneTo(getLocation());
        info.getOwner().cloneTo(mOwner);
    }
}
