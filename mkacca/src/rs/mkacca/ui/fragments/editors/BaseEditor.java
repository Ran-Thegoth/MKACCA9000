package rs.mkacca.ui.fragments.editors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import cs.U;
import cs.ui.UIBinder;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;

public class BaseEditor<T> extends BaseFragment implements OnClickListener,cs.ui.BackHandler {
	public static interface OnValueChangedListener<T> {
		public void onChanged(T value);
		public void onDeleted(T value);
	}
	private T _item;
	private int _editorLayout;
	
	private OnValueChangedListener<T> _listener;
	protected void setEditorLayout(int layout) {
		_editorLayout = layout;
	}
	public void setOnChangedListener(OnValueChangedListener<T> l) {
		_listener = l;
	}
	public void setItem(T item) {
		_item = item;
	}
	protected T getEditItem() { return _item; }
	private View _content;
	public BaseEditor() {
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if(_content == null) {
			if(_editorLayout != 0) 
				_content = inflater.inflate(_editorLayout, container,false);
			if(_content != null) {
				beforeDataBind(_content);
				UIBinder.bind(_item, _content);
				afterDataBind(_content);
			} else
				return super.onCreateView(inflater, container, savedInstanceState);
		}
		return _content;
	}
	@Override
	public void onStart() {
		super.onStart();
		setupButtons(this, R.id.iv_save);
	}
	protected void doSave() {
		if(!checkSaveConditions(_item)) return;
		if(UIBinder.obtain(getView(), _item) && storeItem(_item)) {
			if(_listener != null)
				_listener.onChanged(getEditItem());
			getFragmentManager().popBackStack();
		}
	}
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.iv_save) {
			doSave();
		}
	}
	protected boolean deleteItem(T item) {
		return true;
	}
	protected void doDelete() {
		if(!deleteItem(getEditItem())) return;
		if(_listener != null)
			_listener.onDeleted(getEditItem());
		getFragmentManager().popBackStack();
		
	}
	protected void beforeDataBind(View v) { }
	protected void afterDataBind(View v) {} 
	protected boolean checkSaveConditions(T item) {
		return true;
	}
	protected boolean storeItem(T item) {
		return true;
	}
	@Override
	public boolean onBackPressed() {
		U.confirm(getActivity(), R.string.request_save_changes, new Runnable() {
			@Override
			public void run() {
				doSave();
			}
		}, new Runnable() {
			@Override
			public void run() {
				getFragmentManager().popBackStack();
			}
		});
		return false;
	}

}
