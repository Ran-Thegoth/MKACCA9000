package rs.mkacca.ui.fragments.editors;

import android.view.View;
import rs.data.goods.GoodGroup;
import rs.mkacca.R;

public class GoodGroupEditor extends BaseEditor<GoodGroup> {
	public static GoodGroupEditor newInstance(GoodGroup c, OnValueChangedListener<GoodGroup> l) {
		GoodGroupEditor result = new GoodGroupEditor();
		result.setItem(c);
		result.setOnChangedListener(l);
		return result;
	}
	
	private GoodGroup _item;
	
	public void setItem(GoodGroup item) {
		_item = item;
		GoodGroup shadow = new GoodGroup();
		shadow.assign(item);
		super.setItem(shadow);
	};
	@Override
	protected void beforeDataBind(View v) {
	}
	public GoodGroupEditor() {
		setEditorLayout(R.layout.good_category_editor);
	}
	@Override
	protected boolean storeItem(GoodGroup item) {
		_item.assign(item);
		if(_item.store()) {
			super.setItem(_item);
			return true;
		}
		return false;	
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Группа номенклатур");
	}
}
