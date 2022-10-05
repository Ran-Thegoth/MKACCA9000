package rs.mkacca.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.data.goods.Good;
import rs.data.goods.GoodGroup;
import rs.mkacca.R;

public class CategoryCard extends LinearLayout implements ItemCard<GoodGroup>{

	private TextView _name, _goods;
	public CategoryCard(Context context) {
		super(context);
	}

	public CategoryCard(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CategoryCard(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_name = findViewById(R.id.lbl_name);
		_goods = findViewById(R.id.lbl_n_goods);
	}

	@Override
	public void setItem(GoodGroup item) {
		_name.setText(item.name());
		_goods.setText("Всего "+Good.getGoodsCountInGroup(item));
		
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
