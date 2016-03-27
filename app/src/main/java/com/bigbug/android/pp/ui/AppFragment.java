package com.bigbug.android.pp.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import com.bigbug.android.pp.provider.AppContract;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * The base class for other fragment.
 * It defines the common communication between each fragment and the TrackerActivity.
 */
public abstract class AppFragment extends Fragment implements LoaderCallbacks<Cursor> {
    protected static final String TAG = makeLogTag(AppFragment.class);

    public static final String SELECTED_ROUND = "selected_track";
    public static final String BEGIN_DATE = "begin_date";
    public static final String END_DATE = "end_date";

    protected Handler mHandler;

    public AppFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    public static void reloadPrayers(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(PrayersQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadPairedPrayers(LoaderManager loaderManager, String pairId, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        args.putString(PrayersQuery.PAIR_ID_KEY, pairId);
        loaderManager.restartLoader(PrayersQuery.TOKEN_SEARCH, args, callbacks);
    }

    public static void reloadPairs(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(PairsQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadPhotos(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(PhotosQuery.TOKEN_NORMAL, args, callbacks);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", args=" + args);
        Loader<Cursor> loader = null;

        switch (id) {
            case PrayersQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.Prayers.CONTENT_URI,
                        null,
                        null,
                        null,
                        AppContract.Prayers.NAME);
                break;
            }
            case PrayersQuery.TOKEN_SEARCH: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.Prayers.buildPairedPrayersUri(
                                args.getString(PrayersQuery.PAIR_ID_KEY, AppContract.Prayers.DEFAULT_PAIR_ID)),
                        null,
                        null,
                        null,
                        AppContract.PairPrayers.PARTNER_ID);
                break;
            }
            case PairsQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.Pairs.CONTENT_URI,
                        null,
                        null,
                        null,
                        AppContract.Pairs.CREATED + " DESC");
                break;
            }
            case PhotosQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        null,
                        null,
                        null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC");
                break;
            }
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected interface PrayersQuery {
        int TOKEN_NORMAL = 100;
        int TOKEN_SEARCH = 101;

        String PAIR_ID_KEY = "pair_id";
    }

    protected interface PairsQuery {
        int TOKEN_NORMAL = 200;
    }

    protected interface PairPrayersQuery {
        int TOKEN_NORMAL = 300;
    }

    protected interface PhotosQuery {
        int TOKEN_NORMAL = 400;
    }
}
