package rs.fncore2.fn.fn12;

import static rs.utils.Utils.readUint16LE;

import android.content.Context;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import rs.fncore.Const;
import rs.fncore.data.DocServerSettings;
import rs.fncore2.fn.FNManager;
import rs.fncore2.utils.BufferFactory;
import rs.fncore2.utils.UtilsCore;
import rs.utils.Utils;
import rs.log.Logger;

public class OKPSender {

    private static final byte[] SIGNATURE_OKP =
            {(byte) 0xDD, (byte) 0x80, (byte) 0xCA, (byte) 0xA1};
    private static final byte[] PROTOCOL_VERSION_OKP_S = {(byte) 0x82, (byte) 0xA1};
    private static final byte[] PROTOCOL_VERSION_OKP_P = {(byte) 0x00, (byte) 0x01};
    protected static final short MSG_FLAGS = (1 << 4) | (1 << 2);
    private static final boolean LOG_PRINT = true;

    public static byte [] getOkpResp(Context ctx, byte [] requestData, DocServerSettings okpServer){
        if (!UtilsCore.isConnected(ctx)) return null;
        String FNSN = FNManager.getInstance().getFN().getKKMInfo().getFNNumber();
        ByteBuffer tmpDataBuffer = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        try {
            byte[] dataOkp = prepareOkpDocument(tmpDataBuffer, requestData, FNSN);
            return sendPackToOKP(tmpDataBuffer, dataOkp, FNSN, okpServer);
        } finally{
            BufferFactory.release(tmpDataBuffer);
        }
    }

    private static byte [] prepareOkpDocument(ByteBuffer tmpBuffer, byte [] dataFN, String FNSN)  {
        tmpBuffer.clear();
        tmpBuffer.put(SIGNATURE_OKP);
        tmpBuffer.put(PROTOCOL_VERSION_OKP_S);
        tmpBuffer.put(PROTOCOL_VERSION_OKP_P);


        tmpBuffer.put(FNSN.getBytes(Const.ENCODING));
        tmpBuffer.putShort((short) (dataFN.length & 0xFFFF));
        tmpBuffer.putShort(MSG_FLAGS);
        tmpBuffer.putShort((short) 0);
        tmpBuffer.put(dataFN);

        byte[] outData = new byte[tmpBuffer.position()];
        System.arraycopy(tmpBuffer.array(), 0, outData, 0, outData.length);
        return outData;
    }

    public static byte[] sendPackToOKP(ByteBuffer tmpBuffer, byte[] pack, String FNSN, DocServerSettings mOFDSettings) {
        try {
            Logger.i("Соединение с ОФД ОКП %s : %s ", mOFDSettings.getServerAddress(), mOFDSettings.getServerPort());

            try (Socket socket = new Socket(mOFDSettings.getServerAddress(), mOFDSettings.getServerPort())) {
                socket.setSoTimeout(mOFDSettings.getServerTimeout() * 1000);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                os.write(pack);
                os.flush();

                if (LOG_PRINT) {
                    Logger.i(">> OFD OKP %s", Utils.dump(pack));
                }

                int read = is.read(tmpBuffer.array());
                if (read > (SIGNATURE_OKP.length + PROTOCOL_VERSION_OKP_S.length)) {
                    if (LOG_PRINT) {
                        Logger.i("OFD OKP << \n %s", Utils.dump(tmpBuffer.array(), 0, read));
                    }

                    tmpBuffer.limit(read);
                    tmpBuffer.position(0);

                    for (byte b : SIGNATURE_OKP) {
                        if (tmpBuffer.get() != b)
                            return null;
                    }

                    for (byte b : PROTOCOL_VERSION_OKP_S) {
                        if (tmpBuffer.get() != b)
                            return null;
                    }

                    tmpBuffer.getShort();
                    byte[] fn = new byte[16];
                    tmpBuffer.get(fn);

                    if (!new String(fn, Const.ENCODING).equals(FNSN)) return null;

                    int size = readUint16LE(tmpBuffer);
                    tmpBuffer.getInt();
                    byte[] answer = new byte[size];
                    tmpBuffer.get(answer);

                    Logger.i("Документ передан в ОФД OKП");
                    return answer;
                } else {
                    Logger.e("ОФД OKП не принял документ, ошибка: %s", read);
                }
                return null;

            }

        } catch (IOException ioe) {
            Logger.e(ioe,"Ошибка ОФД OKП");
            return null;
        }
    }
}
