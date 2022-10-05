package rs.mkacca.ui.widgets.pages;



import cs.ui.UIRecyclerAdapter;
import rs.data.goods.Variant;
import rs.data.goods.Good;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.VariantEditor;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.widgets.ItemCard;
import rs.mkacca.ui.widgets.ItemList;
import rs.mkacca.ui.widgets.ItemList.EditorFragmentFactory;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class Variants extends LinearLayout implements ItemCard<Good>,EditorFragmentFactory<Variant> {

	private ItemList<Variant> _itemList;
	private Good _good;
	
	public Variants(Context context) {
		super(context);
		
	}

	public Variants(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Variants(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_itemList = new ItemList<>(this,this,R.id.v_variants);
		
	}
	@Override
	public void setItem(Good item) {
		_good = item;
		_itemList.setAdapter(new UIRecyclerAdapter(getContext(), _good.variants()));
		_itemList.setSelectable(true);
	}


	@Override
	public Fragment newInstance(Variant value, OnValueChangedListener<Variant> l) {
		return VariantEditor.newInstance(value,l);
	}

	@Override
	public Variant newObject() {
		return new Variant(_good);
	}

	@Override
	public boolean obtain() {
		_good.variants().clear();
		_good.variants().addAll(_itemList.getAdapter().asList(Variant.class));
		return true;
	}
	
	@Override
	public View getView() {
		return this;
	}


}
