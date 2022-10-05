package rs.fncore2.fn;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import rs.fncore.Errors;
import rs.fncore.data.FNCounters;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.fn.storage.Transaction;

public class FNManager {


    public static final String FN_CHANGED ="FN changed";
    public static final String FN_READY ="FN ready";

    private final PropertyChangeSupport mFNChanged;
    private final PropertyChangeSupport mFNReady;

    private volatile FNBaseI mFN;
    private Object mFNLockObject = new Object();

    private FNManager(){
        mFNChanged = new PropertyChangeSupport(this);
        mFNReady = new PropertyChangeSupport(this);
    }

    public int setFN(FNBaseI fn) {
        synchronized (mFNLockObject) {
            int res= Errors.NO_ERROR;

            if (mFN == fn) return res;

            if ((mFN = fn)!=null){
                if (res == Errors.NO_ERROR) {
                    notifyFNReady();
                }
            }

            mFNChanged.firePropertyChange(FN_CHANGED, null, mFN);
            return res;
        }
    }

    public void emptyFN(){
        setFN(null);
    }
    public FNBaseI getFN(){
        return mFN;
    }

    public String getStorageName(){
        synchronized (mFNLockObject) {
            if (mFN !=null){
                return mFN.getStorage().getClass().getSimpleName();
            }
            return "no FN";
        }
    }

    public FNBaseI getReadyFN() throws InterruptedException {
        synchronized (mFNLockObject) {
            waitFNReady();
            return mFN;
        }
    }

    public void notifyFNReady(){
        synchronized (mFNLockObject) {
            if (!isFNReady()) return;
            mFNLockObject.notifyAll();
            mFNReady.firePropertyChange(FN_READY, null, mFN);
        }
    }

    public void waitFNReady() throws InterruptedException {
        waitFNReady(0);
    }

    public boolean waitFNReady(long mills) throws InterruptedException {
        synchronized (mFNLockObject){
            if (!isFNReady()) {
                mFNLockObject.wait(mills);
            }
            return isFNReady();
        }
    }

    public boolean isFNReady(){
        synchronized (mFNLockObject) {
            if (mFN != null && mFN.getStorage().isReady()) return true;
            return false;
        }
    }

    //region observer
    public void addFNChangedListener(PropertyChangeListener pcl) {
        mFNChanged.addPropertyChangeListener(pcl);
    }

    public void removeFNChangedListener(PropertyChangeListener pcl) {
        mFNChanged.removePropertyChangeListener(pcl);
    }

    public void addFNReadyListener(PropertyChangeListener pcl) {
        mFNReady.addPropertyChangeListener(pcl);
    }

    public void removeFNReadyListener(PropertyChangeListener pcl) {
        mFNReady.removePropertyChangeListener(pcl);
    }
    //endregion
    //region singleton
    private static class LazyHolder {
        static final FNManager INSTANCE = new FNManager();
    }

    public static FNManager getInstance() {
        return LazyHolder.INSTANCE;
    }
    //endregion

	public FNCounters readCounters(Transaction transaction, boolean isTotal) {
		return mFN.getFnCounters(transaction,isTotal);
	}
}
