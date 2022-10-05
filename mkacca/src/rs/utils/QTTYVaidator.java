package rs.utils;

import java.lang.reflect.Field;

import android.content.Context;
import cs.ui.annotations.Validator;

public class QTTYVaidator implements Validator {

	public static final QTTYVaidator INSTANCE = new QTTYVaidator(); 
	private QTTYVaidator() {
	}
	@Override
	public boolean isCorrect(String value, Field field, Context context, Object owner) {
		try {
			return Double.parseDouble(value.replace(",", ".")) > 0;
		} catch(NumberFormatException nfe) {
			return false;
		}
	}

}
