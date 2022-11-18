package rs.fncore2.fn.fn12.marking;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore3.BuildConfig;
import rs.fncore2.FNCore;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.storage.Transaction;
import rs.utils.Utils;
import rs.log.Logger;

public class MarkingOfflineReport {

    private KKMInfoExBase mInfo;
    MarkingOfflineDocument [] mDocs;

    enum DownloadSubCmdE {
        GET_DOWNLOAD_STATUS,
        START_DOWNLOAD
    }



    public MarkingOfflineReport(KKMInfoExBase info){
        mInfo=info;
    }

    public int read(Transaction transaction, ByteBuffer bb){
        if (mInfo.isMarkingGoods()&&!mInfo.isOfflineMode()) return Errors.WRONG_FN_MODE;
        
        transaction.write(FNCommandsE.START_DOWNLOAD_MARKING_NOTIFY, (byte) DownloadSubCmdE.START_DOWNLOAD.ordinal());
        int res=transaction.read(bb);
        if (res != Errors.NO_ERROR) return res;

        int totalUnsentDocNum =Utils.readUint16LE(bb);
        Utils.readUint32LE(bb);
        Utils.readUint16LE(bb);
        Utils.readUint32LE(bb);
        mDocs=new MarkingOfflineDocument[totalUnsentDocNum];

        for (int i = 0; i< totalUnsentDocNum; i++){
            mDocs[i] = new MarkingOfflineDocument();
            res = mDocs[i].read(transaction, bb);
            if (res != Errors.NO_ERROR) return res;
        }
        return Errors.NO_ERROR;
    }

    public int confirm(Transaction transaction, ByteBuffer bb){
        if (mInfo.isMarkingGoods()&&!mInfo.isOfflineMode()) return Errors.WRONG_FN_MODE;

        transaction.write(FNCommandsE.CONFIRM_MARKING_NOTIFY, (byte) MarkingOfflineDocument.ConfirmSubCmdE.GET_PARAMETERS.ordinal());
        int res=transaction.read(bb);
        if (res != Errors.NO_ERROR) return res;

        int totalCountUnconfirmed = Utils.readUint16LE(bb);
        long currDocUnconfirmed = Utils.readUint32LE(bb);

        if (mDocs[0].getNumber()!=currDocUnconfirmed){
            return Errors.WRONG_DOC_NUM;
        }

        for (int i = 0; i< totalCountUnconfirmed; i++){
            res=mDocs[i].confirm(transaction, bb);
            if (res != Errors.NO_ERROR) return res;
        }
        return Errors.NO_ERROR;
    }

    private byte[] getFileData(){
    	ByteBuffer tmpBB = ByteBuffer.allocate(65535);
        int confirmedDocs=getConfirmedDocs();
        if (confirmedDocs==0) return null;

        tmpBB.clear();

        String name="Отчет о реализации маркированного товара";
        while (name.length()<66)name+=' ';
        tmpBB.put(name.getBytes(Const.ENCODING));

        String AppForDownload="FNCore2 "+ Logger.BUILD_VERSION+", buildType: "+(BuildConfig.DEBUG ? "debug": "release");
        while(AppForDownload.length()<256)AppForDownload+=' ';
        tmpBB.put(AppForDownload.getBytes(Const.ENCODING));

        String kktNumber=mInfo.getKKMNumber();
        while (kktNumber.length()<20)kktNumber+=' ';
        tmpBB.put(kktNumber.getBytes(Const.ENCODING));
        String fnSn = mInfo.getFNNumber();
        
        while (fnSn.length()<16) fnSn+=' ';
        tmpBB.put(fnSn.getBytes());

        tmpBB.put(mInfo.getFFDProtocolVersion().bVal);

        tmpBB.putInt((int)mDocs[0].getNumber());
        tmpBB.putInt((int)mDocs[confirmedDocs-1].getNumber());
        
        tmpBB.putInt(confirmedDocs);
        tmpBB.putInt(0);
        final int HEADER_SIZE=tmpBB.position();
        for(int i=0;i<confirmedDocs;i++) {
            byte [] data = mDocs[i].getData();
            if(tmpBB.position() + data.length > tmpBB.capacity()) {
            	ByteBuffer bb = ByteBuffer.allocate(tmpBB.position() + 65535);
            	bb.put(tmpBB.array(),0,tmpBB.position());
            	tmpBB = bb;
            }
            tmpBB.put(data);
        }

        CRC32 crc = new CRC32();
        crc.update(tmpBB.array(),HEADER_SIZE,tmpBB.position() - HEADER_SIZE);
        long crcFromCalc=crc.getValue();
        int sz = tmpBB.position();
        tmpBB.position(HEADER_SIZE-4);
        tmpBB.putInt((int)crcFromCalc);
        byte [] res = new byte[sz];
        System.arraycopy(tmpBB.array(), 0, res, 0, res.length);
        return res;
    }

    public int getConfirmedDocs(){
        for (int i=0;i<mDocs.length;i++) {
            if (!mDocs[i].isConfirmed()) return i;
        }
        return mDocs.length;
    }

    int getTotalSize(int numDocs){
        int length=0;
        for (int i=0;i<numDocs;i++) {
            length+=mDocs[i].getSize();
        }
        return length;
    }

    public void writeToFile(OutputStream fStream) throws IOException {
        byte[] data=getFileData();
        fStream.write(data);
    }

    public boolean writeToDB(){
        if (FNCore.getInstance() != null && FNCore.getInstance().getDB() != null) {
            byte[] data=getFileData();
            FNCore.getInstance().getDB().addReport(mDocs[0].getNumber(),mDocs[getConfirmedDocs()-1].getNumber(),data);
            return true;
        }
        else return false;
    }
}
