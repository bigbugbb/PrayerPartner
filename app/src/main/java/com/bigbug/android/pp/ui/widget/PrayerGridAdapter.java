package com.bigbug.android.pp.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class PrayerGridAdapter extends ArrayAdapter<PrayerGridAdapter.PrayerState> {

    private static final float IMAGE_SCALE_FACTOR = 0.75f;
    private static final int IMAGE_SCALE_ANIM_DURATION = 250;
    private static final int SELECT_ALPHA_ANIM_DURATION = 250;

    public PrayerGridAdapter(Context context) {
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

    public class ViewHolder {
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

    public static class PrayerState {
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