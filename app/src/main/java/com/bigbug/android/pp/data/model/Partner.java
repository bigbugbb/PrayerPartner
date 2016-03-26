package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.bigbug.android.pp.provider.AppContract;

public final class Partner implements Parcelable {

    public long id;
    public long created;
    public long updated;

    public Partner() {
    }

    public Partner(Cursor cursor) {
        id      = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        created = cursor.getLong(cursor.getColumnIndex(AppContract.Partners.CREATED));
        updated = cursor.getLong(cursor.getColumnIndex(AppContract.Partners.UPDATED));
    }

    public static Partner[] partnersFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Partner[] partners = new Partner[size];
            int i = 0;
            do {
                partners[i++] = new Partner(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return partners;
        } else {
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(created);
        dest.writeLong(updated);
    }

    private Partner(Parcel in) {
        id      = in.readLong();
        created = in.readLong();
        updated = in.readLong();
    }

    public static final Creator<Partner> CREATOR = new Creator<Partner>() {

        public Partner createFromParcel(Parcel source) {
            return new Partner(source);
        }

        public Partner[] newArray(int size) {
            return new Partner[size];
        }
    };
}
