package rocks.tbog.livewallpaperit.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import androidx.annotation.NonNull;

public class RedditDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "reddit.s3db";
    private static final int DB_VERSION = 2;
    private static final String TAG = "DB";
    static final String TABLE_IGNORE = "ignore_artwork";
    static final String TABLE_SUBREDDITS = "subreddits";
    static final String ARTWORK_TOKEN = "token";
    static final String SUBREDDIT_NAME = "sub_name";
    static final String SUBREDDIT_MIN_UPVOTE_PERCENTAGE = "min_upvote_perc";
    static final String SUBREDDIT_MIN_SCORE = "min_score";
    static final String SUBREDDIT_MIN_COMMENTS = "min_comments";

    RedditDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_IGNORE + " ( "
                + "\"" + BaseColumns._ID + "\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "\"" + ARTWORK_TOKEN + "\" TEXT UNIQUE);");
        createSubredditTable(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Updating database from version " + oldVersion + " to version " + newVersion);
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                    createSubredditTable(db);
                    // fall through
                default:
                    break;
            }
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
}
