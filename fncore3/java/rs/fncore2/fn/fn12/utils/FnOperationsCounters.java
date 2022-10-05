package rs.fncore2.fn.fn12.utils;

import java.nio.ByteBuffer;

import rs.fncore.FZ54Tag;
import rs.fncore.data.Tag;
import rs.utils.Utils;

public class FnOperationsCounters {
    public final int shiftNumber;
    public final long totalBillsAndBSONumber;
    public final long incomeBillsNumber;
    public final long returnIncomeBillsNumber;
    public final long outcomeBillsNumber;
    public final long returnOutcomeBillsNumber;
    public final long totalBSONumber;
    public final long incomeCorrectionBillsNumber;
    public final long returnIncomeCorrectionBillsNumber;
    public final long outcomeCorrectionBillsNumber;
    public final long returnOutcomeCorrectionBillsNumber;

    public enum FnOperationCountersParamE {
        CurrentShift(0),
        TotalFnCounters(1)
        ;

        public final byte bVal;
        private FnOperationCountersParamE(int val) {
            this.bVal = (byte)val;
        }
    }

    public FnOperationsCounters(ByteBuffer bb){
        shiftNumber = Utils.readUint16LE(bb);
        totalBillsAndBSONumber = Utils.readUint32LE(bb);
        incomeBillsNumber = Utils.readUint32LE(bb);
        returnIncomeBillsNumber = Utils.readUint32LE(bb);
        outcomeBillsNumber = Utils.readUint32LE(bb);
        returnOutcomeBillsNumber = Utils.readUint32LE(bb);
        totalBSONumber = Utils.readUint32LE(bb);
        incomeCorrectionBillsNumber = Utils.readUint32LE(bb);
        returnIncomeCorrectionBillsNumber = Utils.readUint32LE(bb);
        outcomeCorrectionBillsNumber = Utils.readUint32LE(bb);
        returnOutcomeCorrectionBillsNumber = Utils.readUint32LE(bb);
    }

    public Tag pack(){
        Tag tlv = new Tag();
        tlv.add(FZ54Tag.T1134_TOTAL_BILLS_AND_BSO, totalBillsAndBSONumber);
        //TODO : implement may be if needed?
//        tlv.put(FZ54Tag.T1129_INCOME_COUNTERS_TLV, new Tag(incomeBillsNumber));
//        tlv.put(FZ54Tag.T1130_RETURN_INCOME_COUNTERS_TLV, new Tag(returnIncomeBillsNumber));
//        tlv.put(FZ54Tag.T1131_OUTCOME_COUNTERS_TLVM, new Tag(totalBillsAndBSONumber));
//        tlv.put(FZ54Tag.T1132_RETURN_OUTCOME_COUNTERS_TLV, new Tag(totalBillsAndBSONumber));
//        tlv.put(FZ54Tag.T1133_CORRECTION_COUNTERS_TLV, new Tag(totalBillsAndBSONumber));

        return tlv;
    }
}
