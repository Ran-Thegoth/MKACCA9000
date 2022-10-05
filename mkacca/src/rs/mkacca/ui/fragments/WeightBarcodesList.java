package rs.mkacca.ui.fragments;

import rs.data.WeightBarcodeParser;
import rs.data.WeigthService;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.fragments.editors.WeightBarcodeEditor;

public class WeightBarcodesList extends BaseListFragment<WeightBarcodeParser> implements OnValueChangedListener<WeightBarcodeParser> {
	public WeightBarcodesList() {
		setListItems(WeigthService.getAll());
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle(R.string.weight_barcodes);
	}
	@Override
	protected void onNewItem() {
		onEditItem(new WeightBarcodeParser());
	}
	@Override
	protected void onEditItem(WeightBarcodeParser item) {
		showFragment(WeightBarcodeEditor.newInstance((WeightBarcodeParser)item, this));
	}
	@Override
	protected boolean onDeleteItem(WeightBarcodeParser item) {
		return  ((WeightBarcodeParser)item).delete(); 
	}
	@Override
	public void onChanged(WeightBarcodeParser value) {
		if(!getItems().contains(value))
			getItems().add(value);
		update();
	}
	@Override
	public void onDeleted(WeightBarcodeParser value) {
		
	}
	@Override
	protected boolean allowDisable() {
		return false;
	}

}
