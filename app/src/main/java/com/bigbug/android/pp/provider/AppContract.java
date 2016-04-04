package com.bigbug.android.pp.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

/**
 * Contract class for interacting with {@link AppProvider}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class AppContract {
    /**
     * Query parameter to create a distinct query.
     */
    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    public interface SyncColumns {
        /** Whether the item needs to sync or not. */
        String DIRTY = "dirty";
        /** Whether the item is syncing or not. Each sync has a unique id. */
        String SYNC = "sync";
    }

    public interface TimeColumns {
        /** Item created time. */
        String CREATED = "created";
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }

    interface AppInfoColumns {
        String LAST_PAIR_ID    = "last_pair_id";
        String LAST_PARTNER_ID = "last_partner_id";
    }

    interface PrayerColumns extends TimeColumns {
        String NAME  = "name";
        String PHOTO = "photo";
        String EMAIL = "email";
    }

    /** Every time the user clicks the 'Pair' button, it generates a new pair. */
    interface PairColumns extends TimeColumns {
        /** The number of prayers in this pair session. */
        String NUMBER = "number";
        /** The flag to indicate whether the pair result is notified to the prayers or not. */
        String NOTIFIED = "notified";
    }

    interface PairPrayerColumns extends TimeColumns {
        String PAIR_ID    = "pair_id";
        String PRAYER_ID  = "prayer_id";
        String PARTNER_ID = "partner_id";
    }

    public static final String CONTENT_AUTHORITY = "com.bigbug.pp";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String SELECTION_BY_DIRTY = String.format("%s = ?", SyncColumns.DIRTY);

    public static class AppInfo implements AppInfoColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("app_info").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.app_info";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.app_info";

        /** Build a {@link Uri} that references a given app info. */
        public static Uri buildAppInfoUri(String appInfoId) {
            return CONTENT_URI.buildUpon().appendPath(appInfoId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getAppInfoId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Prayers implements PrayerColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("prayers").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.prayer";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.prayer";

        public static final String QUERY_BY_EMAIL = EMAIL + "=?";

        /** Build a {@link Uri} that references a given prayer. */
        public static Uri buildPrayerUri(String prayerId) {
            return CONTENT_URI.buildUpon().appendPath(prayerId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPrayerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Pairs implements PairColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("pairs").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.pair";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.pair";

        /** Build a {@link Uri} that references a given pair. */
        public static Uri buildPairUri(String pairId) {
            return CONTENT_URI.buildUpon().appendPath(pairId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPairId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class PairPrayers implements PairPrayerColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("pair_prayers").build();

        public static final Uri INCOMPLETED_PARTNER_URI = CONTENT_URI.buildUpon().appendPath("incompleted_partners").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.pair_prayer";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.pair_prayer";

        public static final String DEFAULT_PAIR_ID = "latest";
        public static final String SELECTION_VALID_PAIR_ID = String.format("%s > 0", PAIR_ID);

        public static final String ORDER_BY_CREATED = AppDatabase.Tables.PAIR_PRAYERS + "." + CREATED;

        public static final String COUNT_OF_PARTNER = String.format("COUNT(%s) AS count", PARTNER_ID);
        public static final String HAVING_INCOMPLETED_PARTNERS = "count < 2";

        public static final String[] PAIR_PRAYER_PROJECTION = new String[]{
                BaseColumns._ID,
                PairPrayerColumns.PAIR_ID,
                PairPrayerColumns.PRAYER_ID,
                PairPrayerColumns.PARTNER_ID,
                PrayerColumns.NAME,
                PrayerColumns.EMAIL,
                PrayerColumns.PHOTO,
                PairPrayerColumns.CREATED,
                PairPrayerColumns.UPDATED
        };

        /** Build a {@link Uri} that references a given pair prayer. */
        public static Uri buildPairPrayerUri(String pairPrayerId) {
            return CONTENT_URI.buildUpon().appendPath(pairPrayerId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPairPrayerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /** Build a {@link Uri} that references all prayers for a given pair session. */
        public static Uri buildPairSessionUri(String pairId) {
            return CONTENT_URI.buildUpon().appendQueryParameter(PAIR_ID, pairId).build();
        }
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true", uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private AppContract() {
    }
}
