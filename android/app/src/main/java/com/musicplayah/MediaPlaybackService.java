package com.musicplayah;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements PlaybackManager.PlaybackServiceCallback {
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static String TAG = Constants.TAG;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private MusicProvider musicProvider;

    PlaybackManager playbackManager;

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

        playbackManager = new PlaybackManager(this);

        mediaSession.setCallback(playbackManager.getMediaSessionCallback());

        setSessionToken(mediaSession.getSessionToken());
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
        Log.d(TAG, "MediaPlaybackService onLoadChildren");
        result.sendResult(null);
    }


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

    @Override
    public void onNotificationRequired() {
        Log.d(TAG, "onNotificationRequired!");
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        Log.d(TAG, "onPlaybackStateUpdated!");
    }
}
