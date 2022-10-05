package rs.fncore2.fn.fn12.marking;

import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import rs.fncore.FZ54Tag;
import rs.fncore.data.GS1Code128Data;
import rs.log.Logger;

public class MarkingCodeParam {

    private static final int MAX_UNK_CODE_LEN = 32;
    private final String mCode;
    private final CodeTypesParamE mCodeType;
    private static final String GS1_DIV = "" + (char) 0x1d;
    private static final String MARKING_DIVIDER_SYMBOLS = "!\"%&'()*+,-./_:;=<>?" + GS1_DIV;
    volatile private GS1Code128Data mGS1data = null;

    public enum CodeTypesParamE {
        UNKNOWN(0, "unknown", FZ54Tag.T1300_CODE_UNKNOWN),
        EAN_8(1, "КТ EAN-8", FZ54Tag.T1301_CODE_EAN8),
        EAN_13(2, "КТ EAN-13", FZ54Tag.T1302_CODE_EAN13),
        ITF_14(3, "КТ ITF-14", FZ54Tag.T1303_CODE_ITF14),
        GS1_0(4, "КТ GS1.0", FZ54Tag.T1304_CODE_GS10),
        GS1_M(5, "КТ GS1.М", FZ54Tag.T1305_CODE_GS1M),
        KMK(6, "КТ КМК", FZ54Tag.T1306_CODE_KMK),
        MI(7, "КТ МИ", FZ54Tag.T1307_CODE_MI),
        EGAIS_2(8, "КТ ЕГАИС-2.0", FZ54Tag.T1308_CODE_EGAIS2),
        EGAIS_3(9, "КТ ЕГАИС-3.0", FZ54Tag.T1309_CODE_EGAIS3),
        KT_F1(10, "КТ Ф.1", FZ54Tag.T1320_CODE_F1),
        KT_F2(11, "КТ Ф.2", FZ54Tag.T1321_CODE_F2),
        KT_F3(12, "КТ Ф.3", FZ54Tag.T1322_CODE_F3),
        KT_F4(13, "КТ Ф.4", FZ54Tag.T1323_CODE_F4),
        KT_F5(14, "КТ Ф.5", FZ54Tag.T1324_CODE_F5),
        KT_F6(15, "КТ Ф.6", FZ54Tag.T1325_CODE_F6),
        ;

        CodeTypesParamE(final int bVal, final String name, final int tag) {
            this.bVal = (byte) bVal;
            this.name = name;
            this.tag = tag;
        }

        public final byte bVal;
        public final String name;
        public final int tag;

        public static CodeTypesParamE fromByte(byte bVal) {
            for (CodeTypesParamE v : values())
                if (v.bVal == bVal) return v;
            return UNKNOWN;
        }
    }

    public MarkingCodeParam(String code) {
        mCode = code;
        mCodeType = getCodeType(code);
    }

    public CodeTypesParamE getType() {
        return mCodeType;
    }

    public String getCode() {
        return mCode;
    }

    public Byte getGS1MarkingCheckKeyOffset() {
        if (mGS1data == null || mGS1data.getMarkingCheckKey() == null) return null;
        return (byte) mGS1data.getMarkingCheckKey().offset;
    }

    public Byte getGS1MarkingCheckCodeOffset() {
        if (mGS1data == null || mGS1data.getMarkingCheckCode() == null) return null;
        return (byte) mGS1data.getMarkingCheckCode().offset;
    }

    public String getGS1MarkingCheckKey() {
        if (mGS1data == null || mGS1data.getMarkingCheckKey() == null) return null;
        return mGS1data.getMarkingCheckKey().data;
    }

    public String getGS1MarkingCheckCode() {
        if (mGS1data == null || mGS1data.getMarkingCheckCode() == null) return null;
        return mGS1data.getMarkingCheckCode().data;
    }

    public String getGS1MarkingCryptoTail() {
        if (mGS1data == null || mGS1data.getMarkingCryptoTail() == null) return null;
        return mGS1data.getMarkingCryptoTail().data;
    }

    public byte[] getGS1MarkingCheckCodeDecrypted() {
    	if(getGS1MarkingCheckCode() == null || getGS1MarkingCheckCode().isEmpty()) return new byte [] {};
        return android.util.Base64.decode(getGS1MarkingCheckCode(), android.util.Base64.DEFAULT);
    }

    public String getIdentificator(){
        switch (mCodeType){
            case EAN_8:
            case EAN_13:
            case ITF_14:
            case MI:
                return mCode;
            case EGAIS_2:
                return mCode.substring(9,31);
            case EGAIS_3:
                return mCode.substring(0,14);
            case GS1_0:
            case GS1_M: {
                String gtin = "";
                if (mGS1data != null && mGS1data.getGTIN() != null) gtin = mGS1data.getGTIN().data;

                String sn = "";
                if (mGS1data != null && mGS1data.getSerial() != null)
                    sn = mGS1data.getSerial().data;
                return "01" + gtin + "21" + sn;
            }
            case KMK:{
                String gtin=mCode.substring(0,0+14);
                String sn=mCode.substring(14,14+7);
                return "01" + gtin + "21" + sn;
            }
            case UNKNOWN: {
                int maxLen = Math.min(mCode.length(), MAX_UNK_CODE_LEN);
                return mCode.substring(maxLen);
            }
            default:
                return "";
        }
    }

    public static long getUInt32(byte[] bytes, int offset) {
        long value =
                ((bytes[offset + 0] & 0xFF) << 0) |
                        ((bytes[offset + 1] & 0xFF) << 8) |
                        ((bytes[offset + 2] & 0xFF) << 16) |
                        ((long) (bytes[offset + 3] & 0xFF) << 24);
        return value;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public String getGS1MarkingCheckCodeCRC() {
        byte[] data = getGS1MarkingCheckCodeDecrypted();
        return String.valueOf(getUInt32(data, data.length - 4));
    	
    }
    private Pair<Long, Long> getGS1MarkingCheckCodeDecryptedCRC32() {
        CRC32 crc = new CRC32();
        byte[] data = getGS1MarkingCheckCodeDecrypted();
        crc.update(data, 0, 28);
        long crcFromArray = getUInt32(data, data.length - 4);
        long crcFromCalc = crc.getValue();
        return new Pair<>(crcFromCalc, crcFromArray);
    }

    public boolean isCodeForFNCheck() {
        Pair<Long, Long> res = getGS1MarkingCheckCodeDecryptedCRC32();
        return res.first.longValue() != res.second.longValue();
    }

/*    private Integer getGS1offset(byte divider, byte id) {
        byte[] data = mCode.getBytes();
        for (int i = 0; i < (data.length - 1); i++) {
            if (data[i] == divider && data[i + 1] == id) {
                return i + 1;
            }
        }
        return null;
    } */

    private CodeTypesParamE getCodeType(String code) {
    	Log.d("fncore2", "Code length is "+code.length());
        if (checkEAN8(code)) {
            return CodeTypesParamE.EAN_8;
        } else if (checkEAN13(code)) {
            return CodeTypesParamE.EAN_13;
        } else if (checkEGAIS2(code)) {
            return CodeTypesParamE.EGAIS_2;
        } else if (checkEGAIS3(code)) {
            return CodeTypesParamE.EGAIS_3;
        } else if (checkMI(code)) {
            return CodeTypesParamE.MI;
        }
        else if (checkITF14(code)) {
            return CodeTypesParamE.ITF_14;
        } else if (checkKMK(code)) {
            return CodeTypesParamE.KMK;
        } else if (checkGS1_M(code)) {
            return CodeTypesParamE.GS1_M;
        } else if (checkGS1_0(code)) {
            return CodeTypesParamE.GS1_0;
        } 
        return CodeTypesParamE.UNKNOWN;
    }

    private boolean checkEAN8(String barcode) {
        if (barcode.length() != 8) return false;
        return true;
/*        String checksum = barcode.substring(barcode.length() - 1, barcode.length());
        String calcChecksum = checksumEAN(barcode.substring(0, barcode.length() - 1));
        if (checksum.equals(calcChecksum)) {
            return true;
        }
        return false; */
    }

    private boolean checkEAN13(String barcode) {
        if (barcode.length() != 13) return false;

        String checksum = barcode.substring(barcode.length() - 1, barcode.length());
        String calcChecksum = checksumEAN(barcode.substring(0, barcode.length() - 1));
        if (checksum.equals(calcChecksum)) {
            return true;
        }
        return false;
    }

    private boolean checkITF14(String barcode) {
        return barcode.length() == 14;
    }

    private static String checksumEAN(String barcode) {
        int first = 0;
        int second = 0;

        if (!barcode.matches("[0-9]+")) return null;

        if (barcode.length() == 7 || barcode.length() == 12) {

            for (int counter = 0; counter < barcode.length() - 1; counter++) {
                first = (first + Integer.parseInt(barcode.substring(counter, counter + 1)));
                counter++;
                second = (second + Integer.parseInt(barcode.substring(counter, counter + 1)));
            }
            second = second * 3;
            int total = second + first;
            int roundedNum = Math.round((total + 9) / 10 * 10);

            return String.valueOf(roundedNum - total);
        }
        return null;
    }

    private boolean checkGS1(String barcode) {
        try {
            if (!barcode.matches("[a-zA-Z0-9" + MARKING_DIVIDER_SYMBOLS + "]+")) {
                return false;
            }
            if (mGS1data == null) mGS1data = new GS1Code128Data(barcode, GS1_DIV);
            if (    mGS1data.getGTIN() == null ||
                    mGS1data.getSerial() == null
                    ) {
                return false;
            }
        } catch (Exception e) {
            Logger.e(e, "error detect GS1 marking ");
            return false;
        }

        return true;
    }

    private boolean checkGS1_M(String barcode) {
        if (!checkGS1(barcode)) return false;
        return mGS1data.hasMarkingCheckKey() ||
                mGS1data.hasMarkingCheckCode() ||
                mGS1data.hasMarkingCryptoTail();
    }

    private boolean checkGS1_0(String barcode) {
        return checkGS1(barcode);
    }

    private boolean checkKMK(String barcode) {
        if (barcode.length() != 29) return false;

        if (!barcode.matches("[a-zA-Z0-9%&'()*+,-./_:;=<>?!\"]+")) {
            return false;
        }
        //barcode = removeLeadingZeros(barcode);
        String ean13Part = barcode.substring(0, 13);
        return checkEAN13(ean13Part);
    }

    private boolean checkMI(String barcode) {
        return barcode.length() == 20;
/*        Pattern p = Pattern.compile("[a-zA-Z][a-zA-Z]-\\d\\d\\d\\d\\d\\d-[a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z]");
        Matcher m = p.matcher(barcode);
        return m.matches(); */
    }

    private boolean checkEGAIS2(String barcode) {
        return (barcode.length() == 68);
//        return true; 
    }

    private boolean checkEGAIS3(String barcode) {
        return (barcode.length() == 150);
//        return barcode.matches("[a-zA-Z0-9]");
    }
}
