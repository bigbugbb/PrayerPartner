package com.bigbug.android.pp.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.bigbug.android.pp.Config;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.util.AccountUtils;
import com.bigbug.android.pp.util.LogUtils;
import com.bigbug.android.pp.util.PrefUtils;
import com.bigbug.android.pp.util.UIUtils;

import java.io.IOException;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGE;
import static com.bigbug.android.pp.util.LogUtils.LOGW;

/**
 * A helper class for dealing with conference data synchronization.
 * All operations occur on the thread they're called from, so it's best to wrap
 * calls in an {@link android.os.AsyncTask}, or better yet, a
 * {@link android.app.Service}.
 */
public class SyncHelper {
    private static final String TAG = LogUtils.makeLogTag("SyncHelper");

    private Context mContext;
    private TrackerDataHandler mTrackerDataHandler;
    private RemoteTrackerDataFetcher mRemoteDataFetcher;

//    TransferUtility mTransferUtility;

    public SyncHelper(Context context) {
        mContext = context;
        mTrackerDataHandler = new TrackerDataHandler(mContext);
        mRemoteDataFetcher = new RemoteTrackerDataFetcher(mContext);
    }

    public static void requestManualSync(Account chosenAccount) {
        if (chosenAccount != null) {
            LOGD(TAG, "Requesting manual sync for account " + chosenAccount.name);
            Bundle b = new Bundle();
            b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            ContentResolver.setSyncAutomatically(chosenAccount, AppContract.CONTENT_AUTHORITY, true);
            ContentResolver.setIsSyncable(chosenAccount, AppContract.CONTENT_AUTHORITY, 1);

            boolean pending = ContentResolver.isSyncPending(chosenAccount, AppContract.CONTENT_AUTHORITY);
            if (pending) {
                LOGD(TAG, "Warning: sync is PENDING. Will cancel.");
            }
            boolean active = ContentResolver.isSyncActive(chosenAccount, AppContract.CONTENT_AUTHORITY);
            if (active) {
                LOGD(TAG, "Warning: sync is ACTIVE. Will cancel.");
            }

            if (pending || active) {
                LOGD(TAG, "Cancelling previously pending/active sync.");
                ContentResolver.cancelSync(chosenAccount, AppContract.CONTENT_AUTHORITY);
            }

            LOGD(TAG, "Requesting sync now.");
            ContentResolver.requestSync(chosenAccount, AppContract.CONTENT_AUTHORITY, b);
        } else {
            LOGD(TAG, "Can't request manual sync -- no chosen account.");
        }
    }

    /**
     * Attempts to perform conference data synchronization. The data comes from the remote URL
     * configured in {@link com.bigbug.android.pp.Config#MANIFEST_URL}. The remote URL
     * must point to a manifest file that, in turn, can reference other files. For more details
     * about conference data synchronization, refer to the documentation at
     * http://code.google.com/p/iosched.
     *
     * @param syncResult (optional) the sync result object to update with statistics.
     * @param account    the account associated with this sync
     * @return Whether or not the synchronization made any changes to the data.
     */
    public boolean performSync(SyncResult syncResult, Account account, Bundle extras) {
        boolean dataChanged = false;

        // Get auth token and use it for each data request
        final String authToken = AccountUtils.getAuthToken(mContext);

//        // Initialize the Amazon Cognito credentials provider
//        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//            mContext,
//            Config.COGNITO_IDENTITY_POOL_ID,
//            Regions.US_EAST_1 // Region
//        );
//
//        // Initialize s3 client and the transfer utility
//        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
//        mTransferUtility = new TransferUtility(s3, mContext);

        long lastAttemptTime = PrefUtils.getLastSyncAttemptedTime(mContext);
        long now = UIUtils.getCurrentTime(mContext);
        long timeSinceAttempt = now - lastAttemptTime;
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

        if (!manualSync && timeSinceAttempt >= 0 && timeSinceAttempt < Config.MIN_INTERVAL_BETWEEN_SYNCS) {
            /*
            Code removed because it was causing a runaway sync; probably because we are setting
            syncResult.delayUntil incorrectly.

            Random r = new Random();
            long toWait = 10000 + r.nextInt(30000) // random jitter between 10 - 40 seconds
                    + Config.MIN_INTERVAL_BETWEEN_SYNCS - timeSinceAttempt;
            LOGW(TAG, "Sync throttled!! Another sync was attempted just " + timeSinceAttempt
                    + "ms ago. Requesting delay of " + toWait + "ms.");
            syncResult.fullSyncRequested = true;
            syncResult.delayUntil = (System.currentTimeMillis() + toWait) / 1000L;
            return false;*/
        }

        LogUtils.LOGI(TAG, "Performing sync for account: " + account);
        PrefUtils.markSyncAttemptedNow(mContext);
        long opStart;
        long remoteSyncDuration, choresDuration;

        opStart = System.currentTimeMillis();

        // remote sync consists of these operations, which we try one by one (and tolerate individual failures on each)
        final int OP_USER_DATA_SYNC  = 0;
        final int OP_TRACK_DATA_SYNC = 1;
        int[] opsToPerform = new int[] { OP_TRACK_DATA_SYNC };

        for (int op : opsToPerform) {
            try {
                switch (op) {
                    case OP_USER_DATA_SYNC:
                        break;
                    case OP_TRACK_DATA_SYNC:
                        dataChanged |= doTrackDataSync(authToken);
                        break;
//                    case OP_USER_FEEDBACK_SYNC:
//                        doUserFeedbackSync();
//                        break;
                }
            } catch (AuthException ex) {
                syncResult.stats.numAuthExceptions++;

                // if we have a token, try to refresh it
                if (AccountUtils.hasToken(mContext)) {
                    AccountUtils.refreshAuthToken(mContext);
                } else {
                    LOGW(TAG, "No auth token yet for this account. Skipping remote sync.");
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing remote sync.");
                increaseIoExceptions(syncResult);
            }
        }
        remoteSyncDuration = System.currentTimeMillis() - opStart;

        // If data has changed, there are a few chores we have to do
        opStart = System.currentTimeMillis();
        if (dataChanged) {
            try {
                performPostSyncChores(mContext);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                LOGE(TAG, "Error performing post sync chores.");
            }
        }
        choresDuration = System.currentTimeMillis() - opStart;

        int operations = mTrackerDataHandler.getContentProviderOperationsDone();
        if (syncResult != null && syncResult.stats != null) {
            syncResult.stats.numEntries += operations;
            syncResult.stats.numUpdates += operations;
        }

        if (dataChanged) {
            long totalDuration = choresDuration + remoteSyncDuration;
            LOGD(TAG, "SYNC STATS:\n" +
                    " *  Account synced: " + (account == null ? "null" : account.name) + "\n" +
                    " *  Content provider operations: " + operations + "\n" +
                    " *  Remote sync took: " + remoteSyncDuration + "ms\n" +
                    " *  Post-sync chores took: " + choresDuration + "ms\n" +
                    " *  Total time: " + totalDuration + "ms\n" +
                    " *  Total data read from cache: \n" +
                    (mRemoteDataFetcher.getTotalBytesReadFromCache() / 1024) + "kB\n" +
                    " *  Total data downloaded: \n" +
                    (mRemoteDataFetcher.getTotalBytesDownloaded() / 1024) + "kB");
        }

        LogUtils.LOGI(TAG, "End of sync (" + (dataChanged ? "data changed" : "no data change") + ")");

        updateSyncInterval(mContext, account);

        return dataChanged;
    }

    public static void performPostSyncChores(final Context context) {
        // Update search index
        LOGD(TAG, "Updating search index.");
//        context.getContentResolver().update(TrackerContract.SearchIndex.CONTENT_URI,
//                new ContentValues(), null, null);

        // Sync calendars
        LOGD(TAG, "Session data changed. Syncing starred sessions with Calendar.");
        syncCalendar(context);
    }

    private static void syncCalendar(Context context) {
//        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
//        intent.setClass(context, SessionCalendarService.class);
//        context.startService(intent);
    }

    private void doUserFeedbackSync() {
        LOGD(TAG, "Syncing feedback");
        new FeedbackSyncHelper(mContext).sync();
    }

    private boolean doUserDataSync() throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }
        return true;
    }

    /**
     * Checks if the remote server has new data that we need to import. If so, download
     * the new data and import it into the database.
     *
     * @return Whether or not data was changed.
     * @throws IOException if there is a problem downloading or importing the data.
     */
    private boolean doTrackDataSync(@NonNull final String authToken) throws IOException {
        if (!isOnline()) {
            LOGD(TAG, "Not attempting remote sync because device is OFFLINE");
            return false;
        }

        if (Config.WIFI_ONLY_SYNC_ENABLED && !isUsingWifi()) {
            LOGD(TAG, "Not attempting remote sync because wifi-only sync is enabled but the device is not connected to WIFI");
            return false;
        }

        LOGD(TAG, "Starting track data sync.");



        // TODO: upload the payload with the track data and the track url list (dirty)

        return true;
    }

    private void syncData(@NonNull Uri uri, OnDataSyncingListener listener) {

    }

    // Returns whether we are connected to the internet.
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private boolean isUsingWifi() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    private void increaseIoExceptions(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numIoExceptions;
        }
    }

    private void increaseSuccesses(SyncResult syncResult) {
        if (syncResult != null && syncResult.stats != null) {
            ++syncResult.stats.numEntries;
            ++syncResult.stats.numUpdates;
        }
    }

    public static class AuthException extends RuntimeException {
    }

    public static long calculateRecommendedSyncInterval(final Context context) {
        // TODO: dynamically sync interval change
        return DateUtils.HOUR_IN_MILLIS * 6;
    }

    public static void updateSyncInterval(final Context context, final Account account) {
        LOGD(TAG, "Checking sync interval for " + account);
        long recommended = calculateRecommendedSyncInterval(context);
        long current = PrefUtils.getCurSyncInterval(context);
        LOGD(TAG, "Recommended sync interval " + recommended + ", current " + current);
        if (recommended != current) {
            LOGD(TAG, "Setting up sync for account " + account + ", interval " + recommended + "ms");
            ContentResolver.setIsSyncable(account, AppContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AppContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, AppContract.CONTENT_AUTHORITY, new Bundle(), recommended / DateUtils.SECOND_IN_MILLIS);
            PrefUtils.setCurSyncInterval(context, recommended);
        } else {
            LOGD(TAG, "No need to update sync interval.");
        }
    }

    private interface OnDataSyncingListener {
        void onDataSyncing(Cursor cursor);
    }
}