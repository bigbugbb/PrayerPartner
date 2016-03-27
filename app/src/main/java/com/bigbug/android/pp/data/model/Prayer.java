package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.bigbug.android.pp.provider.AppContract;

public class Prayer implements Parcelable {

    public long   id;
    public String name;
    public String email;
    public String photo;
    public long   created;
    public long   updated;

    public Prayer() {
    }

    public Prayer(Cursor cursor) {
        id      = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        name    = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.NAME));
        email   = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.EMAIL));
        photo   = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.PHOTO));
        created = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.CREATED));
        updated = cursor.getLong(cursor.getColumnIndex(AppContract.TimeColumns.UPDATED));
    }

    // The cursor window should be larger than the whole block of data.
    public static Prayer[] prayersFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Prayer[] prayers = new Prayer[size];
            int i = 0;
            do {
                prayers[i++] = new Prayer(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return prayers;
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
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(photo);
        dest.writeLong(created);
        dest.writeLong(updated);
    }

    private Prayer(Parcel in) {
        id      = in.readLong();
        name    = in.readString();
        email   = in.readString();
        photo   = in.readString();
        created = in.readLong();
        updated = in.readLong();
    }

    public static final Creator<Prayer> CREATOR = new Creator<Prayer>() {

        public Prayer createFromParcel(Parcel source) {
            return new Prayer(source);
        }

        public Prayer[] newArray(int size) {
            return new Prayer[size];
        }
    };
}
