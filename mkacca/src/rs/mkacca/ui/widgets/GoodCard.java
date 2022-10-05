package rs.mkacca.ui.widgets;

import java.util.Locale;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.data.goods.Good;
import rs.mkacca.R;

public class GoodCard extends LinearLayout implements ItemCard<Good>{
	private TextView _name,_price;
	private Good _g;
	public GoodCard(Context context) {
		super(context);
	}

	public GoodCard(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GoodCard(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_name = findViewById(R.id.lbl_name);
		_price = findViewById(R.id.lbl_price);
	}
	@Override
	public void setItem(Good item) {
		_g = item;
		_name.setText(item.name());
		if(item.isUsed())
			_name.setTextColor(Color.BLACK);
		else 
			_name.setTextColor(Color.LTGRAY);
		if(item.price() == 0)
			 _price.setText("Свободная цена");
		else
			_price.setText(String.format(Locale.ROOT,"%.2f", item.price()));
	}

	public Good getItem() {
		return _g;
	}

	@Override
	public boolean obtain() {
		return true;
		
	}

	@Override
	public View getView() {
		return this;
	}

}
