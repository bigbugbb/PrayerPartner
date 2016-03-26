package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.bigbug.android.pp.provider.AppContract;

public final class PartnerPrayer implements Parcelable {

    public long id;
    public long prayer_id;
    public long partner_id;
    public long created;
    public long updated;

    public PartnerPrayer() {
    }

    public PartnerPrayer(Cursor cursor) {
        id         = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        prayer_id  = cursor.getLong(cursor.getColumnIndex(AppContract.PartnerPrayers.PRAYER_ID));
        partner_id = cursor.getLong(cursor.getColumnIndex(AppContract.PartnerPrayers.PARTNER_ID));
        created    = cursor.getLong(cursor.getColumnIndex(AppContract.Partners.CREATED));
        updated    = cursor.getLong(cursor.getColumnIndex(AppContract.Partners.UPDATED));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(prayer_id);
        dest.writeLong(partner_id);
        dest.writeLong(created);
        dest.writeLong(updated);
    }

    private PartnerPrayer(Parcel in) {
        id         = in.readLong();
        prayer_id  = in.readLong();
        partner_id = in.readLong();
        created    = in.readLong();
        updated    = in.readLong();
    }

    public static final Creator<PartnerPrayer> CREATOR = new Creator<PartnerPrayer>() {

        public PartnerPrayer createFromParcel(Parcel source) {
            return new PartnerPrayer(source);
        }

        public PartnerPrayer[] newArray(int size) {
            return new PartnerPrayer[size];
        }
    };
}
