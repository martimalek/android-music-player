package com.musicplayah;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;


public class PlaybackManager {
    private static String TAG = Constants.TAG;

    private MediaSessionCallback mediaSessionCallback;
    private MediaPlaybackService playbackService;

    PlaybackManager(MediaPlaybackService playbackService) {
        Log.d(TAG, "Inside PlaybackManager constructor!");
        mediaSessionCallback = new MediaSessionCallback();
        this.playbackService = playbackService;
    }

    public void handlePlayRequest() {
        Log.d(TAG, "Handling Play request!"); // TODO
    }

    public void handlePauseRequest() {
        Log.d(TAG, "Handling Pause request!"); // TODO
    }

    public void handleStopRequest() {
        Log.d(TAG, "Handling Pause request!"); // TODO
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest();
        }
    }

    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
