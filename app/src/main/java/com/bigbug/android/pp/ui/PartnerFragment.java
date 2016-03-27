package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.content.ContentProviderOperation;
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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.ui.widget.PrayerGridAdapter;
import com.bigbug.android.pp.util.ThrottledContentObserver;

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

    private RecyclerView mPartnerList;
    private FloatingActionButton mFabPairPartner;

    private ThrottledContentObserver mPrayersObserver;
    private ThrottledContentObserver mLatestPairedPrayersObserver;

    private LinearLayout mPrayerSelector;
    private GridView mPrayersGrid;
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

        mLatestPairedPrayersObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (pair_prayers). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting partners cursor reload as a result of ContentObserver firing.");
                    reloadPairedPrayers(getLoaderManager(), null, PartnerFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.PairPrayers.CONTENT_URI, true, mLatestPairedPrayersObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPrayersObserver);
        getActivity().getContentResolver().unregisterContentObserver(mLatestPairedPrayersObserver);
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

        mPrayerSelector = (LinearLayout) root.findViewById(R.id.prayer_selector);
        mPrayersGrid = (GridView) mPrayerSelector.findViewById(R.id.grid_prayers);

        mPrayerGridAdapter = new PrayerGridAdapter(getActivity());
        mPrayersGrid.setAdapter(mPrayerGridAdapter);
        mPrayersGrid.setEmptyView(root.findViewById(R.id.empty_view));

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

        mPrayersGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        reloadPairedPrayers(getLoaderManager(), null, this);
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
            case PrayersQuery.TOKEN_NORMAL: {
                final Prayer[] prayers = Prayer.prayersFromCursor(data);
                if (prayers != null) {
                    mPrayerGridAdapter.clear();
                    mPrayerGridAdapter.addAll(prayersToPrayerStates(prayers));
                }
                break;
            }
            case PrayersQuery.TOKEN_SEARCH: {
                if (data != null && data.moveToFirst()) {
                    LOGD(TAG, "PrayersQuery.TOKEN_SEARCH");
                    do {
                        DatabaseUtils.dumpCurrentRow(data);
                    } while (data.moveToNext());
                }
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
        shuffle(selectedPrayers);

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
}
