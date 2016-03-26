package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.ui.widget.CollectionView;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGE;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 2/28/16.
 */
public class PartnerFragment extends AppFragment {
    private static final String TAG = makeLogTag(PartnerFragment.class);

    private CollectionView mPhotoCollectionView;
    private FloatingActionButton mFabMakePartner;

    private ThrottledContentObserver mPrayersObserver;
    private ThrottledContentObserver mPartnersObserver;

    private LinearLayout mPrayerSelector;
    private GridView mPrayersGrid;
    private PrayerAdapter mPrayerAdapter;

    private int mFragmentHeight;

    private static final float IMAGE_SCALE_FACTOR = 0.75f;
    private static final int IMAGE_SCALE_ANIM_DURATION = 250;
    private static final int SELECT_ALPHA_ANIM_DURATION = 250;

    public PartnerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mPrayersObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (photos). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting prayers cursor reload as a result of ContentObserver firing.");
                    reloadPrayers(getLoaderManager(), PartnerFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.Prayers.CONTENT_URI, true, mPrayersObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPrayersObserver);
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

        mPrayerSelector = (LinearLayout) root.findViewById(R.id.prayer_selector);
        mPrayersGrid = (GridView) mPrayerSelector.findViewById(R.id.grid_prayers);

        mPrayerAdapter = new PrayerAdapter(getActivity());
        mPrayersGrid.setAdapter(mPrayerAdapter);
        mPrayersGrid.setEmptyView(root.findViewById(R.id.empty_view));

        mPrayerSelector.setVisibility(View.INVISIBLE);

        mFabMakePartner = (FloatingActionButton) root.findViewById(R.id.fab_make_partner);
        mFabMakePartner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelector.setY(mFragmentHeight * 0.6f);
                mPrayerSelector.setVisibility(View.VISIBLE);
                mPrayerSelector.animate().translationY(0).setDuration(500).start();
                mFabMakePartner.hide();
                mPrayerAdapter.notifyDataSetChanged();
            }
        });

        mPrayerSelector.findViewById(R.id.cancel_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelector.animate().translationY(mFragmentHeight * 0.6f).setDuration(500).start();
                mFabMakePartner.show();
                cancelSelection();
            }
        });
        mPrayerSelector.findViewById(R.id.pair_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int count = getSelectionCount();
                if (count == 0) {
                    Toast.makeText(getActivity(), R.string.no_prayer_selected, Toast.LENGTH_LONG).show();
                    return;
                } else if (count % 2 != 0) {
                    Toast.makeText(getActivity(), R.string.odd_prayers_count, Toast.LENGTH_LONG).show();
                    return;
                }
                mPrayerSelector.animate().translationY(mFragmentHeight * 0.6f).setDuration(500).start();
                mFabMakePartner.show();
                applySelection();
            }
        });

        mPrayersGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LOGD(TAG, String.format("item %d is clicked", position));
                PrayerAdapter.PrayerState state = mPrayerAdapter.getItem(position);
                state.toggleSelection();
                PrayerAdapter.ViewHolder holder = (PrayerAdapter.ViewHolder) view.getTag();
                holder.updateViews(state, true);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mFragmentHeight = view.getHeight();
                mPrayerSelector.setPadding(0, Math.round(mFragmentHeight * 0.4f), 0, 0);
            }
        });
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
        mPrayersObserver.cancelPendingCallback();
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
                final Prayer[] prayers = Prayer.prayersFromCursor(data);
                if (prayers != null) {
                    mPrayerAdapter.clear();
                    mPrayerAdapter.addAll(prayersToPrayerStates(prayers));
                }
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

    private PrayerAdapter.PrayerState[] prayersToPrayerStates(Prayer[] prayers) {
        PrayerAdapter.PrayerState[] states = new PrayerAdapter.PrayerState[prayers.length];
        for (int i = 0; i < states.length; ++i) {
            states[i] = new PrayerAdapter.PrayerState(prayers[i], false);
        }
        return states;
    }

    private int getSelectionCount() {
        int count = 0;
        for (int i = 0; i < mPrayerAdapter.getCount(); ++i) {
            if (mPrayerAdapter.getItem(i).isSelected()) {
                count++;
            }
        }
        return count;
    }

    private void cancelSelection() {
        for (int i = 0; i < mPrayerAdapter.getCount(); ++i) {
            mPrayerAdapter.getItem(i).select(false);
        }
    }

    private void applySelection() {
        // Get selected prayers
        List<Prayer> selectedPrayers = new ArrayList<>(mPrayerAdapter.getCount());
        for (int i = 0; i < mPrayerAdapter.getCount(); ++i) {
            PrayerAdapter.PrayerState state = mPrayerAdapter.getItem(i);
            if (state.isSelected()) {
                selectedPrayers.add(state.getPrayer());
                state.select(false);
            }
        }

        // Pair prayers randomly (now only need to support two prayers into one partner group)
        shuffle(selectedPrayers);

        // Build and apply the operations to create a new partner and its associated pairs.
        long now = System.currentTimeMillis();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(selectedPrayers.size() / 2 + 1);
        ops.add(ContentProviderOperation
                .newInsert(AppContract.Partners.CONTENT_URI)
                .withValue(AppContract.TimeColumns.UPDATED, now)
                .withValue(AppContract.TimeColumns.CREATED, now)
                .build());
        for (int i = 0; i < selectedPrayers.size(); i += 2) {
            long id1 = selectedPrayers.get(i).id;
            long id2 = selectedPrayers.get(i + 1).id;
            ops.add(ContentProviderOperation
                    .newInsert(AppContract.PartnerPrayers.CONTENT_URI)
                    .withValueBackReference(AppContract.PartnerPrayers.PARTNER_ID, 0)
                    .withValue(AppContract.PartnerPrayers.PRAYER_ID, id1)
                    .withValue(AppContract.TimeColumns.UPDATED, now)
                    .withValue(AppContract.TimeColumns.CREATED, now)
                    .build());
            ops.add(ContentProviderOperation
                    .newInsert(AppContract.PartnerPrayers.CONTENT_URI)
                    .withValueBackReference(AppContract.PartnerPrayers.PARTNER_ID, 0)
                    .withValue(AppContract.PartnerPrayers.PRAYER_ID, id2)
                    .withValue(AppContract.TimeColumns.UPDATED, now)
                    .withValue(AppContract.TimeColumns.CREATED, now)
                    .build());
        }
        try {
            getActivity().getContentResolver().applyBatch(AppContract.CONTENT_AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            LOGE(TAG, "Fail to pair partners: " + e);
        }
    }

    private void shuffle(List list) {
        int size = list.size();
        Random rand = new Random();
        for (int i = 0; i < size; ++i) {
            int p = rand.nextInt(size);
            Object t = list.get(i);
            list.set(i, list.get(p));
            list.set(p, t);
        }
    }

    private static class PrayerAdapter extends ArrayAdapter<PrayerAdapter.PrayerState> {

        public PrayerAdapter(Context context) {
            super(context, 0, new ArrayList<PrayerState>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_grid_prayer, null);
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.text   = (TextView) convertView.findViewById(R.id.prayer_name);
                holder.image  = (ImageView) convertView.findViewById(R.id.prayer_photo);
                holder.select = (ImageView) convertView.findViewById(R.id.prayer_selected);
                convertView.setTag(holder);
            }
            holder.updateViews(getItem(position), false);

            return convertView;
        }

        class ViewHolder {
            private TextView  text;
            private ImageView image;
            private ImageView select;

            public void updateViews(final PrayerState state, boolean animated) {
                Prayer prayer = state.getPrayer();
                text.setText(prayer.name);
                if (state.isSelected()) {
                    image.animate()
                            .scaleX(IMAGE_SCALE_FACTOR)
                            .scaleY(IMAGE_SCALE_FACTOR)
                            .setDuration(animated ? IMAGE_SCALE_ANIM_DURATION : 0)
                            .start();
                    select.animate()
                            .alpha(1)
                            .setDuration(animated ? SELECT_ALPHA_ANIM_DURATION : 0)
                            .start();
                } else {
                    image.animate()
                            .scaleX(1)
                            .scaleY(1)
                            .setDuration(animated ? IMAGE_SCALE_ANIM_DURATION : 0)
                            .start();
                    select.animate()
                            .alpha(0)
                            .setDuration(animated ? SELECT_ALPHA_ANIM_DURATION : 0)
                            .start();
                }
                Glide.with(getContext())
                        .load(prayer.photo)
                        .centerCrop()
                        .crossFade()
                        .into(image);
            }
        }

        static class PrayerState {
            private Prayer  mPrayer;
            private boolean mSelected;

            public PrayerState(Prayer prayer, boolean selected) {
                mPrayer = prayer;
                mSelected = selected;
            }

            public Prayer getPrayer() {
                return mPrayer;
            }

            public boolean isSelected() {
                return mSelected;
            }

            public void toggleSelection() {
                mSelected = !mSelected;
            }

            public void select(boolean selected) {
                mSelected = selected;
            }
        }
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }
}
