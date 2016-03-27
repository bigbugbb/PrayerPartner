package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.bigbug.android.pp.provider.AppContract;

public final class PairPrayer implements Parcelable {

    public long id;
    public long pair_id;
    public long prayer_id;
    public long partner_id;
    public long created;
    public long updated;

    public PairPrayer() {
    }

    public PairPrayer(Cursor cursor) {
        id         = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        pair_id    = cursor.getLong(cursor.getColumnIndex(AppContract.PairPrayers.PAIR_ID));
        prayer_id  = cursor.getLong(cursor.getColumnIndex(AppContract.PairPrayers.PRAYER_ID));
        partner_id = cursor.getLong(cursor.getColumnIndex(AppContract.PairPrayers.PARTNER_ID));
        created    = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.CREATED));
        updated    = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.UPDATED));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(pair_id);
        dest.writeLong(prayer_id);
        dest.writeLong(partner_id);
        dest.writeLong(created);
        dest.writeLong(updated);
    }

    private PairPrayer(Parcel in) {
        id         = in.readLong();
        pair_id    = in.readLong();
        prayer_id  = in.readLong();
        partner_id = in.readLong();
        created    = in.readLong();
        updated    = in.readLong();
    }

    public static final Creator<PairPrayer> CREATOR = new Creator<PairPrayer>() {

        public PairPrayer createFromParcel(Parcel source) {
            return new PairPrayer(source);
        }

        public PairPrayer[] newArray(int size) {
            return new PairPrayer[size];
        }
    };
}
