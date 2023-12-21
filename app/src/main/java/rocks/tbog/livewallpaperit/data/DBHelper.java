package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private static int delete(SQLiteDatabase db, @NonNull String table, ContentValues filter) {
        StringBuilder where = new StringBuilder();
        String[] args = new String[filter.size()];
        int i = 0;
        for (var field : filter.valueSet()) {
            if (i == 0) where.append("\"");
            else where.append(" AND \"");
            where.append(field.getKey()).append("\"=?");
            args[i++] = field.getValue().toString();
        }

        return db.delete(table, where.toString(), args);
    }

    private static int delete(SQLiteDatabase db, @NonNull String table, @NonNull String whereField, String whereValue) {
        return db.delete(table, "\"" + whereField + "\"=?", new String[] {whereValue});
    }

    public static boolean insertIgnoreMedia(@NonNull Context context, @NonNull MediaInfo info) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        info.fillValues(values);

        return -1 != db.insertWithOnConflict(RedditDatabase.TABLE_IGNORE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static int insertIgnoreMedia(@NonNull Context context, @NonNull Collection<MediaInfo> mediaList) {
        SQLiteDatabase db = getDatabase(context);

        int count = 0;
        ContentValues values = new ContentValues();
        db.beginTransaction();
        try {
            for (var info : mediaList) {
                info.fillValues(values);
                if (db.insertWithOnConflict(RedditDatabase.TABLE_IGNORE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        != -1) {
                    count += 1;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    public static void removeIgnoreMedia(@NonNull Context context, @NonNull MediaInfo info) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        info.fillValues(values);

        delete(db, RedditDatabase.TABLE_IGNORE, values);
    }

    public static int removeIgnoreMedia(@NonNull Context context, @NonNull Collection<MediaInfo> infoList) {
        SQLiteDatabase db = getDatabase(context);

        int count = 0;
        ContentValues values = new ContentValues();

        db.beginTransaction();
        try {
            for (var info : infoList) {
                info.fillValues(values);
                count += delete(db, RedditDatabase.TABLE_IGNORE, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    @NonNull
    public static List<MediaInfo> getIgnoreMediaList(@NonNull Context context, String subreddit) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<MediaInfo> records = null;
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_IGNORE,
                new String[] {RedditDatabase.IGNORE_MEDIA_ID, RedditDatabase.IGNORE_TOPIC_ID},
                "\"" + RedditDatabase.IGNORE_SUBREDDIT + "\"=?",
                new String[] {subreddit},
                null,
                null,
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    String mediaId = cursor.getString(0);
                    String topicId = cursor.getString(1);
                    records.add(new MediaInfo(mediaId, topicId, subreddit));

                    cursor.moveToNext();
                }
            }
        }
        if (records == null) {
            return Collections.emptyList();
        }
        return records;
    }

    public static List<Source> getSources(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<Source> records = null;
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_SUBREDDITS,
                new String[] {
                    RedditDatabase.SUBREDDIT_NAME,
                    RedditDatabase.SUBREDDIT_MIN_UPVOTE_PERCENTAGE,
                    RedditDatabase.SUBREDDIT_MIN_SCORE,
                    RedditDatabase.SUBREDDIT_MIN_COMMENTS,
                    RedditDatabase.SUBREDDIT_IMAGE_MIN_WIDTH,
                    RedditDatabase.SUBREDDIT_IMAGE_MIN_HEIGHT,
                    RedditDatabase.SUBREDDIT_IMAGE_ORIENTATION,
                    RedditDatabase.SUBREDDIT_ENABLED,
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
                    int columnIdx = 0;
                    Source source = new Source(cursor.getString(columnIdx++));
                    source.minUpvotePercentage = cursor.getInt(columnIdx++);
                    source.minScore = cursor.getInt(columnIdx++);
                    source.minComments = cursor.getInt(columnIdx++);
                    source.imageMinWidth = cursor.getInt(columnIdx++);
                    source.imageMinHeight = cursor.getInt(columnIdx++);
                    source.imageOrientation = Source.Orientation.fromInt(cursor.getInt(columnIdx++));
                    source.isEnabled = 0 != cursor.getInt(columnIdx++);
                    //noinspection ConstantValue
                    if (columnIdx != 8) throw new IllegalStateException("Invalid column count");
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
        value.put(RedditDatabase.SUBREDDIT_IMAGE_MIN_WIDTH, source.imageMinWidth);
        value.put(RedditDatabase.SUBREDDIT_IMAGE_MIN_HEIGHT, source.imageMinHeight);
        value.put(RedditDatabase.SUBREDDIT_IMAGE_ORIENTATION, source.imageOrientation.toInt());
        value.put(RedditDatabase.SUBREDDIT_ENABLED, source.isEnabled);
        return -1
                != db.insertWithOnConflict(
                        RedditDatabase.TABLE_SUBREDDITS, null, value, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void updateSource(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues value = new ContentValues();
        value.put(RedditDatabase.SUBREDDIT_MIN_UPVOTE_PERCENTAGE, source.minUpvotePercentage);
        value.put(RedditDatabase.SUBREDDIT_MIN_SCORE, source.minScore);
        value.put(RedditDatabase.SUBREDDIT_MIN_COMMENTS, source.minComments);
        value.put(RedditDatabase.SUBREDDIT_IMAGE_MIN_WIDTH, source.imageMinWidth);
        value.put(RedditDatabase.SUBREDDIT_IMAGE_MIN_HEIGHT, source.imageMinHeight);
        value.put(RedditDatabase.SUBREDDIT_IMAGE_ORIENTATION, source.imageOrientation.toInt());
        value.put(RedditDatabase.SUBREDDIT_ENABLED, source.isEnabled);
        db.updateWithOnConflict(
                RedditDatabase.TABLE_SUBREDDITS,
                value,
                RedditDatabase.SUBREDDIT_NAME + "=?",
                new String[] {source.subreddit},
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void removeSource(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);
        delete(db, RedditDatabase.TABLE_SUBREDDITS, RedditDatabase.SUBREDDIT_NAME, source.subreddit);
    }

    public static void removeSourceSubTopics(Context context, Source source) {
        SQLiteDatabase db = getDatabase(context);
        delete(db, RedditDatabase.TABLE_TOPICS, RedditDatabase.TOPIC_SUBREDDIT_NAME, source.subreddit);
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

    /**
     * Update only topic values from the TABLE_TOPICS table
     */
    public static void updateSubTopic(Context context, SubTopic topic) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        topic.fillTopicValues(values);
        db.updateWithOnConflict(
                RedditDatabase.TABLE_TOPICS,
                values,
                "\"" + RedditDatabase.TOPIC_ID + "\"=?",
                new String[] {topic.id},
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Nullable
    private static String getValueAsString(@NonNull ContentValues map, String key) {
        var value = map.get(key);
        if (value instanceof Boolean) {
            if ((Boolean) value) return "1";
            return "0";
        }
        if (value != null) return value.toString();
        return null;
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
                columnValues[colIdx] = getValueAsString(values, columnName);
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
                    whereArgs[i] = getValueAsString(values, columnName);
                }
                db.updateWithOnConflict(table, values, where.toString(), whereArgs, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    public static boolean removeSubTopic(@NonNull Context context, @NonNull SubTopic topic) {
        SQLiteDatabase db = getDatabase(context);
        return 0 <= delete(db, RedditDatabase.TABLE_TOPICS, RedditDatabase.TOPIC_ID, topic.id);
    }

    public static int removeSubTopicsNotMatching(
            @NonNull Context context, String subreddit, @NonNull Collection<String> topicIds) {
        if (topicIds.isEmpty()) return 0;
        SQLiteDatabase db = getDatabase(context);

        StringBuilder where = new StringBuilder()
                .append("\"")
                .append(RedditDatabase.TOPIC_SUBREDDIT_NAME)
                .append("\"=? AND \"")
                .append(RedditDatabase.TOPIC_ID)
                .append("\" NOT IN (?");
        if (topicIds.size() > 1) where.append(",?".repeat(topicIds.size() - 1));
        where.append(")");

        String[] args = new String[1 + topicIds.size()];
        int idx = 0;
        args[idx++] = subreddit;
        for (var topicId : topicIds) {
            args[idx++] = topicId;
        }

        return db.delete(RedditDatabase.TABLE_TOPICS, where.toString(), args);
    }

    @Nullable
    public static SubTopic getSubTopic(@NonNull Context context, String topicId) {
        SQLiteDatabase db = getDatabase(context);

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
                    RedditDatabase.TOPIC_SELECTED,
                },
                "\"" + RedditDatabase.TOPIC_ID + "\" = ?",
                new String[] {topicId},
                null,
                null,
                null,
                "1")) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return SubTopic.fromCursor(cursor);
                }
            }
        }

        return null;
    }

    public static List<SubTopic> getSubTopics(@NonNull Context context, String subreddit) {
        return getSubTopics(context, subreddit, 0);
    }

    @NonNull
    public static List<SubTopic> getSubTopics(@NonNull Context context, String subreddit, int limit) {
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
                    RedditDatabase.TOPIC_SELECTED,
                },
                "\"" + RedditDatabase.TOPIC_SUBREDDIT_NAME + "\" = ?",
                new String[] {subreddit},
                null,
                null,
                "\"" + RedditDatabase.TOPIC_CREATED_UTC + "\" DESC",
                limit < 1 ? null : Integer.toString(limit))) {
            if (cursor != null && cursor.moveToFirst()) {
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

    public static List<SubTopic> getSubTopicsWithFavorites(@NonNull Context context, String subreddit) {
        SQLiteDatabase db = getDatabase(context);
        ArrayList<SubTopic> records = null;

        try (Cursor cursor = db.rawQuery(
                "SELECT "
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".* "
                        + "FROM "
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\",\"" + RedditDatabase.TABLE_FAVORITE + "\" "
                        + "WHERE "
                        + "\"" + RedditDatabase.TABLE_FAVORITE + "\".\"" + RedditDatabase.FAVORITE_TOPIC_ID + "\"="
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_ID + "\" "
                        + "AND "
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_SUBREDDIT_NAME + "\"=? "
                        + "ORDER BY "
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_CREATED_UTC + "\" DESC",
                new String[] {subreddit})) {
            if (cursor != null && cursor.moveToFirst()) {
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
            SQLiteDatabase db, @NonNull String topicId, @NonNull Collection<Image> outImages) {
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
                    Image image = Image.fromCursor(cursor);
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
                    final Image image = Image.fromCursor(cursor);
                    final String topicId = cursor.getString(columnTopicId);
                    topics.stream()
                            .filter(topic -> topic.id.equals(topicId))
                            .findAny()
                            .ifPresent(topic -> topic.images.add(image));
                }
            }
        }
    }

    public static void removeImages(@NonNull Context context, @NonNull Collection<String> mediaIds) {
        if (mediaIds.isEmpty()) return;
        SQLiteDatabase db = getDatabase(context);

        StringBuilder where = new StringBuilder()
                .append("\"")
                .append(RedditDatabase.IMAGE_MEDIA_ID)
                .append("\" IN (?");
        if (mediaIds.size() > 1) where.append(",?".repeat(mediaIds.size() - 1));
        where.append(")");
        String[] args = mediaIds.toArray(new String[0]);
        db.delete(RedditDatabase.TABLE_TOPIC_IMAGES, where.toString(), args);
    }

    public static void insertFavorite(@NonNull Context context, @NonNull MediaInfo info) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        info.fillValues(values);

        db.insertWithOnConflict(RedditDatabase.TABLE_FAVORITE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static boolean removeFavorite(@NonNull Context context, @NonNull MediaInfo info) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        info.fillValues(values);

        return delete(db, RedditDatabase.TABLE_FAVORITE, values) > 0;
    }

    public static int removeFavorite(@NonNull Context context, @NonNull Collection<MediaInfo> mediaInfos) {
        SQLiteDatabase db = getDatabase(context);

        int count = 0;
        ContentValues values = new ContentValues();

        db.beginTransaction();
        try {
            for (var info : mediaInfos) {
                info.fillValues(values);
                count += delete(db, RedditDatabase.TABLE_FAVORITE, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    @NonNull
    public static List<MediaInfo> getFavoriteMediaList(@NonNull Context context, @NonNull String subreddit) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<MediaInfo> records = null;
        try (Cursor cursor = db.query(
                RedditDatabase.TABLE_FAVORITE,
                new String[] {RedditDatabase.FAVORITE_MEDIA_ID, RedditDatabase.FAVORITE_TOPIC_ID},
                "\"" + RedditDatabase.FAVORITE_SUBREDDIT + "\"=?",
                new String[] {subreddit},
                null,
                null,
                null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    String mediaId = cursor.getString(0);
                    String topicId = cursor.getString(1);
                    records.add(new MediaInfo(mediaId, topicId, subreddit));

                    cursor.moveToNext();
                }
            }
        }
        if (records == null) {
            return Collections.emptyList();
        }
        return records;
    }

    @NonNull
    public static List<Image> getFavoriteImages(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<Image> records = null;
        try (Cursor cursor = db.rawQuery(
                "SELECT "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".*,"
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_SUBREDDIT_NAME + "\" "
                        + "FROM "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\",\"" + RedditDatabase.TABLE_TOPICS
                        + "\",\"" + RedditDatabase.TABLE_FAVORITE + "\" "
                        + "WHERE "
                        + "\"" + RedditDatabase.TABLE_FAVORITE + "\".\"" + RedditDatabase.FAVORITE_TOPIC_ID + "\"="
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_ID + "\" "
                        + "AND "
                        + "\"" + RedditDatabase.TABLE_FAVORITE + "\".\"" + RedditDatabase.FAVORITE_MEDIA_ID + "\"="
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".\"" + RedditDatabase.IMAGE_MEDIA_ID + "\" ",
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                records = new ArrayList<>(cursor.getCount());
                while (!cursor.isAfterLast()) {
                    records.add(Image.fromCursor(cursor));

                    cursor.moveToNext();
                }
            }
        }
        if (records == null) {
            return Collections.emptyList();
        }
        return records;
    }

    @Nullable
    public static MediaInfo getMediaByToken(@NonNull Context context, String artworkToken) {
        SQLiteDatabase db = getDatabase(context);

        try (Cursor cursor = db.rawQuery(
                "SELECT "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".\"" + RedditDatabase.IMAGE_MEDIA_ID + "\","
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".\"" + RedditDatabase.IMAGE_TOPIC_ID + "\","
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_SUBREDDIT_NAME + "\" "
                        + "FROM "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\",\"" + RedditDatabase.TABLE_TOPICS + "\" "
                        + "WHERE "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".\"" + RedditDatabase.IMAGE_TOPIC_ID + "\"="
                        + "\"" + RedditDatabase.TABLE_TOPICS + "\".\"" + RedditDatabase.TOPIC_ID + "\" "
                        + "AND "
                        + "\"" + RedditDatabase.TABLE_TOPIC_IMAGES + "\".\"" + RedditDatabase.IMAGE_MEDIA_ID + "\"=?"
                        + " LIMIT 1",
                new String[] {artworkToken})) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String mediaId = cursor.getString(0);
                    String topicId = cursor.getString(1);
                    String subreddit = cursor.getString(2);
                    return new MediaInfo(mediaId, topicId, subreddit);
                }
            }
        }
        return null;
    }
}
