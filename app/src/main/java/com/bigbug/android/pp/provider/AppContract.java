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

    interface PrayerColumns extends TimeColumns {
        String NAME  = "name";
        String PHOTO = "photo";
        String EMAIL = "email";
    }

    interface PartnerColumns extends TimeColumns {
        /** More extra fields */
    }

    interface PartnerPrayerColumns extends TimeColumns {
        String PARTNER_ID = "partner_id";
        String PRAYER_ID  = "prayer_id";
    }

    public static final String CONTENT_AUTHORITY = "com.bigbug.pp";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String SELECTION_BY_DIRTY = String.format("%s = ?", SyncColumns.DIRTY);

    public static class Prayers implements PrayerColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("prayers").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.prayer";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.prayer";

        /** Build a {@link Uri} that references a given prayer. */
        public static Uri buildPrayerUri(String prayerId) {
            return CONTENT_URI.buildUpon().appendPath(prayerId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPrayerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Partners implements PartnerColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("partners").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.partner";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.partner";

        /** Build a {@link Uri} that references a given partner. */
        public static Uri buildPartnerUri(String partnerId) {
            return CONTENT_URI.buildUpon().appendPath(partnerId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPartnerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class PartnerPrayers implements PartnerPrayerColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("partner_prayers").build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.pp.partner_prayer";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.pp.partner_prayer";

        /** Build a {@link Uri} that references a given partner prayer. */
        public static Uri buildPartnerPrayerUri(String partnerId) {
            return CONTENT_URI.buildUpon().appendPath(partnerId).build();
        }

        /** Read {@link #_ID} from {@link BaseColumns} {@link Uri}. */
        public static String getPartnerPrayerId(Uri uri) {
            return uri.getPathSegments().get(1);
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
