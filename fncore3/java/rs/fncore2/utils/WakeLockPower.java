package rs.fncore2.utils;

import android.content.Context;
import android.os.PowerManager;

import java.util.concurrent.atomic.AtomicInteger;

import rs.fncore2.FNCore;

public class WakeLockPower {

    private final PowerManager.WakeLock mWakeLock;
    private final String mName;
    private static final AtomicInteger counter=new AtomicInteger(0);

    public WakeLockPower(String name){
        if(name==null) name="rs.fncore.XXService "+counter.getAndIncrement();
        mName =name;

        PowerManager pm = (PowerManager) FNCore.getInstance().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mName);
    }

    public synchronized void acquireWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
    }

    public synchronized void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
