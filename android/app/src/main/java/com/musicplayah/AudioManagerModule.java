package com.musicplayah;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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

import java.util.HashMap;
import java.util.Map;

import static com.musicplayah.Constants.PERMS_REQUEST_CODE;
import static com.musicplayah.Constants.permissions;

public class AudioManagerModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;
    private static String TAG = Constants.TAG;

    private MediaPlayer mp;

    private PermissionManager permissionManager;

    private boolean isNoisyReceiverRegistered = false;

    private MediaBrowserCompat mediaBrowser;

    private Activity currActivity;

    AudioManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        Log.d(TAG, "Inside constructor");

        permissionManager = new PermissionManager();

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
            Log.d(TAG, "There is an Activity");

            MediaControllerCompat mediaController = new MediaControllerCompat(reactContext, token);

            MediaControllerCompat.setMediaController(currentActivity, mediaController);

            mediaController.registerCallback(controllerCallback);
        } else {
            Log.d(TAG, "There is no Activity");
        }
    }

    MediaControllerCompat.Callback controllerCallback =
        new MediaControllerCompat.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                Log.d(TAG, "Metadata changed!");
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                Log.d(TAG, "State changed!");
            }
        };

    private Activity getActivity() {
        if (currActivity != null) return currActivity;
        return getCurrentActivity();
    }

    @NonNull
    @Override
    public String getName() {
        return "AudioManager";
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

    @ReactMethod
    public void toggle() {
        Log.d(TAG, "Toggling...");

        Activity activity = getActivity();
        if (activity != null) {
            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(activity);
            Log.d(TAG, "State " + mediaController.getPlaybackState().getState());
            if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) mediaController.getTransportControls().pause();
            else mediaController.getTransportControls().play();
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
        permissionManager.permsGrantedPromise = promise; // TODO: Await for perms granted here and Promise.resolve on mediaBrowser.connect
        grantPermissions();

        Log.d(TAG, "Trying to instance service");

        mediaBrowser = new MediaBrowserCompat(reactContext, new ComponentName(reactContext, MediaPlaybackService.class), connectionCallback, null);

        Log.d(TAG, "Service instanced, going to connect");

        mediaBrowser.connect();

        Log.d(TAG, "Service connected");
    };

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(Constants.AUDIO_ENDED_EVENT, Constants.AUDIO_ENDED_EVENT);
        constants.put(Constants.AUDIO_PAUSED_EVENT, Constants.AUDIO_PAUSED_EVENT);
        return constants;
    }
}
