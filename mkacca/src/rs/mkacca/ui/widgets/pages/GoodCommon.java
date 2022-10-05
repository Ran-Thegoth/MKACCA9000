package rs.mkacca.ui.widgets.pages;

import cs.ui.widgets.DialogSpinner;
import cs.ui.widgets.DialogSpinner.OnMoreClickListener;
import rs.data.goods.Good;
import rs.data.goods.Good.MarkTypeE;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem.SellItemTypeE;
import rs.fncore.data.VatE;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.widgets.ItemCard;
import cs.orm.ORMAdapter;
import cs.ui.UIView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemSelectedListener;

public class GoodCommon extends LinearLayout implements OnItemSelectedListener,
		ItemCard<Good>, View.OnClickListener, UIView {


	public GoodCommon(Context context) {
		super(context);
	}

	public GoodCommon(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GoodCommon(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		DialogSpinner sp = findViewById(R.id.sp_mu);
		sp.setAdapter(new ArrayAdapter<MeasureTypeE>(getContext(), android.R.layout.simple_list_item_1,MeasureTypeE.values()));
		sp = findViewById(R.id.sp_good_type);
		sp.setAdapter(new ArrayAdapter<SellItemTypeE>(getContext(), android.R.layout.simple_list_item_1,SellItemTypeE.values()));
		sp = findViewById(R.id.sp_nds);
		sp.setAdapter(new ArrayAdapter<VatE>(getContext(), android.R.layout.simple_list_item_1,VatE.values()));
		sp = findViewById(R.id.sp_mark_type);
		sp.setAdapter(new ArrayAdapter<MarkTypeE>(getContext(), android.R.layout.simple_list_item_1,MarkTypeE.values()));
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View v, int position,
			long id) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}


	@Override
	public void setItem(Good item) {
	}

	@Override
	public void onClick(View v) {
		
	}

	@Override
	public void onBined(Object o) {
		
	}

	@Override
	public void onObtain(Object o) {
		// TODO Auto-generated method stub
		
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
