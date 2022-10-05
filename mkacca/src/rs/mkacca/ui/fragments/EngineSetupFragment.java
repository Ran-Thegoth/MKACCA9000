package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;

public class EngineSetupFragment extends BaseFragment implements View.OnClickListener {

	private LinearLayout _content;
	public static EngineSetupFragment newInstance(EPayment engine) {
		EngineSetupFragment result = new EngineSetupFragment();
		result._engine = engine;
		return result;
	}
	
	private EPayment _engine;
	public EngineSetupFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = new LinearLayout(getContext());
			int p = (int)getResources().getDimension(R.dimen.tooltip_margin);
			_content.setPadding(p, p, p, p);
			_content.setOrientation(LinearLayout.VERTICAL);
			_engine.setup(_content);
		}
		return _content;
	}
	@Override
	public void onStart() {
		super.onStart();
		for(String s : EPayment.knownEngines().keySet()) {
			if(EPayment.knownEngines().get(s) == _engine) {
				getActivity().setTitle(s);
				break;
			}
		}
		setupButtons(this, R.id.iv_save);
	}
	@Override
	public void onClick(View arg0) {
		if(_engine.applySetup(_content))
			getFragmentManager().popBackStack();
		
	}

}
