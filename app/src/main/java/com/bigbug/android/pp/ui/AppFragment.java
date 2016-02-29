package com.bigbug.android.pp.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import com.bigbug.android.pp.data.model.Round;
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

    protected long mBeginTime;
    protected long mEndTime;

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

    public static void reloadRounds(LoaderManager loaderManager, LoaderCallbacks callbacks) {
        Bundle args = new Bundle();
        loaderManager.restartLoader(RoundsQuery.TOKEN_NORMAL, args, callbacks);
    }

    public static void reloadPartners(LoaderManager loaderManager, Round round, LoaderCallbacks callbacks) {
        if (round != null) {
            Bundle args = new Bundle();
            args.putParcelable(SELECTED_ROUND, round);
            loaderManager.restartLoader(PartnersQuery.TOKEN_NORMAL, args, callbacks);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        LOGD(TAG, "onCreateLoader, id=" + id + ", args=" + args);
        Loader<Cursor> loader = null;

        switch (id) {
            case RoundsQuery.TOKEN_NORMAL: {
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.Rounds.CONTENT_URI,
                        null,
                        null,
                        new String[]{ args.getLong(BEGIN_DATE) + "", args.getLong(END_DATE) + "" },
                        AppContract.Rounds.TIME + " DESC");
                break;
            }
            case PartnersQuery.TOKEN_NORMAL: {
                Round track = args.getParcelable(SELECTED_ROUND);
                loader = new CursorLoader(
                        getActivity(),
                        AppContract.Partners.CONTENT_URI,
                        null,
                        AppContract.Partners.SELECTION_BY_ROUND_ID,
                        new String[]{ track.id + "" },
                        AppContract.Partners.FIRST + " ASC");
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

    protected interface RoundsQuery {
        int TOKEN_NORMAL = 100;
    }

    protected interface PartnersQuery {
        int TOKEN_NORMAL = 200;
    }
}
