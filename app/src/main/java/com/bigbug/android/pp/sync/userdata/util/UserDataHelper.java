/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bigbug.android.pp.sync.userdata.util;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.bigbug.android.pp.sync.userdata.UserAction;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;


public class UserDataHelper {

    private static final String TAG = makeLogTag(UserDataHelper.class);

    // Constants related to the JSON serialization:
    static final String JSON_STARRED_SESSIONS_KEY = "starred_sessions";

    static public String toSessionsString(Set<String> sessionIds) {
        JsonArray array = new JsonArray();
        for (String sessionId: sessionIds) {
            array.add(new JsonPrimitive(sessionId));
        }
        JsonObject obj = new JsonObject();
        obj.add(JSON_STARRED_SESSIONS_KEY, array);
        return obj.toString();
    }

    static public byte[] toByteArray(Set<String> sessionIds) {
        return null;//toSessionsString(sessionIds).getBytes(Charsets.UTF_8);
    }


    static public Set<String> fromString(String str) {
        TreeSet<String> result = new TreeSet<String>();
        if (str == null || str.isEmpty()) {
            return result;
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(str));
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (JSON_STARRED_SESSIONS_KEY.equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        result.add(reader.nextString());
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
        } catch (Exception ex) {
            Log.w(TAG, "Ignoring invalid remote content.", ex);
            return null;
        }
        return result;
    }

    static public SortedSet<String> getSessionIDs(List<UserAction> actions) {
        TreeSet<String> starredSessions = new TreeSet<String>();
        if (actions != null) {
            for (UserAction action : actions) {
                if (action.type == UserAction.TYPE.ADD_STAR) {
                    starredSessions.add(action.sessionId);
                }
            }
        }
        return starredSessions;
    }

//    static public Set<String> getLocalStarredSessionIDs(Context context) {
//        Cursor sessionsCursor = context.getContentResolver().query(
//                ShowContract.MySchedule.CONTENT_URI,
//                new String[]{ShowContract.MySchedule.BLOG_ID}, null, null, null);
//        Set<String> starredSessions = new HashSet<String>();
//        while (sessionsCursor.moveToNext()) {
//            starredSessions.add(sessionsCursor.getString(0));
//        }
//        sessionsCursor.close();
//        return starredSessions;
//    }

    static public void setLocalStarredSessions(Context context, Set<String> sessionIds, String accountName) {
        // first clear all stars
//        context.getContentResolver().delete(ShowContract.MySchedule.CONTENT_URI,
//                ShowContract.MySchedule.MY_SCHEDULE_ACCOUNT_NAME +" = ?", new String[]{accountName});

        // now add only the ones in sessionIds
        ArrayList<UserAction> actions = new ArrayList<UserAction>();
        for (String sessionId: sessionIds) {
            UserAction action = new UserAction();
            action.type = UserAction.TYPE.ADD_STAR;
            action.sessionId = sessionId;
            actions.add(action);
        }
        UserActionHelper.updateContentProvider(context, actions, accountName);
    }


    /**
     * Reads an input stream line by line and converts it into String.
     * @param inputStream
     * @throws IOException
     */
//    public static String fromStreamToString(InputStream inputStream)
//            throws IOException {
//        BufferedReader bufferedReader = null;
//        StringBuilder stringBuilder = new StringBuilder();
//        try {
//            String line;
//            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
//            while ((line = bufferedReader.readLine()) != null) {
//                stringBuilder.append(line);
//            }
//        } finally {
//            if (bufferedReader != null) {
//                bufferedReader.close();
//            }
//        }
//        return stringBuilder.toString();
//    }


}
