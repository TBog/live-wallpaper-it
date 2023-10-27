package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBHelper {
    private static SQLiteOpenHelper database = null;

    private static SQLiteDatabase getDatabase(Context context) {
        if (database == null) {
            database = new RedditDatabase(context);
        }
        return database.getReadableDatabase();
    }

    public static boolean insertIgnoreToken(@NonNull Context context, @NonNull String token) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put(RedditDatabase.ARTWORK_TOKEN, token);

        return -1
                != db.insertWithOnConflict(RedditDatabase.TABLE_IGNORE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @NonNull
    public static List<String> getIgnoreTokenList(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<String> records = null;
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_IGNORE,
                new String[] {RedditDatabase.ARTWORK_TOKEN},
                null,
                null,
                null,
                null,
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    records.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
        }
        if (records == null) {
            return Collections.emptyList();
        } else {
            return records;
        }
    }
}
