package com.musicplayah.Playback;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.musicplayah.Constants;
import com.musicplayah.MediaPlaybackService;
import com.musicplayah.MusicProvider;
import com.musicplayah.QueueManager;

public class PlaybackManager implements Playback.Callback {
    private static String TAG = Constants.TAG;

    private MediaSessionCallback mediaSessionCallback;
    private MediaPlaybackService playbackService;

    private Context context;
    private QueueManager queueManager;
    private ExoPlayback exoPlayback;

    public PlaybackManager(MediaPlaybackService playbackService, Context context, MusicProvider musicProvider, QueueManager queueManager, ExoPlayback playback) {
        Log.d(TAG, "Inside PlaybackManager constructor!");
        this.mediaSessionCallback = new MediaSessionCallback();
        this.playbackService = playbackService;
        this.context = context;
        this.queueManager = queueManager;
        this.exoPlayback = playback;

        this.exoPlayback.setCallback(this);
    }

    public void handlePlayRequest() {
        Log.d(TAG, "Handling Play request!" + queueManager.toString());

        MediaSessionCompat.QueueItem currentItem = queueManager.getCurrentMusic();

        Log.d(TAG, "currentItem => " + currentItem);

        if (currentItem != null) {
            Log.d(TAG, "Current item => " + currentItem.getDescription().getMediaId());
            playbackService.onPlaybackStart();
            exoPlayback.play(currentItem);
        }
    }

    public void handlePauseRequest() {
        Log.d(TAG, "Handling Pause request!");
        if (exoPlayback.isPlaying()) {
            exoPlayback.pause();
            playbackService.onPlaybackStop();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void handleStopRequest() {
        Log.d(TAG, "Handling Stop request!");
        exoPlayback.stop();
        playbackService.onPlaybackStop();
        updatePlaybackState();
    }

    private MediaItem mapToExoMediaItem(MediaBrowserCompat.MediaItem mediaItem) {
        MediaMetadata metadata;
        try {
            metadata = new MediaMetadata.Builder()
                    .setTitle(mediaItem.getDescription().getTitle().toString())
                    .build();
        } catch (NullPointerException e) {
            metadata = new MediaMetadata.Builder()
                    .setTitle("") // No title
                    .build();
        }

        return new MediaItem.Builder()
                .setUri(mediaItem.getDescription().getMediaUri())
                .setMediaMetadata(metadata)
                .build();
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    @Override
    public void onCompletion() {
        // TODO: Handle track end logic, like play next or stop playing and go to sleep
        Log.d(TAG, "Track ended, should we play another one?");

        // TODO: Use queueManager.goToNextSong()
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState();
    }

    @Override
    public void onError(String error) {
        // TODO: Should we handle errors?
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        // TODO: Is this necessary?
        Log.d(TAG, "setCurrentMediaId was called, but no logic is in it");
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (queueManager.getCurrentMusic() == null) queueManager.fillRandomQueue();
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
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
        Log.d(TAG, "Updating playback state");
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (exoPlayback != null && exoPlayback.isConnected()) {
            position = exoPlayback.getCurrentPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        int state = exoPlayback.getState();

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        MediaSessionCompat.QueueItem currentMedia = queueManager.getCurrentMusic();
        if (currentMedia != null) {
            stateBuilder.setActiveQueueItemId(currentMedia.getQueueId());
        }

        playbackService.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) playbackService.onNotificationRequired();
    }

    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
