package com.bigbug.android.pp.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;


public class PrayersHandler extends JSONHandler {
    private static final String TAG = makeLogTag(PrayersHandler.class);

    private List<Prayer> mPrayers = new ArrayList<>();

    public PrayersHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Prayer prayer : new Gson().fromJson(element, Prayer[].class)) {
            mPrayers.add(prayer);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = AppContract.addCallerIsSyncAdapterParameter(AppContract.Prayers.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Prayer prayer : mPrayers) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            // TODO:
            list.add(builder.build());
        }
    }
}
