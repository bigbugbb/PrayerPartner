package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.bigbug.android.pp.provider.AppContract;

/**
 * Created by bbo on 1/15/16.
 */
public final class Round implements Parcelable {

    public long id;
    public long time;

    public Round() {
    }

    public Round(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(AppContract.Rounds._ID));
        time = cursor.getLong(cursor.getColumnIndex(AppContract.Rounds.TIME));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(time);
    }

    private Round(Parcel in) {
        id = in.readLong();
        time = in.readLong();
    }

    public static final Creator<Round> CREATOR = new Creator<Round>() {

        public Round createFromParcel(Parcel source) {
            return new Round(source);
        }

        public Round[] newArray(int size) {
            return new Round[size];
        }
    };
}
