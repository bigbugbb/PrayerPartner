package com.bigbug.android.pp.provider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bigbug.android.pp.provider.AppContract.AppInfo;
import com.bigbug.android.pp.provider.AppContract.PairPrayers;
import com.bigbug.android.pp.provider.AppContract.Pairs;
import com.bigbug.android.pp.provider.AppContract.Prayers;
import com.bigbug.android.pp.sync.AppDataHandler;
import com.bigbug.android.pp.sync.SyncHelper;
import com.bigbug.android.pp.util.AccountUtils;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGI;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link AppProvider}.
 */
public class AppDatabase extends SQLiteOpenHelper {
    private static final String TAG = makeLogTag(AppDatabase.class);

    private static final String DATABASE_NAME = "pp.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int DATABASE_VERSION = 1;

    private final Context mContext;

    interface Tables {
        String APP_INFO = "app_info";
        String PAIRS = "pairs";
        String PRAYERS = "prayers";
        String PAIR_PRAYERS = "pair_prayers";
        String PRAYERS_JOIN_PAIR_THROUGH_PAIR_PRAYERS = "prayers "
                + "LEFT OUTER JOIN pair_prayers ON prayers._id=pair_prayers.prayer_id "
                + "LEFT OUTER JOIN pairs ON pair_prayers.pair_id=pairs._id";
    }

    interface FOREIGN_KEY {
        String PAIR_ID   = "FOREIGN KEY(pair_id) ";
        String PRAYER_ID = "FOREIGN KEY(prayer_id) ";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String PAIR_ID   = "REFERENCES " + Tables.PAIRS + "(" + Pairs._ID + ")";
        String PRAYER_ID = "REFERENCES " + Tables.PRAYERS + "(" + Prayers._ID + ")";
    }

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL("CREATE TABLE " + Tables.APP_INFO + " ("
                + AppInfo._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + AppInfo.LAST_PAIR_ID + " INTEGER,"
                + AppInfo.LAST_PARTNER_ID + " INTEGER);");

        db.execSQL("CREATE TABLE " + Tables.PRAYERS + " ("
                + Prayers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Prayers.DIRTY + " INTEGER DEFAULT 1,"
                + Prayers.SYNC + " TEXT,"
                + Prayers.UPDATED + " INTEGER NOT NULL,"
                + Prayers.CREATED + " INTEGER NOT NULL,"
                + Prayers.NAME + " TEXT NOT NULL,"
                + Prayers.PHOTO + " TEXT,"
                + Prayers.EMAIL + " TEXT UNIQUE NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.PAIRS + " ("
                + Pairs._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Pairs.DIRTY + " INTEGER DEFAULT 1,"
                + Pairs.SYNC + " TEXT,"
                + Pairs.UPDATED + " INTEGER NOT NULL,"
                + Pairs.CREATED + " INTEGER NOT NULL,"
                + Pairs.NUMBER + " INTEGER NOT NULL,"
                + Pairs.NOTIFIED + " INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE " + Tables.PAIR_PRAYERS + " ("
                + PairPrayers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PairPrayers.DIRTY + " INTEGER DEFAULT 1,"
                + PairPrayers.SYNC + " TEXT,"
                + PairPrayers.UPDATED + " INTEGER NOT NULL,"
                + PairPrayers.CREATED + " INTEGER NOT NULL,"
                + PairPrayers.PAIR_ID + " INTEGER NOT NULL,"
                + PairPrayers.PRAYER_ID + " INTEGER NOT NULL,"
                + PairPrayers.PARTNER_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.PAIR_ID + References.PAIR_ID + " ON DELETE CASCADE,"
                + FOREIGN_KEY.PRAYER_ID + References.PRAYER_ID + " ON DELETE CASCADE);");

//        // Create indexes on dirty
//        db.execSQL("CREATE INDEX partner_prayer_dirty_index ON " + Tables.PARTNER_PRAYERS + "(" + PartnerPrayers.DIRTY + ");");

        db.execSQL("INSERT INTO " + Tables.APP_INFO + " ("
                + AppInfo.LAST_PAIR_ID + ","
                + AppInfo.LAST_PARTNER_ID + ") VALUES (0, 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        Account account = AccountUtils.getActiveAccount(mContext);
        if (account != null) {
            LOGI(TAG, "Cancelling any pending syncs for account");
            ContentResolver.cancelSync(account, AppContract.CONTENT_AUTHORITY);
        }

        // Current DB version. We update this variable as we perform upgrades to reflect
        // the current version we are in.
        int version = oldVersion;

        // Indicates whether the data we currently have should be invalidated as a
        // result of the db upgrade. Default is true (invalidate); if we detect that this
        // is a trivial DB upgrade, we set this to false.
        boolean dataInvalidated = true;

        LOGD(TAG, "After upgrade logic, at version " + version);

        // at this point, we ran out of upgrade logic, so if we are still at the wrong
        // version, we have no choice but to delete everything and create everything again.
        if (version != DATABASE_VERSION) {
            // Do something here
            version = DATABASE_VERSION;
        }

        if (dataInvalidated) {
            LOGD(TAG, "Data invalidated; resetting our data timestamp.");
            AppDataHandler.resetDataTimestamp(mContext);
            if (account != null) {
                LOGI(TAG, "DB upgrade complete. Requesting resync.");
                SyncHelper.requestManualSync(account);
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}