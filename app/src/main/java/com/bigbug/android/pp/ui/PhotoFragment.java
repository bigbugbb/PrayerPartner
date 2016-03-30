package com.bigbug.android.pp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Photo;
import com.bigbug.android.pp.ui.widget.CollectionView;
import com.bigbug.android.pp.ui.widget.CollectionViewCallbacks;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGE;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PhotoFragment extends AppFragment {

    private static final String TAG = makeLogTag(PhotoFragment.class);

    private CollectionView mPhotoCollectionView;
    private PhotoCollectionAdapter mPhotoCollectionAdapter;

    private CollectionView.Inventory mPhotoInventory;

    private String mCurrentPhotoPath;
    private ThrottledContentObserver mPhotosObserver;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    public PhotoFragment() {
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
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_photo, container, false);
        mPhotoCollectionView = (CollectionView) root.findViewById(R.id.photos_view);
        mPhotoCollectionAdapter = new PhotoCollectionAdapter();
        mPhotoCollectionView.setCollectionAdapter(mPhotoCollectionAdapter);

//        mFabTakePhoto = (FloatingActionButton) root.findViewById(R.id.fab_take_photo);
//        mFabTakePhoto.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {

//            }
//        });

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            galleryAddPhoto();
            data.putExtra(PrayerSettingDialog.PRAYER_PHOTO_URI, Uri.fromFile(new File(mCurrentPhotoPath)));
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPhoto() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File image = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(image);
        mediaScanIntent.setData(contentUri);
        getActivity().sendOrderedBroadcast(mediaScanIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reloadPhotosWithRequiredPermission();
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }

        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                mPhotoInventory = Photo.photoInventoryFromCursor(data);
                if (mPhotoInventory != null) {
                    mPhotoCollectionView.updateInventory(mPhotoInventory, true);
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case PhotosQuery.TOKEN_NORMAL: {
                break;
            }
        }
    }

    private void reloadPhotosWithRequiredPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            reloadPhotos(getLoaderManager(), this);
        }
    }

    private ArrayList<Photo> inventoryToList(CollectionView.Inventory inventory) {
        ArrayList<Photo> photos = new ArrayList<>();
        for (CollectionView.InventoryGroup group : inventory) {
            for (int i = 0; i < group.getItemCount(); ++i) {
                Object itemTag = group.getItemTag(i);
                if (itemTag instanceof Photo) {
                    photos.add((Photo) itemTag);
                }
            }
        }
        return photos;
    }

    private class PhotoCollectionAdapter implements CollectionViewCallbacks {

        @Override
        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.photo_collection_header, parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel, Object headerTag) {
            TextView photoDate = (TextView) view.findViewById(R.id.photo_date);
            photoDate.setText(getDateLabel((DateTime) headerTag));
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.photo_collection_item, parent, false);
            final ItemViewHolder holder = new ItemViewHolder();
            holder.photoImage = (ImageView) view.findViewById(R.id.photo_image);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindCollectionItemView(final Context context, View view, int groupId, int indexInGroup, int dataIndex, Object itemTag) {
            final Object tag = view.getTag();
            final ItemViewHolder holder = (ItemViewHolder) tag;
            if (itemTag instanceof Photo) {
                final Photo photo = (Photo) itemTag;
                if (holder.photoImage != null) {
                    holder.photoImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent data = new Intent();
                            data.putExtra(PrayerSettingDialog.PRAYER_PHOTO_URI, Uri.fromFile(new File(photo.data)));
                            getActivity().setResult(Activity.RESULT_OK, data);
                            getActivity().finish();
                        }
                    });
                    Glide.with(context)
                            .load(photo.data)
                            .centerCrop()
                            .crossFade()
                            .into(holder.photoImage);
                }
            } else {
                holder.photoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Ensure that there's a camera activity to handle the intent
                        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                            // Create the File where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException e) {
                                // Error occurred while creating the File
                                LOGE(TAG, "IOException while creating the file: " + e.getMessage());
                            }
                            // Continue only if the File was successfully created
                            if (photoFile != null) {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                            }
                        }
                    }
                });
                Glide.with(context)
                        .load(R.drawable.ic_take_photo)
                        .centerCrop()
                        .crossFade()
                        .into(holder.photoImage);
            }
        }

        private String getDateLabel(final DateTime date) {
            if (date == null) {
                return "";
            }

            DateTime today = DateTime.now().withTimeAtStartOfDay();
            if (date.equals(today)) {
                return "Today";
            }

            DateTime yesterday = today.minusDays(1).withTimeAtStartOfDay(); // For daytime saving adjustment
            if (date.equals(yesterday)) {
                return "Yesterday";
            }

            StringBuilder sb = new StringBuilder()
                    .append(date.dayOfWeek().getAsText())
                    .append(", ")
                    .append(date.monthOfYear().getAsShortText())
                    .append(" ")
                    .append(date.dayOfMonth().getAsText());

            if (date.year().get() != today.year().get()) {
                sb.append(", ").append(date.year().getAsText());
            }

            return sb.toString();
        }

        private class ItemViewHolder {
            ImageView photoImage;
        }
    }
}