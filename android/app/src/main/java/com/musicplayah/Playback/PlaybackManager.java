package com.musicplayah.Playback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.musicplayah.Utils.Constants;

import java.util.List;

public class PlaybackManager implements Playback.Callback {
    private static final String TAG = Constants.TAG;

    private final MediaSessionCallback mediaSessionCallback;
    private final MediaPlaybackService playbackService;

    private Context context;
    private final QueueManager queueManager;
    private final ExoPlayback exoPlayback;

    PlaybackStateCompat.Builder stateBuilder;

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
            queueManager.updateMetadata();
        }
    }

    @SuppressLint("NewApi")
    public void handlePauseRequest() {
        Log.d(TAG, "Handling Pause request!");
        if (exoPlayback.isPlaying()) {
            Log.d(TAG, "isPlaying");
            exoPlayback.pause();
            updatePlaybackState();
            queueManager.updateMetadata();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void handleStopRequest() {
        Log.d(TAG, "Handling Stop request!");
        exoPlayback.stop();
        playbackService.onPlaybackStop();
        updatePlaybackState();
        queueManager.updateMetadata();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleSkipToNext() {
        Log.d(TAG, "Skipping to next");
        if (queueManager.goToNextSong()) handlePlayRequest();
        else handleStopRequest(); // Skipping is impossible
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleSkipToPrevious() {
        Log.d(TAG, "Skipping to next");
        if (queueManager.goToPreviousSong()) handlePlayRequest();
        else handleStopRequest(); // Skipping is impossible
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    public List<MediaSessionCompat.QueueItem> getCurrentQueue() {
        return queueManager.getDefaultQueue();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCompletion() {
        Log.d(TAG, "Track ended, should we play another one?");
        handleSkipToNext();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState();
    }

    @Override
    public void onError(String error) {
        // TODO: Should we handle errors?
        Log.d(TAG, "Unhandled error! " + error);
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        // TODO: Is this necessary?
        Log.d(TAG, "setCurrentMediaId was called, but no logic is in it");
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (queueManager.getCurrentMusic() == null) queueManager.fillQueueWithAllSongs();
            handlePlayRequest();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onStop() {
            handleStopRequest();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSkipToNext() {
            handleSkipToNext();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSkipToPrevious() {
            handleSkipToPrevious();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            queueManager.setCurrentQueueItem(id);
            queueManager.updateMetadata();
            Log.d(TAG, "Skipping to queue item " + id);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.d(TAG, "New mediaButtonEvent " + mediaButtonEvent);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Log.d(TAG, "Custom action!! " + extras.toString());
            int position = extras.getInt("position");
            if (action.equals(Constants.CUSTOM_ACTION_ADD_TO_SELECTED_QUEUE)) {
                queueManager.addSongToSelectedQueueByIndex(position);
                queueManager.updateMetadata();
            }
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void updatePlaybackState() {
        Log.d(TAG, "Updating playback state");
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (exoPlayback != null && exoPlayback.isConnected()) position = exoPlayback.getCurrentPosition();

        stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        int state = exoPlayback.getState();

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        MediaSessionCompat.QueueItem currentMedia = queueManager.getCurrentMusic();
        if (currentMedia != null) stateBuilder.setActiveQueueItemId(currentMedia.getQueueId());

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
