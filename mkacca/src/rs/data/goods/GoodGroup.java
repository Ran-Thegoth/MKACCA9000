package rs.data.goods;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.FieldHelper;
import cs.orm.ORMHelper;
import cs.ui.annotations.BindTo;

@DBTable(name = DB.GOOD_CATS, unique = {DB.UUID_FLD},indeces = {"PARENT_ID"})
public class GoodGroup extends DBObject implements Moveable {

	private static final String PARENT_FIELD_NAME = "PARENT_ID";
	private static TLongObjectMap<GoodGroup> ALL = new TLongObjectHashMap<>();

	public static class GoodCategorySetter implements FieldHelper {

		public static final GoodCategorySetter INSTANCE = new GoodCategorySetter();

		private GoodCategorySetter() {
		}

		@Override
		public Object getFieldValue(Object owner, Field field) throws Exception {
			return null;
		}

		@Override
		public void setFieldValue(Object owner, Field field, Object value)
				throws Exception {
			field.set(owner, ALL.get(((Number) value).longValue()));
		}

	}

	@DBField(name = "NAME")
	@BindTo(ui = R.id.ed_name, required = true)
	private String _name;

	@DBField(name = DB.UUID_FLD)
	public String UUID = java.util.UUID.randomUUID().toString();
	
	@DBField(name = PARENT_FIELD_NAME, setter = GoodCategorySetter.class)
	private GoodGroup _parent;

	public GoodGroup() {

	}
	public void assign(GoodGroup src) {
		_name = src._name;
		_parent = src._parent;
	}

	public GoodGroup(GoodGroup parent) {
		_parent = parent;
	}

	public String uuid() { return UUID; }
	public static void load() {
		try {
			SQLiteDatabase db = Core.getInstance().db()
					.getReadableDatabase();
			Cursor c = db.rawQuery("SELECT * FROM `GOOD_CATS` ORDER BY `"
					+ PARENT_FIELD_NAME + "`", null);
			try {
				if (c.moveToFirst())
					do {
						try {
							ORMHelper.read(new GoodGroup(), c);
						} catch (Exception e) {
							break;
						}
					} while (c.moveToNext());
			} finally {
				c.close();
			}
		} catch (Exception e) {

		}
	}

	public static List<GoodGroup> getChildrenFor(GoodGroup owner) {
		List<GoodGroup> result = new ArrayList<>();
		for (GoodGroup c : ALL.valueCollection())
			if (c._parent == owner)
				result.add(c);
		return result;
	}

	public GoodGroup getParent() {
		return _parent;
	}

	public String name() {
		return _name;
	}

	@Override
	public String toString() {
		return _name;
	}

	@Override
	public void onLoaded() {
		ALL.put(id(), this);
	}

	@Override
	public boolean store() {
		if(super.store()) {
			ALL.put(id(), this);
			return true;
		}
		return false;
	}
	@Override
	public boolean delete() {
		SQLiteDatabase db = Core.getInstance().db().getWritableDatabase();
		try {
			db.beginTransaction();
			String newId = _parent == null ? "NULL" : String.valueOf(_parent
					.id());
			db.execSQL("UPDATE `GOOD_CATS` SET `" + PARENT_FIELD_NAME + "` = "
					+ newId + " WHERE `" + PARENT_FIELD_NAME + "`=" + id());
			db.execSQL("UPDATE `GOODS` SET `" + Good.GROUP_ID_FLD + "` = "
					+ newId + " WHERE `" + Good.GROUP_ID_FLD + "`=" + id());

			if (ORMHelper.delete(this)) {
				ALL.remove(id());
				db.setTransactionSuccessful();
				return true;
			}
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
		return false;
	}

	@Override
	public boolean moveTo(GoodGroup group) {
		SQLiteDatabase db = Core.getInstance().db().getWritableDatabase();
		try {
			db.beginTransaction();
			String newId = _parent == null ? "NULL" : String.valueOf(_parent
					.id());
			db.execSQL("UPDATE `GOOD_CATS` SET `" + PARENT_FIELD_NAME + "` = "
					+ newId + " WHERE `" + PARENT_FIELD_NAME + "`=" + id());
			_parent = group;
			if(store()) {
				db.setTransactionSuccessful();
				return true;
			}
		} finally {
			db.endTransaction();
		}
		return false;
	}
	public void setName(String name) {
		_name = name;
		
	}
	public void setParent(GoodGroup p) {
		_parent = p;
	}
	public void setUUID(String uuid) {
		UUID = uuid;
	}

}
