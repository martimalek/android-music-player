package com.musicplayah;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

import java.util.ArrayList;
import java.util.List;

import static com.musicplayah.Constants.PERMS_REQUEST_CODE;
import static com.musicplayah.Constants.permissions;

public class AudioManager extends ReactContextBaseJavaModule implements PermissionListener {
    private static ReactApplicationContext reactContext;
    private static String TAG = Constants.TAG;

    private MediaPlayer mp;

    private Promise permsPromise;

    AudioManager(ReactApplicationContext context) {
        super(context);
        reactContext = context;

        Log.d(TAG, "Inside constructor");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private List<Audio> getAudioList() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
        };

        Cursor audioCursor = reactContext.getContentResolver().query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC");

        Log.d(TAG, "Cursor created");

        List<Audio> audios = new ArrayList<>();

        try {
            if (audioCursor != null && audioCursor.moveToFirst()) {
                Log.d(TAG, "Cursor valid " + audioCursor.toString());

                int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int dataColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int displayNameColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                do {
                    Audio audio = new Audio(
                        audioCursor.getInt(idColumn),
                        audioCursor.getString(artistColumn),
                        audioCursor.getString(titleColumn),
                        audioCursor.getString(dataColumn),
                        audioCursor.getString(displayNameColumn),
                        audioCursor.getInt(durationColumn)
                    );
                    audios.add(audio);
                    Log.d(TAG, audio.title);
                } while (audioCursor.moveToNext());

                Log.d(TAG, "Size: " + String.valueOf(audios.size()));

//                playAudio(audios.get(0).data);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception occurred " + e.getMessage());
        }
        return audios;
    }

    @NonNull
    @Override
    public String getName() {
        return "AudioManager";
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @ReactMethod
    public void playAudio(String path){
        if (mp != null) mp.stop();

        mp = new MediaPlayer();

        try {
            Uri audioUri = Uri.parse("file:///" + path);;
            mp.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
            );
            mp.setDataSource(getReactApplicationContext(), audioUri);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            Log.d(TAG, "Exception while trying to play " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void grantPermissions() {
        if (PackageManager.PERMISSION_GRANTED != reactContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                PackageManager.PERMISSION_GRANTED != reactContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Activity currentActivity = reactContext.getCurrentActivity();
            if (currentActivity != null) {
                PermissionAwareActivity activity = (PermissionAwareActivity) currentActivity;
                activity.requestPermissions(permissions, PERMS_REQUEST_CODE, this);
            }
        } else Log.d(TAG, "Permissions already granted!");
    }

    @ReactMethod
    public void play() {
        if (mp != null) mp.start();
    }

    @ReactMethod
    public void pause() {
        if (mp != null) mp.pause();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @ReactMethod
    public void getAudios(Promise promise) {
        WritableArray audioArray = new WritableNativeArray();

        List<Audio> audios = getAudioList();

        try {
            for (Audio audio: audios) {
                audioArray.pushMap(audio.toMap());
            }

            promise.resolve(audioArray);
        } catch (Exception e) {
            promise.reject("E_AUDIO", "Could not get audios");
            Log.d(TAG, "Exception while parsing " + e.getMessage());
        }
    }

    @ReactMethod
    public void init(Promise promise) {
        permsPromise = promise;
        grantPermissions();
    };

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMS_REQUEST_CODE) {
            permsPromise.resolve(true);
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        permsPromise.reject("E_PERMS", "Permission not accepted");
        return false;
    }
}
