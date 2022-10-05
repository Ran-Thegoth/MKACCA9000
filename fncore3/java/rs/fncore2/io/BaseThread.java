package rs.fncore2.io;

public abstract class BaseThread extends Thread{

    protected volatile boolean isStopped;

    @Override
    public void start(){
        super.start();
        isStopped=false;
    }

    @Override
    public void interrupt(){
        isStopped=true;
        super.interrupt();
        unblockWait();
    }

    protected abstract void unblockWait();
}
