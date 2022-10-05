package rs.mkacca.ui.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.Core;
import rs.mkacca.R;

public class SellReport extends BaseFragment {

	private class ReportRowHolder extends ViewHolder {

		private TextView _sName, _sQtty, _sSum;
		private String _name;
		private double _qtty,_sum;
		public ReportRowHolder(View v) {
			super(v);
			_sName = v.findViewById(R.id.lb_name);
			_sQtty = v.findViewById(R.id.lb_qtty);
			_sSum = v.findViewById(R.id.lb_sum);
		}
		private void update(Cursor c) {
			_qtty = c.getDouble(2);
			_sum = c.getDouble(1)*_qtty;
			_name = c.getString(4);
			_sName.setText(_name);
			_sQtty.setText(String.format("x %.3f", _qtty));
			_sSum.setText(String.format("%.2f", _sum));
		}
		
	}
	
	private class ReportAdapter extends Adapter<ReportRowHolder> {

		private Cursor _c;
		public ReportAdapter() {
			_c = Core.getInstance().db().getReadableDatabase().rawQuery("SELECT A.SHIFT, A.PRICE, A.QTTY,A.STYPE,B.NAME FROM SHIFT_SELLS A "
					+ "INNER JOIN VARIANTS B on B._id = A.SID AND A.STYPE = 1 "
					+ "UNION ALL "
					+ "SELECT A.SHIFT, A.PRICE, A.QTTY,A.STYPE,C.NAME FROM SHIFT_SELLS A "
					+ "INNER JOIN GOODS C on C._id = A.SID AND A.STYPE = 0"
					,null);
		}
		@Override
		public int getItemCount() {
			return _c.getCount();
		}

		@Override
		public void onBindViewHolder(ReportRowHolder vh, int p) {
			_c.moveToPosition(p);
			vh.update(_c);
			
		}

		@Override
		public ReportRowHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			return new ReportRowHolder(getActivity().getLayoutInflater().inflate(R.layout.report_row, vg,false));
		}
		
	}
	
	private RecyclerView _rw;
	public SellReport() {
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_rw == null ) {
			_rw = new RecyclerView(getContext());
			_rw.setPadding(6, 6, 6, 6);
			_rw.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
			_rw.setAdapter(new ReportAdapter());
		}
		return _rw;
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Отчет о продажах");
	}

}
