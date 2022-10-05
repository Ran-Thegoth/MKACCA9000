package rs.utils;

import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import rs.fncore.data.SellItem;
import rs.mkacca.R;

public class SellOrderUtils {

	public SellOrderUtils() {
		// TODO Auto-generated constructor stub
	}
	
	public static interface OnSellItemClickListener {
		public void onClick(SellItem item);
	}
	
	private static class SellItemCard extends ViewHolder implements View.OnClickListener {
		private SellItem _item;
		private TextView _name, _type, _sellType, _price;
		private OnSellItemClickListener _l;

		public SellItemCard(View itemView, OnSellItemClickListener l) {
			super(itemView);
			_l = l;
			_name = itemView.findViewById(R.id.lbl_name);
			_type = itemView.findViewById(R.id.lbl_item_type);
			_sellType = itemView.findViewById(R.id.lbl_item_sell_type);
			_price = itemView.findViewById(R.id.lbl_price);
			itemView.setOnClickListener(this);
		}

		public void update(SellItem item) {
			_item = item;
			_name.setText(_item.getName());
			_type.setText(_item.getType().toString());
			_sellType.setText(_item.getPaymentType().pName);
			_price.setText(String.format("%.3f %s x %.2f = %.2f", _item.getQTTY().doubleValue(),
					item.getMeasure().pName, item.getPrice().doubleValue(), item.getSum().doubleValue()));
		}

		@Override
		public void onClick(View arg0) {
			if(_l != null)
				_l.onClick(_item);
//			

		}

	}
	
	public static  class SellItemsAdapter extends Adapter<SellItemCard> implements ListAdapter {

		private List<SellItem> _items;
		private OnSellItemClickListener _l;
		private LayoutInflater _inflater;
		public SellItemsAdapter(Context ctx, List<SellItem> list, OnSellItemClickListener l) {
			_items = list;
			_l = l;
			_inflater = LayoutInflater.from(ctx);
		}
		@Override
		public int getItemCount() {
			return _items.size();
		}

		@Override
		public void onBindViewHolder(SellItemCard h, int p) {
			h.update(_items.get(p));
		}

		@Override
		public SellItemCard onCreateViewHolder(ViewGroup vg, int arg1) {
			return new SellItemCard(_inflater.inflate(R.layout.sell_item_card, vg, false),_l);
		}
		@Override
		public int getCount() {
			return getItemCount();
		}
		@Override
		public SellItem getItem(int index) {
			return _items.get(index);
		}
		@Override
		public View getView(int index, View v, ViewGroup vg) {
			if(v  == null) {
				SellItemCard card = createViewHolder(vg, 0);
				v = card.itemView;
				v.setTag(card);
			}
			SellItemCard card =  (SellItemCard)v.getTag();
			card.update(getItem(index));
			return v;
		}
		@Override
		public int getViewTypeCount() {
			return 1;
		}
		@Override
		public boolean isEmpty() {
			return _items.isEmpty();
		}
		@Override
		public void registerDataSetObserver(DataSetObserver o) {
			
		}
		@Override
		public void unregisterDataSetObserver(DataSetObserver o) {
			
		}
		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}
		@Override
		public boolean isEnabled(int arg0) {
			return true;
		}

	}

}
