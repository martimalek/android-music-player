package com.musicplayah.Utils;

import android.Manifest;

public class Constants {
    public static final int PERMS_REQUEST_CODE = 1337;
    public static String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    public static String TAG = "MUSIC_PLAYAH_TAG";

    public static String AUDIO_ENDED_EVENT = "ON_AUDIO_ENDED";
    public static String AUDIO_PAUSED_EVENT = "ON_AUDIO_PAUSED";
    public static String AUDIO_RESUMED_EVENT = "ON_AUDIO_RESUMED";
    public static String CHILDREN_UPDATED_EVENT = "ON_CHILDREN_UPDATED";
    public static String POSITION_CHANGED_EVENT = "ON_POSITION_CHANGED";

    public static String CUSTOM_ACTION_ADD_TO_SELECTED_QUEUE = "CUSTOM_ACTION_ADD_TO_SELECTED_QUEUE";

    public static String PERMISSION_OBSERVER_KEY = "PERMISSION_OBSERVER_KEY";

}

