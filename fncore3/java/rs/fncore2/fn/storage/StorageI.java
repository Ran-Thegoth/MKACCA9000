package rs.fncore2.fn.storage;

public interface StorageI {
     void release(Transaction transction);

     Transaction open();

     void close();

     boolean isReady();
     boolean isBusy();

     void waitReady() throws InterruptedException;

     void openExisting(Transaction transaction);
}
