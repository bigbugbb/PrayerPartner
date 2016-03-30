package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.bigbug.android.pp.provider.AppContract;

public final class Pair implements Parcelable {

    public long id;
    public int  number;
    public int  notified;
    public long created;
    public long updated;

    public Pair() {
    }

    public Pair(Cursor cursor) {
        id       = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        number   = cursor.getInt(cursor.getColumnIndex(AppContract.Pairs.NUMBER));
        notified = cursor.getInt(cursor.getColumnIndex(AppContract.Pairs.NOTIFIED));
        created  = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.CREATED));
        updated  = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.UPDATED));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(number);
        dest.writeInt(notified);
        dest.writeLong(created);
        dest.writeLong(updated);
    }

    private Pair(Parcel in) {
        id       = in.readLong();
        number   = in.readInt();
        notified = in.readInt();
        created  = in.readLong();
        updated  = in.readLong();
    }

    public static final Creator<Pair> CREATOR = new Creator<Pair>() {

        public Pair createFromParcel(Parcel source) {
            return new Pair(source);
        }

        public Pair[] newArray(int size) {
            return new Pair[size];
        }
    };
}
