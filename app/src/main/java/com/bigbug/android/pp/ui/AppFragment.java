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

    public static void reloadPairs(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(PairsQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadPairPrayers(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(PairPrayersQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadPairPrayers(LoaderManager loaderManager, String pairId, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        args.putString(AppContract.PairPrayers.QUERY_PARAMETER_PAIR_ID, pairId);
        loaderManager.restartLoader(PairPrayersQuery.TOKEN_SEARCH, args, callbacks);
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
            case PairPrayersQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.PairPrayers.CONTENT_URI,
                        AppContract.PairPrayers.PAIR_PRAYER_PROJECTION,
                        null,
                        null,
                        AppContract.PairPrayers.ORDER_BY_CREATED + " DESC");
                break;
            }
            case PairPrayersQuery.TOKEN_SEARCH: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.PairPrayers.buildPairSessionUri(
                                args.getString(AppContract.PairPrayers.QUERY_PARAMETER_PAIR_ID, AppContract.PairPrayers.DEFAULT_PAIR_ID)),
                        AppContract.PairPrayers.PAIR_PRAYER_PROJECTION,
                        null,
                        null,
                        null);
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
    }

    protected interface PairsQuery {
        int TOKEN_NORMAL = 200;
    }

    protected interface PairPrayersQuery {
        int TOKEN_NORMAL = 300;
        int TOKEN_SEARCH = 301;
    }

    protected interface PhotosQuery {
        int TOKEN_NORMAL = 400;
    }
}
