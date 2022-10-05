package rs.fncore2.fn.fn12.marking;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import rs.fncore.Errors;
import rs.fncore.data.KKMInfo;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.Transaction;
import rs.utils.Utils;

public class FnMarkingStatus {
    KKMInfo mKkmInfo;

    enum CheckStateE{
        TABLE_OVERLOADED,
        NO_CODE_IN_CHECK,
        CODE_TRANSFERRED_IN_B1_CMD,
        MARKNG_REQUEST_CREATED_B5_CMD,
        RECEIVED_AND_STORED_ANSWER_B6_CMD
    }

    enum MarkingInfoStateE{
        NOT_CREATING,
        STARTED,
        BLOCKED_FINISHED_SPACE
    }

    enum AllowedCmdsE {
        UNKNOWN(0),
        ALLOWED_B1_CMD(1 << 0),
        ALLOWED_B2_CMD(1 << 1),
        ALLOWED_B3_CMD(1 << 2),
        ALLOWED_B5_CMD(1 << 3),
        ALLOWED_B6_CMD(1 << 4),
        ALLOWED_B7_1_CMD(1 << 5),
        ALLOWED_B7_2_CMD(1 << 6),
        ALLOWED_B7_3_CMD(1 << 7);

        public final byte bVal;

        private AllowedCmdsE(int value) {
            this.bVal = (byte) value;
        }

        public static AllowedCmdsE fromByte(byte number) {
            for (AllowedCmdsE val : values()) {
                if (val.bVal == number) {
                    return val;
                }
            }
            return UNKNOWN;
        }

        public static byte toByteArray(Iterable<AllowedCmdsE> modes) {
            byte b = 0;
            for (AllowedCmdsE mode : modes)
                b |= mode.bVal;
            return b;
        }

        public static Set<AllowedCmdsE> fromByteArray(byte val) {
            Set<AllowedCmdsE> result = new HashSet<>();
            for (AllowedCmdsE mode : values()) {
                if (mode == UNKNOWN) continue;
                if ((val & mode.bVal) == mode.bVal)
                    result.add(mode);
            }
            return result;
        }
    }

    enum MarkingCodeStoreSpaceE{
        LESS_50,
        FROM_50_TO_80,
        FROM_80_TO_90,
        MORE_90,
        FULL
    }

    public CheckStateE mCodeCheckState;
    public MarkingInfoStateE mMarkingInfoState;
    public Set<AllowedCmdsE> mAllowedCmds;
    public int mNumSavedMarkingCodes;
    public int mNumNotifyMarkingCodes;
    public MarkingCodeStoreSpaceE mMarkingCodesStoreSpace;
    public int mNumMarkingNotifyUnsent;

    public FnMarkingStatus(KKMInfo kkmInfo){
        mKkmInfo=kkmInfo;
    }

    public int read(Transaction transaction, ByteBuffer bb){
        if (!mKkmInfo.isMarkingGoods()) return Errors.NO_ERROR;

        transaction.write(FNCommandsE.GET_FN_MARKING_STATUS);
        int res=transaction.read(bb);
        if (res != Errors.NO_ERROR) return res;

        mCodeCheckState = CheckStateE.values()[bb.get()];
        mMarkingInfoState = MarkingInfoStateE.values()[bb.get()];
        mAllowedCmds = AllowedCmdsE.fromByteArray(bb.get());
        mNumSavedMarkingCodes = bb.get();
        mNumNotifyMarkingCodes=bb.get();
        mMarkingCodesStoreSpace=MarkingCodeStoreSpaceE.values()[bb.get()];
        mNumMarkingNotifyUnsent= Utils.readUint16LE(bb);

        return Errors.NO_ERROR;
    }
}
