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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.ui.widget.CollectionView;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bigbug.android.pp.R;
import com.google.android.gms.plus.PlusOneButton;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PrayerFragment extends AppFragment {
    private static final String TAG = makeLogTag(PrayerFragment.class);


    private CollectionView mPhotoCollectionView;
//    private PhotoCollectionAdapter mPhotoCollectionAdapter;

    private FloatingActionButton mFabTakePhoto;

    private ThrottledContentObserver mPhotosObserver;

    public PrayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Should be triggered after we taking a new photo
        mPhotosObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (photos). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting photos cursor reload as a result of ContentObserver firing.");
                    reloadPhotosWithRequiredPermission();
                }
            }
        });
        activity.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mPhotosObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPhotosObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_prayer, container, false);
//        mPhotoCollectionView = (CollectionView) root.findViewById(R.id.photos_view);
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
        mPhotosObserver.cancelPendingCallback();
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
            case AppFragment.PartnersQuery.TOKEN_NORMAL: {

                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case AppFragment.PartnersQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    private void reloadPhotosWithRequiredPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
//            reloadPhotos(getLoaderManager(), mBeginTime, mEndTime, this);
        }
    }

    private void updateInventoryDisplayColumns(CollectionView.Inventory inventory) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        for (CollectionView.InventoryGroup group : inventory) {
            if (dpWidth < 400) {
                group.setDisplayCols(3);
            } else if (dpWidth < 600) {
                group.setDisplayCols(4);
            } else {
                group.setDisplayCols(5);
            }
        }
    }
}
