package rs.mkacca.ui.fragments.editors;




import android.view.View;
import android.widget.ArrayAdapter;
import cs.ui.widgets.DialogSpinner;
import rs.data.goods.Variant;
import rs.fncore.data.MeasureTypeE;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.pages.Barcodes;

public class VariantEditor extends BaseEditor<Variant>  {
	private Barcodes _barcodes;
	public static VariantEditor newInstance(Variant bc,OnValueChangedListener<Variant> l) {
		VariantEditor result = new VariantEditor();
		result.setItem(bc);
		result.setOnChangedListener(l);
		return result;
	}
	public VariantEditor() {
		setEditorLayout(R.layout.variant_editor);
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Вариант");

	}
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	protected void beforeDataBind(View v) {
		DialogSpinner sp = v.findViewById(R.id.sp_mu);
		sp.setAdapter(new ArrayAdapter<MeasureTypeE>(getContext(), android.R.layout.simple_list_item_1,MeasureTypeE.values()));
	}
	@Override
	protected void afterDataBind(View v) {
		super.afterDataBind(v);
		_barcodes = v.findViewById(R.id.v_codes);
		_barcodes.setItem(getEditItem());
	}
	

	@Override
	protected boolean storeItem(Variant item) {
		_barcodes.obtain();
		return item.store();
	}
	
	
}
