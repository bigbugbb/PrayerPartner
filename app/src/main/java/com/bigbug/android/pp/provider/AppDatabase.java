package com.bigbug.android.pp.provider;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bigbug.android.pp.provider.AppContract.PartnerPrayers;
import com.bigbug.android.pp.provider.AppContract.Partners;
import com.bigbug.android.pp.provider.AppContract.Prayers;
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
        String PARTNERS = "partners";
        String PARTNER_PRAYERS = "partner_prayers";
    }

    interface FOREIGN_KEY {
        String PRAYER_ID = "FOREIGN KEY(prayer_id) ";
        String PARTNER_ID = "FOREIGN KEY(partner_id) ";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String PRAYER_ID  = "REFERENCES " + Tables.PRAYERS + "(" + Prayers._ID + ")";
        String PARTNER_ID = "REFERENCES " + Tables.PARTNERS + "(" + Partners._ID + ")";
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
                + Prayers.CREATED + " INTEGER NOT NULL,"
                + Prayers.NAME + " TEXT NOT NULL,"
                + Prayers.PHOTO + " TEXT,"
                + Prayers.EMAIL + " TEXT UNIQUE NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.PARTNERS + " ("
                + Partners._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Partners.DIRTY + " INTEGER DEFAULT 1,"
                + Partners.SYNC + " TEXT,"
                + Partners.UPDATED + " INTEGER NOT NULL,"
                + Partners.CREATED + " INTEGER NOT NULL);");

        db.execSQL("CREATE TABLE " + Tables.PARTNER_PRAYERS + " ("
                + PartnerPrayers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PartnerPrayers.DIRTY + " INTEGER DEFAULT 1,"
                + PartnerPrayers.SYNC + " TEXT,"
                + PartnerPrayers.UPDATED + " INTEGER NOT NULL,"
                + PartnerPrayers.CREATED + " INTEGER NOT NULL,"
                + PartnerPrayers.PARTNER_ID + " INTEGER NOT NULL,"
                + PartnerPrayers.PRAYER_ID + " INTEGER NOT NULL,"
                + FOREIGN_KEY.PARTNER_ID + References.PARTNER_ID + " ON DELETE CASCADE,"
                + FOREIGN_KEY.PRAYER_ID + References.PRAYER_ID + " ON DELETE CASCADE);");

//        // Create indexes on prayer_id and partner_id
//        db.execSQL("CREATE INDEX partner_prayer_prayer_id_index ON " + Tables.PARTNER_PRAYERS + "(" + PartnerPrayers.PRAYER_ID + ");");
//        db.execSQL("CREATE INDEX partner_prayer_partner_id_index ON " + Tables.PARTNER_PRAYERS + "(" + PartnerPrayers.PARTNER_ID + ");");
//
//        // Create indexes on dirty
//        db.execSQL("CREATE INDEX partner_prayer_dirty_index ON " + Tables.PARTNER_PRAYERS + "(" + PartnerPrayers.DIRTY + ");");
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