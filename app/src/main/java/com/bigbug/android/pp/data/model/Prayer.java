package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.bigbug.android.pp.provider.AppContract;

public class Prayer implements Parcelable {

    public String name;
    public String email;
    public String photo;

    public Prayer() {
    }

    public Prayer(Cursor cursor) {
        name = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.NAME));
        email = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.EMAIL));
        photo = cursor.getString(cursor.getColumnIndex(AppContract.Prayers.PHOTO));
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
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(photo);
    }

    private Prayer(Parcel in) {
        name = in.readString();
        email = in.readString();
        photo = in.readString();
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
