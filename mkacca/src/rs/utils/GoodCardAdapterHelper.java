package rs.utils;

import java.security.acl.Group;

import com.google.zxing.common.StringUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cs.U;
import cs.orm.ORMHelper;
import rs.data.goods.Good;
import rs.data.goods.GoodGroup;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.GoodList.OnItemLongClickListener;

public class GoodCardAdapterHelper {

	public static interface OnObjectClickListener {
		void onObjectClick(Object g);
	}

	private static int ODD_COLOR;

	public static class GoodCardHolder extends ViewHolder {

		private long _id;
		private int _type;
		private TextView _name, _vars;

		public GoodCardHolder(View itemView) {
			super(itemView);
			if (ODD_COLOR == 0)
				ODD_COLOR = itemView.getContext().getResources().getColor(R.color.odd_color);
			_name = itemView.findViewById(R.id.lbl_name);
			_vars = itemView.findViewById(R.id.lbl_price);
			itemView.setTag(this);
		}

		private void bind(Cursor c, int idx) {
			_type = c.getInt(0);
			_id = c.getLong(1);
			_name.setText(c.getString(2));
			double p = c.getDouble(3);
			int vars = c.getInt(4);
			if (_type == 0) {
				if (vars > 1)
					_vars.setText("" + vars + " варианта(ов)");
				else if (p > 0)
					_vars.setText(String.format("Цена %.2f", p));
				else
					_vars.setText("Cвободная цена");
			} else
				_vars.setText("Группа номенклатур");
			if(idx > -1)
				itemView.setBackgroundColor(idx % 2 != 0 ? ODD_COLOR : Color.WHITE);
		}

		public Object get() {
			return ORMHelper.load(_type == 0 ? Good.class : GoodGroup.class, U.pair(DB.ID_FLD, _id));
		}

	}

	public static class FavoriteAdapter extends Adapter<GoodCardHolder> implements View.OnClickListener {
		private Cursor _cur;
		private int _skip;
		private LayoutInflater _inf;
		private OnObjectClickListener _l;
		private int _h;

		private FavoriteAdapter(Context ctx, OnObjectClickListener l) {
			_inf = LayoutInflater.from(ctx);
			_h = ctx.getResources().getDisplayMetrics().heightPixels / 6;
			_l = l;
			_cur = Core.getInstance().db().getReadableDatabase()
					.rawQuery("SELECT 0,A._id,A.NAME, ifnull(B.PRICE,A.PRICE),COUNT(B._id)  FROM GOODS A "
							+ "LEFT OUTER JOIN VARIANTS B on B.GOOD_ID = A._id WHERE A.IS_FAV = 1 AND A.USED = 1 "
							+ "GROUP BY A._id", null);
			if (_cur.moveToLast()) {
				do {
					if (_cur.getLong(1) == 0)
						_skip++;
					else
						break;
				} while (_cur.moveToPrevious());
			}
		}

		@Override
		public void onClick(View v) {
			_l.onObjectClick(((GoodCardHolder) v.getTag()).get());
		}

		@Override
		public int getItemCount() {
			return _cur.getCount()-_skip;
		}

		@Override
		protected void finalize() throws Throwable {
			_cur.close();
		}
		@Override
		public void onBindViewHolder(GoodCardHolder h, int p) {
			if (_cur.moveToPosition(p))
				h.bind(_cur, -1);
		}

		@Override
		public GoodCardHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			View v = _inf.inflate(R.layout.good_card, vg, false);
			v.setBackgroundResource(R.drawable.blue_button);
			v.setMinimumHeight(_h);
			v.setPadding(6, 6, 12, 0);
			if (_l != null)
				v.setOnClickListener(this);
			return new GoodCardHolder(v);
		}
	}

	public static class GoodCardAdapter extends Adapter<GoodCardHolder>
			implements View.OnLongClickListener, View.OnClickListener {

		private OnItemLongClickListener _ll;
		private Cursor _cur;
		private int _skip;
		private LayoutInflater _inf;
		private OnObjectClickListener _l;
		private boolean _isSearch;

		private GoodCardAdapter(Context ctx, String query, OnItemLongClickListener ll, OnObjectClickListener l) {
			_inf = LayoutInflater.from(ctx);
			_ll = ll;
			_l = l;
			query = query.replaceAll("'", "''");
			query = "LIKE '" + query + "%'";
			String SQL = "SELECT 1,A._id, A.NAME,0,0 FROM GOOD_CATS A WHERE A.NAME " + query + " UNION ALL "
					+ "SELECT 0,A._id,A.NAME, ifnull(B.PRICE,A.PRICE),COUNT(B._id)  FROM BARCODES C "
					+ "INNER JOIN GOODS A ON A._id = C.OWNER_ID " + "LEFT OUTER JOIN VARIANTS B on B.GOOD_ID = A._id "
					+ "WHERE C.CODE " + query + " AND C.OWNER_TYPE=0 " + "GROUP BY A._id " + "UNION ALL "
					+ "SELECT 0,A._id,A.NAME, ifnull(B.PRICE,A.PRICE),COUNT(B._id)  FROM BARCODES C "
					+ "INNER JOIN VARIANTS B on B._id = C.OWNER_ID " + "INNER JOIN GOODS A ON A._id = B.GOOD_ID "
					+ "WHERE C.CODE " + query + " AND C.OWNER_TYPE=1 " + "GROUP BY A._id " + "UNION ALL "
					+ "SELECT 0,A._id,A.NAME, ifnull(B.PRICE,A.PRICE),COUNT(B._id)  FROM GOODS A "
					+ "LEFT OUTER JOIN VARIANTS B on B.GOOD_ID = A._id " + "WHERE A.NAME " + query + " OR A.CODE "
					+ query + " GROUP BY A._id";
			_isSearch = true;
			_cur = Core.getInstance().db().getReadableDatabase().rawQuery(SQL, null);
			if (_cur.moveToLast()) {
				do {
					if (_cur.getLong(1) == 0)
						_skip++;
					else
						break;
				} while (_cur.moveToPrevious());
			}
		}

		private GoodCardAdapter(Context ctx, long group, OnItemLongClickListener ll, OnObjectClickListener l) {
			_inf = LayoutInflater.from(ctx);
			_ll = ll;
			_l = l;
			_isSearch = false;
			String SQL = "SELECT 1,A._id, A.NAME,0,0 FROM GOOD_CATS A WHERE A.PARENT_ID "
					+ (group == 0 ? "is NULL" : "=" + group) + " UNION ALL "
					+ "SELECT 0,A._id,A.NAME,ifnull(B.PRICE,A.PRICE),COUNT(B._id) FROM GOODS A LEFT OUTER JOIN VARIANTS B on B.GOOD_ID = A._id WHERE A.GROUP_ID "
					+ (group == 0 ? "is NULL" : "=" + group) + " GROUP BY A._id";
			_cur = Core.getInstance().db().getReadableDatabase().rawQuery(SQL, null);
			if (_cur.moveToLast()) {
				do {
					if (_cur.getLong(1) == 0)
						_skip++;
					else
						break;
				} while (_cur.moveToPrevious());
			}
		}

		public boolean isSearch() {
			return _isSearch;
		}

		@Override
		public int getItemCount() {
			return _cur.getCount() - _skip;
		}

		@Override
		protected void finalize() throws Throwable {
			_cur.close();
			super.finalize();
		}

		@Override
		public void onBindViewHolder(GoodCardHolder h, int p) {
			if (_cur.moveToPosition(p))
				h.bind(_cur, p);
		}

		@Override
		public GoodCardHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			View v = _inf.inflate(R.layout.good_card, vg, false);
			if (_ll != null)
				v.setOnLongClickListener(this);
			v.setOnClickListener(this);
			return new GoodCardHolder(v);
		}

		@Override
		public boolean onLongClick(View v) {
			GoodCardHolder holder = (GoodCardHolder) v.getTag();
			_ll.onLongClick(holder.get());
			return true;
		}

		@Override
		public void onClick(View v) {
			GoodCardHolder holder = (GoodCardHolder) v.getTag();
			if (_l != null)
				_l.onObjectClick(holder.get());

		}

	}

	private GoodCardAdapterHelper() {
	}

	public static GoodCardAdapter createAdapter(Context ctx, long groupId, OnItemLongClickListener ll,
			OnObjectClickListener l) {
		return new GoodCardAdapter(ctx, groupId, ll, l);
	}

	public static GoodCardAdapter createSearchAdapter(Context ctx, String s, OnItemLongClickListener ll,
			OnObjectClickListener l) {
		return new GoodCardAdapter(ctx, s, ll, l);
	}
	public static FavoriteAdapter createFavoriteAdapter(Context ctx, OnObjectClickListener l) {
		return new FavoriteAdapter(ctx, l);
	}

}
