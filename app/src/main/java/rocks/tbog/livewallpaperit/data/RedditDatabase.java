package rocks.tbog.livewallpaperit.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class RedditDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "reddit.s3db";
    private static final int DB_VERSION = 1;
    private static final String TAG = "DB";
    static final String TABLE_IGNORE = "ignore_artwork";
    static final String ARTWORK_TOKEN = "token";

    RedditDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_IGNORE + " ( "
                + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "\"" + ARTWORK_TOKEN + "\" TEXT UNIQUE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Updating database from version " + oldVersion + " to version " + newVersion);
    }
}
