package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.bigbug.android.pp.provider.AppContract;

public final class Partner implements Parcelable {

    public String first;
    public String second;
    public String round_id;

    public Partner() {
    }

    public Partner(Cursor cursor) {
        first = cursor.getString(cursor.getColumnIndex(AppContract.Partners.FIRST));
        second = cursor.getString(cursor.getColumnIndex(AppContract.Partners.SECOND));
        round_id = cursor.getString(cursor.getColumnIndex(AppContract.Partners.ROUND_ID));
    }

    // The cursor window should be larger than the whole block of data.
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
        dest.writeString(first);
        dest.writeString(second);
        dest.writeString(round_id);
    }

    private Partner(Parcel in) {
        first = in.readString();
        second = in.readString();
        round_id = in.readString();
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
