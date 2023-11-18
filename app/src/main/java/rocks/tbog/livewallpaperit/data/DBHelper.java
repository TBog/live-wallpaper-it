package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rocks.tbog.livewallpaperit.Source;

public class DBHelper {
    private static final String TAG = DBHelper.class.getSimpleName();
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

    public static void addSubTopic(Context context, String subreddit, SubTopic comment) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues value = new ContentValues();
        comment.fillTopicValues(value);
        value.put(RedditDatabase.TOPIC_SUBREDDIT, subreddit);

        long rowId = db.insertWithOnConflict(RedditDatabase.TABLE_TOPICS, null, value, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            Log.e(TAG, "failed to insert in " + RedditDatabase.TABLE_TOPICS + " " + value);
            return;
        }

        value.clear();
        db.beginTransaction();
        try {
            for (var image : comment.images) {
                comment.fillImageValues(image, value);
                rowId = db.insertWithOnConflict(
                        RedditDatabase.TABLE_TOPIC_IMAGES, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                if (rowId == -1) {
                    Log.e(TAG, "failed to insert in " + RedditDatabase.TABLE_TOPIC_IMAGES + " " + value);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static List<SubTopic> getSubTopics(Context context, String subreddit) {
        SQLiteDatabase db = getDatabase(context);
        ArrayList<SubTopic> records = null;

        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_TOPICS,
                new String[] {
                    RedditDatabase.TOPIC_ID,
                    RedditDatabase.TOPIC_TITLE,
                    RedditDatabase.TOPIC_AUTHOR,
                    RedditDatabase.TOPIC_LINK_FLAIR_TEXT,
                    RedditDatabase.TOPIC_PERMALINK,
                    RedditDatabase.TOPIC_THUMBNAIL,
                    RedditDatabase.TOPIC_CREATED_UTC,
                    RedditDatabase.TOPIC_SCORE,
                    RedditDatabase.TOPIC_UPVOTE_RATIO,
                    RedditDatabase.TOPIC_NUM_COMMENTS,
                    RedditDatabase.TOPIC_OVER_18,
                },
                "\"" + RedditDatabase.TOPIC_SUBREDDIT + "\" = ?",
                new String[] {subreddit},
                null,
                null,
                "\"" + RedditDatabase.TOPIC_CREATED_UTC + "\" DESC",
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    SubTopic topic = SubTopic.fromCursor(cursor);
                    records.add(topic);

                    cursor.moveToNext();
                }
            }
        }

        if (records == null) {
            return Collections.emptyList();
        }
        return records;
    }

    public static void loadSubTopicImages(@NonNull Context context, @NonNull SubTopic topic) {
        SQLiteDatabase db = getDatabase(context);

        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_TOPIC_IMAGES,
                new String[] {
                    RedditDatabase.IMAGE_URL,
                    RedditDatabase.IMAGE_MEDIA_ID,
                    RedditDatabase.IMAGE_WIDTH,
                    RedditDatabase.IMAGE_HEIGHT,
                    RedditDatabase.IMAGE_IS_NSFW,
                    RedditDatabase.IMAGE_IS_SOURCE,
                },
                "\"" + RedditDatabase.IMAGE_TOPIC_ID + "\" = ?",
                new String[] {topic.id},
                null,
                null,
                null,
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    SubTopic.Image image = SubTopic.Image.fromCursor(cursor);
                    topic.images.add(image);
                }
            }
        }
    }
}
