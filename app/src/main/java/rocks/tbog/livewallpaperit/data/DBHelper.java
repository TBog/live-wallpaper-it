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

import rocks.tbog.livewallpaperit.Source;

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
        }
        return records;
    }

    public static List<Source> loadSources(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<Source> records = null;
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_SUBREDDITS,
                new String[] {
                    RedditDatabase.SUBREDDIT_NAME,
                    RedditDatabase.SUBREDDIT_MIN_UPVOTE_PERCENTAGE,
                    RedditDatabase.SUBREDDIT_MIN_SCORE,
                    RedditDatabase.SUBREDDIT_MIN_COMMENTS
                },
                null,
                null,
                null,
                null,
                null,
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    Source source = new Source(cursor.getString(0));
                    source.minUpvotePercentage = cursor.getInt(1);
                    source.minScore = cursor.getInt(2);
                    source.minComments = cursor.getInt(3);
                    records.add(source);

                    cursor.moveToNext();
                }
            }
        }
        if (records == null) {
            return Collections.emptyList();
        }
        return records;
    }

    public static boolean insertSource(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues value = new ContentValues();
        value.put(RedditDatabase.SUBREDDIT_NAME, source.subreddit);
        value.put(RedditDatabase.SUBREDDIT_MIN_UPVOTE_PERCENTAGE, source.minUpvotePercentage);
        value.put(RedditDatabase.SUBREDDIT_MIN_SCORE, source.minScore);
        value.put(RedditDatabase.SUBREDDIT_MIN_COMMENTS, source.minComments);
        return -1
                != db.insertWithOnConflict(
                        RedditDatabase.TABLE_SUBREDDITS, null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static boolean updateSource(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues value = new ContentValues();
        value.put(RedditDatabase.SUBREDDIT_MIN_UPVOTE_PERCENTAGE, source.minUpvotePercentage);
        value.put(RedditDatabase.SUBREDDIT_MIN_SCORE, source.minScore);
        value.put(RedditDatabase.SUBREDDIT_MIN_COMMENTS, source.minComments);
        return -1
                != db.updateWithOnConflict(
                        RedditDatabase.TABLE_SUBREDDITS,
                        value,
                        RedditDatabase.SUBREDDIT_NAME + "=?",
                        new String[] {source.subreddit},
                        SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static boolean removeSource(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);

        return -1
                != db.delete(RedditDatabase.TABLE_SUBREDDITS, RedditDatabase.SUBREDDIT_NAME + "=?", new String[] {
                    source.subreddit
                });
    }
}
