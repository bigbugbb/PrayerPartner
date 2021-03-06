package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.PairPrayer;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.ui.widget.PrayerGridAdapter;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

    private RecyclerView   mPartnerList;
    private PartnerAdapter mPartnerAdapter;

    private FloatingActionButton mFabPairPartner;

    private ThrottledContentObserver mPrayersObserver;
    private ThrottledContentObserver mLatestPairPrayersObserver;

    private LinearLayout mPrayerSelector;

    private GridView          mPrayerGrid;
    private PrayerGridAdapter mPrayerGridAdapter;

    private int mFragmentHeight;

    public PartnerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mPrayersObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (prayers). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting prayers cursor reload as a result of ContentObserver firing.");
                    reloadPrayers(getLoaderManager(), PartnerFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.Prayers.CONTENT_URI, true, mPrayersObserver);

        mLatestPairPrayersObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (pair_prayers). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting partners cursor reload as a result of ContentObserver firing.");
                    reloadPairPrayers(getLoaderManager(), null, PartnerFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.PairPrayers.CONTENT_URI, true, mLatestPairPrayersObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPrayersObserver);
        getActivity().getContentResolver().unregisterContentObserver(mLatestPairPrayersObserver);
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
        mPartnerList = (RecyclerView) root.findViewById(R.id.partner_list);

        mPartnerAdapter = new PartnerAdapter(getActivity());
        mPartnerList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPartnerList.setItemAnimator(new DefaultItemAnimator());
        mPartnerList.setAdapter(mPartnerAdapter);

        mPrayerSelector = (LinearLayout) root.findViewById(R.id.prayer_selector);
        mPrayerGrid = (GridView) mPrayerSelector.findViewById(R.id.grid_prayers);

        mPrayerGridAdapter = new PrayerGridAdapter(getActivity());
        mPrayerGrid.setAdapter(mPrayerGridAdapter);
        mPrayerGrid.setEmptyView(root.findViewById(R.id.empty_view));

        mPrayerSelector.setVisibility(View.INVISIBLE);

        mFabPairPartner = (FloatingActionButton) root.findViewById(R.id.fab_pair_partner);
        mFabPairPartner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelector.setY(mFragmentHeight * 0.6f);
                mPrayerSelector.setVisibility(View.VISIBLE);
                mPrayerSelector.animate().translationY(0).setDuration(500).start();
                mFabPairPartner.hide();
                mPrayerGridAdapter.notifyDataSetChanged();
            }
        });

        mPrayerSelector.findViewById(R.id.cancel_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelector.animate().translationY(mFragmentHeight * 0.6f).setDuration(500).start();
                mFabPairPartner.show();
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
                mFabPairPartner.show();
                applySelection();
            }
        });

        mPrayerGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LOGD(TAG, String.format("item %d is clicked", position));
                PrayerGridAdapter.PrayerState state = mPrayerGridAdapter.getItem(position);
                state.toggleSelection();
                PrayerGridAdapter.ViewHolder holder = (PrayerGridAdapter.ViewHolder) view.getTag();
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
        reloadPairPrayers(getLoaderManager(), null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
//        mPrayersObserver.cancelPendingCallback();
//        mLatestPairPrayersObserver.cancelPendingCallback();
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
            case PrayersQuery.TOKEN_NORMAL: {
                final Prayer[] prayers = Prayer.prayersFromCursor(data);
                if (prayers != null) {
                    mPrayerGridAdapter.clear();
                    mPrayerGridAdapter.addAll(prayersToPrayerStates(prayers));
                }
                break;
            }
            case PairPrayersQuery.TOKEN_SEARCH: {
                LOGD(TAG, DatabaseUtils.dumpCursorToString(data));
                final Partner[] partners = pairPrayersToPartners(PairPrayer.pairPrayersFromCursor(data));
                mPartnerAdapter.setPartners(partners);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private Partner[] pairPrayersToPartners(PairPrayer[] pairPrayers) {
        if (pairPrayers == null || pairPrayers.length == 0) {
            return null;
        }
        Partner[] partners = new Partner[pairPrayers.length >> 1];
        for (int i = 0; i < pairPrayers.length; i += 2) {
            // The query makes sure the results are grouped by partner id, so it easy to create partners.
            partners[i >> 1] = new Partner(pairPrayers[i], pairPrayers[i + 1]);
        }
        return partners;
    }

    private PrayerGridAdapter.PrayerState[] prayersToPrayerStates(Prayer[] prayers) {
        PrayerGridAdapter.PrayerState[] states = new PrayerGridAdapter.PrayerState[prayers.length];
        for (int i = 0; i < states.length; ++i) {
            states[i] = new PrayerGridAdapter.PrayerState(prayers[i], false);
        }
        return states;
    }

    private int getSelectionCount() {
        int count = 0;
        for (int i = 0; i < mPrayerGridAdapter.getCount(); ++i) {
            if (mPrayerGridAdapter.getItem(i).isSelected()) {
                count++;
            }
        }
        return count;
    }

    private void cancelSelection() {
        for (int i = 0; i < mPrayerGridAdapter.getCount(); ++i) {
            mPrayerGridAdapter.getItem(i).select(false);
        }
    }

    private void applySelection() {
        // Get selected prayers
        final List<Prayer> selectedPrayers = new ArrayList<>(mPrayerGridAdapter.getCount());
        for (int i = 0; i < mPrayerGridAdapter.getCount(); ++i) {
            PrayerGridAdapter.PrayerState state = mPrayerGridAdapter.getItem(i);
            if (state.isSelected()) {
                selectedPrayers.add(state.getPrayer());
                state.select(false);
            }
        }

        // Pair prayers randomly (now only need to support two prayers into one partner group)
        int size = selectedPrayers.size();
        Random rand = new Random();
        for (int i = 0; i < size; ++i) {
            int p = rand.nextInt(size);
            Prayer t = selectedPrayers.get(i);
            selectedPrayers.set(i, selectedPrayers.get(p));
            selectedPrayers.set(p, t);
        }

        /**
         *  Build and apply the operations to create a new partner and its associated pairs.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get the last partner id.
                long appInfoId = 0, lastPartnerId = 0;
                Cursor cursor = getActivity().getContentResolver().query(AppContract.AppInfo.CONTENT_URI, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        appInfoId = cursor.getLong(cursor.getColumnIndex(AppContract.AppInfo._ID));
                        lastPartnerId = cursor.getLong(cursor.getColumnIndex(AppContract.AppInfo.LAST_PARTNER_ID));
                    } else {
                        return;
                    }
                } finally {
                    cursor.close();
                }

                long now = System.currentTimeMillis();
                ArrayList<ContentProviderOperation> ops = new ArrayList<>(selectedPrayers.size() / 2 + 1);
                // Make a new pair session.
                ops.add(ContentProviderOperation
                        .newInsert(AppContract.Pairs.CONTENT_URI)
                        .withValue(AppContract.Pairs.NUMBER, selectedPrayers.size())
                        .withValue(AppContract.TimeColumns.UPDATED, now)
                        .withValue(AppContract.TimeColumns.CREATED, now)
                        .build());
                // Every two prayers are paired as a partner group.
                for (int i = 0; i < selectedPrayers.size(); i += 2) {
                    lastPartnerId++;
                    ops.add(ContentProviderOperation
                            .newInsert(AppContract.PairPrayers.CONTENT_URI)
                            .withValueBackReference(AppContract.PairPrayers.PAIR_ID, 0)
                            .withValue(AppContract.PairPrayers.PRAYER_ID, selectedPrayers.get(i).id)
                            .withValue(AppContract.PairPrayers.PARTNER_ID, lastPartnerId)
                            .withValue(AppContract.TimeColumns.UPDATED, now)
                            .withValue(AppContract.TimeColumns.CREATED, now)
                            .build());
                    ops.add(ContentProviderOperation
                            .newInsert(AppContract.PairPrayers.CONTENT_URI)
                            .withValueBackReference(AppContract.PairPrayers.PAIR_ID, 0)
                            .withValue(AppContract.PairPrayers.PRAYER_ID, selectedPrayers.get(i + 1).id)
                            .withValue(AppContract.PairPrayers.PARTNER_ID, lastPartnerId)
                            .withValue(AppContract.TimeColumns.UPDATED, now)
                            .withValue(AppContract.TimeColumns.CREATED, now)
                            .build());
                }
                // Update the last pair id and partner id.
                ops.add(ContentProviderOperation.newUpdate(AppContract.AppInfo.buildAppInfoUri("" + appInfoId))
                        .withValueBackReference(AppContract.AppInfo.LAST_PAIR_ID, 0)
                        .withValue(AppContract.AppInfo.LAST_PARTNER_ID, lastPartnerId)
                        .build());
                try {
                    getActivity().getContentResolver().applyBatch(AppContract.CONTENT_AUTHORITY, ops);
                } catch (RemoteException | OperationApplicationException e) {
                    LOGE(TAG, "Fail to pair partners: " + e);
                }
            }
        }).start();
    }

    public static class PartnerAdapter extends RecyclerView.Adapter<PartnerAdapter.ViewHolder> {
        private Context mContext;
        private List<Partner> mPartners = new ArrayList<>();

        public PartnerAdapter(Context context) {
            mContext = context;
        }

        public void setPartners(Partner[] partners) {
            mPartners.clear();
            if (partners != null) {
                Collections.addAll(mPartners, partners);
            }
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(mContext).inflate(R.layout.item_partner, parent, false);
            return new ViewHolder(item);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindData(mPartners.get(position));
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return mPartners.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView  mName1;
            TextView  mName2;
            ImageView mImage1;
            ImageView mImage2;

            ViewHolder(final View view) {
                super(view);
                mName1  = (TextView) view.findViewById(R.id.partner1_name);
                mName2  = (TextView) view.findViewById(R.id.partner2_name);
                mImage1 = (ImageView) view.findViewById(R.id.partner1_photo);
                mImage2 = (ImageView) view.findViewById(R.id.partner2_photo);
            }

            void bindData(final Partner partner) {
                PairPrayer prayer1 = partner.first;
                PairPrayer prayer2 = partner.second;
                if (prayer1 != null && prayer2 != null) {
                    mName1.setText(prayer1.name);
                    mName2.setText(prayer2.name);
                    loadImage(prayer1.photo, mImage1);
                    loadImage(prayer2.photo, mImage2);
                }
            }

            private void loadImage(final String imagePath, final ImageView imageView) {
                if (!TextUtils.isEmpty(imagePath)) {
                    File imageFile = FileUtils.getFile(imagePath);
                    if (imageFile.exists() && imageFile.length() > 0) {
                        Glide.with(mContext)
                                .load(imageFile)
                                .centerCrop()
                                .crossFade()
                                .into(imageView);
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_default_prayer);
                }
            }
        }
    }

    public static class Partner extends Pair<PairPrayer, PairPrayer> {

        public Partner(PairPrayer first, PairPrayer second) {
            super(first, second);
        }
    }
}
