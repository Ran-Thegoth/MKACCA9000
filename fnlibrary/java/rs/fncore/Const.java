package rs.fncore;

import android.content.Intent;
import android.net.Uri;

import java.nio.charset.Charset;

public class Const {

    private Const() {
    }

    /**
     * Пустая строка
     */
    public static final String EMPTY_STRING = "";
    /**
     * Кодировка текста для ФН
     */
    public static final Charset ENCODING = Charset.forName("CP866");
    /**
     * Максимальный размер одного тега
     */
    public static final int MAX_TAG_SIZE = 1024;
    /**
     * Полином, для вычисления CRC16
     */
    public static final short CCIT_POLY = (short) 0x1021;

    /**
     * Одни сутки в милисекундах
     */
    public static final long ONE_DAY = 24 * 60 * 60000L;
    /**
     * Интент для обращения к FiscalStorage
     */
    public static Intent FISCAL_STORAGE = new Intent("rs.fncore2.FiscalStorage");

    /**
     * Имя пакета FNCore
     */
    public static final String CORE_PACKAGE = "rs.fncore2"; 
    static {
        FISCAL_STORAGE.setPackage(CORE_PACKAGE);
    }

    /**
     *  URI для доступа к журналу документов
     */
    public static final Uri DOCUMENT_JOURNAL = Uri.parse("content://rs.fncore2.data/documents");
    
    /**
     * 
     * Событие, отправляемое при изменении ФН
     */
    public static final String FN_STATE_CHANGED_ACTION = "rs.fncore2.fn.changed";

    /**
     * Событие, отправляемое при отправке очередного документа в ОФД
     */
    public static final String OFD_SENT_ACTION = "rs.fncore2.ofd.info";

    /**
     * Количество документов, оставшееся к отправке в ОФД, int для OFD_SENT_ACTION
     */
    public static final String OFD_DOCUMENTS = "ofd.documents";

    /**
     * Количество документов, оставшееся к отправке в ОИСМ, int для OFD_SENT_ACTION
     */
    public static final String OISM_DOCUMENTS = "oism.documents";

    
}
