package rs.data.goods;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellItem.ItemPaymentTypeE;
import rs.fncore.data.SellItem.SellItemTypeE;
import rs.fncore.data.VatE;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import cs.U;
import cs.orm.DBCollection;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.ORMHelper;
import cs.ui.annotations.BindTo;

@DBTable(name = DB.GOODS, unique = { DB.UUID_FLD }, indeces = { "GROUP_ID" })
public class Good extends DBObject implements Moveable, IBarcodeOwner, ISellable {

	public static enum MarkTypeE {
		NONE("Не используется"), FUR("Изделия из меха"), TOBACO("Табачная продукция"), SHOES("Обувные товары"),
		WEAR("Товары легкой промышленности и одежды"), TYRES("Шины и автопокрышки"),
		MILK("Молоко и молочная продукция"), PHOTO("Фотокамеры и лампы-вспышки"), BYCICLE("Велосипеды"),
		VELO_CHAIR("Кресла-коляски"), PARFUM("Духи и туалетная вода"), ALT_TABACO("Альтернативный табак"),
		WATER("Упакованная вода"), ANTISEPTIC("Антисептики"), BAD("БАД"), SMOKE("Никотиносодержащая продукция"),
		BEER("Пиво");

		private final String pName;

		MarkTypeE(String name) {
			pName = name;
		}

		@Override
		public String toString() {
			return pName;
		}
	}

	public static final String GOOD_ID_FLD = "GOOD_ID";
	public static final String GROUP_ID_FLD = "GROUP_ID";

	@DBField(name = DB.UUID_FLD)
	public String UUID = java.util.UUID.randomUUID().toString();
	@DBField(name = "NAME")
	@BindTo(ui = R.id.ed_name, required = true)
	private String _name;

	@DBField(name = "CODE")
	@BindTo(ui = R.id.ed_code)
	private String _code;

	@DBField(name = GROUP_ID_FLD, setter = GoodGroup.GoodCategorySetter.class)
	private GoodGroup _group;

	@DBField(name = "VAT")
	@BindTo(ui = { R.id.sp_nds })
	private VatE _vat = VatE.VAT_20_120;

	@DBField(name = "MARK_TYPE")
	@BindTo(ui = { R.id.sp_mark_type })
	private MarkTypeE _mark = MarkTypeE.NONE;

	@DBField(name = "ITEM_TYPE")
	@BindTo(ui = { R.id.sp_good_type })
	private SellItemTypeE _itemType = SellItemTypeE.GOOD;

	@DBField(name="MU")
	@BindTo(ui = R.id.sp_mu, required = true)
	private MeasureTypeE _baseMU = MeasureTypeE.PIECE;

	@DBField(name = "PRICE")
	@BindTo(ui = R.id.ed_price, format = "%.2f", ignore = "0,00")
	private double _price;

	/*
	 * @DBCollection(itemClass = MUValue.class, linkField = GOOD_ID_FLD, order = 0)
	 * 
	 * @BindTo(ui = R.id.v_mu_list) private List<MUValue> _otherMU = new
	 * ArrayList<>();
	 */

	@DBCollection(itemClass = Variant.class, linkField = GOOD_ID_FLD, order = 1)
	private List<Variant> _variants = new ArrayList<>();

	@DBField(name = "IS_FAV")
	@BindTo(ui = R.id.cb_isfav)
	private boolean _isFavorite = true;

	@DBCollection(itemClass = Barcode.class, linkField = Barcode.OWNER_TYPE_FLD + "=0 AND "
			+ Barcode.OWNER_FLD, order = 2)
	private List<Barcode> _barcodes = new ArrayList<>();

//	private MUValue _baseMUVal;

	public Good() {
	}

	public Good(String name, double price, VatE vat) {
		_group = null;
		_name = name;
		_price = price;
		_vat = vat;
		_baseMU = MeasureTypeE.PIECE;
	}
	public Good(GoodGroup c) {
		_group = c;
	}

	public Good(GoodGroup c, String uuid) {
		_group = c;
		UUID = uuid;
	}

	public MeasureTypeE baseMU() {
		return _baseMU;
	}

	public double price() {
		return _price;
	}

	/*
	 * public List<MUValue> otherMU() { return _otherMU; }
	 */

	public List<Variant> variants() {
		return _variants;
	}

	public SellItemTypeE itemType() {
		return _itemType;
	}

	@Override
	public void onLoaded() {
	}

	public static int getGoodsCountInGroup(GoodGroup group) {
		String sql = "SELECT COUNT(`" + DB.ID_FLD + "`) FROM `GOODS` WHERE `" + GROUP_ID_FLD + "`";
		if (group == null)
			sql += " IS NULL";
		else
			sql += "=" + group.id();
		Cursor c = Core.getInstance().db().getReadableDatabase().rawQuery(sql, null);
		try {
			if (c.moveToFirst())
				return c.getInt(0);
			return 0;
		} finally {
			c.close();
		}
	}

	public static List<Good> getGoodsInGroup(GoodGroup group, boolean enabledOnly) {
		if (!enabledOnly)
			return ORMHelper.loadAll(Good.class, U.pair(GROUP_ID_FLD, group == null ? null : group.id()));
		else
			return ORMHelper.loadAll(Good.class, U.pair(GROUP_ID_FLD, group == null ? null : group.id()),
					U.pair("USED", 1));

	}

	public String name() {
		return _name;
	}

	@Override
	public String toString() {
		return _name;
	}

	public GoodGroup group() {
		return _group;
	}

	public VatE nds() {
		return _vat;
	}

	public MarkTypeE markType() {
		return _mark;
	}

	@Override
	public boolean moveTo(GoodGroup group) {
		_group = group;
		return store();
	}

	@Override
	public boolean store() {
		return super.store();
	}

	public static int getFavoriteCount() {
		Cursor c = Core.getInstance().db().getReadableDatabase()
				.rawQuery("SELECT COUNT(" + DB.ID_FLD + ") FROM `GOODS` WHERE `IS_FAV` <> 0", null);
		try {

			if (c.moveToFirst())
				return c.getInt(0);
			return 0;
		} finally {
			c.close();
		}
	}

	public static TLongList getFavoritesIds() {
		TLongList result = new TLongArrayList();
		Cursor c = Core.getInstance().db().getReadableDatabase().rawQuery(
				"SELECT `" + DB.ID_FLD + "` FROM `GOODS` WHERE `IS_FAV` <> 0 AND `USED` = 1 ORDER BY `NAME` ", null);
		if (c.moveToFirst())
			do {
				result.add(c.getLong(0));
			} while (c.moveToNext());
		c.close();
		return result;
	}

	/*
	 * public static Pair<Good, Variant> findByBarcode(String s) { Cursor c =
	 * Core.getInstance().db().getReadableDatabase() .rawQuery("SELECT G." +
	 * DB.ID_FLD + " FROM `BARCODES` B  INNER JOIN `GOODS` G ON G." + DB.ID_FLD +
	 * "=B." + Good.GOOD_ID_FLD + " WHERE B.`CODE` LIKE '%" + s + "%'", null); try {
	 * if (c.moveToFirst()) { Good g = ORMHelper.load(Good.class, U.pair(DB.ID_FLD,
	 * c.getLong(0))); for (Variant b : g._variants) {
	 * 
	 * } } } catch (Exception e) {
	 * 
	 * } finally { c.close(); } return null; }
	 */

	public Good setNDS(VatE vat) {
		_vat = vat;
		return this;

	}

	public Good setName(String val) {
		_name = val;
		return this;

	}

	public Good setCode(String val) {
		_code = val;
		return this;

	}

	public Good setType(SellItemTypeE type) {
		_itemType = type;
		return this;

	}

	public Good setMarkType(MarkTypeE value) {
		_mark = value;
		return this;
	}

	public Good setMU(MeasureTypeE mu) {
		_baseMU = mu;
		return this;
	}

	public List<Barcode> barcodes() {
		return _barcodes;
	}

	public BaseAdapter getVariantsAdapter(final Context ctx) {
		return new BaseAdapter() {
			private LayoutInflater _inf;
			{
				_inf = LayoutInflater.from(ctx);
			}

			@Override
			public View getView(int p, View v, ViewGroup vg) {
				if (v == null) {
					v = _inf.inflate(R.layout.good_card, vg, false);
				}
				ISellable var = getItem(p);
				((TextView) v.findViewById(R.id.lbl_name)).setText(var.name());
				((TextView) v.findViewById(R.id.lbl_price)).setText(
						String.format("%.3f %s за %.2f", var.qtty(), var.measure(), var.price()));
				return v;
			}

			@Override
			public long getItemId(int p) {
				return p;
			}

			@Override
			public ISellable getItem(int p) {
				if(p == 0) return Good.this;
				return _variants.get(p-1);
			}

			@Override
			public int getCount() {
				return _variants.size()+1;
			}

			@Override
			public boolean hasStableIds() {
				return false;
			}
		};
	}

	public Barcode findBarcode(String code) {
		for (Barcode bc : _barcodes)
			if (bc.toString().equals(code))
				return bc;
		return null;
	}

	public static void clearAll() {
		SQLiteDatabase db = Core.getInstance().db().getWritableDatabase();
		try {
			db.execSQL("DELETE FROM " + DB.BARCODES);
		} catch (SQLException sqe) {
		}
		try {
			db.execSQL("DELETE FROM " + DB.VARIANTS);
		} catch (SQLException sqe) {
		}
		try {
			db.execSQL("DELETE FROM " + DB.GOOD_CATS);
		} catch (SQLException sqe) {
		}
		try {
			db.execSQL("DELETE FROM " + DB.GOODS);
		} catch (SQLException sqe) {
		}

	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public SellItem createSellItem() {
		return new SellItem(_itemType, ItemPaymentTypeE.FULL, _name, BigDecimal.ONE, _baseMU,
				BigDecimal.valueOf(_price), _vat).attach(this);
	}

	public VatE vat() {
		return _vat;
	}
	public MarkTypeE getMarkType() { return _mark; }

	@Override
	public Good good() {
		return this;
	}

	@Override
	public MeasureTypeE measure() {
		return _baseMU;
	}

	@Override
	public double maxQtty() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p) {
		p.writeByte((byte)0);
		p.writeLong(id());
		
	}

	public Variant getVariantById(long id) {
		for(Variant v : _variants)
			if(v.id() == id) return v;
		return null;
	}

	@Override
	public double qtty() {
		return 1.0;
	}

}
