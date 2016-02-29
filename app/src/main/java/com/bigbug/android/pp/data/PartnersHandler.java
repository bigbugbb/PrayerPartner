package com.bigbug.android.pp.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.bigbug.android.pp.data.model.Partner;
import com.bigbug.android.pp.provider.AppContract;
import com.bigbug.android.pp.provider.AppContract.Partners;

import java.util.ArrayList;
import java.util.HashMap;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;


public class PartnersHandler extends JSONHandler {
    private static final String TAG = makeLogTag(PartnersHandler.class);

    private HashMap<String, Partner> mPartners = new HashMap<>();

    public PartnersHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Partner partner : new Gson().fromJson(element, Partner[].class)) {
//            mPartners.put(partner.id, partner);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = AppContract.addCallerIsSyncAdapterParameter(Partners.CONTENT_URI);

        // since the number of tags is very small, for simplicity we delete them all and reinsert
        list.add(ContentProviderOperation.newDelete(uri).build());
//        for (Motion motion : mMotions.values()) {
//            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
//            builder.withValue(Motions.MOTION_ID, motion.id);
//            builder.withValue(Motions.MOTION_DATE, motion.date);
//            builder.withValue(Motions.MOTION_NOTE, motion.note);
//            list.add(builder.build());
//        }
    }

    public HashMap<String, Partner> getMotionMap() {
        return mPartners;
    }
}
