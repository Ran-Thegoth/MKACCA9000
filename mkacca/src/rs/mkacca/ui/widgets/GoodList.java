package rs.mkacca.ui.widgets;

import java.util.ArrayList;
import java.util.List;


import rs.data.BarcodeValue;
import rs.data.goods.GoodGroup;
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.utils.GoodCardAdapterHelper;
import rs.utils.GoodCardAdapterHelper.GoodCardAdapter;
import rs.utils.GoodCardAdapterHelper.OnObjectClickListener;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class GoodList extends LinearLayout implements OnQueryTextListener, View.OnClickListener, BarcodeReceiver,OnObjectClickListener {

	public static interface OnItemLongClickListener {
		public void onLongClick(Object item);
	}
	
	private TextView _path;
	private GoodGroup _root;
	private RecyclerView _list;
	private SearchView _sw;
	private OnItemLongClickListener _ll;
	private boolean _enabledOnly;
	private class CategorySpan extends ClickableSpan {
		private GoodGroup _c;
		CategorySpan(GoodGroup c) {
			_c = c;
		}
		@Override
		public void onClick(View v) {
			setRoot(_c);
		}

		String getName() {
			if (_c == null)
				return "Все";
			return _c.name();
		}
	}

	public GoodList(Context context) {
		super(context);
	}
	
	public GoodList(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GoodList(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private void buildPath() {
		GoodGroup c = _root;
		List<CategorySpan> areas = new ArrayList<>();
		while(c != null) {
			areas.add(new CategorySpan(c));
			c = c.getParent();
		}
		areas.add(new CategorySpan(null));
		String p = "";
		for (int i = areas.size() - 1; i >= 0; i--) {
			if (!p.isEmpty())
				p += "/";
			p += areas.get(i).getName();
		}
		SpannableString s = new SpannableString(p);
		int iStart = 0, iEnd;
		for (int i = areas.size() - 1; i > 0; i--) {
			iEnd = iStart + areas.get(i).getName().length();
			s.setSpan(areas.get(i), iStart, iEnd,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			iStart += areas.get(i).getName().length() + 1;
		}
		_path.setMovementMethod(LinkMovementMethod.getInstance());
		_path.setText(s);
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_path = findViewById(R.id.lbl_path);
		_list = findViewById(R.id.v_list);
		_list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
		_sw = findViewById(R.id.sw_serach);
		_sw.setOnQueryTextListener(this);
	}
	public void setShowEnabledOnly() {
		_enabledOnly = true;
	}
	public void setRoot(GoodGroup c) {
		_root = c;
		_list.setAdapter(GoodCardAdapterHelper.createAdapter(getContext(), c == null ? 0 : c.id(),_ll,this));
		buildPath();
	}
	public GoodGroup getRoot() {
		return _root;
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(_list.getAdapter() == null)
			setRoot(_root);
		Core.getInstance().scaner().start(getContext(), this);
	}
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Core.getInstance().scaner().stop();
	}
	public void setOnItemLongClickListener(OnItemLongClickListener ll) {
		_ll = ll;
	}
	

	public void update() {
		setRoot(_root);
	}

	@Override
	public boolean onQueryTextChange(String s) {
		if(s.isEmpty())
			setRoot(_root);
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String s) {
		if(s==null || s.isEmpty()) {
			if(getAdapter().isSearch())
				setRoot(_root);
		} else  {
			_list.setAdapter(GoodCardAdapterHelper.createSearchAdapter(getContext(), s, _ll,this));
			_path.setText("Найдено:");
		}
		return false; 
	}
	
	private GoodCardAdapter getAdapter() {
		return (GoodCardAdapter)_list.getAdapter();
	}
	
	public boolean doBack() {
		if(getAdapter().isSearch()) {
			_sw.setQuery(Const.EMPTY_STRING, true);
			_sw.clearFocus();
			return false;
		} 
		if(_root!= null) {
			setRoot(_root.getParent());
			return false;
		}
		return true;
	}

	@Override
	public void onBarcode(BarcodeValue code) {
		_sw.setQuery(code.GOOD_CODE, true);
	}

	@Override
	public void onClick(View v) {
	}

	public void renew() {
		setRoot(null);
	}

	@Override
	public void onObjectClick(Object g) {
		if(g instanceof GoodGroup)
			setRoot((GoodGroup)g);
		else if(_ll != null) 
				_ll.onLongClick(g);
	}

}
