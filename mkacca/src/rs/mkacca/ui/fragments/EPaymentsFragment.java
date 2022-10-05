package rs.mkacca.ui.fragments;

import java.util.Iterator;
import java.util.Map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;

public class EPaymentsFragment extends BaseFragment {

	private ListView _list;
	public EPaymentsFragment() {
	}
	

	
	private class PaymentEngineAdapter extends BaseAdapter {

		private class RowHolder implements OnCheckedChangeListener,OnClickListener {
			private View _row;
			private Switch _state;
			private EPayment _engine;
			RowHolder(ViewGroup vg) {
				_row = getActivity().getLayoutInflater().inflate(R.layout.payment_engine,vg,false);
				_row.setTag(this);
				_state = _row.findViewById(R.id.sw_engine_state);
				_state.setOnCheckedChangeListener(this);
				_row.findViewById(R.id.iv_more).setOnClickListener(this);
			}
			public void update(Map.Entry<String, EPayment> e) {
				_state.setOnCheckedChangeListener(null);
				_state.setText(e.getKey());
				_engine = e.getValue();
				_state.setChecked(_engine.isEnabled());
				_state.setOnCheckedChangeListener(this);
			}
			@Override
			public void onClick(View v) {
				showFragment(EngineSetupFragment.newInstance(_engine));
			}
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean val) {
				_engine.setEnabled(val);
				if(val) onClick(null);
			}
		}
		@Override
		public int getCount() {
			return EPayment.knownEngines().size();
		}

		@Override
		public Map.Entry<String, EPayment> getItem(int pos) {
			Iterator<Map.Entry<String, EPayment>> i = EPayment.knownEngines().entrySet().iterator();
			while(pos-- > 0) i.next();
			return i.next();
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int pos, View v, ViewGroup vg) {
			if(v == null) v = new RowHolder(vg)._row;
			RowHolder holder = (RowHolder)v.getTag();
			holder.update(getItem(pos));
			return v;
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_list == null) {
			_list = new ListView(getContext());
			int p = (int)getResources().getDimension(R.dimen.tooltip_margin);
			_list.setPadding(p, p, p, p);
			_list.setAdapter(new PaymentEngineAdapter());
		}
		return _list;
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Безналичная оплата");
	}
	

}
