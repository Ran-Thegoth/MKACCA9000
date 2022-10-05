package rs.mkacca.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import cs.ui.fragments.BaseFragment;
import rs.data.goods.Good;
import rs.data.goods.ISellable;
import rs.fncore.data.SellItem;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.widgets.Favorites;
import rs.mkacca.ui.widgets.GoodList;
import rs.mkacca.ui.widgets.GoodList.OnItemLongClickListener;

public class GoodSelectFragment extends BaseFragment implements TabContentFactory, View.OnClickListener, OnItemLongClickListener {

	private static final String FAV = "FAV";
	private static final String LIST = "LIST";
	private TabHost _tabs;
	private GoodList _list;
	private OnValueChangedListener<SellItem> _l;
	public static GoodSelectFragment newInstance(OnValueChangedListener<SellItem> l) {
		GoodSelectFragment result = new GoodSelectFragment();
		result._l = l;
		return result;
	}
	public GoodSelectFragment() {
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Номенклатура");
		setupButtons(this, R.id.iv_add);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if(_tabs == null) { 
			_tabs  = (TabHost)inflater.inflate(R.layout.tabs, container,false);
			_tabs.setup();
			TabSpec s = _tabs.newTabSpec(FAV);
			s.setContent(this);
			s.setIndicator("Предпочтения");
			_tabs.addTab(s);
			s = _tabs.newTabSpec(LIST);
			s.setContent(this);
			s.setIndicator("Все");
			_tabs.addTab(s);
		}
		return _tabs;
	}
	@Override
	public View createTabContent(String tab) {
		if(FAV.equals(tab)) {
			Favorites fav = new Favorites(getContext(),this);
			fav.setPadding(0, 8, 0, 0);
			return fav;
		} else {
			_list = (GoodList)getActivity().getLayoutInflater().inflate(R.layout.good_list, _tabs,false);
			_list.setOnItemLongClickListener(this);
			_list.setShowEnabledOnly();
			return _list;
		}
			
	}
	@Override
	public void onClick(View v) {
	}
	@Override
	public void onLongClick(Object item) {
		if(item instanceof Good) {
			final Good g = (Good)item;
			if(g.variants().size() > 0) {
				AlertDialog.Builder b = new AlertDialog.Builder(getContext());
				b.setTitle(g.name());
				final ListAdapter a = g.getVariantsAdapter(getContext()); 
				b.setAdapter(a, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int p) {
						ISellable s = (ISellable)a.getItem(p);
						_l.onChanged(s.createSellItem());
						getFragmentManager().popBackStack();
					}
				});
				b.show();
			}
			else  {
				_l.onChanged(g.createSellItem());
				getFragmentManager().popBackStack();
			}
		}
	}

}
