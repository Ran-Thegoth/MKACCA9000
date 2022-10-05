package rs.mkacca.ui.widgets.pages;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.TaxModeE;
import rs.mkacca.ui.widgets.ItemCard;

public class SNOInfo extends LinearLayout implements ItemCard<KKMInfo> {

	private CheckBox [] SNO;
	private KKMInfo _info;
	public SNOInfo(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public SNOInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public SNOInfo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public SNOInfo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		SNO = new CheckBox[TaxModeE.values().length];
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		lp.bottomMargin = 6;
		for(int i=0;i<SNO.length;i++) {
			SNO[i] = new CheckBox(getContext());
			SNO[i].setText(TaxModeE.values()[i].desc);
			 addView(SNO[i],lp);
		}
	}
	
	@Override
	public void setItem(KKMInfo item) {
		_info = item;
		Set<TaxModeE> modes =  _info.getTaxModes();
		for(int i=0;i<SNO.length;i++) 
			SNO[i].setChecked(modes.contains(TaxModeE.values()[i]));
		
	}

	@Override
	public boolean obtain() {
		Set<TaxModeE> modes = new HashSet<>();
		for(int i=0;i<SNO.length;i++)
			if(SNO[i].isChecked()) 
				modes.add(TaxModeE.values()[i]);
		if(modes.isEmpty()) {
			Toast.makeText(getContext(), "Укажите хотя бы одну систему налогообложения", Toast.LENGTH_LONG).show();
			return false;
		}
		_info.getTaxModes().clear();
		_info.getTaxModes().addAll(modes);
		return true;
	}
	@Override
	public View getView() {
		return this;
	}

}
