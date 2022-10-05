package rs.mkacca.ui.widgets.pages;



import cs.ui.UIRecyclerAdapter;
import rs.data.goods.Variant;
import rs.data.BarcodeValue;
import rs.data.goods.Barcode;
import rs.data.goods.Good;
import rs.data.goods.IBarcodeOwner;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.ui.RequestStringDialog;
import rs.mkacca.ui.RequestStringDialog.OnValueConfirmListener;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.widgets.ItemCard;
import rs.mkacca.ui.widgets.ItemList;
import rs.mkacca.ui.widgets.ItemList.EditorFragmentFactory;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class Barcodes extends LinearLayout implements ItemCard<IBarcodeOwner>,EditorFragmentFactory<Barcode>,OnValueConfirmListener,BarcodeReceiver {

	private ItemList<Barcode> _itemList;
	private IBarcodeOwner _var;
	private Barcode _edited;
	
	public Barcodes(Context context) {
		super(context);
		
	}

	public Barcodes(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Barcodes(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_itemList = new ItemList<>(this,this,R.id.v_barcode_list);
		
	}
	
	public UIRecyclerAdapter getAdapter() { return _itemList.getAdapter(); }
	
	@Override
	public void setItem(IBarcodeOwner item) {
		_var = item;
		_itemList.setAdapter(new UIRecyclerAdapter(getContext(), _var.barcodes()));
		_itemList.setSelectable(true);
	}


	@Override
	public Fragment newInstance(Barcode value, OnValueChangedListener<Barcode> l) {
		_edited = value;
		new RequestStringDialog(R.string.barcode, getContext(), _edited.toString(), this);
		return null; // VariantEditor.newInstance(value,l);
	}

	@Override
	public Barcode newObject() {
		return new Barcode(_var);
	}
	
	public boolean has(String code) {
		for(int i=0;i<_itemList.getAdapter().getItemCount();i++) {
			Barcode bc = (Barcode)_itemList.getAdapter().get(i);
			if(code.equals(bc.toString()))
				return true;
		}
		return false;
	}

	public boolean obtain() {
		_var.barcodes().clear();
		_var.barcodes().addAll(_itemList.getAdapter().asList(Barcode.class));
		return true;
	}

	@Override
	public void onConfirm(String r) {
		if(_edited != null) {
			_edited.setCode(r);
			if(!_itemList.getAdapter().contains(_edited))
				_itemList.getAdapter().add(_edited);
			_itemList.getAdapter().refresh();
			_edited = null;
		}
		
	}

	@Override
	public View getView() {
		return this;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Core.getInstance().scaner().start(getContext(), this);
	}
	@Override
	protected void onDetachedFromWindow() {
		Core.getInstance().scaner().stop();
		super.onDetachedFromWindow();
	}
	@Override
	public void onBarcode(BarcodeValue code) {
		if(!has(code.GOOD_CODE))
			_itemList.getAdapter().add(new Barcode(_var,code.GOOD_CODE));
		
	}



}
