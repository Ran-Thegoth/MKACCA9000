package cs.cashier.ui.widgets;

import java.math.BigDecimal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import ir.esfandune.calculatorlibe.CalculatorDialog;

public class NumberEdit extends TextView {

	public static interface OnValueChangeListener {
		public boolean onNewValue(NumberEdit sender, double value);
	}
	private String _fmt = "%.2f";
	private OnValueChangeListener _l;
	public NumberEdit(Context context) {
		super(context);
		setupUI();
	}
	public NumberEdit(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUI();
	}
	public NumberEdit(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setupUI();
	}
	public NumberEdit(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setupUI();
	}
	
	public void setOnValueChangeListener(OnValueChangeListener l) {
		_l = l;
	}
	public void setDecimalDigits(int v) {
		_fmt = "%."+v+"f";
	}
	
	@SuppressWarnings("deprecation")
	private void setupUI() {
		setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Widget_EditText);
		setBackground(getResources().getDrawable(android.R.drawable.editbox_background));
		setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				new CalculatorDialog(getContext()) {
					@Override
					public void onResult(BigDecimal value) {
						if(_l != null)
							if(!_l.onNewValue(NumberEdit.this, value.doubleValue())) return;
						setText(String.format(_fmt, value.doubleValue()));
					}
				}.setValue(doubleValue()).showDIalog();
				
			}
		});
	}
	public double doubleValue() {
		try {
			return Double.parseDouble(getText().toString().replace(',', '.'));
		} catch(NumberFormatException nfe) {
			return 0;
		}
	}
	
}
