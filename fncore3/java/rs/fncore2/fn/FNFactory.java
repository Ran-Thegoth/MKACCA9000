package rs.fncore2.fn;

import rs.fncore.data.KKMInfo;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.fn12.FN2;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.log.Logger;


public class FNFactory {
    public static FNBaseI getFN(StorageI storage) {

        KKMInfoExBase.FFDVersionInfo ffdVerFN=KKMInfoExBase.getFNFFDVersion(storage);
        Logger.i("FN current FFD: %s, supported FFD: %s", ffdVerFN.current,ffdVerFN.supported);

        if (ffdVerFN.supported == KKMInfo.FFDVersionE.VER_12){
            return new FN2(storage);
        }
        else{
        	Logger.i("Checking FN 1.1 comaptibility");
        	try(Transaction transaction = storage.open()) {
        		if(!transaction.write(FNCommandsE.GET_FN_COUNTERS, (byte)0).check()) {
        			return new FNNotSupported(storage);
        		}
        			
        	}
            return new rs.fncore2.fn.fn1_05.FN(storage);
        }
    }
}
