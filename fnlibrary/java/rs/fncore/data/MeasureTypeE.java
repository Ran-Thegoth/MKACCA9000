package rs.fncore.data;

import java.security.InvalidParameterException;

/**
 * Тип единицы изамерения
 *
 * @author amv
 */
public enum MeasureTypeE {
    /**
     * шт. или ед.
     * Применяется для предметов расчета, которые могут быть реализованы поштучно или единицами
     */
    PIECE(0,"шт.",	796),
    /**
     * Грамм
     */
    GRAM(10, "г",163),
    /**
     * Килограмм
     */
    KILOGRAM(11,"кг",166),
    /**
     * Тонна
     */
    TON(12,"т",168),
    /**
     * Сантиметр
     */
    CENTIMETER(20,"см",004),
    /**
     * Дециметр
     */
    DECIMETER(21,"дм",005),
    /**
     * Метр
     */
    METER(22,"м",006),
    /**
     * Квадратный сантиметр
     */
    SQUARE_CENTIMETER(30,"кв. см",051),
    /**
     * Квадратный дециметр
     */
    SQUARE_DECIMETER(31,"кв. дм",053),
    /**
     * Квадратный метр
     */
    SQUARE_METER(32,"кв. м",055),
    /**
     * Миллилитр
     */
    MILLILITER(40,"мл",111),
    /**
     * Литр
     */
    LITER(41,"л",112),
    /**
     * Кубический метр
     */
    CUBIC_METER(42,"куб. м",113),
    /**
     * Киловатт час
     */
    KILOWATT_HOUR(50,"кВт ч",245),
    /**
     * Гигакалория
     */
    GIGACALORIE(51,"Гкал",233),
    /**
     * Сутки (день)
     */
    DAY(70,"сутки",359),
    /**
     * Час
     */
    HOUR(71,"час",356),
    /**
     * Минута
     */
    MINUTE(72,"мин",355),
    /**
     * Секунда
     */
    SECOND(73,"с",354),
    /**
     * Килобайт
     */
    KILOBYTE(80,"Кбайт",256),
    /**
     * Мегабайт
     */
    MEGABYTE(81,"Мбайт",257),
    /**
     * Гигабайт
     */
    GIGABYTE(82,"Гбайт",2553),
    /**
     * Терабайт
     */
    TERABYTE(83,"Тбайт",2554),
    /**
     * Применяется при использовании иных единиц измерения, не поименованных в п.п. 1 - 23
     */
    OTHER(255,"Другое",657)
    ;

    public final byte bVal;
    public final String pName;
    public final int OKEI;

    private MeasureTypeE(int value, String name, int code) {
        this.bVal = (byte)value;
        this.pName = name;
        this.OKEI = code;
    }

    public static MeasureTypeE fromByte(byte number){
        for (MeasureTypeE val:values()){
            if (val.bVal == number){
                return val;
            }
        }
        throw new InvalidParameterException("unknown value");
    }

    /**
     * Получить единицу измерения по коду ОКЕИ
     * @param code код по ОКЕИ
     * @return соответствующая ед. измерения или OTHER если не найдена
     */
    public static MeasureTypeE byOKEI(int code) {
    	return byOKEI(code, OTHER);
    }
    /**
     * Получить единицу измерения по коду ОКЕИ, если код не найдет вернуть def
     * @param code  код по ОКЕИ
     * @param def значение ед. измерения, если код не найден
     * @return соответствующая ед. измерения 
     */
    public static MeasureTypeE byOKEI(int code, MeasureTypeE def) {
    	for(MeasureTypeE v : values())
    		if(v.OKEI == code) return v;
    	return def;
    }
    
    @Override
    public String toString() {
    	return pName;
    }
}
