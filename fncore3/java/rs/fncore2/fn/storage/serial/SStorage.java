package rs.fncore2.fn.storage.serial;

import java.io.FileDescriptor;

import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.UrovoUtils;
import rs.log.Logger;

public class SStorage implements StorageI {

    static {
        System.loadLibrary("fnserial");
    }

    private volatile Transaction mActive=null;
    private volatile FileDescriptor mFD;
    private Object mSyncTransaction = new Object();
    private Object mReadyTransaction = new Object();

    public SStorage() {

        synchronized (mSyncTransaction) {

            mFD = openFd(UrovoUtils.getUART());
            if (mFD == null) return;

            try {
                mActive = open();

                for (int i = 0; i < 10; i++) {
                    if (mActive.write(FNCommandsE.GET_FN_STATUS).check()) {
                        Logger.i("SAStorage initialized and ready, cycle %s", i);
                        break;
                    }
                }
            } finally {
                mActive.close();

                mSyncTransaction.notify();

                synchronized (mReadyTransaction) {
                    mReadyTransaction.notifyAll();
                }
            }
        }
    }

    public Transaction open() {
        synchronized (mSyncTransaction) {
            while(mActive !=null) {
                try {
                	
                    mSyncTransaction.wait();
                } catch (InterruptedException ie) {
                    return new STransaction(null, this);
                }
            }
            mActive = new STransaction(mFD, this);
            return mActive;
        }
    }

    public void openExisting(Transaction transaction) {
        synchronized (mSyncTransaction) {
            while(mActive !=null) {
                try {
                    mSyncTransaction.wait();
                } catch (InterruptedException ie) {
                    return;
                }
            }

            mActive = transaction;
        }
    }

    public void close() {
        synchronized (mSyncTransaction) {
            if (mFD != null) {
                closeFd(mFD);
                mFD = null;
            }
            mSyncTransaction.notify();
        }
    }

    protected void finalize() throws Throwable {
        close();
    }

    private native FileDescriptor openFd(int deviceId);
    private native void closeFd(FileDescriptor fd);

    public void release(Transaction transaction) {
        synchronized (mSyncTransaction) {
            if (mActive == transaction) {
                mActive = null;
                mSyncTransaction.notify();
            }
        }
    }

    @Override
    public boolean isReady() {
        return mFD != null;
    }

    @Override
    public boolean isBusy(){
        return mActive!=null;
    }

    @Override
    public void waitReady() throws InterruptedException {
        synchronized (mReadyTransaction){
            while (!isReady()){
                mReadyTransaction.wait();
            }
        }
    }

    @Override
    public String toString() {
        return "SAStorage@" + mFD;
    }
}
