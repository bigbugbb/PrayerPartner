package com.bigbug.android.pp.provider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bigbug.android.pp.provider.AppContract.Partners;
import com.bigbug.android.pp.provider.AppContract.Prayers;
import com.bigbug.android.pp.provider.AppContract.Rounds;
import com.bigbug.android.pp.sync.SyncHelper;
import com.bigbug.android.pp.sync.TrackerDataHandler;
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
        String PRAYERS = "prayers";
        String ROUNDS = "rounds";
        String PARTNERS = "partners";
    }

    interface FOREIGN_KEY {
        String ROUND_ID = "FOREIGN KEY(round_id) ";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String ROUND_ID = "REFERENCES " + Tables.ROUNDS + "(" + AppContract.Rounds._ID + ")";
    }

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL("CREATE TABLE " + Tables.PRAYERS + " ("
                + Prayers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Prayers.DIRTY + " INTEGER DEFAULT 1,"
                + Prayers.SYNC + " TEXT,"
                + Prayers.UPDATED + " INTEGER NOT NULL,"
                + Prayers.NAME + " TEXT NOT NULL,"
                + Prayers.PHOTO + " TEXT,"
                + Prayers.EMAIL + " TEXT NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.ROUNDS + " ("
                + Rounds._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Rounds.DIRTY + " INTEGER DEFAULT 1,"
                + Rounds.SYNC + " TEXT,"
                + Rounds.UPDATED + " INTEGER NOT NULL,"
                + Rounds.TIME + " INTEGER UNIQUE NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.PARTNERS + " ("
                + Partners._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Partners.DIRTY + " INTEGER DEFAULT 1,"
                + Partners.SYNC + " TEXT,"
                + Partners.UPDATED + " INTEGER NOT NULL,"
                + Partners.FIRST + " TEXT UNIQUE NOT NULL,"
                + Partners.SECOND + " TEXT UNIQUE NOT NULL,"
                + Partners.ROUND_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.ROUND_ID + References.ROUND_ID + " ON DELETE CASCADE);");

        // Create indexes on round_id
        db.execSQL("CREATE INDEX partner_round_id_index ON " + Tables.PARTNERS + "(" + Partners.ROUND_ID + ");");

        // Create indexes on dirty
        db.execSQL("CREATE INDEX partner_dirty_index ON " + Tables.PARTNERS + "(" + Partners.DIRTY + ");");
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
            TrackerDataHandler.resetDataTimestamp(mContext);
            if (account != null) {
                LOGI(TAG, "DB upgrade complete. Requesting resync.");
                SyncHelper.requestManualSync(account);
            }
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}