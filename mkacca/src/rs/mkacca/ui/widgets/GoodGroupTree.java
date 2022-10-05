package rs.mkacca.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import rs.data.goods.GoodGroup;

public class GoodGroupTree extends RecyclerView {

	public static interface OnGroupSelectListener {
		public void OnGroupSelect(GoodGroup g);
	}
	
	private class GoodGroupWrapper {
		private GoodGroup _g;
		private int offset = 0;
		GoodGroupWrapper(GoodGroup g) {
			_g = g;
			GoodGroup c = g;
			while(c != null) {
				offset += 14;
				c = c.getParent();
			}
		}
		public String toString() {
			if(_g == null)
				return "Без категории";
			else
				return _g.name();
		}
	}
	
	private class GoodGroupHolder extends ViewHolder implements OnClickListener {
		private TextView _v;
		private GoodGroupWrapper _item;
		public GoodGroupHolder(View itemView) {
			super(itemView);
			_v = (TextView)itemView;
			_v.setOnClickListener(this);
		}

		@Override
		public void onClick(View arg0) {
			if(_l != null)
				_l.OnGroupSelect(_item._g);
		}
		public void update(GoodGroupWrapper w) {
			_item = w;
			_v.setText(w.toString());
			_v.setPadding(w.offset, 8, 8, 0);
		}
		
	}
	private class GoodGroupAdapter extends Adapter<GoodGroupHolder> {
		
		GoodGroupAdapter() {
		}
		@Override
		public int getItemCount() {
			return _items.size();
		}

		@Override
		public void onBindViewHolder(GoodGroupHolder vh, int p) {
			vh.update(_items.get(p));
		}

		@Override
		public GoodGroupHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			TextView v = new TextView(getContext());
			v.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getContext().getResources().getDisplayMetrics()));
			return new GoodGroupHolder(v);
		}
		
	}
	
	private List<GoodGroupWrapper> _items = new ArrayList<>();
	private OnGroupSelectListener _l;
	
	private void addGroup(GoodGroup parent) {
		_items.add(new GoodGroupWrapper(parent));
		for(GoodGroup g : GoodGroup.getChildrenFor(parent)) {
			addGroup(g);
		}
	}
	
	public GoodGroupTree(Context context) {
		super(context);
		setupUI();
	}

	public GoodGroupTree(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUI();
	}

	
	
	public GoodGroupTree(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
		setupUI();
		
	}
	
	private void setupUI() {
		setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		addGroup(null);
		setPadding(8, 12, 8, 8);
		setAdapter(new GoodGroupAdapter());
	}
	public void setOnGroupSelectListener(OnGroupSelectListener l ) {
		_l = l;
	}
	
}
