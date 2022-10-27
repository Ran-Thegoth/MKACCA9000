package rs.fncore;

import android.util.SparseArray;

/**
 * Ошибки возвращаемые операциями FiscalStorage
 *
 * @author nick
 */
public class Errors {
    private Errors() {
    }

    public static final int NO_ERROR = 0;
    public static final int NO_MORE_DATA = 0x8;

    public static final int NEW_FN = 0xCA;
    public static final int OLD_FN_HAS_DATA = 0xCB;
    public static final int FN_REPLACEMENT = 0xCD;
    public static final int NO_CASH = 0xCE;
    public static final int NOT_IMPLEMENTED = 0xCF;

    public static final int SUM_MISMATCH = 0xD0;
    public static final int CASH_LESS_REFUND = 0xD1;
    public static final int WRONG_INN = 0xD3;
    public static final int WRONG_DOC_NUM = 0xD4;

    public static final int CHECK_MARKING_CODE = 0xE0;
    public static final int CHECK_MARK_CODE_OISM = 0xE1;
    public static final int CHECK_MARK_ITEM_REJECTED = 0xE2;
    public static final int NO_MARKING_CODE_IN_ITEM = 0xE3;
    public static final int MARKING_CHECK_FAILED = 0xE4;
    public static final int REMOTE_EXCEPTION = 0xE5;
    public static final int REJECTED_ITEM = 0xE6;
    public static final int NOT_SUPPORTED = 0xE7;
    public static final int FN_VERSION_TOO_OLD = 0xE8; 

    public static final int INVALID_SHIFT_STATE = 0xF0;
    public static final int DEVICE_ABSEND = 0xF1;
    public static final int READ_TIMEOUT = 0xF2;
    public static final int OPERATION_ABORTED = 0xF3;
    public static final int CRC_ERROR = 0xF4;
    public static final int WRITE_ERROR = 0xF5;
    public static final int READ_ERROR = 0xF6;
    public static final int DATA_ERROR = 0xF7;
    public static final int WRONG_FN_MODE = 0xF8;
    public static final int SYSTEM_ERROR = 0xF9;
    public static final int UNKNOWN_CONNECTION_MODE = 0xFA;
    public static final int ERROR_ADD_ITEM = 0xFB;
    public static final int DATE_MISMATCH = 0xFC;
    public static final int HAS_UNSENT_DOCS = 0xFD;
    public static final int SETTINGS_LOST = 0xFE;
    public static final int UNHANDLED_EXCEPTION = 0xFF;
    
    private final static SparseArray<String> ERRORS = new SparseArray<>();
    static {
    	ERRORS.put(NO_ERROR, "Нет ошибок");
    	ERRORS.put(NO_MORE_DATA,"Нет данных");

    	ERRORS.put(NEW_FN,"Новый ФН");
    	ERRORS.put(NO_CASH,"Нет наличных");
    	ERRORS.put(NOT_IMPLEMENTED,"Не поддерживается");

    	ERRORS.put(SUM_MISMATCH,"Несовпадение сумм сека и оплаты");
    	ERRORS.put(CASH_LESS_REFUND,"Наличных в кассе меньше чем пребуется для сдачи");
    	ERRORS.put(WRONG_INN,"Неверный ИНН");
    	ERRORS.put(WRONG_DOC_NUM,"Неверный номер документа");

    	ERRORS.put(CHECK_MARKING_CODE,"Проверка кода маркировки");
    	ERRORS.put(CHECK_MARK_CODE_OISM,"Проверка кода маркировки средствами ОИСМ");
    	ERRORS.put(CHECK_MARK_ITEM_REJECTED,"Неверный код маркировки");
    	ERRORS.put(NO_MARKING_CODE_IN_ITEM,"Нет кода маркировки для предмета расчета");
    	ERRORS.put(MARKING_CHECK_FAILED,"Ошибка проверки кода маркировки");
    	ERRORS.put(REMOTE_EXCEPTION,"Ошибка соединения с фискальным ядром");
    	ERRORS.put(REJECTED_ITEM,"Неверный предмет расчета");
    	ERRORS.put(NOT_SUPPORTED,"Не поддерживается");

    	ERRORS.put(DEVICE_ABSEND,"ФН не установлен");
    	ERRORS.put(READ_TIMEOUT,"Ошибка чтения");
    	ERRORS.put(OPERATION_ABORTED,"Операция прервана");
    	ERRORS.put(CRC_ERROR,"Ошибка контрольной суммы");
    	ERRORS.put(WRITE_ERROR,"Ошибка записи");
    	ERRORS.put(READ_ERROR,"Ошибка чтения");
    	
    	ERRORS.put(INVALID_SHIFT_STATE,"Неверное состояние смены");
    	
    	ERRORS.put(DATA_ERROR,"Ошибка данных");
    	ERRORS.put(SYSTEM_ERROR,"Системная ошибка");
    	ERRORS.put(UNKNOWN_CONNECTION_MODE,"Неверный редим подключения ФН");
    	ERRORS.put(ERROR_ADD_ITEM,"Ошибка добавления предмета расчета");
    	ERRORS.put(HAS_UNSENT_DOCS,"Есть неотправленные документы");
    	ERRORS.put(SETTINGS_LOST,"Требуется обновление насторек");
    	ERRORS.put(UNHANDLED_EXCEPTION,"Необработанная ошибка");
    	ERRORS.put(FN_VERSION_TOO_OLD, "Требуется ФН с поддержкой ФФД 1.1 минимум");
    }
    
    /**
     * Проверка на отсутствие ошибок
     * @param err - код, возвращаемый операцией
     * @return true если операция выполнена успешно
     */
    public static boolean isOK(int err) { return err == NO_ERROR; }
    /**
     * Получение текстового описания ошибки
     * @param err - код, возвращаемый операцией
     * @return текстовое описание ошибки
     */
    public static String getErrorDescr(int err) {
    	String s = ERRORS.get(err);
    	if(s == null)
    		s =String.format("Ошибка %02X",err);
    	return s;
    	}
    
}
