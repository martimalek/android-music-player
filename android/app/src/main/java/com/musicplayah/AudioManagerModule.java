package com.musicplayah;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import static com.musicplayah.Constants.PERMISSION_OBSERVER_KEY;
import static com.musicplayah.Constants.PERMS_REQUEST_CODE;
import static com.musicplayah.Constants.permissions;

public class AudioManagerModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;
    private static String TAG = Constants.TAG;

    private PermissionManager permissionManager;
    private Observer permissionObserver;

    private MediaBrowserCompat mediaBrowser;

    private Promise initialPromise;

    AudioManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        Log.d(TAG, "Inside constructor");

        permissionManager = new PermissionManager();

        permissionObserver = (o, permsObject) -> {
            Log.d(TAG, "Updating....");
            Pair<String, Boolean> permsPair = (Pair<String, Boolean>) permsObject;
            if (permsPair.first.equals(PERMISSION_OBSERVER_KEY) && permsPair.second) {
                Log.d(TAG, "Perms have been granted! Connecting to mediaBrowser...");
                mediaBrowser.connect();

                initialPromise.resolve(true);
            } else initialPromise.reject("E_PERMS", "User did not accept permission");
        };

        permissionManager.isPermissionGranted.addObserver(permissionObserver);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            connectToSession(mediaBrowser.getSessionToken());
            subscribeToChildrenChanges();
        }
    };

    private void subscribeToChildrenChanges() {
        Log.d(TAG, "Subscribed to children changes!");

        try {
            String root = mediaBrowser.getRoot();
            mediaBrowser.subscribe(root, new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.d(TAG, "CHILDREN LOADED!");
                    if (children == null || children.isEmpty()) {
                        Log.d(TAG, "children is f*cking empty T.T");
                        return;
                    }
                    Log.d(TAG, "There are " + children.size() + " children!");
                    sendChildrenToReact(children);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "Error while subscribing to children " + e.getMessage());
        }
    }

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

            @Override
            public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
                Log.d(TAG, "Queue changed! Size => " + queue.size());
            }
        };

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

    private MediaControllerCompat getMediaController() {
        Activity activity = getCurrentActivity();
        if (activity != null) return MediaControllerCompat.getMediaController(activity);
        return null;
    }

    @ReactMethod
    public void toggle() {
        Log.d(TAG, "Toggling...");

        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) {
            Log.d(TAG, "State " + mediaController.getPlaybackState().getState());
            if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                mediaController.getTransportControls().pause();
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_PAUSED_EVENT, true);
            } else {
                mediaController.getTransportControls().play();
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_RESUMED_EVENT, true);
            }
        }
    }

    @ReactMethod
    public void playNext() {
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) {
            mediaController.getTransportControls().skipToNext();
        }
    }

    @ReactMethod
    public void playPrevious() {
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) {
            mediaController.getTransportControls().skipToPrevious();
        }
    }

    private void sendChildrenToReact(List<MediaBrowserCompat.MediaItem> updatedChildren) {
        Log.d(TAG, "Sending children to react");
        WritableArray childrenArray = new WritableNativeArray();

        Log.d(TAG, "Example of child " + updatedChildren.get(0).getDescription().toString());

        for (MediaBrowserCompat.MediaItem child: updatedChildren) {
            WritableMap map = new WritableNativeMap();

            MediaDescriptionCompat description = child.getDescription();

            map.putString("id", description.getMediaId());
            CharSequence title = description.getTitle();
            if (title != null) map.putString("title", title.toString());
            else map.putNull("title");

            CharSequence subtitle = description.getSubtitle();
            if (subtitle != null && !subtitle.toString().equals("<unknown>")) map.putString("subtitle", subtitle.toString());
            else map.putNull("subtitle");

//            map.putString("artist", this.artist);
//            map.putString("data", this.data);
//            map.putString("displayName", this.displayName);
//            map.putInt("duration", this.duration);

            childrenArray.pushMap(map);
        }
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.CHILDREN_UPDATED_EVENT, childrenArray);
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
        Log.d(TAG, "Initializing service...");
        initialPromise = promise;
        grantPermissions();
        mediaBrowser = new MediaBrowserCompat(reactContext, new ComponentName(reactContext, MediaPlaybackService.class), connectionCallback, null);
    };

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(Constants.AUDIO_ENDED_EVENT, Constants.AUDIO_ENDED_EVENT);
        constants.put(Constants.AUDIO_PAUSED_EVENT, Constants.AUDIO_PAUSED_EVENT);
        constants.put(Constants.AUDIO_RESUMED_EVENT, Constants.AUDIO_RESUMED_EVENT);
        constants.put(Constants.CHILDREN_UPDATED_EVENT, Constants.CHILDREN_UPDATED_EVENT);
        return constants;
    }
}
