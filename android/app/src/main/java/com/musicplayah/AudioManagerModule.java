package com.musicplayah;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.LifecycleEventListener;
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
import com.musicplayah.Playback.MediaPlaybackService;
import com.musicplayah.Utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observer;

import static com.musicplayah.Utils.Constants.PERMISSION_OBSERVER_KEY;
import static com.musicplayah.Utils.Constants.PERMS_REQUEST_CODE;
import static com.musicplayah.Utils.Constants.permissions;

public class AudioManagerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private static ReactApplicationContext reactContext;
    private static final String TAG = Constants.TAG;

    private final PermissionManager permissionManager;
    private MediaBrowserCompat mediaBrowser;

    private Promise initialPromise;

    private boolean isPlayingSelectedQueue = false;

    AudioManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        Log.d(TAG, "AudioManagerModule constructor");

        permissionManager = new PermissionManager();

        Observer permissionObserver = (o, permsObject) -> {
            Pair<String, Boolean> permsPair = (Pair<String, Boolean>) permsObject;
            if (permsPair.first.equals(PERMISSION_OBSERVER_KEY) && permsPair.second) {
                mediaBrowser.connect();
                initialPromise.resolve(true);
            } else initialPromise.reject("E_PERMS", "User did not accept permission");
        };

        permissionManager.isPermissionGranted.addObserver(permissionObserver);

        reactContext.addLifecycleEventListener(this);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            connectToSession(mediaBrowser.getSessionToken());
            subscribeToChildrenChanges();
        }
    };

    private void subscribeToChildrenChanges() {
        try {
            String root = mediaBrowser.getRoot();
            mediaBrowser.subscribe(root, new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    if (children == null || children.isEmpty()) return;
                    Log.d(TAG, "There are " + children.size() + " children!");
                    sendChildrenToReact(children);
                    updateQueue();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "Error while subscribing to children " + e.getMessage());
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity != null) {
            MediaControllerCompat mediaController = new MediaControllerCompat(reactContext, token);

            MediaControllerCompat.setMediaController(currentActivity, mediaController);

            mediaController.registerCallback(controllerCallback);
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
                Log.d(TAG, "State changed! " + state.getState());
                int currentState = state.getState();
                if (currentState == PlaybackStateCompat.STATE_PLAYING) reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_RESUMED_EVENT, true);
                else if (currentState == PlaybackStateCompat.STATE_PAUSED) reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.AUDIO_PAUSED_EVENT, true);
            }

            @Override
            public void onExtrasChanged(Bundle extras) {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.POSITION_CHANGED_EVENT, extras.getInt("position"));
            }

            @Override
            public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
                Log.d(TAG, "Queue changed! Size => " + queue.size());
                if (isPlayingSelectedQueue) {
                    WritableMap selectedMap = new WritableNativeMap();

                    for (MediaSessionCompat.QueueItem child : queue) {
                        WritableMap childMap = buildItemMap(child.getDescription());
                        selectedMap.putMap(Objects.requireNonNull(childMap.getString("id")), childMap);
                    }
                    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.SELECTED_QUEUE_CHANGED_EVENT, selectedMap);
                }
            }

            @Override
            public void onQueueTitleChanged(CharSequence title) {
                super.onQueueTitleChanged(title);
                if (title.toString().equals(Constants.SELECTED_QUEUE)) isPlayingSelectedQueue = true;
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
        }
    }

    private MediaControllerCompat getMediaController() {
        Activity activity = getCurrentActivity();
        if (activity != null) return MediaControllerCompat.getMediaController(activity);
        return null;
    }

    private void sendChildrenToReact(List<MediaBrowserCompat.MediaItem> updatedChildren) {
        Log.d(TAG, "Sending children to react");
        WritableArray childrenArray = new WritableNativeArray();

        for (MediaBrowserCompat.MediaItem child: updatedChildren) {
            childrenArray.pushMap(buildItemMap(child.getDescription()));
        }
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(Constants.CHILDREN_UPDATED_EVENT, childrenArray);
    }

    private void updateQueue() {
        Log.d(TAG, "UPDATE QUEUE");
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) mediaController.getTransportControls().sendCustomAction(Constants.CUSTOM_ACTION_UPDATE_QUEUE, new Bundle());
    }

    private WritableMap buildItemMap(MediaDescriptionCompat description) {
        WritableMap map = new WritableNativeMap();

        map.putString("id", description.getMediaId());
        CharSequence title = description.getTitle();
        if (title != null) map.putString("title", title.toString());
        else map.putNull("title");

        CharSequence subtitle = description.getSubtitle();
        if (subtitle != null && !subtitle.toString().equals("<unknown>")) map.putString("subtitle", subtitle.toString());
        else map.putNull("subtitle");

        return map;
    }

    @ReactMethod
    public void toggle() {
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) {
            Log.d(TAG, "State " + mediaController.getPlaybackState().getState());
            if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) mediaController.getTransportControls().pause();
            else mediaController.getTransportControls().play();
        }
    }

    @ReactMethod
    public void playNext() {
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) mediaController.getTransportControls().skipToNext();
    }

    @ReactMethod
    public void playPrevious() {
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) mediaController.getTransportControls().skipToPrevious();
    }

    @ReactMethod
    public void playFromQueuePosition(int queueItem, Promise promise) {
        try {
            MediaControllerCompat mediaController = getMediaController();
            if (mediaController != null) mediaController.getTransportControls().skipToQueueItem(queueItem);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("MEDIA_NOT_FOUND", "The specified media could not be found in the current queue");
        }
    }

    @ReactMethod
    public void addSongToSelectedQueueByPosition(int queueIndex) {
        MediaControllerCompat mediaController = getMediaController();
        Bundle selectedQueueBundle = new Bundle();
        selectedQueueBundle.putInt("position", queueIndex);
        if (mediaController != null) mediaController.getTransportControls().sendCustomAction(Constants.CUSTOM_ACTION_ADD_TO_SELECTED_QUEUE, selectedQueueBundle);
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
        constants.put(Constants.POSITION_CHANGED_EVENT, Constants.POSITION_CHANGED_EVENT);
        constants.put(Constants.SELECTED_QUEUE_CHANGED_EVENT, Constants.SELECTED_QUEUE_CHANGED_EVENT);
        return constants;
    }

    @Override
    public void onHostResume() {
        Log.d(TAG, "onHostResume");
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) {
            Log.d(TAG, "SENDING CUSTOM ACTION");
            mediaController.getTransportControls().sendCustomAction(Constants.CUSTOM_ACTION_UPDATE_QUEUE, new Bundle());
        }
        Objects.requireNonNull(reactContext.getCurrentActivity()).setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onHostPause() {
        Log.d(TAG, "onHostPause");
    }

    @Override
    public void onHostDestroy() {
        Log.d(TAG, "onHostDestroy");
        MediaControllerCompat mediaController = getMediaController();
        if (mediaController != null) mediaController.unregisterCallback(controllerCallback);
        mediaBrowser.disconnect();
    }
}
