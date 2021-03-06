package com.bigbug.android.pp.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.bigbug.android.pp.data.JSONHandler;
import com.bigbug.android.pp.data.PrayersHandler;
import com.bigbug.android.pp.provider.AppContract;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGE;
import static com.bigbug.android.pp.util.LogUtils.LOGW;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

//import com.google.android.gms.maps.model.Tile;

/**
 * Helper class that parses company data and imports them into the app's
 * Content Provider.
 */
public class AppDataHandler {
    private static final String TAG = makeLogTag(SyncHelper.class);

    // Shared preferences key under which we store the timestamp that corresponds to
    // the data we currently have in our content provider.
    private static final String SP_KEY_DATA_TIMESTAMP = "data_timestamp";

    // symbolic timestamp to use when we are missing timestamp data (which means our data is
    // really old or nonexistent)
    private static final String DEFAULT_TIMESTAMP = "Sat, 1 Jan 2015 00:00:00 GMT";

    private static final String DATA_KEY_BLOGS = "blogs";
    private static final String DATA_KEY_TAGS = "tags";
    private static final String DATA_KEY_BLOGS_TAGS = "blogs_tags";
    private static final String DATA_KEY_UPCOMING_EVENTS = "upcoming_events";

    private static final String[] DATA_KEYS_IN_ORDER = {
        DATA_KEY_BLOGS,
        DATA_KEY_TAGS,
        DATA_KEY_BLOGS_TAGS,
        DATA_KEY_UPCOMING_EVENTS
    };

    Context mContext = null;

    // Handlers for each entity type:
    PrayersHandler mPrayersHandler = null;

    // Convenience map that maps the key name to its corresponding handler (e.g.
    // "blocks" to mBlocksHandler (to avoid very tedious if-elses)
    HashMap<String, JSONHandler> mHandlerForKey = new HashMap<String, JSONHandler>();

    // Tally of total content provider operations we carried out (for statistical purposes)
    private int mContentProviderOperationsDone = 0;

    public AppDataHandler(Context ctx) {
        mContext = ctx;
    }

    /**
     * Parses the company data in the given objects and imports the data into the
     * content provider.
     *
     * @param dataBodies The collection of JSON objects to parse and import.
     * @param dataTimestamp The timestamp of the data. This should be in RFC1123 format.
     * @param downloadsAllowed Whether or not we are supposed to download data from the internet if needed.
     * @throws IOException If there is a problem parsing the data.
     */
    public void applyCompanyData(String[] dataBodies, String dataTimestamp, boolean downloadsAllowed)
            throws IOException {
        LOGD(TAG, "Applying data from " + dataBodies.length + " files, timestamp " + dataTimestamp);

        // create handlers for each data type
        mHandlerForKey.put(DATA_KEY_BLOGS, mPrayersHandler = new PrayersHandler(mContext));

        // process the jsons. This will call each of the handlers when appropriate to deal
        // with the objects we see in the data.
        LOGD(TAG, "Processing " + dataBodies.length + " JSON objects.");
        for (int i = 0; i < dataBodies.length; i++) {
            LOGD(TAG, "Processing json object #" + (i + 1) + " of " + dataBodies.length);
            processDataBody(dataBodies[i]);
        }

        // produce the necessary content provider operations
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (String key : DATA_KEYS_IN_ORDER) {
            LOGD(TAG, "Building content provider operations for: " + key);
            mHandlerForKey.get(key).makeContentProviderOperations(batch);
            LOGD(TAG, "Content provider operations so far: " + batch.size());
        }
        LOGD(TAG, "Total content provider operations: " + batch.size());

        // download or process local map tile overlay files (SVG files)
        LOGD(TAG, "Processing map overlay files");

        // finally, push the changes into the Content Provider
        LOGD(TAG, "Applying " + batch.size() + " content provider operations.");
        try {
            int operations = batch.size();
            if (operations > 0) {
                mContext.getContentResolver().applyBatch(AppContract.CONTENT_AUTHORITY, batch);
            }
            LOGD(TAG, "Successfully applied " + operations + " content provider operations.");
            mContentProviderOperationsDone += operations;
        } catch (RemoteException ex) {
            LOGE(TAG, "RemoteException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        } catch (OperationApplicationException ex) {
            LOGE(TAG, "OperationApplicationException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        }

        // notify all top-level paths
//        LOGD(TAG, "Notifying changes on all top-level paths on Content Resolver.");
//        ContentResolver resolver = mContext.getContentResolver();
//        for (String path : TrackerContract.TOP_LEVEL_PATHS) {
//            Uri uri = TrackerContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
//            resolver.notifyChange(uri, null);
//        }

        // update our data timestamp
        setDataTimestamp(dataTimestamp);
        LOGD(TAG, "Done applying conference data.");
    }

    public int getContentProviderOperationsDone() {
        return mContentProviderOperationsDone;
    }

    /**
     * Processes a conference data body and calls the appropriate data type handlers
     * to process each of the objects represented therein.
     *
     * @param dataBody The body of data to process
     * @throws IOException If there is an error parsing the data.
     */
    private void processDataBody(String dataBody) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(dataBody));
        JsonParser parser = new JsonParser();
        try {
            reader.setLenient(true); // To err is human

            // the whole file is a single JSON object
            reader.beginObject();

            while (reader.hasNext()) {
                // the key is "rooms", "speakers", "tracks", etc.
                String key = reader.nextName();
                if (mHandlerForKey.containsKey(key)) {
                    // pass the value to the corresponding handler
                    mHandlerForKey.get(key).process(parser.parse(reader));
                } else {
                    LOGW(TAG, "Skipping unknown key in conference data json: " + key);
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
    }

    // Returns the timestamp of the data we have in the content provider.
    public String getDataTimestamp() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SP_KEY_DATA_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    // Sets the timestamp of the data we have in the content provider.
    public void setDataTimestamp(String timestamp) {
        LOGD(TAG, "Setting data timestamp to: " + timestamp);
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(
                SP_KEY_DATA_TIMESTAMP, timestamp).commit();
    }

    // Reset the timestamp of the data we have in the content provider
    public static void resetDataTimestamp(final Context context) {
        LOGD(TAG, "Resetting data timestamp to default (to invalidate our synced data)");
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(
                SP_KEY_DATA_TIMESTAMP).commit();
    }

    /**
     * A type of ConsoleRequestLogger that does not log requests and responses.
     */
//    private RequestLogger mQuietLogger = new ConsoleRequestLogger(){
//        @Override
//        public void logRequest(HttpURLConnection uc, Object content) throws IOException { }
//
//        @Override
//        public void logResponse(HttpResponse res) { }
//    };

}