package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.Version;

public class DBHelper {
    private static final Version UPSERT_added_ver = new Version("3.24.0");
    private static SQLiteOpenHelper database = null;
    private static Version sqliteVersion = null;

    private static SQLiteDatabase getDatabase(Context context) {
        synchronized (DBHelper.class) {
            if (database == null) {
                database = new RedditDatabase(context);
            }
            return database.getReadableDatabase();
        }
    }

    private static Version getSQLiteVersion(SQLiteDatabase db) {
        if (sqliteVersion != null) return sqliteVersion;
        try (Cursor cursor = db.rawQuery("SELECT sqlite_version() AS sqlite_version", null)) {
            if (cursor.moveToNext()) {
                return sqliteVersion = new Version(cursor.getString(0));
            }
        }
        return sqliteVersion = new Version("0.0.0");
    }

    public static boolean insertIgnoreToken(@NonNull Context context, @NonNull String token) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put(RedditDatabase.ARTWORK_TOKEN, token);

        return -1 != db.insertWithOnConflict(RedditDatabase.TABLE_IGNORE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static int insertIgnoreTokens(@NonNull Context context, @NonNull String[] mediaIdArray) {
        SQLiteDatabase db = getDatabase(context);

        int count = 0;
        ContentValues values = new ContentValues();
        db.beginTransaction();
        try {
            for (var mediaId : mediaIdArray) {
                values.put(RedditDatabase.ARTWORK_TOKEN, mediaId);
                count += db.insertWithOnConflict(
                        RedditDatabase.TABLE_IGNORE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
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

    public static boolean removeSourceSubTopics(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);

        return -1
                != db.delete(RedditDatabase.TABLE_TOPICS, RedditDatabase.TOPIC_SUBREDDIT_NAME + "=?", new String[] {
                    source.subreddit
                });
    }

    public static void insertOrUpdateSubTopic(Context context, String subreddit, SubTopic topic) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues value = new ContentValues();
        topic.fillTopicValues(value);
        value.put(RedditDatabase.TOPIC_SUBREDDIT_NAME, subreddit);

        db.beginTransaction();
        try {
            insertOrUpdate(db, RedditDatabase.TABLE_TOPICS, value, new String[] {RedditDatabase.TOPIC_ID});

            value.clear();
            for (var image : topic.images) {
                topic.fillImageValues(image, value);
                insertOrUpdate(db, RedditDatabase.TABLE_TOPIC_IMAGES, value, new String[] {
                    RedditDatabase.IMAGE_MEDIA_ID,
                    RedditDatabase.IMAGE_TOPIC_ID,
                    RedditDatabase.IMAGE_WIDTH,
                    RedditDatabase.IMAGE_HEIGHT,
                    RedditDatabase.IMAGE_IS_BLUR,
                    RedditDatabase.IMAGE_IS_SOURCE
                });
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void insertOrUpdate(
            SQLiteDatabase db, @NonNull String table, @NonNull ContentValues values, @NonNull String[] onConflict) {
        var ver = DBHelper.getSQLiteVersion(db);

        // UPSERT syntax was added to SQLite with version 3.24.0 (2018-06-04) https://sqlite.org/lang_upsert.html
        if (ver.compareTo(UPSERT_added_ver) >= 0) {
            // INSERT INTO _table(column1, column2) VALUES(?,?) ON CONFLICT(column1) DO UPDATE SET
            // column1=excluded.column1,column2=excluded.column2
            var columns = new ArrayList<>(values.keySet());
            var query = new StringBuilder("INSERT INTO \"").append(table).append("\"(");
            int colCount = columns.size();
            String[] columnValues = new String[colCount];
            for (int colIdx = 0; colIdx < colCount; colIdx++) {
                if (colIdx == 0) query.append("\"");
                else query.append(",\"");
                String columnName = columns.get(colIdx);
                query.append(columnName).append("\"");
                columnValues[colIdx] = values.getAsString(columnName);
            }
            query.append(") VALUES (?");
            if (colCount > 1) query.append(",?".repeat(colCount - 1));
            query.append(") ON CONFLICT (");
            for (int i = 0; i < onConflict.length; i++) {
                if (i == 0) query.append("\"");
                else query.append(",\"");
                query.append(onConflict[i]).append("\"");
            }
            query.append(") DO UPDATE SET ");
            for (int colIdx = 0; colIdx < colCount; colIdx++) {
                if (colIdx == 0) query.append("\"");
                else query.append(",\"");
                var column = columns.get(colIdx);
                query.append(column).append("\"=excluded.\"").append(column).append("\"");
            }
            db.execSQL(query.toString(), columnValues);
        } else {
            var rowId = db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (rowId == -1) {
                String[] whereArgs = new String[onConflict.length];
                StringBuilder where = new StringBuilder();
                for (int i = 0; i < onConflict.length; i++) {
                    if (i == 0) where.append("\"");
                    else where.append(" AND \"");
                    String columnName = onConflict[i];
                    where.append(columnName).append("\"=?");
                    whereArgs[i] = values.getAsString(columnName);
                }
                db.updateWithOnConflict(table, values, where.toString(), whereArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    public static boolean removeSubTopic(@NonNull Context context, @NonNull SubTopic topic) {
        SQLiteDatabase db = getDatabase(context);

        return -1
                != db.delete(
                        RedditDatabase.TABLE_TOPICS, "\"" + RedditDatabase.TOPIC_ID + "\" = ?", new String[] {topic.id
                        });
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
                "\"" + RedditDatabase.TOPIC_SUBREDDIT_NAME + "\" = ?",
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
        loadSubTopicImages(db, topic.id, topic.images);
    }

    private static void loadSubTopicImages(
            SQLiteDatabase db, @NonNull String topicId, @NonNull List<SubTopic.Image> outImages) {
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_TOPIC_IMAGES,
                new String[] {
                    RedditDatabase.IMAGE_URL,
                    RedditDatabase.IMAGE_MEDIA_ID,
                    RedditDatabase.IMAGE_WIDTH,
                    RedditDatabase.IMAGE_HEIGHT,
                    RedditDatabase.IMAGE_IS_BLUR,
                    RedditDatabase.IMAGE_IS_SOURCE,
                },
                "\"" + RedditDatabase.IMAGE_TOPIC_ID + "\" = ?",
                new String[] {topicId},
                null,
                null,
                "\"" + BaseColumns._ID + "\" ASC",
                null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SubTopic.Image image = SubTopic.Image.fromCursor(cursor);
                    outImages.add(image);
                }
            }
        }
    }

    public static void loadSubTopicImages(@NonNull Context context, @NonNull Collection<SubTopic> topics) {
        if (topics.isEmpty()) return;
        if (topics.size() == 1) {
            loadSubTopicImages(context, topics.stream().findAny().get());
            return;
        }
        SQLiteDatabase db = getDatabase(context);

        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_TOPIC_IMAGES,
                new String[] {
                    RedditDatabase.IMAGE_TOPIC_ID,
                    RedditDatabase.IMAGE_URL,
                    RedditDatabase.IMAGE_MEDIA_ID,
                    RedditDatabase.IMAGE_WIDTH,
                    RedditDatabase.IMAGE_HEIGHT,
                    RedditDatabase.IMAGE_IS_BLUR,
                    RedditDatabase.IMAGE_IS_SOURCE,
                },
                "\"" + RedditDatabase.IMAGE_TOPIC_ID + "\" IN (?" + ",?".repeat(topics.size() - 1) + ")",
                topics.stream().map(topic -> topic.id).toArray(String[]::new),
                null,
                null,
                "\"" + BaseColumns._ID + "\" ASC",
                null)) {
            if (cursor != null) {
                final int columnTopicId = cursor.getColumnIndex(RedditDatabase.IMAGE_TOPIC_ID);
                while (cursor.moveToNext()) {
                    final SubTopic.Image image = SubTopic.Image.fromCursor(cursor);
                    final String topicId = cursor.getString(columnTopicId);
                    topics.stream()
                            .filter(topic -> topic.id.equals(topicId))
                            .findAny()
                            .ifPresent(topic -> topic.images.add(image));
                }
            }
        }
    }
}
