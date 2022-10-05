package rs.mkacca.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Parcel;
import android.view.View;
import android.widget.Toast;
import cs.U;
import rs.data.goods.Good;
import rs.data.goods.ISellable;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.mkacca.R;
import rs.utils.SellOrderUtils;
import rs.utils.SellOrderUtils.OnSellItemClickListener;

public class ReturnOrderFragment extends SellOrderFragment  {

	
	private class RefundGood implements ISellable {

		private SellItem _item;
		public RefundGood(SellItem item) {
			_item = item;
			_item.attach(this);
		}
		@Override
		public SellItem createSellItem() {
			SellItem result = new SellItem(_item.getName(), _item.getQTTY(), _item.getPrice(), _item.getVATType());
			result.attach(this);
			return result;
		}

		@Override
		public Good good() {
			return new Good();
		}

		@Override
		public double price() {
			return _item.getPrice().doubleValue();
		}

		@Override
		public double maxQtty() {
			return _item.getQTTY().doubleValue();
		}

		@Override
		public MeasureTypeE measure() {
			return _item.getMeasure();
		}
		@Override
		public void writeToParcel(Parcel p) {
			p.writeByte((byte)3);
		}
		@Override
		public String name() {
			return _item.getName();
		}
		@Override
		public double qtty() {
			return _item.getQTTY().doubleValue();
		}
		@Override
		public long id() {
			return 0;
		}
		@Override
		public int getType() {
			return 0;
		}
		
	}
	
	public static ReturnOrderFragment newInstance(SellOrder o) {
		ReturnOrderFragment result = new ReturnOrderFragment();
		result._order = new SellOrder(o.getType() == OrderTypeE.INCOME ? OrderTypeE.RETURN_INCOME : OrderTypeE.RETURN_OUTCOME , o.getTaxMode());
		result._baseItems = new ArrayList<>(o.getItems());
		return result;
	}
	
	private OnSellItemClickListener ADDER = new OnSellItemClickListener() {
		@Override
		public void onClick(SellItem item) {
			_baseItems.remove(item);
			onChanged(new RefundGood(item).createSellItem());
			_adapter._dialog.dismiss();
		}
		
	};
	private List<SellItem> _baseItems;
	private class ItemsToRefundAdapter extends SellOrderUtils.SellItemsAdapter {

		private AlertDialog _dialog;
		public ItemsToRefundAdapter(Context ctx, List<SellItem> list, OnSellItemClickListener l) {
			super(ctx, list, l);
		}
		
	}
	private ItemsToRefundAdapter _adapter;
	public ReturnOrderFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void bind(View v) {
		v.findViewById(R.id.sp_order_type).setEnabled(false);
		v.findViewById(R.id.sp_sno_type).setEnabled(false);
	}
	@Override
	protected void onAddItem() {
		if(_baseItems.isEmpty()) {
			Toast.makeText(getContext(), "Нет предметов расчета для возврата", Toast.LENGTH_SHORT).show();
			return;
		}
		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
		b.setTitle("К возрату");
		_adapter = new  ItemsToRefundAdapter(getContext(),_baseItems,ADDER); 
		b.setAdapter(_adapter, null);
		_adapter._dialog = b.create();
		_adapter._dialog.show();
	}

	@Override
	public void onDeleted(SellItem value) {
		super.onDeleted(value);
		RefundGood rg = (RefundGood)value.attachment();
		_baseItems.add(rg._item);
	}
	
	@Override
	public boolean onBackPressed() {
		U.confirm(getContext(), "Отменить чек возврата?", new Runnable() {
			@Override
			public void run() {
				getFragmentManager().popBackStack();
			}
		});
		return false;
	}

}
