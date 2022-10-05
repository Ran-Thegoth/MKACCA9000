package rs.fncore.data;

/**
 * Обработчик шаблонов печати
 * @author nick
 *
 */
public interface TemplateProcessor {
	/**
	 * Получить значение поля печати
	 * @param key поле из шаблона печати
	 * @return значение поля
	 */
       String onKey(String key);
}
