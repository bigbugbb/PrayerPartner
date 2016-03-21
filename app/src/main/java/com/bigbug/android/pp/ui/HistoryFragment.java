package com.bigbug.android.pp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.util.ThrottledContentObserver;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 2/28/16.
 */
public class HistoryFragment extends AppFragment {
    private static final String TAG = makeLogTag(PartnerFragment.class);

    private ThrottledContentObserver mHistoryObserver;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Should be triggered after we taking a new photo
        mHistoryObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (photos). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting photos cursor reload as a result of ContentObserver firing.");
                    reloadPhotosWithRequiredPermission();
                }
            }
        });
        activity.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mHistoryObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mHistoryObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_history, container, false);
//        mPhotoCollectionAdapter = new PhotoCollectionAdapter();
//        mPhotoCollectionView.setCollectionAdapter(mPhotoCollectionAdapter);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");
        reloadPhotosWithRequiredPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHistoryObserver.cancelPendingCallback();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case AppFragment.RoundsQuery.TOKEN_NORMAL: {
//                CollectionView.Inventory photoInventory = Round.photoInventoryFromCursor(data);
//                if (photoInventory != null) {
//                    updateInventoryDisplayColumns(photoInventory);
//                    mPhotoCollectionView.updateInventory(photoInventory, true);
//                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case AppFragment.RoundsQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    private void reloadPhotosWithRequiredPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            reloadRounds(getLoaderManager(), this);
        }
    }
}
