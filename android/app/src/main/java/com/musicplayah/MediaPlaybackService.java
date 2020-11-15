package com.musicplayah;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.MediaItem;
import com.musicplayah.Playback.PlaybackManager;

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements PlaybackManager.PlaybackServiceCallback {
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static String TAG = Constants.TAG;

    private MediaSessionCompat mediaSession;
    private MediaNotificationManager mediaNotificationManager;
    private PlaybackStateCompat.Builder stateBuilder;

    private MusicProvider musicProvider;
    private PlaybackManager playbackManager;

    public static final String ACTION_CMD = "com.musicplayah.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";

    public ArrayList<MediaBrowserCompat.MediaItem> tracks; // TODO: Create a QueueManager

    public MediaPlaybackService() {
        Log.d(TAG, "MediaPlaybackService constructed!");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        musicProvider = new MusicProvider(this);

        Log.d(TAG, "Inside MediaPlaybackService onCreate");
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

        stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mediaSession.setPlaybackState(stateBuilder.build());
        Log.d(TAG, "Inside MediaPlaybackService onCreate");

        playbackManager = new PlaybackManager(this, getApplicationContext(), musicProvider);

        mediaSession.setCallback(playbackManager.getMediaSessionCallback());

        setSessionToken(mediaSession.getSessionToken());

        Context context = getApplicationContext();

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT); // 99 is the request code
        mediaSession.setSessionActivity(pendingIntent);

        tracks = musicProvider.getAllSongs();

        playbackManager.updatePlaybackState();

        mediaNotificationManager = new MediaNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) playbackManager.handlePauseRequest();
            } else {
//                MediaButtonReceiver.handleIntent(mediaSession, intent); // TODO: Uncomment once media button receiver is done
            }
        }

//        mDelayedStopHandler.removeCallbacksAndMessages(null);
//        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "MediaPlaybackService onGetRoot packageName " + clientPackageName);
        return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
//        if (allowBrowsing(clientPackageName, clientUid)) {
//            // Returns a root ID that clients can use with onLoadChildren() to retrieve
//            // the content hierarchy.
//            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
//        } else {
//            // Clients can connect, but this BrowserRoot is an empty hierachy
//            // so onLoadChildren returns nothing. This disables the ability to browse for content.
//            return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
//        }
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "MediaPlaybackService onLoadChildren HEREEEEEE");

        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = musicProvider.getAllSongs();
        result.sendResult(mediaItems);
    }

//    private List<MediaBrowserCompat.MediaItem> mapToMediaItems(List<MediaItem> mediaExoItems) {
//        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//        for (MediaItem mediaItem : mediaExoItems) {
//            MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
//                    .setTitle(mediaItem.title)
//                    .setSubtitle(mediaItem.subtitle)
//                    .setMediaId(mediaItem.mediaId)
//                    .build();
//
//            int flags = MediaBrowser.MediaItem.FLAG_PLAYABLE;
//
//            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, flags);
//            mediaItems.add(mediaItem);
//    }

    @Override
    public void onPlaybackStart() {
        Log.d(TAG, "onPlaybackStart!");

        mediaSession.setActive(true);

        startService(new Intent(getApplicationContext(), MediaPlaybackService.class));
    }

    @Override
    public void onPlaybackStop() {
        Log.d(TAG, "onPlaybackStop!");

        mediaSession.setActive(false);

        stopForeground(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onNotificationRequired() {
        Log.d(TAG, "onNotificationRequired!");
        mediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        Log.d(TAG, "onPlaybackStateUpdated!");
        mediaSession.setPlaybackState(newState);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service!");
        playbackManager.handleStopRequest();
        mediaNotificationManager.stopNotification();

        // delayedStopHandler.removeCallbacksAndMessages(null);
        mediaSession.release();
    }
}
