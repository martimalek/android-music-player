package com.musicplayah;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.musicplayah.Constants.PERMS_REQUEST_CODE;
import static com.musicplayah.Constants.permissions;

public class AudioManagerModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;
    private static String TAG = Constants.TAG;

    private MediaPlayer mp;

    private PermissionManager permissionManager;

    private BroadcastReceiver becomingNoisyReceiver;

    private boolean isNoisyReceiverRegistered = false;

    private MediaBrowserCompat mediaBrowser;

    AudioManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        Log.d(TAG, "Inside constructor");

        permissionManager = new PermissionManager();

        becomingNoisyReceiver = new BecomingNoisyReceiver(new Runnable() {
            @Override
            public void run() {
                pause();
            }
        });

//        remoteControlReceiver = new RemoteControlReceiver();
//
//        Log.d(TAG, "Getting AudioManager service");
//        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        Log.d(TAG, "Success!");
//
//        AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
//            @Override
//            public void onAudioFocusChange(int focusChange) {
//
//            }
//        };
//
//        int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
//        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            audioManager.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
//            // Start playback.
//        }
//        Log.d(TAG, "Requested successfully");

    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            connectToSession(mediaBrowser.getSessionToken());
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");

            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");

            // The Service has refused our connection
        }
    };

    private void connectToSession(MediaSessionCompat.Token token) {
        Log.d(TAG, "connectToSession");

        Activity currentActivity = getCurrentActivity();

        if (currentActivity != null) {
            MediaControllerCompat mediaController = new MediaControllerCompat(reactContext, token);

            MediaControllerCompat.setMediaController(currentActivity, mediaController);
        }
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
                } while (audioCursor.moveToNext());

                Log.d(TAG, "Size: " + String.valueOf(audios.size()));
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

    private void registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            getReactApplicationContext().registerReceiver(becomingNoisyReceiver, BecomingNoisyReceiver.intentFilter);
            isNoisyReceiverRegistered = true;
        }
    }

    private void unRegisterNoisyReceiver() {
        getReactApplicationContext().unregisterReceiver(becomingNoisyReceiver);
        isNoisyReceiverRegistered = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @ReactMethod
    public void playAudio(String path){
        if (mp != null) mp.stop();
        mp = new MediaPlayer();

        registerNoisyReceiver();

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

            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "Audio ended");
                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_ENDED_EVENT, true);
                }
            });
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
                activity.requestPermissions(permissions, PERMS_REQUEST_CODE, this.permissionManager);
            }
        } else Log.d(TAG, "Permissions already granted!");
    }

    public void stop() {
        if (mp != null) mp.stop();
        unRegisterNoisyReceiver();
    }

    @ReactMethod
    public void play() {
        if (mp != null) {
            mp.start();
            registerNoisyReceiver();
        }
    }

    @ReactMethod
    public void pause() {
        if (mp != null) mp.pause();
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_PAUSED_EVENT, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @ReactMethod
    public void getAudios(Promise promise) {
        WritableArray audioArray = new WritableNativeArray();

//        List<Audio> audios = getAudioList();

        try {
//            for (Audio audio: audios) {
//                audioArray.pushMap(audio.toMap());
//            }

            promise.resolve(audioArray);
        } catch (Exception e) {
            promise.reject("E_AUDIO", "Could not get audios");
            Log.d(TAG, "Exception while parsing " + e.getMessage());
        }
    }

    @ReactMethod
    public void init(Promise promise) {
        permissionManager.permsGrantedPromise = promise;
        grantPermissions();

//        Log.d(TAG, "Trying to instance service");
//
//        mediaBrowser = new MediaBrowserCompat(reactContext, new ComponentName(reactContext, MediaPlaybackService.class), connectionCallback, null);
    };

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(Constants.AUDIO_ENDED_EVENT, Constants.AUDIO_ENDED_EVENT);
        constants.put(Constants.AUDIO_PAUSED_EVENT, Constants.AUDIO_PAUSED_EVENT);
        return constants;
    }
}
