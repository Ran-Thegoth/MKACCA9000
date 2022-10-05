package rs.mkacca;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import cs.orm.ORMHelper;

public class DB extends SQLiteOpenHelper {
	
	public static final String DB_FILE = "data.db";
	public static final String USER_TABLE = "USERS";
	public static final String ID_FLD = "_id";
	public static final String UUID_FLD = "UUID";
	public static final String GOODS  = "GOODS";
	public static final String GOOD_CATS = "GOOD_CATS";
	public static final String VARIANTS = "VARIANTS";
	public static final String BARCODES = "BARCODES";
	
	public DB(Context ctx) {
		super(ctx,DB_FILE,null,100);
		ORMHelper.setSQLiteHelper(this);
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
	}
}
