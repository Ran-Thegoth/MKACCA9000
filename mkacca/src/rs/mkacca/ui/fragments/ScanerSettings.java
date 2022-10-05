package rs.mkacca.ui.fragments;

import java.util.Collection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import cs.ui.fragments.BaseFragment;
import cs.ui.widgets.DialogSpinner;
import rs.data.AppSettings;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.scaner.BarcodeScaner;

public class ScanerSettings extends BaseFragment implements  View.OnClickListener {

	private View _content;
	private DialogSpinner _sp;
	public ScanerSettings() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Сканер штрихкодов");
		setupButtons(this, R.id.iv_save);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = inflater.inflate(R.layout.scaner_settings, container,false);
			_sp = _content.findViewById(R.id.sp_scan_type);
			Collection<String> c = BarcodeScaner.knownEngines().keySet();
			_sp.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,c.toArray(new String[c.size()])));
			_sp.setSelectedItem(Core.getInstance().appSettings().scanEngineName());
			
		}
		return _content;
	}
	
	@Override
	public void onClick(View arg0) {
		AppSettings as = Core.getInstance().appSettings();
		as.setScanEngineName(_sp.getSelectedItem().toString());
		as.store();
		getFragmentManager().popBackStack();
	}

}
