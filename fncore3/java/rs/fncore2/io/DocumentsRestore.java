package rs.fncore2.io;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.fncore2.utils.WakeLockPower;
import rs.log.Logger;

public class DocumentsRestore extends BaseThread implements PropertyChangeListener {

    private Object mStorageReadySync = new Object();
    private volatile boolean mNeedToCheckNewStorage = true;

    FNManager mFNManager = FNManager.getInstance();

    public DocumentsRestore(){
        setName(this.getClass().getSimpleName());

        mFNManager.addFNReadyListener(this);
        mFNManager.addFNChangedListener(this);

        start();
    }

    protected void unblockWait(){
        synchronized (mStorageReadySync) {
            mStorageReadySync.notifyAll();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(FNManager.FN_READY)){
            synchronized (mStorageReadySync){
                mNeedToCheckNewStorage =true;
                mStorageReadySync.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        WakeLockPower mWakeLockPower =  new WakeLockPower(this.getClass().getSimpleName());
        while (!isStopped) {
            try {
                synchronized (mStorageReadySync) {
                    while (!isStopped && !mNeedToCheckNewStorage) {
                        mStorageReadySync.wait();
                    }
                }
                if (mNeedToCheckNewStorage) {
                    mWakeLockPower.acquireWakeLock();
                    testDocumentsIntegrity();
                }
                mNeedToCheckNewStorage = false;
            } catch (InterruptedException ie) {
                break;
            } catch (Exception e) {
                Logger.e(e,"DocumentsRestore execute error");
            } finally {
                mWakeLockPower.releaseWakeLock();
            }
        }
        mFNManager.removeFNChangedListener(this);
        mFNManager.removeFNReadyListener(this);
        Logger.i("DocumentsRestore ended");
    }

    //TODO: fix
//    private Document readDocumentFromFN(Storage storage, long documentNumber) {
//        ByteBuffer bb = BufferFactory.allocateDocument();
//        try(Transaction transaction = storage.open()) {
//            if (transaction.write(FNCommandsE.FIND_ARCHIVE_BILL,
//                    (int) documentNumber).getLastError() == Const.Errors.NO_ERROR) {
//                if (transaction.read(bb) != Const.Errors.NO_ERROR) return null;
//                int docType = bb.get();
//                int readyOFD = bb.get();
//                int docLen = 0;
//                if (docType == FNDocumentsE.KKT_REGISTRATION_REPORT.value) {
//                    docLen = 48;
//                } else if (docType == FNDocumentsE.OPEN_SHIFT_REPORT.value) {
//                    docLen = 15;
//                } else if (docType == FNDocumentsE.FISCAL_ORDER.value) {
//                    docLen = 19;
//                } else if (docType == FNDocumentsE.CLOSE_SHIFT_REPORT.value) {
//                    docLen = 15;
//                } else if (docType == FNDocumentsE.REPORT_OF_CURRENT_STATUS.value) {
//                    docLen = 20;
//                } else {
//                    throw new Exception("uknown document" + docType);
//                }
//
//                byte[] doc = new byte[docLen];
//                bb.get(doc);
//                //TODO: finish
//                if (doc[1] == 1) {
//                    doc[1] = 1;
//                }
//            }
//        } catch (Exception e) {
//            Logger.e(e,"Transaction error");
//        } finally {
//            BufferFactory.release(bb);
//        }
//        return null;
//    }

    private void testDocumentsIntegrity() {
    	long whenShiftOpen = -1;
    	long totalDocsInFN = mFNManager.getFN().getKKMInfo().getLastFNDocNumber();
    	long totalDocsInDB = FNCore.getInstance().getDB().getDocumentsCount();
    	if(totalDocsInFN != totalDocsInDB) {
    		ByteBuffer bb = BufferFactory.allocateRecord();
    		String fnsn = mFNManager.getFN().getKKMInfo().getFNNumber();
    		FNCore.getInstance().getDB().clear();
    		try (Transaction transaction = mFNManager.getFN().getStorage().open()) {
        		for(long i=totalDocsInFN;i>0;i--) {
        			if(transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_INFO, (int)i).getLastError() == Errors.NO_ERROR) {
        				transaction.read(bb);
        				long date = 0;
        				int type = bb.getShort();
        				while(true) {
        					if(transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_DATA).getLastError() != Errors.NO_ERROR)break;
        					if(transaction.read(bb) != Errors.NO_ERROR || bb.limit() == 0) break;
        					if(bb.getShort() == FZ54Tag.T1012_DATE_TIME) {
        						bb.getShort();
        						date = (bb.getInt() & 0xFFFFFFFF)*1000L;
        						break;
        					}
        				}
        				if(type == 5) whenShiftOpen = 0;
        				else if(type == 2 && whenShiftOpen < 0) {
        					whenShiftOpen = date;
        					Settings.getInstance().setWhenShiftOpen(fnsn, date);
        					mFNManager.getFN().getKKMInfo().getShift().setWhenOpen(date);
        				}
        				FNCore.getInstance().getDB().storeDocument(fnsn, i, date, type);
        			}
        		}
    			
    		} finally {
    			BufferFactory.release(bb);
    		}
    	}

/*        long totalDocsInFN = mFNManager.getFN().getKKMInfo().getLastFNDocNumber();
        long totalDocsInDB = FNCore.getInstance().getDB().getDocumentsCount();

        if (totalDocsInFN > totalDocsInDB && totalDocsInDB==0) {
            Logger.e("Got error in syn FN and DB documents, trying to fix it totalDocsInFN=%s, totalDocsInDB=%s",
                    totalDocsInFN, totalDocsInDB);
            totalDocsInFN=1;//TODO: apply for more documents
            for (int i = 1; i <= totalDocsInFN; i++) {
                try {
                    Thread.sleep(100); // to not overheat FN connection
                    Cursor c = FNCore.getInstance().getDB().getDocument(i);
                    if (c == null || c.getCount() == 0) {
                        throw new Exception("missing document");
                    }
                } catch (Exception e) {
                    Logger.e("Fixing document %s", i);
                    Document doc = mFNManager.getFN().readDocumentFromTLV(i);
                    if (doc!=null){
                        try {
                            Logger.i("restored doc: %s", doc.printTags().toString());
                            FNCore.getInstance().getDB().storeDocument(doc);
                        }
                        catch (Exception eStore){
                            Logger.e(eStore,"Error store document");
                        }
                    }
                }
            }
            Logger.e("end fix FN and DB documents");
        } */
    }
}
