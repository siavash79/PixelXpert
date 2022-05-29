package sh.siava.AOSPMods;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseTable {
    private static final String TAG = "SearchKeywords";
    public static final String KEYWORD = "KEYWORD";
    private static final String DATABASE_NAME = "DICTIONARY";
    private static final int DATABASE_VERSION = 1;


    static String FTS_VIRTUAL_TABLE = "FTS";


    private final DatabaseOpenHelper databaseOpenHelper;

    public DatabaseTable(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context);
    }

    public Cursor getWordMatches(String query, Object o) {
        return null;
    }

    static class DatabaseOpenHelper extends SQLiteOpenHelper {
        public Context helperContext;
        public SQLiteDatabase mDatabase;

        static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        KEYWORD + ")";

        DatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            helperContext = context;

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

    }

}
