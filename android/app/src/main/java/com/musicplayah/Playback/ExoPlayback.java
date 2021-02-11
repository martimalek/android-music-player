package com.musicplayah.Playback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.musicplayah.Receivers.BecomingNoisyReceiver;
import com.musicplayah.Constants;
import com.musicplayah.MediaPlaybackService;
import com.musicplayah.MusicProvider;
import android.support.v4.media.session.MediaSessionCompat;

public class ExoPlayback implements Playback {
    private String TAG = Constants.TAG;

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private SimpleExoPlayer exoPlayer;
    private ExoPlayerEventListener eventListener = new ExoPlayerEventListener();
    private MusicProvider musicProvider;

    private String currentMediaId;

    private int currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;

    private boolean isExoPlayerStopped = false;
    private boolean isNoisyReceiverRegistered = false;
    private boolean shouldPlayOnFocusGain;

    private final IntentFilter audioNoisyIntentFIlter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private Context context;

    private AudioManager audioManager;

    Playback.Callback callback;

    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver(
        new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "becomingNoisyReceiver!");
                Intent i = new Intent(context, MediaPlaybackService.class);
                i.setAction(MediaPlaybackService.ACTION_CMD);
                i.putExtra(MediaPlaybackService.CMD_NAME, MediaPlaybackService.CMD_PAUSE); // See onStartCommand
                context.startService(i);
            }
        }
    );

    public ExoPlayback(Context context, MusicProvider musicProvider) {
        Log.d(TAG, "Inside ExoPlayback constructor");
        this.context = context;
        this.musicProvider = musicProvider;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void play(MediaSessionCompat.QueueItem item) {
        Log.d(TAG, "Exoplayer play!");
        shouldPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerNoisyReceiver();

        String mediaId = item.getDescription().getMediaId();
        Log.d(TAG, "current mediaId " + currentMediaId);
        Log.d(TAG, "new mediaId " + mediaId);
        assert mediaId != null;
        boolean hasMediaChanged = !mediaId.equals(currentMediaId);
        if (hasMediaChanged) currentMediaId = mediaId;

        Log.d(TAG, "Has media changed ? " + hasMediaChanged);

        if (hasMediaChanged || exoPlayer == null) {
            releaseResources(false);

            MediaMetadataCompat track = musicProvider.getTrackById(mediaId);
//            String mediaUri = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI); // TODO: Is this useless ???
//
//            String source = track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE); // TODO: Is this useless ???
//            if (source != null) source = source.replaceAll(" ", "%20");

            if (exoPlayer == null) {
                exoPlayer = new SimpleExoPlayer.Builder(context).build();
                exoPlayer.addListener(eventListener);
            }

            exoPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build());

            MediaItem mediaItem = mapToExoMediaItem(track);

            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "musicplayah"), null);
            ProgressiveMediaSource.Factory mediaFactory = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory());

            exoPlayer.setMediaSource(mediaFactory.createMediaSource(mediaItem));
            exoPlayer.prepare();
            Log.d(TAG, "ExoPlayer prepared");
        }

        configurePlayerState();
    }

    @Override
    public void pause() {
        Log.d(TAG, "Pausing...");
        if (exoPlayer != null) exoPlayer.setPlayWhenReady(false);

        releaseResources(false);
        unregisterNoisyReceiver();
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public MediaItem mapToExoMediaItem(MediaMetadataCompat mediaItem) {
        MediaMetadata metadata;
        try {
            metadata = new MediaMetadata.Builder()
                    .setTitle(mediaItem.getDescription().getTitle().toString())
                    .build();
        } catch (NullPointerException e) {
            Log.d(TAG, "mapToExoMediaItem error catched!");
            metadata = new MediaMetadata.Builder()
                    .setTitle("") // No title
                    .build();
        }

        return new MediaItem.Builder()
                .setUri(mediaItem.getDescription().getMediaUri())
                .setMediaMetadata(metadata)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void stop() {
        giveUpAudioFocus();
        unregisterNoisyReceiver();
        releaseResources(true);
    }

    @Override
    public MediaItem getCurrentPlaying() {
        Log.d(TAG, "Getting current item playing");

        if (exoPlayer != null){ return exoPlayer.getCurrentMediaItem();}
        return null;
    }

    private void releaseResources(boolean releasePlayer) {
        if (releasePlayer && exoPlayer != null) {
            exoPlayer.release();
            exoPlayer.removeListener(eventListener);
            exoPlayer = null;
            isExoPlayerStopped = true;
            shouldPlayOnFocusGain = false;
        }
    }

    private void registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            context.registerReceiver(becomingNoisyReceiver, audioNoisyIntentFIlter);
            isNoisyReceiverRegistered = true;
        }
    }

    private void unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            context.unregisterReceiver(becomingNoisyReceiver);
            isNoisyReceiverRegistered = false;
        }
    }

    @Override
    public int getState() {
        Log.d(TAG, "getState");
        if (exoPlayer == null) return isExoPlayerStopped ? PlaybackStateCompat.STATE_STOPPED : PlaybackStateCompat.STATE_NONE;

        Log.d(TAG, "State => " + exoPlayer.getPlaybackState());
        switch (exoPlayer.getPlaybackState()) {
            case ExoPlayer.STATE_IDLE:
            case ExoPlayer.STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            case ExoPlayer.STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case ExoPlayer.STATE_READY:
                return exoPlayer.getPlayWhenReady()
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    @Override
    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return shouldPlayOnFocusGain || (exoPlayer != null && exoPlayer.getPlayWhenReady());
//        if (exoPlayer != null) { return exoPlayer.isPlaying();}
//        return false;
    }

    private void tryToGetAudioFocus() {
        Log.d(TAG, "Trying to get audio focus");
        int result = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus granted");
            currentAudioFocusState = AUDIO_FOCUSED;
        }
        else currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");

        int audioFocusRequestGranted = audioManager.abandonAudioFocusRequest(
            new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                .build()
        );

        if (audioFocusRequestGranted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    }

    private final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange. focusChange=" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    currentAudioFocusState = AUDIO_FOCUSED;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                    currentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Lost audio focus, but will gain it back (shortly), so note whether
                    // playback should resume
                    currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                    shouldPlayOnFocusGain = exoPlayer != null && exoPlayer.getPlayWhenReady();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // Lost audio focus, probably "permanently"
                    currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                    break;
            }

            if (exoPlayer != null) configurePlayerState();
        }
    };

    private void configurePlayerState() {
        Log.d(TAG, "configurePlayerState currentAudioFocusState => " + currentAudioFocusState);
        if (currentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) pause();
        else {
            registerNoisyReceiver();

            if (currentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) exoPlayer.setVolume(VOLUME_DUCK);
            else exoPlayer.setVolume(VOLUME_NORMAL);

            if (shouldPlayOnFocusGain) {
                exoPlayer.setPlayWhenReady(true);
                shouldPlayOnFocusGain = false;
            }
        }
    }

    private final class ExoPlayerEventListener implements ExoPlayer.EventListener {
        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d(TAG, "Error on exoPlayer " + error.getMessage());
        }

//        @Override
//        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            Log.d(TAG, "Inside deprecated onPlayerStateChanged");
//            switch (playbackState) {
//                case ExoPlayer.STATE_IDLE:
//                case ExoPlayer.STATE_BUFFERING:
//                case ExoPlayer.STATE_READY:
//                    if (callback != null) callback.onPlaybackStatusChanged(getState());
//                    break;
//                case ExoPlayer.STATE_ENDED:
//                    // The media player finished playing the current song.
//                    if (callback != null) callback.onCompletion();
//                    break;
//            }
//        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            Log.d(TAG, "Play when ready changed => " + playWhenReady);
            if (callback != null) callback.onPlaybackStatusChanged(getState()); // TODO: This seems to work but not clear why
        }

        //
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Log.d(TAG, "Inside onPlaybackStateChanged " + playbackState);

            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                case ExoPlayer.STATE_BUFFERING:
                case ExoPlayer.STATE_READY:
                    if (callback != null) callback.onPlaybackStatusChanged(getState());
                    break;
                case ExoPlayer.STATE_ENDED:
                    // The media player finished playing the current song.
                    if (callback != null) callback.onCompletion();
                    break;
            }
        }
    }
}
