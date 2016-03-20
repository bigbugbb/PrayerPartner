package com.bigbug.android.pp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.ui.widget.CollectionView;
import com.bigbug.android.pp.util.ThrottledContentObserver;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 2/28/16.
 */
public class PartnerFragment extends AppFragment {
    private static final String TAG = makeLogTag(PartnerFragment.class);

    private CollectionView mPhotoCollectionView;
//    private PhotoCollectionAdapter mPhotoCollectionAdapter;

    private FloatingActionButton mFabTakePhoto;

    private ThrottledContentObserver mPhotosObserver;

    public PartnerFragment() {
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
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_partner, container, false);
        mPhotoCollectionView = (CollectionView) root.findViewById(R.id.photos_view);
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
//
//    private static class PhotoCollectionAdapter implements CollectionViewCallbacks {
//
//        @Override
//        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
//            return LayoutInflater.from(context).inflate(R.layout.photo_collection_header, parent, false);
//        }
//
//        @Override
//        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel, Object headerTag) {
//            TextView photoDate = (TextView) view.findViewById(R.id.photo_date);
//            photoDate.setText(getDateLabel((DateTime) headerTag));
//        }
//
//        @Override
//        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
//            final View view = LayoutInflater.from(context).inflate(R.layout.photo_collection_item, parent, false);
//            final ItemViewHolder holder = new ItemViewHolder();
//            holder.photoImage = (ImageView) view.findViewById(R.id.photo_image);
//            view.setTag(holder);
//            return view;
//        }
//
//        @Override
//        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object itemTag) {
//            final Object tag = view.getTag();
//            if (tag instanceof ItemViewHolder) {
//                final ItemViewHolder holder = (ItemViewHolder) tag;
//                final Photo photo = (Photo) itemTag;
//                if (holder.photoImage != null) {
//                    Glide.with(context)
//                            .load(photo.data)
//                            .centerCrop()
//                            .crossFade()
//                            .into(holder.photoImage);
//                }
//            }
//        }
//
//        private String getDateLabel(final DateTime date) {
//            if (date == null) {
//                return "";
//            }
//
//            DateTime today = DateTime.now().withTimeAtStartOfDay();
//            if (date.equals(today)) {
//                return "Today";
//            }
//
//            DateTime yesterday = today.minusDays(1).withTimeAtStartOfDay(); // For daytime saving adjustment
//            if (date.equals(yesterday)) {
//                return "Yesterday";
//            }
//
//            StringBuilder sb = new StringBuilder()
//                    .append(date.dayOfWeek().getAsText())
//                    .append(", ")
//                    .append(date.monthOfYear().getAsShortText())
//                    .append(" ")
//                    .append(date.dayOfMonth().getAsText());
//
//            if (date.year().get() != today.year().get()) {
//                sb.append(", ").append(date.year().getAsText());
//            }
//
//            return sb.toString();
//        }
//
//        private static class ItemViewHolder {
//            ImageView photoImage;
//        }
//    }
}