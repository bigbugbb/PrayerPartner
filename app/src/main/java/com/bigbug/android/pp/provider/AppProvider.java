package com.bigbug.android.pp.provider;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.bigbug.android.pp.provider.AppContract.AppInfo;
import com.bigbug.android.pp.provider.AppContract.PairPrayers;
import com.bigbug.android.pp.provider.AppContract.Pairs;
import com.bigbug.android.pp.provider.AppContract.Prayers;
import com.bigbug.android.pp.provider.AppContract.TimeColumns;
import com.bigbug.android.pp.provider.AppDatabase.Tables;
import com.bigbug.android.pp.util.AccountUtils;
import com.bigbug.android.pp.util.SelectionBuilder;

import java.util.ArrayList;
import java.util.Arrays;

import static com.bigbug.android.pp.util.LogUtils.LOGV;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Provider that stores {@link AppContract} data. Data is usually inserted
 * by {@link com.bigbug.android.pp.sync.SyncHelper}, and queried by various
 * {@link Activity} instances.
 */

public class AppProvider extends ContentProvider {
    private static final String TAG = makeLogTag(AppProvider.class);

    private AppDatabase mOpenHelper;

    private final ThreadLocal<Boolean> mIsInBatchMode = new ThreadLocal<>();

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int APP_INFO = 100;
    private static final int APP_INFO_ID = 101;

    private static final int PRAYERS = 200;
    private static final int PRAYERS_ID = 201;

    private static final int PAIRS = 300;
    private static final int PAIRS_ID = 301;

    private static final int PAIR_PRAYERS = 400;
    private static final int PAIR_PRAYERS_ID = 401;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AppContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "app_info", APP_INFO);
        matcher.addURI(authority, "app_info/*", APP_INFO_ID);

        matcher.addURI(authority, "prayers", PRAYERS);
        matcher.addURI(authority, "prayers/*", PRAYERS_ID);

        matcher.addURI(authority, "pairs", PAIRS);
        matcher.addURI(authority, "pairs/*", PAIRS_ID);

        matcher.addURI(authority, "pair_prayers", PAIR_PRAYERS);
        matcher.addURI(authority, "pair_prayers/*", PAIR_PRAYERS_ID);

        return matcher;
    }

    /**
     * Implement this to initialize your content provider on startup.
     * This method is called for all registered content providers on the
     * application main thread at application launch time.  It must not perform
     * lengthy operations, or application startup will be delayed.
     * <p/>
     * <p>You should defer nontrivial initialization (such as opening,
     * upgrading, and scanning databases) until the content provider is used
     * (via {@link #query}, {@link #insert}, etc).  Deferred initialization
     * keeps application startup fast, avoids unnecessary work if the provider
     * turns out not to be needed, and stops database errors (such as a full
     * disk) from halting application launch.
     * <p/>
     * <p>If you use SQLite, {@link android.database.sqlite.SQLiteOpenHelper}
     * is a helpful utility class that makes it easy to manage databases,
     * and will automatically defer opening until first use.  If you do use
     * SQLiteOpenHelper, make sure to avoid calling
     * {@link android.database.sqlite.SQLiteOpenHelper#getReadableDatabase} or
     * {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase}
     * from this method.  (Instead, override
     * {@link android.database.sqlite.SQLiteOpenHelper#onOpen} to initialize the
     * database when it is first opened.)
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new AppDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        AppDatabase.deleteDatabase(context);
        mOpenHelper = new AppDatabase(getContext());
    }

    /** Returns a tuple of question marks. For example, if count is 3, returns "(?,?,?)". */
    private String makeQuestionMarkTuple(int count) {
        if (count < 1) {
            return "()";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(?");
        for (int i = 1; i < count; i++) {
            stringBuilder.append(",?");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);

        // avoid the expensive string concatenation below if not loggable
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            LOGV(TAG, "uri=" + uri + " match=" + match + " proj=" + Arrays.toString(projection) +
                    " selection=" + selection + " args=" + Arrays.toString(selectionArgs) + ")");
        }

        SelectionBuilder builder = null;
        switch (match) {
            case PAIR_PRAYERS: {
                builder = buildExpandedSelection(uri, match);
            }
            default: {
                // Most cases are handled with simple SelectionBuilder
                if (builder == null) {
                    builder = buildSimpleSelection(uri);
                }

                boolean distinct = !TextUtils.isEmpty(uri.getQueryParameter(AppContract.QUERY_PARAMETER_DISTINCT));

                try {
                    Cursor cursor = builder
                            .where(selection, selectionArgs)
                            .query(db, distinct, projection, sortOrder, null);
                    Context context = getContext();
                    if (null != context) {
                        cursor.setNotificationUri(context.getContentResolver(), uri);
                    }
                    return cursor;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private String getCurrentAccountName(Uri uri, boolean sanitize) {
        String accountName = AccountUtils.getActiveAccountName(getContext());
        if (sanitize) {
            // sanitize accountName when concatenating (http://xkcd.com/327/)
            accountName = (accountName != null) ? accountName.replace("'", "''") : null;
        }
        return accountName;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case APP_INFO:
                return AppInfo.CONTENT_TYPE;
            case APP_INFO_ID:
                return AppInfo.CONTENT_ITEM_TYPE;
            case PRAYERS:
                return Prayers.CONTENT_TYPE;
            case PRAYERS_ID:
                return Prayers.CONTENT_ITEM_TYPE;
            case PAIRS:
                return Pairs.CONTENT_TYPE;
            case PAIRS_ID:
                return Pairs.CONTENT_ITEM_TYPE;
            case PAIR_PRAYERS:
                return PairPrayers.CONTENT_TYPE;
            case PAIR_PRAYERS_ID:
                return PairPrayers.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        mIsInBatchMode.set(true);
        // the next line works because SQLiteDatabase
        // uses a thread local SQLiteSession object for all manipulations
        db.beginTransaction();
        try {
            final ContentProviderResult[] retResult = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return retResult;
        } finally {
            mIsInBatchMode.remove();
            db.endTransaction();
        }
    }

    private boolean isInBatchMode() {
        return mIsInBatchMode.get() != null && mIsInBatchMode.get();
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        boolean syncToNetwork = !AppContract.hasCallerIsSyncAdapterParameter(uri);
        if (syncToNetwork) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

            // Widgets can't register content observers so we refresh widgets separately.
//            context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        LOGV(TAG, "bulkInsert(uri=" + uri + ", values=" + values.toString() + ", account=" + getCurrentAccountName(uri, false) + ")");
        int numInserted = 0;
        String table;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRAYERS: {
                table = Tables.PRAYERS;
                break;
            }
            case PAIRS: {
                table = Tables.PAIRS;
                break;
            }
            case PAIR_PRAYERS: {
                table = Tables.PAIR_PRAYERS;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }

        db.beginTransaction();
        try {
            for (ContentValues cv : values) {
                long newID = db.insertOrThrow(table, null, cv);
                if (newID <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
            }
            db.setTransactionSuccessful();
            notifyChange(uri);
            numInserted = values.length;
        } finally {
            db.endTransaction();
        }
        return numInserted;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ", account=" + getCurrentAccountName(uri, false) + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case APP_INFO: {
                long newId = db.insertOrThrow(Tables.APP_INFO, null, values);
                notifyChange(uri);
                return AppInfo.buildAppInfoUri("" + newId);
            }
            case PRAYERS: {
                long newId = db.insertOrThrow(Tables.PRAYERS, null, values);
                notifyChange(uri);
                return Prayers.buildPrayerUri("" + newId);
            }
            case PAIRS: {
                long newId = db.insertOrThrow(Tables.PAIRS, null, values);
                notifyChange(uri);
                return Pairs.buildPairUri("" + newId);
            }
            case PAIR_PRAYERS: {
                long newId = db.insertOrThrow(Tables.PAIR_PRAYERS, null, values);
                notifyChange(uri);
                return PairPrayers.buildPairPrayerUri("" + newId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "delete(uri=" + uri + ", account=" + accountName + ")");
        if (uri.equals(AppContract.BASE_CONTENT_URI)) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String accountName = getCurrentAccountName(uri, false);
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ", account=" + accountName + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case APP_INFO: {
                return builder.table(Tables.APP_INFO);
            }
            case APP_INFO_ID: {
                final String id = AppInfo.getAppInfoId(uri);
                return builder.table(Tables.APP_INFO).where(AppInfo._ID + "=?", id);
            }
            case PRAYERS: {
                return builder.table(Tables.PRAYERS);
            }
            case PRAYERS_ID: {
                final String id = Prayers.getPrayerId(uri);
                return builder.table(Tables.PRAYERS).where(Prayers._ID + "=?", id);
            }
            case PAIRS: {
                return builder.table(Tables.PAIRS);
            }
            case PAIRS_ID: {
                final String id = Pairs.getPairId(uri);
                return builder.table(Tables.PAIRS).where(Pairs._ID + "=?", id);
            }
            case PAIR_PRAYERS: {
                return builder.table(Tables.PRAYERS_JOIN_PAIR_THROUGH_PAIR_PRAYERS);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + match + ": " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case PAIR_PRAYERS:
                String pairId = uri.getQueryParameter(PairPrayers.QUERY_PARAMETER_PAIR_ID);
                if (TextUtils.isEmpty(pairId)) {
                    return null;
                } else {
                    if (pairId.equals(PairPrayers.DEFAULT_PAIR_ID)) {
                        pairId = getLatestPairId();
                    }
                    if (TextUtils.isEmpty(pairId)) {
                        return null;
                    }
                    return builder.table(Tables.PRAYERS_JOIN_PAIR_THROUGH_PAIR_PRAYERS)
                            .mapToTable(BaseColumns._ID, Tables.PAIR_PRAYERS)
                            .mapToTable(TimeColumns.CREATED, Tables.PAIR_PRAYERS)
                            .mapToTable(TimeColumns.UPDATED, Tables.PAIR_PRAYERS)
                            .where(PairPrayers.QUERY_PARAMETER_PAIR_ID + "=?", pairId)
                            .groupBy(PairPrayers.PARTNER_ID + "," + PairPrayers.PRAYER_ID);
                }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private String getLatestPairId() {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor cursor = db.query(Tables.APP_INFO, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(AppInfo.LAST_PAIR_ID));
        }
        return null;
    }
}
