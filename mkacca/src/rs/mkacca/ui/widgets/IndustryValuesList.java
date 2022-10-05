package rs.mkacca.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;
import cs.ui.UIRecyclerAdapter;
import rs.data.KV;
import rs.fncore.Const;
import rs.mkacca.R;
import rs.mkacca.ui.KVDialog;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.widgets.ItemList.EditorFragmentFactory;

public class IndustryValuesList extends LinearLayout implements EditorFragmentFactory<KV> {

	private UIRecyclerAdapter _adapter;
	private ItemList<KV> _items;
	private List<KV> _list = new ArrayList<>();
	private KVDialog _kv;
	public IndustryValuesList(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	public IndustryValuesList(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_items = new ItemList<>(this, this, R.id.v_rq_list);
		_kv = new KVDialog(getContext());
	}

	public void setValue(String s) {
		int p = s.indexOf('&', 0);
		while(p > -1) {
			if(p < s.length()-1 && s.charAt(p+1) == '&') {
				p+=2;
				p = s.indexOf('&',p);
				continue;
			}
			String [] kv = s.substring(0,p).split("=");
			
			_list.add(new KV(kv[0].replaceAll("&&", "&"),kv[1].replaceAll("&&", "&")));
			s = s.substring(p+1);
		}
		if(!s.isEmpty()) {
			String [] kv = s.split("=");
			_list.add(new KV(kv[0].replaceAll("&&", "&"),kv[1].replaceAll("&&", "&")));
		}
		_adapter = new UIRecyclerAdapter(getContext(), _list);
		_adapter.setSelectable(true);
		_items.setAdapter(_adapter);
	}
	
	public String getValue() {
		_list = _adapter.asList(KV.class);
		String r = Const.EMPTY_STRING;
		for(KV kv : _list) {
			if(!r.isEmpty()) r+="&";
			r+=kv.k.replaceAll("&", "&&");
			r+="=";
			r+=kv.v.replaceAll("&", "&&");
		}
		return r;
	}
	
	@Override
	public Fragment newInstance(KV value, OnValueChangedListener<KV> l) {
		_kv.show(value, l);
		return null;
	}
	

	@Override
	public KV newObject() {
		return new KV();
	}

}
