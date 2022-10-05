package rs.fncore2.fn.fn12;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

import rs.fncore.data.FNCounters;

public class FnCountersEx2  extends FNCounters {
    public enum FnCountersParamE {
        CURRENT_SHIFT(0),
        TOTAL_FN_COUNTERS(1)
        ;

        public final byte bVal;
        private FnCountersParamE(int val) {
            this.bVal = (byte)val;
        }

        public static FnCountersParamE fromByte(byte number){
            for (FnCountersParamE val:values()){
                if (val.bVal == number){
                    return val;
                }
            }
            throw new InvalidParameterException("unknown value");
        }
    }

    public FnCountersEx2( ByteBuffer bb, boolean isTotal){
        super(bb, isTotal);
    } 
}
