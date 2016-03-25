package com.bigbug.android.pp.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.ui.widget.CollectionView;
import com.bigbug.android.pp.util.ThrottledContentObserver;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
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

    private ValueAnimator mPrayerSelectorPopupAnimator;
    private ValueAnimator mPrayerSelectorSlipdownAnimator;

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

        mFabMakePartner = (FloatingActionButton) root.findViewById(R.id.fab_make_partner);
        mFabMakePartner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPrayerSelectorPopupAnimator != null) {
                    mPrayerSelectorPopupAnimator.start();
                    mFabMakePartner.hide();
                }
            }
        });

        mPrayerSelector.findViewById(R.id.cancel_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelectorSlipdownAnimator.start();
            }
        });
        mPrayerSelector.findViewById(R.id.apply_selection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrayerSelectorSlipdownAnimator.start();
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
                mPrayerSelector.setY(mFragmentHeight);
                mPrayerSelectorPopupAnimator = createPrayerSelectorPopupAnimator();
                mPrayerSelectorSlipdownAnimator = createPrayerSelectorSlipdownAnimator();
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
                mPrayerAdapter.clear();
                mPrayerAdapter.addAll(Prayer.prayersFromCursor(data));
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

    private ValueAnimator createPrayerSelectorPopupAnimator() {
        final ValueAnimator animator = ObjectAnimator.ofFloat(mPrayerSelector, "y", mFragmentHeight * 0.4f)
                .setDuration(500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new SimpleAnimatorListener());
        return animator;
    }

    private ValueAnimator createPrayerSelectorSlipdownAnimator() {
        final ValueAnimator animator = ObjectAnimator.ofFloat(mPrayerSelector, "y", mFragmentHeight)
                .setDuration(500);
        animator.setStartDelay(0);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFabMakePartner.show();
            }
        });
        return animator;
    }

    private static class PrayerAdapter extends ArrayAdapter<Prayer> {

        public PrayerAdapter(Context context) {
            super(context, 0, new ArrayList<Prayer>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_grid_prayer, null);
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.text  = (TextView) convertView.findViewById(R.id.prayer_name);
                holder.image = (ImageView) convertView.findViewById(R.id.prayer_photo);
                convertView.setTag(holder);
            }
            holder.updateViews(getItem(position));

            return convertView;
        }

        class ViewHolder {
            TextView  text;
            ImageView image;

            public void updateViews(final Prayer prayer) {
                text.setText(prayer.name);
                Glide.with(getContext())
                        .load(prayer.photo)
                        .centerCrop()
                        .crossFade()
                        .into(image);
            }
        }
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    private class SimpleAnimatorListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
