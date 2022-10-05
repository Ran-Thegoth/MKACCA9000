package rs.fncore2.fn.storage.virtual;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import rs.fncore.data.Tag;

class VFnDB extends SQLiteOpenHelper {
    VFnDB(Context ctx) {
        super(ctx, "vfn.db", null, 100);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE DOCS (ID INTEGER NOT NULL PRIMARY KEY, BODY BLOB NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
    }

    public Tag getDocument(int number) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("DOCS", null, "ID=?", new String[]{String.valueOf(number)}, null, null, null);

        try {
            if (c.moveToFirst()) {
                Tag result = new Tag();
                result.addAll(c.getBlob(1));
                return result;
            }
            return null;

        } finally {
            c.close();
            db.close();
        }
    }

    public int storeDocument(Tag tlv) {
        ContentValues cv = new ContentValues();
        cv.put("BODY", tlv.packToTlvList());

        SQLiteDatabase db = getWritableDatabase();
        try {
            return (int) db.insert("DOCS", null, cv);
        } finally {
            db.close();
        }
    }

    public void clear() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete("DOCS", null, null);
        } finally {
            db.close();
        }

    }
}
