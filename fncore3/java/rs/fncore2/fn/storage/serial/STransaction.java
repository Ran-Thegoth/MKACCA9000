package rs.fncore2.fn.storage.serial;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import rs.fncore2.fn.storage.Transaction;
import rs.log.Logger;

class STransaction extends Transaction {

    private FileDescriptor mFD;
    private InputStream mIs;
    private OutputStream mOs;

    public STransaction(FileDescriptor fd, SStorage storage) {
        super(storage);
        mFD = fd;
        if (mFD != null) {
            mIs = new FileInputStream(mFD);
            mOs = new FileOutputStream(mFD);
        }
    }

    @Override
    public void close() {
        if (mFD != null) {
            try {
                mIs.close();
            } catch (Exception ioe) {}

            try {
                mOs.close();
            } catch (Exception ioe) {}
        }
        mFD = null;
        super.close();
    }

    @Override
    public int writeB(byte[] data, int offset, int size) {
        if (mFD == null || data == null) return 0;

        try {
            mOs.write(data, offset, size);
            return size;
        } catch (Exception ioe) {
            return -1;
        }
    }

    @Override
    public void flushReadBuffer(byte[] data){
        try {
            int dataJamSize= Math.min(data.length, mIs.available());
            mIs.read(data, 0,dataJamSize);
        }
        catch (Exception e) {
            Logger.e(e,"flushReadBuffer exc");
        }
    }

    @Override
    public int readB(byte[] data, int offset, int size, long timeout) {
        if (mFD == null || data == null || size == 0 ) return 0;

        try {
            return mIs.read(data, offset, size);
        } catch (Exception e) {
            Logger.e(e,"readB exc");
            return -1;
        }
    }
}
