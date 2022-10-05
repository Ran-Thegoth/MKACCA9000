package rs.mkacca.ui.fragments.editors;


import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import cs.U;
import cs.orm.ORMHelper;
import cs.ui.UIBinder;
import cs.ui.widgets.DialogSpinner;
import rs.data.goods.Good;
import rs.fncore.Const;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.ItemCard;

public class GoodEditor extends BaseEditor<Good> implements OnItemSelectedListener {
	
	private ViewPager _pager;
	private View _content, _left,_right;
	private DialogSpinner _pageName;

	private class EditPagesAdapter extends PagerAdapter implements OnPageChangeListener, View.OnClickListener {
		private View _curPage;
		private final int [] PAGED_ID = {R.layout.good_page_1,R.layout.good_page_3,R.layout.good_page_4};
		private View [] PAGES = new View[PAGED_ID.length];
		private String [] PAGE_NAMES = {"Основное","Варианты","Штрихкода"};

		@SuppressWarnings("unchecked")
		public EditPagesAdapter() {
			for(int i=0;i<PAGED_ID.length;i++)
			if(PAGES[i] == null) {
				View v  = getActivity().getLayoutInflater().inflate(PAGED_ID[i], new LinearLayout(getContext()),false);
				PAGES[i] = v;
				if(v instanceof ItemCard)
					((ItemCard<Good>)v).setItem(getEditItem());
				
			}
			
			// TODO Auto-generated constructor stub
		}
		public int getCount() {
			return PAGED_ID.length;
		}

		@Override
		public boolean isViewFromObject(View v, Object object) {
			return v == object;
		}

		@Override
		public int getItemPosition(Object object) {
			for(int i=0;i<PAGES.length;i++)
				if(PAGES[i] == object)
					return i;
			return -1;
		}

		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if(PAGES[position].getParent() == null) {
				container.addView(PAGES[position]);				
			}
			return PAGES[position];
		}
		
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int page) {
			UIBinder.obtain(_curPage, getEditItem(),false);
			UIBinder.bind(getEditItem(), PAGES[page]);
			_left.setEnabled(page > 0);
			_right.setEnabled(page < getCount()-1);
			_pageName.setSelection(page);
			_curPage = PAGES[page];
		}

		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.iv_left)
				_pager.setCurrentItem(_pager.getCurrentItem()-1);
			else 
				_pager.setCurrentItem(_pager.getCurrentItem()+1);
			
		}
		
	}
	
	public static final GoodEditor newInstance(Good g,OnValueChangedListener<Good> l) {
		GoodEditor result = new GoodEditor();
		result.setItem(g);
		result.setOnChangedListener(l);
		return result;
	}
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = inflater.inflate(R.layout.paged_editor,container,false);
			_pager = _content.findViewById(R.id.v_pages);
			
			_left = _content.findViewById(R.id.iv_left);
			_right = _content.findViewById(R.id.iv_right);
			_pageName = _content.findViewById(R.id.lbl_page_name);
			
			
			EditPagesAdapter adapter = new EditPagesAdapter();
			_pager.setOffscreenPageLimit(adapter.PAGED_ID.length);
			_pager.setAdapter(adapter);
			_pager.setOnPageChangeListener(adapter);
			_pageName.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,adapter.PAGE_NAMES));
			_pageName.setOnItemSelectedListener(this);
			adapter.onPageSelected(0);
			
			_left.setOnClickListener(adapter);
			_right.setOnClickListener(adapter);
		}
		return _content;
	};
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Номенклатура");
	}
	@SuppressWarnings("rawtypes")
	@Override
	protected boolean storeItem(Good item) {
		for(View v : ((EditPagesAdapter)_pager.getAdapter()).PAGES) {
			if(!UIBinder.obtain(v, item)) return false;
			if(v instanceof ItemCard)
				((ItemCard)v).obtain();
		}
		if(item.baseMU() == null) {
			Toast.makeText(getContext(), "Не указана основаная единица измерения", Toast.LENGTH_SHORT).show();
			return false;
		}
		if(!item.store()) {
			String error = Const.EMPTY_STRING;
			if(ORMHelper.getLastError()!=null)
				error = ORMHelper.getLastError().getLocalizedMessage();
			U.notify(getContext(), "Ошибка сохранения:"+error);
			return false;
		}
		return true;
	}
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int p,
			long arg3) {
		if(_pager.getCurrentItem() != p)
			_pager.setCurrentItem(p);
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
