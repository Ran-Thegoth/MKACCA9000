package rs.fncore2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.ResultSet;
import java.util.Calendar;

import rs.log.Logger;

class DatabaseManager {

	private static DatabaseManager instance;
	private static SQLiteOpenHelper mDatabaseHelper;
	private SQLiteDatabase mDatabase;
	private int mOpenCounter = 0;

	public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
		if (instance == null) {
			instance = new DatabaseManager();
			mDatabaseHelper = helper;
		}
	}

	public static synchronized DatabaseManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException(DatabaseManager.class.getSimpleName()
					+ " is not initialized, call initializeInstance(..) method first.");
		}
		return instance;
	}

	public synchronized SQLiteDatabase openDatabase() {
		if (mOpenCounter++ == 0) {
			mDatabase = mDatabaseHelper.getWritableDatabase();
		} else if (mDatabase != null && !mDatabase.isOpen()) {
			mDatabase = mDatabaseHelper.getWritableDatabase();
			Logger.e("reopen database in wrong manner, counter=%s", mOpenCounter);
		}
		return mDatabase;
	}

	public synchronized void closeDatabase() {
		if (--mOpenCounter == 0) {
			mDatabase.close();
		}
		if (mOpenCounter < 0) {
			throw new IllegalStateException("wrong pairs open and close Database counter: " + mOpenCounter);
		}
	}
}

public class DB extends SQLiteOpenHelper {

	private static final int CURR_VERSION = 302;
	private static final String DOCUMENTS_TABLE = "DOCUMENTS";
	private static final String DOC_NO_FIELD = "DOCNO";
	private static final String DOC_DATE_FIELD = "DOCDATE";
	public static final String DOC_TYPE_FIELD = "DOCTYPE";
	private static final String DOC_OFD_DATA_FIELD = "OFD";
	private static final String DOC_OISM_DATA_FIELD = "OISM";
	private static final String LOG_TABLE = "LOGS";
	private static final String LOG_WHEN = "ETIME";
	private static final String LOG_MSG = "MSG";
	private final String LOG_DATA = "DATA";

	private static final String REPORTS_TABLE = "REPORTS";
	private static final String REPORT_WHEN = "ETIME";
	private static final String REPORT_START_DOC_NO = "START_DOC";
	private static final String REPORT_END_DOC_NO = "END_DOC";
	private static final String REPORT_DATA = "DATA";

	public DB(Context context) {
		super(context, "fncore.db", null, CURR_VERSION);
		DatabaseManager.initializeInstance(this);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE " + DOCUMENTS_TABLE + "( FNSN TEXT, " + DOC_NO_FIELD + " INTEGER NOT NULL PRIMARY KEY,"
				+ DOC_DATE_FIELD + " INTEGER NOT NULL," + DOC_TYPE_FIELD + " INTEGER NOT NULL," + DOC_OFD_DATA_FIELD
				+ " INTEGER ," + DOC_OISM_DATA_FIELD + " INTEGER);");

		db.execSQL("CREATE TABLE " + LOG_TABLE + " ( " + LOG_WHEN + " INTEGER NOT NULL," + LOG_MSG + " TEXT NOT NULL,"
				+ LOG_DATA + " TEXT);");
		db.execSQL("CREATE TABLE " + REPORTS_TABLE + " ( " + REPORT_WHEN + " INTEGER NOT NULL," + REPORT_START_DOC_NO
				+ " INTEGER NOT NULL," + REPORT_END_DOC_NO + " INTEGER NOT NULL," + REPORT_DATA + " BLOB NOT NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Logger.w("table check update, oldVersion=" + oldVersion + ", newVersion=" + newVersion);
		if (oldVersion < 201) {
			db.execSQL("CREATE TABLE " + LOG_TABLE + " ( " + LOG_WHEN + " INTEGER NOT NULL," + LOG_MSG
					+ " TEXT NOT NULL," + LOG_DATA + " TEXT);");
		} else if (oldVersion == 201) {
			db.execSQL("ALTER TABLE " + DOCUMENTS_TABLE + " ADD " + DOC_OISM_DATA_FIELD + " BLOB;");
			Logger.w("table updated ...");
		} else if (oldVersion == 301) {
			db.execSQL("CREATE TABLE " + REPORTS_TABLE + " ( " + REPORT_WHEN + " INTEGER NOT NULL,"
					+ REPORT_START_DOC_NO + " INTEGER NOT NULL," + REPORT_END_DOC_NO + " INTEGER NOT NULL,"
					+ REPORT_DATA + " BLOB NOT NULL);");
		}
	}

	public synchronized void logWrite(int level, String msg, Throwable t) {
		ContentValues cv = new ContentValues();
		cv.put(LOG_WHEN, System.currentTimeMillis());
		String s = "";

		switch (level) {
		case Log.ASSERT:
		case Log.ERROR:
			s = "[E] ";
			break;
		case Log.WARN:
			s = "[W] ";
			break;
		case Log.INFO:
			s = "[I] ";
			break;
		default:
			s = "[D] ";
		}

		s += msg;
		cv.put(LOG_MSG, s);

		s = null;
		if (t != null) {
			s = t.getClass().getName() + ": " + t.getLocalizedMessage() + "\n";
			for (StackTraceElement e : t.getStackTrace()) {
				s += e.getClassName() + "." + e.getMethodName() + " (" + e.getLineNumber() + ")\n";
			}
		}
		cv.put(LOG_DATA, s);

		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		db.delete(LOG_TABLE, LOG_WHEN + " < " + c.getTimeInMillis(), null);
		db.insert(LOG_TABLE, null, cv);
		DatabaseManager.getInstance().closeDatabase();
	}

	public synchronized void addReport(long startDocument, long endDocument, byte[] data) {
		ContentValues cv = new ContentValues();
		cv.put(REPORT_WHEN, System.currentTimeMillis());
		cv.put(REPORT_START_DOC_NO, startDocument);
		cv.put(REPORT_END_DOC_NO, endDocument);
		cv.put(REPORT_DATA, data);

		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		db.delete(REPORTS_TABLE, REPORT_WHEN + " < " + c.getTimeInMillis(), null);
		db.insert(REPORTS_TABLE, null, cv);
		DatabaseManager.getInstance().closeDatabase();
	}

	public void clear() {
		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		db.delete(DOCUMENTS_TABLE, null, null);
		DatabaseManager.getInstance().closeDatabase();
	}

	public void storeDocument(String fnsn, long id, long date, int type) {
		ContentValues cv = new ContentValues();
		cv.put("FNSN", fnsn);
		cv.put(DOC_NO_FIELD, id);
		cv.put(DOC_DATE_FIELD, date);
		cv.put(DOC_TYPE_FIELD, type);

		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		System.err.println("db insert doc: " + db.replace(DOCUMENTS_TABLE, null, cv));
		DatabaseManager.getInstance().closeDatabase();
	}

	@Override
	public synchronized void close() {
		super.close();
	}

	public long getLastDocumentNumber() {
		Cursor c = getReadableDatabase().rawQuery("SELECT MAX(" + DOC_NO_FIELD + ") FROM " + DOCUMENTS_TABLE, null);
		try {
			if (c.moveToFirst())
				return c.getLong(0);
			return 0;
		} finally {
			c.close();
		}
	}

	public long getDocumentsCount() {

		long count = DatabaseUtils.queryNumEntries(getReadableDatabase(), DOCUMENTS_TABLE);
		return count;
	}

	public Cursor getDocument(int docNo) {
		return getReadableDatabase().query(DOCUMENTS_TABLE, null, DOC_NO_FIELD + "=?",
				new String[] { String.valueOf(docNo) }, null, null, null);
	}

	public void storeOFDReply(long docNo, byte[] reply) {
		ContentValues cv = new ContentValues();
		cv.put(DOC_OFD_DATA_FIELD, reply);
		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		db.update(DOCUMENTS_TABLE, cv, DOC_NO_FIELD + "=?", new String[] { String.valueOf(docNo) });
		DatabaseManager.getInstance().closeDatabase();
	}

	public void storeOISMReply(long docNo, byte[] reply) {
		ContentValues cv = new ContentValues();
		cv.put(DOC_OISM_DATA_FIELD, reply);
		SQLiteDatabase db = DatabaseManager.getInstance().openDatabase();
		db.update(DOCUMENTS_TABLE, cv, DOC_NO_FIELD + "=?", new String[] { String.valueOf(docNo) });
		DatabaseManager.getInstance().closeDatabase();
	}

	public Cursor queryDocuments(String[] columns, String selection, String[] args, String order) {
		if (order == null)
			order = "DOCDATE desc";
		return getReadableDatabase().query(DOCUMENTS_TABLE, columns, selection, args, null, null, order);
	}

	public Cursor queryLogs(String[] columns, String selection, String[] args, String order) {
		return getReadableDatabase().query(LOG_TABLE, columns, selection, args, null, null, order);
	}

	public Cursor queryReports(String[] columns, String selection, String[] args, String order) {
		return getReadableDatabase().query(REPORTS_TABLE, columns, selection, args, null, null, order);
	}
}
