package com.musicplayah;

import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AudioManager extends ReactContextBaseJavaModule  {
    private static ReactApplicationContext reactContext;
    private static String TAG = Utils.TAG;

    private MediaPlayer mp;

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

    @ReactMethod
    public void play() {
        if (mp != null) mp.start();
    }

    @ReactMethod
    public void stop() {
        if (mp != null) mp.stop();
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
}
