package rs.fncore2.fn.common;

import static rs.utils.Utils.readUint16LE;
import static rs.utils.Utils.readUint32LE;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;


import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo;
import rs.fncore2.FNCore;
import rs.fncore2.PrintHelper;
import rs.fncore2.fn.fn12.FN2Commands;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

abstract public class KKMInfoExBase extends KKMInfo  {

    protected boolean mUnfinishedDocData;
    protected boolean mShiftIsOpen;

    public KKMInfoExBase() {
        super();
        mShift = new ShiftExBase(this);

        add(FZ54Tag.T1013_KKT_SERIAL_NO, FNCore.getInstance().getDeviceSerial());

        try {
            PackageInfo info = FNCore.getInstance().getPackageManager().getPackageInfo(FNCore.getInstance().getPackageName(), 0);
            mServiceVersion = info.versionName;

/*            File so = new File(info.applicationInfo.nativeLibraryDir);
            so = new File(so, "libfnserial.so");

            if (so.exists()) try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(mServiceVersion.getBytes());
                    byte[] buff = new byte[8129];
                    int read;

                    FileInputStream fs = new FileInputStream(so);
                    while ((read = fs.read(buff)) > 0) md.update(buff, 0, read);
                    fs.close();

                    buff = md.digest();
                    mServiceVersion = Utils.dump(buff, 0, buff.length);

                } catch (Exception e) {
                    Logger.e(e,"get libfnserial md5 exc");
                } */

        } catch (NameNotFoundException nfe) {
            Logger.e(nfe,"get version exc");
            mServiceVersion = "FFFFFFFFFFFF";
        }
    }

    public KKMInfoExBase(KKMInfo src) {
        this();
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        writeToParcelSavedStatus(p, flags);

    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        readFromParcelSavedStatus(p);
    }

    protected void readFromParcelSavedStatus(Parcel p){
        if (p.dataAvail() == 0) return;
        mUnfinishedDocData=p.readInt() != 0;
        mShiftIsOpen=p.readInt() != 0;
    }

    protected void writeToParcelSavedStatus(Parcel p, int flags){
        p.writeInt(mUnfinishedDocData?1:0);
        p.writeInt(mShiftIsOpen?1:0);
    }

    public void cloneTo(KKMInfo dest) {
        Utils.readFromParcel(dest, Utils.writeToParcel(this));
    }

    public int read(Transaction transaction){
        throw new RuntimeException("not implemented");
    }

    public void cloneSavedStatus(KKMInfoExBase dest) {
        Parcel p = Parcel.obtain();
        writeToParcelSavedStatus(p, 0);
        p.setDataPosition(0);
        dest.readFromParcelSavedStatus(p);
        p.recycle();
    }

    @Override
    public byte[][] packToFN() {
        add(FZ54Tag.T1009_TRANSACTION_ADDR, mLocation.getAddress());
        add(FZ54Tag.T1187_TRANSACTION_PLACE, mLocation.getPlace());
        return super.packToFN();
    }

    public int update(Transaction transaction, ByteBuffer buffer){
    	throw new RuntimeException("not implemented");
    }

    public static class FFDVersionInfo {
        public KKMInfo.FFDVersionE current;
        public KKMInfo.FFDVersionE supported;
    }

    public static FFDVersionInfo getFNFFDVersion(StorageI storage) {
        ByteBuffer bb = BufferFactory.allocateRecord();
        try (Transaction transaction = storage.open()) {
            return getFNFFDVersion(transaction, bb);
        } finally {
            BufferFactory.release(bb);
        }
    }

    protected static FFDVersionInfo getFNFFDVersion(Transaction transaction, ByteBuffer bb) {
         FFDVersionInfo res = new FFDVersionInfo();
        transaction.write(FNCommandsE.GET_FN_FFD);
        int result = transaction.read(bb);
        if (result == Errors.NO_ERROR) {
            res.current = KKMInfo.FFDVersionE.fromByte(bb.get());
            res.supported = KKMInfo.FFDVersionE.fromByte(bb.get());
        }
        else{
            res.current = FFDVersionE.UNKNOWN;
            res.supported = FFDVersionE.VER_105;
        }

        return res;
    }

    public int readBase(Transaction transaction, ByteBuffer bb) {
        mFnState = FNStateE.UNKNOWN;
        transaction.write(FNCommandsE.GET_FN_STATUS);
        if (transaction.read(bb) != Errors.NO_ERROR) return transaction.getLastError();

        mFnState = FNStateE.fromByte(bb.get());
        mUnfinishedDocType = bb.get() & 0xFF;
        mUnfinishedDocData = (bb.get() & 0xFF) != 0;
        mShiftIsOpen = (bb.get() & 0xFF) != 0;
        mFnWarnings = new FnWarnings(bb.get());
        long lastDocumentDate = Utils.readDate5(bb);
        byte[] number = new byte[16];
        bb.get(number);
        mFnNumber = new String(number);
        long lastDoc = readUint32LE(bb);
        updateLastDocumentInfo(lastDocumentDate, lastDoc);

        return transaction.getLastError();
    }

    public boolean hasUnfinished() {
        return mUnfinishedDocType != 0;
    }

    public int update(Transaction transaction) {
        return update(transaction, null);
    }

    public void setFailedMarkingCode(boolean val){
    }

    public boolean getFailedMarkingCode(){
        return false;
    }

    public void setForcedFailedMarkingCode(boolean val){
    }

    public boolean getForcedFailedMarkingCode(){
        return false;
    }

    public static Object readTag(Transaction transaction, int tagId, ByteBuffer buffer,
                             Class<?> clazz) {
        ByteBuffer bb = buffer;
        if (bb == null)
            bb = BufferFactory.allocateRecord();
        try {
            transaction.write(FNCommandsE.GET_FISCALIZATION_ARG, (short) (tagId & 0xFFFF));
            int error = transaction.read(bb);
            if (error == Errors.NO_ERROR) {
                bb.getShort();
                int size = readUint16LE(bb);
                if (clazz == byte.class)
                    return bb.get();
                if (clazz == boolean.class)
                    return bb.get() != (byte) 0;
                if (clazz == short.class)
                    return readUint16LE(bb);
                if (clazz == int.class)
                    return bb.getInt();
                if (clazz == String.class) {
                    byte[] b = new byte[size];
                    bb.get(b);
                    return new String(b, Const.ENCODING).trim();
                }
            }

        } finally {
            if (buffer == null) BufferFactory.release(bb);
        }
        return null;
    }

    protected void applyTag(Transaction transaction, int tagId, ByteBuffer buffer, Class<?> clazz) {
        ByteBuffer bb = buffer;
        if (bb == null)
            bb = BufferFactory.allocateRecord();
        try {
            transaction.write(FNCommandsE.GET_FISCALIZATION_ARG, (short) (tagId & 0xFFFF));
            int error = transaction.read(bb);
            if (error == Errors.NO_ERROR) {
                int tag = readUint16LE(bb);
                int size = readUint16LE(bb);
                if (clazz == byte.class)
                    add(tag, bb.get());
                else if (clazz == boolean.class)
                    add(tag, bb.get() != (byte) 0);
                else if (clazz == short.class)
                    add(tag, bb.getShort());
                else if (clazz == int.class)
                    add(tag, bb.getInt());
                else if (clazz == String.class) {
                    byte[] b = new byte[size];
                    bb.get(b);
                    add(tag, new String(b, Const.ENCODING).trim());
                }
            }

        } finally {
            if (buffer == null) BufferFactory.release(bb);
        }
    }

    public String getPF(String template) {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(template, "registration"),
                this);
    }
    @Override
    public ShiftExBase getShift() {
    	// TODO Auto-generated method stub
    	return (ShiftExBase)super.getShift();
    }
    
    protected int updateFnRemainedDays(Transaction transaction, ByteBuffer bb){
   		transaction.write(FNCommandsE.GET_FN_EXPIRE_INFO);
   		if (transaction.read(bb) != Errors.NO_ERROR)
   			return 0;
        FN2Commands.FnExpiriationInfo info = new FN2Commands.FnExpiriationInfo(bb);
        long diff = info.expireDate-System.currentTimeMillis();
        mFnRemainedDays = (int) (diff / Const.ONE_DAY);
        if(mFnRemainedDays < 0) mFnRemainedDays = 470;
        return Errors.NO_ERROR;
    }

}
