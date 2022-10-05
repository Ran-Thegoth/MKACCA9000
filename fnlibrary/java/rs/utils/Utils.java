package rs.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import rs.fncore.Const;
import rs.fncore.data.Document;
import rs.fncore.data.FNCounters;
import rs.fncore.data.IReableFromParcel;

import static rs.fncore.Const.CCIT_POLY;

/**
 * Вспомогательные методы
 *
 * @author nick
 */
public class Utils {

    static final Logger mLogger = Logger.getLogger("rs.utils.Utils");
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    public static final DateFormat DATE_FORMAT_S = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * Вычитать Uint32 из ByteBuffer
     *
     * @param bb буффер с данными
     * @return вычитанное значение
     */
    public static long readUint32LE(ByteBuffer bb) {
        return bb.getInt() & 0xFFFFFFFFL;
    }

    /**
     * Вычитать Uint16 из ByteBuffer
     *
     * @param bb буффер с данными
     * @return вычитанное значение
     */
    public static int readUint16LE(ByteBuffer bb) {
        return bb.getShort()&0xFFFF;
    }

    /**
     * Вычитать Uint48 из ByteBuffer
     *
     * @param bb буффер с данными
     * @return вычитанное значение
     */
    public static long readUint48LE(ByteBuffer bb) {
        byte[] b = new byte[6];
        bb.get(b);
        return (long)((b[5] & 0xFF) << 40) | ((b[4] & 0xFF) << 32) | ((b[3] & 0xFF) << 24) |
                ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
    }

    /**
     * Вычитать Uint8 из ByteBuffer
     *
     * @param bb буффер с данными
     * @return вычитанное значение
     */
    public static short readUint8LE(ByteBuffer bb) {
        return (short)(bb.get()&0xFF);
    }

    /**
     * Получить стек ошибки
     *
     * @return строка с описанием стека ошибки
     */
    public static String getStackTrace(){
        String res="";
        StackTraceElement elem[] = Thread.currentThread().getStackTrace();
        for (int i=3;i<elem.length;i++) {
            res+="\n"+elem[i].toString();
        }
        return res;
    }

    /**
     * Округление до указанного количества знаков после запятой
     *
     * @param number значение суммы
     * @param scale количество знаков после запятой
     * @return округленное значение
     */
    public static double round2(double number, int scale) {
        double pow = 10;
        for (int i = 1; i < scale; i++) {
            pow *= 10;
        }
        double tmp = number * pow;
        return (((long) ((tmp - (long) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow;
    }

    /**
     * Округление до указанного количества знаков после запятой HALF_UP
     *
     * @param number значение суммы
     * @param scale количество знаков после запятой
     * @return округленное значение
     */
    public static BigDecimal round2(BigDecimal number, int scale) {
        return number.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Восстановить Parcelable из массива байт
     *
     * @param bb буффер
     * @param creator creator
     * @param <T> тип класса посылки
     * @return Parcelable
     */
    public static <T extends Parcelable> T deserialize(byte[] bb, Parcelable.Creator<T> creator) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bb, 0, bb.length);
        p.setDataPosition(0);
        try {
            return creator.createFromParcel(p);
        } finally {
            p.recycle();
        }
    }

    /**
     * Прочитать экземпляр  IReableFromParcel из массива байт
     *
     * @param data массива байт
     * @param doc документ
     */
    public static void deserialize(byte[] data, IReableFromParcel doc) {
        Parcel p = Parcel.obtain();
        p.unmarshall(data, 0, data.length);
        p.setDataPosition(0);
        doc.readFromParcel(p);
        p.recycle();
    }

    /**
     * Сохранить Parcelable как массив байт
     *
     * @param doc документ
     * @return массив байт
     */
    public static byte[] serialize(Parcelable doc) {
        Parcel p = Parcel.obtain();
        doc.writeToParcel(p, 0);
        p.setDataPosition(0);
        byte[] result = p.marshall();
        p.recycle();
        return result;
    }

    /**
     * Сохранить Parcelable
     *
     * @param doc документ
     * @return parcel
     */
    public static Parcel writeToParcel(Parcelable doc) {
        Parcel p = Parcel.obtain();
        doc.writeToParcel(p, 0);
        p.setDataPosition(0);
        return p;
    }

    /**
     * Прочитать IReableFromParcel из массива байт
     *
     * @param doc документ
     * @param p parcel
     */
    public static void readFromParcel(IReableFromParcel doc, Parcel p) {
        p.setDataPosition(0);
        doc.readFromParcel(p);
        p.recycle();
    }

    /**
     * Прочитать дату в формате YY mm DD HH MM
     *
     * @param bb буффер с данными
     * @return значение даты в миллисекундах
     */
    public static long readDate5(ByteBuffer bb) {
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.YEAR, bb.get() + 2000);
        cal.set(Calendar.MONTH, bb.get() - 1);
        cal.set(Calendar.DAY_OF_MONTH, bb.get());
        cal.set(Calendar.HOUR_OF_DAY, bb.get());
        cal.set(Calendar.MINUTE, bb.get());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Прочитать дату в формате YY mm DD
     *
     * @param bb байт буффер c данными
     * @return дата в миллисекундах
     */
    public static long readDate3(ByteBuffer bb) {
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        cal.set(Calendar.YEAR, bb.get() + 2000);
        cal.set(Calendar.MONTH, bb.get() - 1);
        cal.set(Calendar.DAY_OF_MONTH, bb.get());
        return cal.getTimeInMillis();
    }

    public static byte[] encodeDate(Calendar cal) {
        byte[] result = new byte[5];
        result[0] = (byte) (cal.get(Calendar.YEAR) - 2000);
        result[1] = (byte) (cal.get(Calendar.MONTH) + 1);
        result[2] = (byte) (cal.get(Calendar.DAY_OF_MONTH));
        result[3] = (byte) (cal.get(Calendar.HOUR_OF_DAY));
        result[4] = (byte) (cal.get(Calendar.MINUTE));
        return result;
    }

    /**
     * Значение CRC16
     *
     * @param data данные
     * @param offset смещение
     * @param length длина данных
     * @param nPoly тип полинома
     * @return Значение CRC16
     */
    public static short CRC16(byte[] data, int offset, int length, short nPoly) {
        short crc = (short) 0xFFFF;
        for (int j = 0; j < length; j++) {
            byte b = data[offset + j];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= nPoly;
            }
        }
        crc &= 0xffff;
        return crc;
    }

    public static String dump(byte[] b, int offset, int size) {
        StringBuilder s = new StringBuilder();
        if (b == null)
            return s.toString();
        for (int i = 0; i < size; i++) {
            if (offset + i >= b.length) {
                return s.toString();
            }
            s.append(String.format("%02X", b[offset + i]));
        }
        return s.toString();
    }

    public static String dump(ByteBuffer bb) {
        return dump(bb.array(),0,bb.position());
    }

    public static String dump(byte[] bArr) {
        return dump(bArr,0,bArr.length);
    }

    public static boolean checkStringNumbersOrSpaces(String s){
        return s.matches("[0-9 ]+");
    }

    public static boolean checkDate(String date){
        return date.matches("^[0-3]?[0-9]\\.[0-3]?[0-9]\\.(?:[0-9]{2})?[0-9]{2}$");
    }

    public static String formatInn(String inn){
        StringBuilder innBuilder = new StringBuilder(inn);
        while (innBuilder.length() < 12) innBuilder.append(" ");
        inn = innBuilder.toString();
        return inn;
    }

    /**
     * Поверить ИНН на валидность
     *
     * @param innStr ИНН для проверки
     * @return валиден ли ИНН
     */
    public static boolean checkINN(String innStr) {
        final int[] MULT_N1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        final int[] MULT_N2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        final int[] MULT_N =  {2, 4, 10, 3, 5, 9, 4, 6, 8};

        int[] inn = new int[innStr.length()];
        for (int i = 0; i < innStr.length(); i++) {
            char currChar=innStr.charAt(i);
            if (!Character.isDigit(currChar)) return false;
            inn[i] = Integer.valueOf(String.valueOf(currChar));
        }

        switch (inn.length) {
            case 12:
                int N1 = getChecksum(inn,MULT_N1);
                int N2 = getChecksum(inn,MULT_N2);
                return (inn[inn.length-1]==N2 && inn[inn.length-2]==N1);
            case 10:
                int N = getChecksum(inn,MULT_N);
                return (inn[inn.length-1]==N);
            default:
                return false;
        }
    }

    private static int getChecksum(int[] digits, int[] multipliers) {
        int checksum = 0;
        for (int i=0; i<multipliers.length; i++) {
            checksum+=(digits[i]*multipliers[i]);
        }
        return (checksum % 11) % 10;
    }

    /**
     * Проверить валидность регистрационного номера ККТ
     *
     * @param number номер
     * @param inn    ИНН пользователя
     * @param device серийный номер ККТ
     * @return валиден ли номер
     */
    public static boolean checkRegNo(String number, String inn, String device) {
        if (number == null || number.length() != 16) return false;
        while (inn.length() < 12) inn = "0" + inn;
        while (device.length() < 20) device = "0" + device;
        String num = number.substring(0, 10) + inn + device;
        String crc = number.substring(10);
        byte[] b = num.getBytes(Const.ENCODING);
        int sCRC = (CRC16(b, 0, b.length, CCIT_POLY) & 0xFFFF);
        try {
            return Integer.parseInt(crc) == sCRC;
        } catch (NumberFormatException nfe) {
            mLogger.log(Level.SEVERE,  "error check RegNo", nfe);
            return false;
        }
    }

    /**
     * Сформатировать дату в виде DD/MM/YYYY, HH:mm
     *
     * @param d дата в милисекундах
     * @return date
     */
    public static String formatDate(long d) {
        return DATE_FORMAT.format(new Date(d));
    }

    public static String formatDate(Date d) {
        return DATE_FORMAT.format(d);
    }

    public static String formatDate(Calendar cal) {
        return DATE_FORMAT.format(cal.getTime());
    }

    public static String formatDateS(long d) {
        return DATE_FORMAT_S.format(new Date(d));
    }

    /**
     * Созать баркод как картинку
     *
     * @param contents текст для баркода
     * @param imgWidth ширина картинки
     * @param imgHeight высота картинки
     * @param format форматирование бар кода
     * @param rotation угол поворота картинки
     * @return картинка с баркодом
     */
    public static Bitmap encodeAsBitmap(String contents, int imgWidth, int imgHeight, BarcodeFormat format, int rotation) {
        if (contents == null) {
            return Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
        }
        Hashtable<EncodeHintType, Object> hints = null;
        hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        int w = imgWidth;
        int h = imgHeight;
        if (rotation == 90 || rotation == 270 || rotation == -90 || rotation == -270) {
            w = imgHeight;
            h = imgWidth;
        }

        try {
            result = writer.encode(contents, format, w, h, hints);
        } catch (Exception iae) {
            return Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        int po = 0;
        switch (rotation) {
            case 0:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++)
                        pixels[po++] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
                break;
            case 90:
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixels[po++] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }
                break;
        }

        Bitmap bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);
        return bitmap;
    }

    public static Document deserializeDocument(byte[] data) {
        Parcel p = Parcel.obtain();
        p.unmarshall(data, 0, data.length);
        p.setDataPosition(0);
        String className = p.readString();
        try {
            Class<?> c = Class.forName(className);
            if (Document.class.isAssignableFrom(c)) {
                Field cField = c.getDeclaredField("CREATOR");
                @SuppressWarnings("rawtypes")
                Parcelable.Creator creator = (Parcelable.Creator) cField.get(null);
                return (Document) creator.createFromParcel(p);
            }
        } catch (Exception e) {
            mLogger.log(Level.SEVERE,  "error deserialize document", e);
        }
        return null;
    }

    public static byte[] hex2bytes(String hex) {
        if (hex == null) return new byte[]{};
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            String s = hex.substring(i * 2, i * 2 + 2);
            result[i] = (byte) (Integer.parseInt(s, 16) & 0xFF);
        }
        return result;
    }

    public static boolean contains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }

    public static boolean startService(Context context){
        try {
            ComponentName cn;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cn = context.startForegroundService(Const.FISCAL_STORAGE);
            } else {
                cn = context.startService(Const.FISCAL_STORAGE);
            }

            if (cn == null) {
                mLogger.log(Level.SEVERE,  "can't start FNCore service");
                return false;
            }

            return true;
        }catch (Exception e){
            mLogger.log(Level.SEVERE,  "can't start FNCore service", e);
            return false;
        }
    }

    public static interface SerialReader {
    	String getSerial();
    }
    private static SerialReader _reader;
    
    public static void setSerialReader(SerialReader reader) {
    	_reader = reader;
    }
	public static String getDeviceSerial() {
		if(_reader != null) return _reader.getSerial();
		return "00000000000";
	}
	public static interface CountersPrinter {
		String printCounters(FNCounters c);
	}
	
	private static CountersPrinter _cPrinter;
	public static void setCountersPrinter(CountersPrinter cp) {
		_cPrinter = cp;
	}

	public static long parseDate(String s) throws java.text.ParseException {
		return DATE_FORMAT.parse(s).getTime();
	}
	public static String printCounters(FNCounters c) {
		if(_cPrinter != null)
			return _cPrinter.printCounters(c);
		return Const.EMPTY_STRING;
	}
	
	public static long CRC32(byte [] bytes, int offset, int size) {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(bytes, offset, size);
		return crc.getValue() & 0x04C11DB7;
	}

}
