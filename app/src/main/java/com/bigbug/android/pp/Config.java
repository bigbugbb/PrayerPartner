package com.bigbug.android.pp;

import android.text.format.DateUtils;

/**
 * Created by bigbug on 10/3/15.
 */
public class Config {

    // Manifest URL override for Debug (staging) builds:
    public static final String URL_BASE = "https://pp-api-bigbugbb.c9users.io";
    public static final String MANIFEST_URL = URL_BASE + "/api";
    public static final String USERS_URL = URL_BASE + "/api/users";
    public static final String SESSIONS_URL = URL_BASE + "/api/sessions";

    public static final String MANIFEST_FORMAT = "pp-api-json-v1";

    // GCM config
    public static final String GCM_SERVER_URL = MANIFEST_URL + "/push_regs";
    public static final String GCM_SENDER_ID = "141069897769";
    public static final String GCM_API_KEY = "";

    // Minimum interval between two consecutive syncs. This is a safety mechanism to throttle
    // syncs in case conference data gets updated too often or something else goes wrong that
    // causes repeated syncs.
    public static final long MIN_INTERVAL_BETWEEN_SYNCS = 10 * DateUtils.MINUTE_IN_MILLIS;

    public static final int MONITORING_DURATION_IN_SECONDS = 10;

    // Values for the EventPoint feedback API. Sync happens at the same time as schedule sync,
    // and before that values are stored locally in the database.

    public static final String FEEDBACK_API_CODE = "";
    public static final String FEEDBACK_URL = "";
    public static final String FEEDBACK_API_KEY = "";
    public static final String FEEDBACK_DUMMY_REGISTRANT_ID = "";
    public static final String FEEDBACK_SURVEY_ID = "";

    // Data sync configurations
    public static final boolean WIFI_ONLY_SYNC_ENABLED = true;
}