package rs.mkacca.ui.widgets;

import cs.U;
import cs.ui.UIRecyclerAdapter;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import cs.ui.MainActivity;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ItemList<T> implements OnItemClickListener, OnValueChangedListener<T>, OnClickListener {

	public static interface EditorFragmentFactory<T> {
		Fragment newInstance(T value, OnValueChangedListener<T> l);

		T newObject();
	}

	private View _edit, _delete;
	private RecyclerView _list;
	private UIRecyclerAdapter _adapter;
	private EditorFragmentFactory<T> _ff;
	private Context _ctx;

	public ItemList(View v, EditorFragmentFactory<T> ff, int id) {
		_ctx = v.getContext();
		_ff = ff;
		_edit = v.findViewById(R.id.iv_edit);
		_delete = v.findViewById(R.id.iv_del);
		_edit.setOnClickListener(this);
		_delete.setOnClickListener(this);
		v.findViewById(R.id.iv_add).setOnClickListener(this);
		_list = v.findViewById(id);
		_list.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.VERTICAL, false));
	}

	public void setAdapter(UIRecyclerAdapter a) {
		if (a == null)
			a = (UIRecyclerAdapter) _list.getAdapter();
		if (_list.getAdapter() != a)
			_list.setAdapter(a);
		_adapter = a;
		_adapter.setOnItemClickListener(this);
		_edit.setEnabled(a.size() > 0);
		_delete.setEnabled(a.size() > 0);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int p, long arg3) {
		_edit.setEnabled(p != -1);
		_delete.setEnabled(p != -1);

	}

	@Override
	public void onChanged(T value) {
		if (!_adapter.contains(value))
			_adapter.add(value);
		else
			_adapter.refresh();
	}

	@Override
	public void onDeleted(T value) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_add:
			performNew();
			break;
		case R.id.iv_edit:
			Fragment f = _ff.newInstance((T) _adapter.getSelectedItem(), this);
			if (f != null) {
				if (f instanceof DialogFragment)
					((DialogFragment) f).show(((MainActivity) v.getContext()).getSupportFragmentManager(),
							f.getClass().getName());
				else
					((MainActivity) v.getContext()).showFragment(f);
			}
			break;
		case R.id.iv_del:
			U.confirm(v.getContext(), R.string.sure_to_delete_item, new Runnable() {
				@Override
				public void run() {
					_adapter.remove(_adapter.getSelectedItem());
					_adapter.refresh();
				}
			});
		}

	}

	public UIRecyclerAdapter getAdapter() {
		return _adapter;
	}

	public void setSelectable(boolean val) {
		_adapter.setSelectable(val);
	}

	public void performNew() {
		Fragment f = _ff.newInstance(_ff.newObject(), this);
		if (f == null)
			return;
		if (f instanceof DialogFragment)
			((DialogFragment) f).show(((MainActivity) _ctx).getSupportFragmentManager(), f.getClass().getName());
		else
			((MainActivity) _ctx).showFragment(f);
	}

}
