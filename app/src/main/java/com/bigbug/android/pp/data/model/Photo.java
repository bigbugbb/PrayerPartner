package com.bigbug.android.pp.data.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.bigbug.android.pp.ui.widget.CollectionView;

import org.apache.commons.io.FileUtils;


public final class Photo implements Parcelable {

    public long   time;
    public String title;
    public String data;
    public String mime;
    public int    size;
    public int    width;
    public int    height;
    public float  latitude;
    public float  longitude;

    public Photo() {
    }

    public Photo(Cursor cursor) {
        time = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.TITLE));
        data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
        mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE));
        size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE));
        width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
        height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
        latitude = cursor.getFloat(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE));
        longitude = cursor.getFloat(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE));
    }

    public static Photo validPhotoFromCursor(Cursor cursor) {
        Photo photo = new Photo(cursor);
        if (FileUtils.getFile(photo.data).length() > 0) {
            return photo;
        } else {
            return null;
        }
    }

    // The cursor window should be larger than the whole block of data.
    public static Photo[] photosFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final int size = cursor.getCount();
            Photo[] images = new Photo[size];
            int i = 0;
            do {
                images[i++] = validPhotoFromCursor(cursor);
            } while (cursor.moveToNext());
            cursor.moveToFirst();
            return images;
        } else {
            return null;
        }
    }

    public static CollectionView.Inventory photoInventoryFromCursor(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final CollectionView.Inventory inventory = new CollectionView.Inventory();
            CollectionView.InventoryGroup group = new CollectionView.InventoryGroup(0);
            group.setDisplayCols(3);
            group.addItemWithTag("Take Photo");
            do {
                Object tag = Photo.validPhotoFromCursor(cursor);
                if (tag != null) {
                    group.addItemWithTag(tag);
                }
            } while (cursor.moveToNext());
            inventory.addGroup(group);
            return inventory;
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(time);
        dest.writeString(title);
        dest.writeString(data);
        dest.writeString(mime);
        dest.writeInt(size);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
    }

    private Photo(Parcel in) {
        time = in.readLong();
        title = in.readString();
        data = in.readString();
        mime = in.readString();
        size = in.readInt();
        width = in.readInt();
        height = in.readInt();
        latitude = in.readFloat();
        longitude = in.readFloat();
    }

    public static final Parcelable.Creator<Photo> CREATOR = new Parcelable.Creator<Photo>() {

        public Photo createFromParcel(Parcel source) {
            return new Photo(source);
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };
}