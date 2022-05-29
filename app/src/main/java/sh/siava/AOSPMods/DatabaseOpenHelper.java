package sh.siava.AOSPMods;

import static android.content.ContentValues.TAG;
import static sh.siava.AOSPMods.DatabaseTable.DatabaseOpenHelper.FTS_TABLE_CREATE;
import static sh.siava.AOSPMods.DatabaseTable.FTS_VIRTUAL_TABLE;
import static sh.siava.AOSPMods.DatabaseTable.KEYWORD;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DatabaseOpenHelper implements DatabaseOpenHelperOnCreate {
    public DatabaseOpenHelper(Context context) {
    }

    private void loadDictionary(ClassLoader helperContext) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    loadWords();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public void loadWords() throws IOException {
        final Resources resources = helperContext.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.definitions);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] strings = TextUtils.split(line, "-");
                if (strings.length < 2) continue;
                long id = addWord(strings[0].trim(), strings[1].trim());
                if (id < 0) {
                    Log.e(TAG, "unable to add word: " + strings[0].trim());
                }
            }
        } finally {
            reader.close();
        }
    }

    public long addWord(String word, String definition) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEYWORD, word);

        return database.insert(FTS_VIRTUAL_TABLE, initialValues, null);

    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(FTS_TABLE_CREATE);
        loadDictionary(helperContext);
    }

    public Cursor getWordMatches(String query, String[] columns, SQLiteOpenHelper databaseOpenHelper) {
        String selection = KEYWORD + " MATCH ?";
        String[] selectionArgs = new String[]{query + "*"};

        return query(selection, selectionArgs, columns, databaseOpenHelper);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns, SQLiteOpenHelper databaseOpenHelper) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);

        Cursor cursor = builder.query(databaseOpenHelper.getReadableDatabase(), columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    public void handleIntent(Intent intent, DatabaseOpenHelper database) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Cursor c = database.getWordMatches(query, null, databaseOpenHelper);
            //process Cursor and display results
        }
    }
}
