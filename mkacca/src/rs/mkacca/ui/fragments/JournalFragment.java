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
import rs.fncore.Const;
import rs.mkacca.R;
import rs.utils.Utils;

public class JournalFragment extends BaseFragment {

	
	private class RowHolder extends ViewHolder implements View.OnClickListener {
		private TextView _num,_type,_date,_ofd;
		private int _id;
		public RowHolder(View v) {
			super(v);
			_num  = v.findViewById(R.id.lb_number);
			_type = v.findViewById(R.id.lb_type);
			_date = v.findViewById(R.id.lb_date);
			_ofd  = v.findViewById(R.id.lb_sent);
			v.setOnClickListener(this);
		}
		@SuppressWarnings("deprecation")
		public void bind(Cursor c, int p) { 
			_id = c.getInt(c.getColumnIndex("DOCNO"));
			_num.setText(String.valueOf(_id));
			int type = c.getInt(c.getColumnIndex("DOCTYPE"));
			switch(type) {
			case 1: _type.setText("Фиск."); break;
			case 11: _type.setText("Изм. рекв."); break;
			case 3:
			case 4: _type.setText("Чек"); break;
			case 2: _type.setText("Откр. см."); break;
			case 5: _type.setText("Закр. см."); break;
			case 6: _type.setText("Архи."); break;
			case 21: _type.setText("Отчет"); break;
			case 31: 
			case 41: _type.setText("Кор."); break;
			default:
				_type.setText(String.valueOf(type));
			}
			long date = c.getLong(c.getColumnIndex("DOCDATE"));
			_date.setText(Utils.formatDateS(date));
			if(c.isNull(c.getColumnIndex("OFD"))) 
				_ofd.setText("Нет");
			else 
				_ofd.setText("Да");
			itemView.setBackgroundColor(getContext().getResources().getColor( p % 2 == 0 ? R.color.odd_color : android.R.color.transparent));
		}
		@Override
		public void onClick(View arg0) {
			showFragment(DocumentView.newInstance(_id));
			
		}
		
	}
	
	private class DocumentsAdapter extends Adapter<RowHolder> {

		private Cursor _c;
		DocumentsAdapter() {
			_c = getContext().getContentResolver().
					query(Const.DOCUMENT_JOURNAL,null,null,null,"DOCNO DESC");
		}
		@Override
		public int getItemCount() {
			return _c.getCount();
		}

		@Override
		public void onBindViewHolder(RowHolder vh, int p) {
			if(_c.moveToPosition(p))
				vh.bind(_c,p);
		}

		@Override
		public RowHolder onCreateViewHolder(ViewGroup vg, int p) {
			return new RowHolder(getActivity().getLayoutInflater().inflate(R.layout.journal_row, vg,false));
		}
		
	}
	
	private RecyclerView _list;
	public JournalFragment() {
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Журнал документов");
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_list == null) {
			_list = new RecyclerView(getContext());
			_list.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
			int p = (int)getContext().getResources().getDimension(R.dimen.tooltip_margin);
			_list.setPadding(p,p,p,p);
			_list.setAdapter(new DocumentsAdapter());
		}
		return _list;
	}

}
