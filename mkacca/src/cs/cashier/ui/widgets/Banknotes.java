package cs.cashier.ui.widgets;

import java.math.BigDecimal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import rs.mkacca.R;

public class Banknotes extends TableLayout implements View.OnClickListener{

	public static interface BanknoteListener {
		public void onValueChanged(Banknotes sender);
	}
	private int [] _nominals;
	private int [] _counts;
	private BanknoteListener _l;
	public Banknotes(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public Banknotes(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@SuppressWarnings("deprecation")
	public void setup(BanknoteListener l, int...nominals) {
		_l = l;
		_nominals = nominals;
		_counts = new int[_nominals.length];
		removeAllViews();
		TableRow row = null;
		for(int i=0;i<_nominals.length+_nominals.length%4;i++) {
			if(i % 4 == 0) {
				row = new TableRow(getContext());
				addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			}
			TableRow.LayoutParams lp = new TableRow.LayoutParams(0,LayoutParams.WRAP_CONTENT);
			lp.weight = 1;
			lp.bottomMargin = lp.topMargin = 4;
			lp.rightMargin = lp.leftMargin = 2;
			if(i >= _nominals.length) {
				View v = new View(getContext());
				lp.height = 1;
				row.addView(v,lp);
			} else {
				TextView banknote = new TextView(getContext());
				banknote.setGravity(Gravity.CENTER);
				banknote.setPadding(12, 12, 12, 12);
				banknote.setMinHeight(72);
				banknote.setTextColor(getContext().getResources().getColorStateList(R.drawable.blue_button_text));
				banknote.setBackgroundResource(R.drawable.blue_button);
				if(_nominals[i] > 0)
					banknote.setText(String.valueOf(_nominals[i]));
				else
					banknote.setText("C");
				banknote.setTextSize(18);
				banknote.setOnClickListener(this);
				banknote.setTag(i);
				row.addView(banknote,lp);
			}
			
		}
	}

	@Override
	public void onClick(View v) {
		Integer tag = (Integer)v.getTag();
		if(_nominals[tag.intValue()] < 0) {
			for(int i=0;i< _counts.length;i++)
				_counts[i] = 0;
			if(_l != null) _l.onValueChanged(this);
			return;
		}
		++_counts[tag.intValue()];
		if(_l != null) _l.onValueChanged(this);
	}
	public void clear() {
		for(int i=0;i<_counts.length;i++) 
			_counts[i] = 0;
	}
	public BigDecimal sum() {
		long val = 0;
		for(int i=0;i<_counts.length;i++) {
			val += _counts[i] * _nominals[i];
		}
		return BigDecimal.valueOf(val);
	}
}
