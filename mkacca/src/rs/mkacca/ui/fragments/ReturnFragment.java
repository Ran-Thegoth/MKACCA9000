package rs.mkacca.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;
import cs.ui.MainActivity;
import cs.ui.fragments.BaseFragment;
import rs.data.BarcodeValue;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.Document;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.fncore.data.Tag;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.ui.Main;
import rs.utils.SellOrderUtils;

public class ReturnFragment extends BaseFragment implements OnQueryTextListener, BarcodeReceiver, View.OnClickListener {

	private RecyclerView _list;
	private View _content, _l_lock;
	private SearchView _sw;
	
	private class DocumentHolder extends ViewHolder implements View.OnClickListener {
		private TextView _no, _type, _sum;
		private SellOrder _o;
		public DocumentHolder(View v) {
			super(v);
			_no = v.findViewById(R.id.lb_number);
			_type = v.findViewById(R.id.lb_type);
			_sum = v.findViewById(R.id.lb_sum);
			v.setOnClickListener(this);
		}
		public void bind(SellOrder o) {
			_o = o;
			_no.setText(String.valueOf(_o.signature().getFdNumber()));
			_type.setText(_o.getType().pName);
			_sum.setText(String.format("%.2f", _o.getTotalSum()));
		}
		@Override
		public void onClick(View v) {
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			b.setTitle(_o.getType().pName+", номер "+_o.signature().getFdNumber());
			b.setAdapter(new SellOrderUtils.SellItemsAdapter(getContext(), _o.getItems(), null), null);
			b.setPositiveButton("Возврат", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					doRefund(_o);
				}
			});
			b.setNegativeButton(android.R.string.cancel, null);
			b.show();
			
		}
		
	}
	private class DocumentAdapter extends Adapter<DocumentHolder> {

		private List<SellOrder> DOCUMENTS = new ArrayList<>();
		@Override
		public int getItemCount() {
			return DOCUMENTS.size();
		}

		@Override
		public void onBindViewHolder(DocumentHolder vh, int p) {
			vh.bind(DOCUMENTS.get(p));
		}

		@Override
		public DocumentHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			return new DocumentHolder(getActivity().getLayoutInflater().inflate(R.layout.return_row, vg,false));
		}
		
	}
	
	private DocumentAdapter _bills;
	public ReturnFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = inflater.inflate(R.layout.return_select, container,false);
			_list = _content.findViewById(R.id.v_list);
			_list.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
			_sw = _content.findViewById(R.id.sw_serach);
			_sw.setOnQueryTextListener(this);
			_content.findViewById(R.id.v_no_check).setOnClickListener(this);
			_l_lock = _content.findViewById(R.id.v_list_lock);
			_bills = new DocumentAdapter();
			new AsyncFNTask() {
				
				@Override
				protected int execute(FiscalStorage fs) throws RemoteException {
					Cursor c = getContext().getContentResolver().
							query(Const.DOCUMENT_JOURNAL,null,null,null,"DOCNO DESC");
					if(c.moveToFirst()) do {
						int id = c.getInt(c.getColumnIndex("DOCNO"));
						int type = c.getInt(c.getColumnIndex("DOCTYPE"));
						if( System.currentTimeMillis() - c.getLong(c.getColumnIndex("DOCDATE")) > Const.ONE_DAY) break;
						if(type == 4 || type == 3) {
							Tag doc = new Tag();
							if(fs.getExistingDocument(id, doc) == Errors.NO_ERROR) {
								SellOrder o = (SellOrder)doc.createInstance();
								if(o.getType() == OrderTypeE.INCOME || o.getType() == OrderTypeE.OUTCOME) {
									_bills.DOCUMENTS.add(o);
									if(_bills.DOCUMENTS.size() > 20) break;
								}
							}
						}
					} while(c.moveToNext());
					c.close();
					return 0;
				}
				protected void postExecute(int result, Object results) {
					_list.setAdapter(_bills);
					_l_lock.setVisibility(View.GONE);
				};
			}.execute();;
		}
		return _content;
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Возврат");
		Core.getInstance().scaner().start(getContext(), this);
	}
	@Override
	public void onStop() {
		Core.getInstance().scaner().stop();
		super.onStop();
	}
	@Override
	public boolean onQueryTextChange(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onQueryTextSubmit(final String no) {
		Main.lock();
		new AsyncFNTask() {
			Document _doc;
			@Override
			protected int execute(FiscalStorage fs) throws RemoteException {
				Tag tag = new Tag();
				if(fs.getExistingDocument(Integer.parseInt(no),tag) != Errors.NO_ERROR) return -1;
				_doc = tag.createInstance();
				if(!(_doc instanceof SellOrder)) return -1;
				SellOrder order = (SellOrder)_doc;
				if(order.getType() != OrderTypeE.INCOME && order.getType() != OrderTypeE.OUTCOME) return -1;
				return 0;
			}
			protected void postExecute(int result, Object results) {
				Main.unlock();
				if(result == 0) 
					doRefund((SellOrder)_doc);
			};
		}.execute();
		return true;
	}
	
	private void doRefund(SellOrder order ) {
		MainActivity m = (MainActivity)getActivity();
		getFragmentManager().popBackStack();
		m.showFragment(ReturnOrderFragment.newInstance(order));
		
	}
	
	@Override
	public void onBarcode(BarcodeValue code) {
		try {
			Uri uri = Uri.parse("https://nalog.ru/?"+code.CODE);
			if(Core.getInstance().kkmInfo().getFNNumber().equals(uri.getQueryParameter("fn"))) {
				String n = uri.getQueryParameter("i");
				if(n!=null) {
					_sw.setQuery(n, true);
					return;
				}
				throw new Exception();
			}
		} catch(Exception ioe) {
			Toast.makeText(getContext(), "Неверный чек", Toast.LENGTH_SHORT).show();
			
		}
		
	}
	@Override
	public void onClick(View arg0) {
		MainActivity m = (MainActivity)getActivity();
		getFragmentManager().popBackStack();
		TaxModeE mode =  Core.getInstance().kkmInfo().getTaxModes().iterator().next();
		m.showFragment(SellOrderFragment.newInstance(new SellOrder(OrderTypeE.RETURN_INCOME, mode)));
	}

}
