package com.musicplayah;

import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AudioManager extends ReactContextBaseJavaModule  {
    private static ReactApplicationContext reactContext;
    private static String TAG = "MUSIC_PLAYAH_TAG";

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";

    private MediaPlayer mp;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    AudioManager(ReactApplicationContext context) {
        super(context);
        reactContext = context;

        Log.d(TAG, "Inside constructor");

        String path = "/storage/emulated/0/Music/";

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
        };

        Cursor audioCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.DISPLAY_NAME + " ASC");

        Log.d(TAG, "Cursor created");

        try {
            if (audioCursor != null && audioCursor.moveToFirst()) {
                Log.d(TAG, "Cursor valid " + audioCursor.toString());

                int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int dataColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int displayNameColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                List<Audio> audios = new ArrayList<>();
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

//                Log.d(TAG, audios.toString());
                Log.d(TAG, "Size: " + String.valueOf(audios.size()));

                playAudio(audios.get(0).data);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception occurred " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "AudioManager";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, 1);
        constants.put(DURATION_LONG_KEY, 2);
        return constants;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void playAudio(String path){
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
    public void show(String message, int duration) {
        Log.d(TAG, "Whatever");
    }
}
