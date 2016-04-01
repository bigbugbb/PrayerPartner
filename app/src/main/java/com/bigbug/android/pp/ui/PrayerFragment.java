package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.adapter.BaseAbstractRecyclerCursorAdapter;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.provider.handler.AppQueryHandler;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import java.io.File;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PrayerFragment extends AppFragment implements OnPrayerSettingListener {
    private static final String TAG = makeLogTag(PrayerFragment.class);

    private RecyclerView mPrayerList;

    private PrayerAdapter mPrayerAdapter;

    private FloatingActionButton mFabAddPrayer;

    private ThrottledContentObserver mPrayerObserver;

    private AppQueryHandler mPrayerSettingHandler;

    public PrayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Should be triggered after we taking a new photo
        mPrayerObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (photos). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting photos cursor reload as a result of ContentObserver firing.");
                    reloadPrayers(getLoaderManager(), PrayerFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.Prayers.CONTENT_URI, true, mPrayerObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPrayerObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mPrayerSettingHandler = new AppQueryHandler(getActivity().getContentResolver()) {

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                switch (token) {
                    case 0:
                        handlePrayerCreate(token, cookie, cursor);
                        break;
                    case 1:
                        handlePrayerUpdate(token, cookie, cursor);
                        break;
                }
            }

            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                // Empty
            }

            @Override
            protected void onError(int token, Object cookie, RuntimeException error) {
                if (isAdded()) {
                    switch (token) {
                        case 0:
                            Toast.makeText(getActivity(), R.string.fail_to_add_prayer, Toast.LENGTH_LONG).show();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), R.string.fail_to_edit_prayer, Toast.LENGTH_LONG).show();
                            break;
                        case 2:
                            Toast.makeText(getActivity(), R.string.fail_to_delete_prayer, Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }

            private void handlePrayerCreate(int token, Object cookie, Cursor cursor) {
                final Prayer prayer = (Prayer) cookie;

                if (cursor != null && cursor.moveToFirst()) {
                    if (isAdded()) {
                        Toast.makeText(getActivity(), R.string.existing_prayer_email, Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                ContentValues values = new ContentValues();
                values.put(AppContract.Prayers.NAME, prayer.name);
                values.put(AppContract.Prayers.EMAIL, prayer.email);
                values.put(AppContract.Prayers.PHOTO, prayer.photo);
                values.put(AppContract.TimeColumns.UPDATED, System.currentTimeMillis());
                values.put(AppContract.TimeColumns.CREATED, System.currentTimeMillis());

                startInsert(token, prayer, AppContract.Prayers.CONTENT_URI, values);
            }

            private void handlePrayerUpdate(int token, Object cookie, Cursor cursor) {
                final Prayer prayer = (Prayer) cookie;

                if (cursor != null && cursor.moveToFirst()) {
                    Prayer existing = new Prayer(cursor);
                    if (existing.id != prayer.id) {
                        if (isAdded()) {
                            Toast.makeText(getActivity(), R.string.existing_prayer_email, Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                }

                ContentValues values = new ContentValues();
                values.put(AppContract.Prayers.NAME, prayer.name);
                values.put(AppContract.Prayers.EMAIL, prayer.email);
                values.put(AppContract.Prayers.PHOTO, prayer.photo);
                values.put(AppContract.TimeColumns.UPDATED, System.currentTimeMillis());

                startUpdate(token, prayer, AppContract.Prayers.buildPrayerUri(prayer.id + ""), values, null, null);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_prayer, container, false);

        mPrayerList = (RecyclerView) root.findViewById(R.id.prayer_list);
        mFabAddPrayer = (FloatingActionButton) root.findViewById(R.id.fab_add_prayer);
        mFabAddPrayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrayerSettingDialog dialog = new PrayerSettingDialog();
                FragmentManager fm = getFragmentManager();
                if (fm.findFragmentByTag(PrayerSettingDialog.TAG) == null) {
                    dialog.setListener(PrayerFragment.this);
                    dialog.show(fm, PrayerSettingDialog.TAG);
                }
            }
        });

        mPrayerAdapter = new PrayerAdapter(getActivity());

        mPrayerList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPrayerList.setItemAnimator(new DefaultItemAnimator());
        mPrayerList.setAdapter(mPrayerAdapter);

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
        reloadPrayers(getLoaderManager(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrayerObserver.cancelPendingCallback();
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
            case AppFragment.PrayersQuery.TOKEN_NORMAL: {
                mPrayerAdapter.changeCursor(data);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreatePrayer(Prayer prayer) {
        if (!isValidPrayer(prayer)) {
            return;
        }
        mPrayerSettingHandler.startQuery(0, prayer, AppContract.Prayers.CONTENT_URI, null, AppContract.Prayers.QUERY_BY_EMAIL, new String[]{prayer.email}, null);
    }

    @Override
    public void onUpdatePrayer(Prayer prayer) {
        if (!isValidPrayer(prayer)) {
            return;
        }
        mPrayerSettingHandler.startQuery(1, prayer, AppContract.Prayers.CONTENT_URI, null, AppContract.Prayers.QUERY_BY_EMAIL, new String[]{prayer.email}, null);
    }

    @Override
    public void onDeletePrayer(Prayer prayer) {
        mPrayerSettingHandler.startDelete(2, prayer, AppContract.Prayers.buildPrayerUri(prayer.id + ""), null, null);
    }

    private boolean isValidPrayer(Prayer prayer) {
        if (TextUtils.isEmpty(prayer.name) || TextUtils.isEmpty(prayer.email)) {
            final String errorMsg = getString(R.string.prayer_dialog_error_data);
            Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private class PrayerAdapter extends BaseAbstractRecyclerCursorAdapter<PrayerAdapter.ViewHolder> {
        private Context mContext;

        public PrayerAdapter(Context context) {
            super(context, null);
            mContext = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(R.layout.item_prayer, parent, false);
            return new ViewHolder(item);
        }

        /**
         * Call when bind view with the cursor
         *
         * @param holder RecyclerView.ViewHolder
         * @param cursor The cursor from which to get the data. The cursor is already
         */
        @Override
        public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
            holder.bindData(new Prayer(cursor));
        }

        @Override
        public Prayer getItem(int position) {
            Cursor cursor = (Cursor) super.getItem(position);
            if (cursor != null) {
                return new Prayer(cursor);
            }
            return null;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView  mName;
            TextView  mEmail;
            ImageView mPhoto;

            Prayer mPrayer;

            ViewHolder(final View view) {
                super(view);
                mName  = (TextView) view.findViewById(R.id.prayer_name);
                mEmail = (TextView) view.findViewById(R.id.prayer_email);
                mPhoto = (ImageView) view.findViewById(R.id.prayer_photo);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PrayerSettingDialog dialog = new PrayerSettingDialog();
                        FragmentManager fm = getFragmentManager();
                        if (fm.findFragmentByTag(PrayerSettingDialog.TAG) == null) {
                            dialog.setPrayer(mPrayer);
                            dialog.setListener(PrayerFragment.this);
                            dialog.show(fm, PrayerSettingDialog.TAG);
                        }
                    }
                });
            }

            void bindData(final Prayer prayer) {
                mPrayer = prayer;
                mName.setText(prayer.name);
                mEmail.setText(prayer.email);
                if (!TextUtils.isEmpty(prayer.photo)) {
                    File photoFile = new File(prayer.photo);
                    if (photoFile.exists() && photoFile.isFile()) {
                        Glide.with(mContext)
                                .load(Uri.fromFile(photoFile))
                                .centerCrop()
                                .crossFade(250)
                                .into(mPhoto);
                    }
                } else {
                    mPhoto.setImageResource(R.drawable.ic_default_prayer);
                }
            }
        }
    }
}
