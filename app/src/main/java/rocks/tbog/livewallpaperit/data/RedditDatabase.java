package rocks.tbog.livewallpaperit.data;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import androidx.annotation.NonNull;

public class RedditDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "reddit.s3db";
    private static final int DB_VERSION = 3;
    private static final String TAG = "DB";
    static final String TABLE_IGNORE = "ignore_artwork";
    static final String TABLE_SUBREDDITS = "subreddits";
    static final String TABLE_TOPICS = "sub_topics";
    static final String TABLE_TOPIC_IMAGES = "topic_images";
    static final String ARTWORK_TOKEN = "token";
    static final String SUBREDDIT_NAME = "sub_name";
    static final String SUBREDDIT_MIN_UPVOTE_PERCENTAGE = "min_upvote_perc";
    static final String SUBREDDIT_MIN_SCORE = "min_score";
    static final String SUBREDDIT_MIN_COMMENTS = "min_comments";
    public static final String TOPIC_SUBREDDIT_NAME = "subreddit";
    public static final String TOPIC_ID = "id";
    public static final String TOPIC_TITLE = "title";
    public static final String TOPIC_AUTHOR = "author";
    public static final String TOPIC_LINK_FLAIR_TEXT = "link_flair_text";
    public static final String TOPIC_PERMALINK = "permalink";
    public static final String TOPIC_THUMBNAIL = "thumbnail";
    public static final String TOPIC_CREATED_UTC = "created_utc";
    public static final String TOPIC_SCORE = "score";
    public static final String TOPIC_UPVOTE_RATIO = "upvote_ratio";
    public static final String TOPIC_NUM_COMMENTS = "num_comments";
    public static final String TOPIC_OVER_18 = "over18";
    public static final String IMAGE_TOPIC_ID = "topic_id";
    public static final String IMAGE_URL = "url";
    public static final String IMAGE_MEDIA_ID = "media_id";
    public static final String IMAGE_WIDTH = "width";
    public static final String IMAGE_HEIGHT = "height";
    public static final String IMAGE_IS_NSFW = "is_obfuscated";
    public static final String IMAGE_IS_SOURCE = "is_source";

    RedditDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            database.execSQL("CREATE TABLE " + TABLE_IGNORE + " ( "
                    + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "\"" + ARTWORK_TOKEN + "\" TEXT UNIQUE);");
            createSubredditTable(database);
            createTopicTable(database);
            createTopicImageTable(database);
        } catch (SQLException e) {
            Log.e(TAG, "database failed to open", e);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Updating database from version " + oldVersion + " to version " + newVersion);
        if (oldVersion >= newVersion) return;
        try {
            switch (oldVersion) {
                case 1:
                    createSubredditTable(db);
                    // fall through
                case 2:
                    createTopicTable(db);
                    createTopicImageTable(db);
                    // fall through
                default:
                    break;
            }
        } catch (SQLException e) {
            Log.e(TAG, "database failed to open after upgrade", e);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "DOWN-grade database from version " + oldVersion + " to version " + newVersion);
        try {
            if (newVersion == 2) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOPICS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOPIC_IMAGES);
            }
        } catch (SQLException e) {
            Log.e(TAG, "downgrade from " + oldVersion + " to " + newVersion + " failed", e);
        }
    }

    private void createSubredditTable(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SUBREDDITS + " ( "
                + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                + "\"" + SUBREDDIT_NAME + "\" TEXT UNIQUE,"
                + "\"" + SUBREDDIT_MIN_UPVOTE_PERCENTAGE + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + SUBREDDIT_MIN_SCORE + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + SUBREDDIT_MIN_COMMENTS + "\" INTEGER NOT NULL DEFAULT 0);");
    }

    private void createTopicTable(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TOPICS + " ( "
                + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                + "\"" + TOPIC_ID + "\" TEXT UNIQUE,"
                + "\"" + TOPIC_SUBREDDIT_NAME + "\" TEXT NOT NULL,"
                + "\"" + TOPIC_TITLE + "\" TEXT NOT NULL,"
                + "\"" + TOPIC_AUTHOR + "\" TEXT,"
                + "\"" + TOPIC_LINK_FLAIR_TEXT + "\" TEXT,"
                + "\"" + TOPIC_PERMALINK + "\" TEXT NOT NULL,"
                + "\"" + TOPIC_THUMBNAIL + "\" TEXT NOT NULL,"
                + "\"" + TOPIC_CREATED_UTC + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + TOPIC_SCORE + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + TOPIC_UPVOTE_RATIO + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + TOPIC_NUM_COMMENTS + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + TOPIC_OVER_18 + "\" INTEGER NOT NULL DEFAULT 0,"
                + "CONSTRAINT fk_subreddit_name FOREIGN KEY(\"" + TOPIC_SUBREDDIT_NAME + "\") "
                + "REFERENCES \"" + TABLE_SUBREDDITS + "\"(\"" + SUBREDDIT_NAME + "\") "
                + "ON DELETE CASCADE ON UPDATE CASCADE);");
    }

    private void createTopicImageTable(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TOPIC_IMAGES + " ( "
                + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                + "\"" + IMAGE_MEDIA_ID + "\" TEXT NOT NULL,"
                + "\"" + IMAGE_TOPIC_ID + "\" TEXT,"
                + "\"" + IMAGE_URL + "\" TEXT NOT NULL,"
                + "\"" + IMAGE_WIDTH + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + IMAGE_HEIGHT + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + IMAGE_IS_NSFW + "\" INTEGER NOT NULL DEFAULT 0,"
                + "\"" + IMAGE_IS_SOURCE + "\" INTEGER NOT NULL DEFAULT 1,"
                + "CONSTRAINT fk_topic_id FOREIGN KEY(\"" + IMAGE_TOPIC_ID + "\") "
                + "REFERENCES \"" + TABLE_TOPICS + "\"(\"" + TOPIC_ID + "\") "
                + "ON DELETE CASCADE ON UPDATE CASCADE);");
    }
}
