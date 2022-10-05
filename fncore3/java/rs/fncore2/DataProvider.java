package rs.fncore2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import rs.log.Logger;

public class DataProvider extends ContentProvider {

    private static final int DOCUMENTS=100;
    private static final int LOGS=101;
    private static final int REPORTS=102;

    private static final String CONTENT_AUTHORITY = "rs.fncore2.data";
    private static final String DOCUMENTS_TYPE = "vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".documents";
    private static final String LOGS_TYPE = "vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".logs";
    private static final String REPORTS_TYPE = "vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".reports";

    private volatile DB mDb;
    private UriMatcher mMatcher = new UriMatcher(UriMatcher.NO_MATCH) {
        {
            addURI(CONTENT_AUTHORITY, "documents", DOCUMENTS);
            addURI(CONTENT_AUTHORITY, "logs", LOGS);
            addURI(CONTENT_AUTHORITY, "reports", REPORTS);
        }
    };

    public DataProvider() {
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        switch (mMatcher.match(uri)){
            case DOCUMENTS: return DOCUMENTS_TYPE;
            case LOGS: return LOGS_TYPE;
            case REPORTS: return REPORTS_TYPE;
            default:
                Logger.e("unknown request: %s", uri.toString());
                return "";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues cv) {
        return null;
    }

    @Override
    public boolean onCreate() {
        if (FNCore.getInstance() != null)
            mDb = FNCore.getInstance().getDB();
        else
            mDb = new DB(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] args, String order) {
        switch (mMatcher.match(uri)){
            case DOCUMENTS: return mDb.queryDocuments(columns, selection, args, order);
            case LOGS: return mDb.queryLogs(columns, selection, args, order);
            case REPORTS:return mDb.queryReports(columns, selection, args, order);
            default:
                Logger.e("unknown request: %s", uri.toString());
                return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues arg1, String arg2, String[] arg3) {
        switch (mMatcher.match(uri)){
            case DOCUMENTS: return 0;
            case LOGS: return 0;
            case REPORTS: return 0;
            default:
                Logger.e("unknown request: %s", uri.toString());
                return -1;
        }
    }
}
