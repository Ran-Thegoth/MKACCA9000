package rs.mkacca.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import cs.U;
import cs.orm.DBObject;
import cs.orm.ORMHelper;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.PopupMenu;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BaseListFragment<T> extends BaseFragment implements View.OnClickListener, DialogInterface.OnClickListener  {

	private List<T> _items = new ArrayList<>();
	private RecyclerView _list;
	private T _selectedItem;
	private PopupMenu _menu = new PopupMenu();
	protected class ListViewHolder extends ViewHolder implements View.OnLongClickListener, View.OnClickListener{
		protected T _item; 
		public ListViewHolder(View v) {
			super(v);
			v.setOnClickListener(this);
			v.setOnLongClickListener(this);
		}
		public void update(T item) {
			_item = item;
			if(itemView instanceof TextView)
				((TextView)itemView).setText(stringifyItem(item));
			
		}
		@Override
		public boolean onLongClick(View arg0) {
			_selectedItem = _item;
			BaseListFragment.this.onLongClick(_item);
			return true;
		}
		@Override
		public void onClick(View arg0) {
			onItemSelected(_item);
		}
	}
	
	protected class ListFragmentAdapter extends Adapter<ListViewHolder> {
		@Override
		public int getItemCount() {
			return _items.size();
		}
		protected T getItem(int positoion) {
			return _items.get(positoion);
		}
		
		@Override
		public void onBindViewHolder(ListViewHolder holder,
				int position) {
			holder.update(getItem(position));
			decorateView(holder.itemView,holder._item,position);
		}

		@Override
		public ListViewHolder onCreateViewHolder(
				ViewGroup vg, int type) {
			return createHolder(type,vg);
		}
		
	}
	
	protected List<T> getItems() { return _items; }
	protected void setListItems(List<T> items) {
		_items = items;
		if(_items == null)
			_items = new ArrayList<>();
		if(_list!=null)
			_list.setAdapter(createAdapter());
	}
	
	public BaseListFragment() {
	}
	
	protected ListFragmentAdapter createAdapter() {
		return new ListFragmentAdapter();
	}
	protected ListViewHolder createHolder(int type, ViewGroup vg) {
		View v = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, vg,false);
		return new ListViewHolder(v);
	}
	
	@SuppressWarnings("unchecked")
	protected ListFragmentAdapter getAdapter() {
		return (ListFragmentAdapter)_list.getAdapter();
	}
	protected void setAdapter(ListFragmentAdapter a) {
		_list.setAdapter(a);
	}
	protected  String stringifyItem(T item) {
		return item.toString();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if(_list == null) 
			return 	new RecyclerView(getContext());
		return _list;
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if(_list == null) {
			if(view instanceof RecyclerView)
				_list = (RecyclerView)view;
			else 
				_list = view.findViewById(R.id.v_list);
			if(_list != null) {
				_list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
				_list.setAdapter(createAdapter());
			}
		}
	}
	@Override
	public void onStart() {
		super.onStart();
		setupButtons(this, R.id.iv_add);
	}
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.iv_add)
			onNewItem();
	}
	protected void onNewItem() {
	}

	protected void onEditItem(T item) {
	}
	protected void update() {
		if(_list == null) return;
		getAdapter().notifyDataSetChanged();
	}
	protected void onItemSelected(T item) { }
	protected boolean onDeleteItem(T item) {
		return false;
	}

	protected void setPopupMenuItems(T item, Menu menu) {
		menu.add(0, R.id.mi_edit, -1, R.string.do_edit);
		if(allowDelete())
			menu.add(0, R.id.mi_delete, -1, R.string.do_delete);
		if(item instanceof DBObject && allowDisable()) {
			if(((DBObject)item).isUsed()) 
				menu.add(0, R.id.mi_used, -1, R.string.do_not_use_in_future);
			else
				menu.add(0, R.id.mi_used, -1, R.string.use_in_future);
		}
	}
	protected void onLongClick(T item) {
		_menu.clear();
		setPopupMenuItems(item,_menu);
		if(_menu.size() > 0) {
			ArrayAdapter<String> a =new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1);
			for(int i=0;i<_menu.size();i++)
				a.add(_menu.getItem(i).getTitle().toString());
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			b.setAdapter(a,this);
			b.setTitle(stringifyItem(item));
			b.show();
		}
	}

	
	protected void onMenuItem(MenuItem menuItem, final T item) {
		switch(menuItem.getItemId()) {
		case R.id.mi_edit:
			onEditItem(item);
			break;
		case R.id.mi_delete:	
			U.confirm(getContext(), R.string.sure_to_delete_item, new Runnable() {
				public void run() {
					if(onDeleteItem(item)) {
						getItems().remove(_selectedItem);
						_selectedItem = null;
						update();
					}
				}
			});
			break;
		case R.id.mi_used:
			DBObject dbo = (DBObject)item;
			dbo.setUsed(!dbo.isUsed());
			try {
				if(ORMHelper.save(dbo))
					update();
			} catch(Exception e) {
				
			}
			break;
		}
		
	}
	@Override
	public void onClick(DialogInterface arg0, int what) {
		onMenuItem(_menu.getItem(what), _selectedItem);
	}
	
	@Override
	public void onStop() {
		_selectedItem = null;
		super.onStop();
	}
	@SuppressWarnings("deprecation")
	protected void decorateView(View v, T item, int position) {
		if (position % 2 == 0)
			v.setBackgroundColor(getResources().getColor(R.color.odd_color));
		else 
			v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
	}
	protected boolean allowDisable() { return true; }
	protected boolean allowDelete() { return true; }

}
