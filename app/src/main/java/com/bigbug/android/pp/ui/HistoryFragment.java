package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.PairPrayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Created by bigbug on 2/28/16.
 */
public class HistoryFragment extends AppFragment {
    private static final String TAG = makeLogTag(HistoryFragment.class);

    private RecyclerView mPairHistoryList;
    private PairHistoryAdapter mPairHistoryAdapter;

    private ThrottledContentObserver mPairPrayersObserver;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mPairPrayersObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                LOGD(TAG, "ThrottledContentObserver fired (pair_prayers). Content changed.");
                if (isAdded()) {
                    LOGD(TAG, "Requesting pair prayers cursor reload as a result of ContentObserver firing.");
                    reloadPairPrayers(getLoaderManager(), HistoryFragment.this);
                }
            }
        });
        activity.getContentResolver().registerContentObserver(AppContract.PairPrayers.CONTENT_URI, true, mPairPrayersObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mPairPrayersObserver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_history, container, false);
        mPairHistoryList = (RecyclerView) root.findViewById(R.id.pair_history_list);

        mPairHistoryAdapter = new PairHistoryAdapter(getActivity());
        mPairHistoryList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPairHistoryList.setItemAnimator(new DefaultItemAnimator());
        mPairHistoryList.setAdapter(mPairHistoryAdapter);

        return root;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGD(TAG, "Reloading data as a result of onResume()");
        reloadPairPrayers(getLoaderManager(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
//        mPairPrayersObserver.cancelPendingCallback();
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
            case PairPrayersQuery.TOKEN_NORMAL: {
                LOGD(TAG, DatabaseUtils.dumpCursorToString(data));
                mPairHistoryAdapter.setPairHistory(buildPairHistory(PairPrayer.pairPrayersFromCursor(data)));
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private Map<Long, PairState> buildPairHistory(PairPrayer[] pairPrayers) {
        // Use LinkedHashMap to keep the insert order.
        final Map<Long, PairState> history = new LinkedHashMap<>();

        if (pairPrayers == null || pairPrayers.length == 0) {
            return history;
        }

        for (int i = 0; i < pairPrayers.length; ++i) {
            // The query results are grouped by (pair_id, partner_id, prayer_id).
            PairPrayer pp = pairPrayers[i];
            if (pp == null) {
                continue;
            }

            // pair_id => (partner_id => array of pair prayers)
            PairState state = history.get(pp.pair_id);
            if (state == null) {
                state = new PairState();
                state.createdTime = pp.created;
                history.put(pp.pair_id, state);
            }

            // partner_id => array of pair prayers
            LongSparseArray<PairPrayer> prayers = state.partners.get(pp.partner_id);
            if (prayers == null) {
                prayers = new LongSparseArray<>(2);
                state.partners.put(pp.partner_id, prayers);
            }

            // Save the PairPrayer
            if (prayers.get(pp.prayer_id) == null) {
                prayers.put(pp.prayer_id, pp);
            }
        }

        return history;
    }

    private static class PairState {
        long createdTime;
        Map<Long, LongSparseArray<PairPrayer>> partners;

        public PairState() {
            createdTime = 0;
            partners = new LinkedHashMap<>();
        }
    }

    private static class PairHistoryAdapter extends RecyclerView.Adapter<PairHistoryAdapter.ViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM   = 1;

        private Context mContext;
        private int mHeaders, mItems;
        private List<Object> mHistory;

        public PairHistoryAdapter(Context context) {
            mContext = context;
            mHistory = new ArrayList<>();
        }

        public void setPairHistory(Map<Long, PairState> history) {
            if (history != null && history.size() > 0) {
                mHeaders = history.size();
                mItems = 0;
                mHistory.clear();

                for (Map.Entry<Long, PairState> pair : history.entrySet()) {
                    PairState state = pair.getValue();
                    mHistory.add(state.createdTime);
                    for (Map.Entry<Long, LongSparseArray<PairPrayer>> partner : state.partners.entrySet()) {
                        mHistory.add(partner.getValue());
                        ++mItems;
                    }
                }

                notifyDataSetChanged();
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = null;
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    item = LayoutInflater.from(mContext).inflate(R.layout.item_pair_history_header, parent, false);
                    break;
                case VIEW_TYPE_ITEM:
                    item = LayoutInflater.from(mContext).inflate(R.layout.item_pair_history_prayers, parent, false);
                    break;
            }
            return new ViewHolder(item, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    holder.bindHeaderData((Long) mHistory.get(position));
                    break;
                case VIEW_TYPE_ITEM:
                    holder.bindItemData((LongSparseArray<PairPrayer>) mHistory.get(position));
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Object item = mHistory.get(position);
            if (item instanceof Long) {
                return VIEW_TYPE_HEADER;
            } else if (item instanceof LongSparseArray) {
                return VIEW_TYPE_ITEM;
            } else {
                throw new RuntimeException("Unknown view type");
            }
        }

        @Override
        public int getItemCount() {
            return mHeaders + mItems;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView  mTime;
            TextView  mName1;
            TextView  mName2;
            ImageView mImage1;
            ImageView mImage2;

            ViewHolder(final View view, final int viewType) {
                super(view);
                switch (viewType) {
                    case VIEW_TYPE_HEADER:
                        mTime = (TextView) view.findViewById(R.id.pair_created_time);
                        break;
                    case VIEW_TYPE_ITEM:
                        mName1  = (TextView) view.findViewById(R.id.prayer1_name);
                        mName2  = (TextView) view.findViewById(R.id.prayer2_name);
                        mImage1 = (ImageView) view.findViewById(R.id.prayer1_photo);
                        mImage2 = (ImageView) view.findViewById(R.id.prayer2_photo);
                        break;
                }
            }

            void bindHeaderData(final long createdTime) {
                String time = DateFormatUtils.format(createdTime, "yyyy-MM-dd HH:mm:ss");
                mTime.setText(time);
            }

            void bindItemData(final LongSparseArray<PairPrayer> prayers) {
                PairPrayer p1 = prayers.valueAt(0);
                PairPrayer p2 = prayers.valueAt(1);
                if (p1 != null && p2 != null) {
                    mName1.setText(p1.name);
                    mName2.setText(p2.name);
                    loadImage(p1.photo, mImage1);
                    loadImage(p2.photo, mImage2);
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
}
