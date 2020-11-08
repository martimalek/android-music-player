package com.musicplayah;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.MediaItem;

public class PlaybackManager {
    private static String TAG = Constants.TAG;

    private MediaSessionCallback mediaSessionCallback;
    private MediaPlaybackService playbackService;

    private Context context;

    private ExoPlayback exoPlayback;

    PlaybackManager(MediaPlaybackService playbackService, Context context, MusicProvider musicProvider) {
        Log.d(TAG, "Inside PlaybackManager constructor!");
        mediaSessionCallback = new MediaSessionCallback();
        this.playbackService = playbackService;
        this.context = context;
        this.exoPlayback = new ExoPlayback(context, musicProvider);
    }

    public void handlePlayRequest() {
        Log.d(TAG, "Handling Play request!"); // TODO

        MediaItem item = exoPlayback.isPlaying() ? exoPlayback.getCurrentPlaying() : playbackService.tracks.get(0);

        Log.d(TAG, "Current item => " + item.mediaId);

        Log.d(TAG, "Item => " + item.mediaId);

        playbackService.onPlaybackStart();
        exoPlayback.play(item);

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

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                       PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                       PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                       PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                       PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (exoPlayback.isPlaying()) actions |= PlaybackStateCompat.ACTION_PAUSE;
        else actions |= PlaybackStateCompat.ACTION_PLAY;
        return actions;
    }

    public void updatePlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());


    }

    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
