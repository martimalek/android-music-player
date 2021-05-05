package com.musicplayah.Playback;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.musicplayah.Utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {
    private static final String TAG = Constants.TAG + "_QUEUE";

    private final MusicProvider musicProvider;
    private final Context context;
    private Resources resources;
    private final List<MediaSessionCompat.QueueItem> defaultQueue;
    private final List<MediaSessionCompat.QueueItem> selectedQueue;
    private boolean isPlayingDefaultQueue = true;
    private boolean shouldReturnToSelectedQueue = false;
    private int currentSelectedQueueSongIndex;
    private final MetadataUpdateListener listener;

    private MediaSessionCompat.QueueItem currentPlayingItem;

    public QueueManager(
            MusicProvider musicProvider,
            Resources resources,
            Context context,
            MetadataUpdateListener listener
    ) {
        this.musicProvider = musicProvider;
        this.resources = resources;
        this.context = context;
        this.listener = listener;

        defaultQueue = Collections.synchronizedList(new ArrayList<>());
        selectedQueue = Collections.synchronizedList(new ArrayList<>());

        currentPlayingItem = null;
    }

    public List<MediaSessionCompat.QueueItem> getDefaultQueue() {
            return defaultQueue;
    }

    public void fillQueueWithAllSongs() {
        Log.d(TAG, "fillQueueWithAllSongs");
        if (defaultQueue.size() < 10)
        {
            ArrayList<MediaMetadataCompat> allSongsOnDevice = musicProvider.getAllSongs();
            List<MediaSessionCompat.QueueItem> newTracks = QueueHelper.convertToQueue(allSongsOnDevice);
            defaultQueue.addAll(newTracks);
        }

        listener.onQueueUpdated(Constants.DEFAULT_QUEUE, defaultQueue);
    }

    public void setCurrentQueueItem(long queueId) {
        Log.d(TAG, "HEREE setCurrentQueueItem " + isPlayingDefaultQueue);
        int index = QueueHelper.getItemIndexOnQueue(defaultQueue, queueId);
        if (index >= 0 && index < defaultQueue.size()) {
            if (!isPlayingDefaultQueue && !QueueHelper.isItemAlreadyOnQueue(selectedQueue, queueId) && !shouldReturnToSelectedQueue) {
                shouldReturnToSelectedQueue = true;
                currentSelectedQueueSongIndex = QueueHelper.getItemIndexOnQueue(selectedQueue, currentPlayingItem.getQueueId());
            }
            currentPlayingItem = defaultQueue.get(index);
            listener.onNowPlayingChanged(currentPlayingItem);
        }
    }

    public boolean goToNextSong() {
        List<MediaSessionCompat.QueueItem> currentQueue = getCurrentQueue();
        Log.d(TAG, "goToNextSong, queue size => " + currentQueue.size());
        if (currentQueue.size() > 0) {
            int index = currentPlayingItem == null ? -1 : QueueHelper.getItemIndexOnQueue(currentQueue, currentPlayingItem.getQueueId());
            if (shouldReturnToSelectedQueue) {
                shouldReturnToSelectedQueue = false;
                index = currentSelectedQueueSongIndex;
            }
            Log.d(TAG, "Index " + index + " queue size " +  currentQueue.size());
            if (index >= 0 && index <= (currentQueue.size() - 1) && index < (currentQueue.size() - 1)) currentPlayingItem = currentQueue.get(index + 1);
            else currentPlayingItem = currentQueue.get(0);
            return true;
        }
        return false;
    }

    public boolean goToPreviousSong() {
        List<MediaSessionCompat.QueueItem> currentQueue = getCurrentQueue();
        Log.d(TAG, "Skipping song, queue size => " + currentQueue.size());
        if (currentQueue.size() > 0) {
            int index = currentPlayingItem == null ? -1 : QueueHelper.getItemIndexOnQueue(currentQueue, currentPlayingItem.getQueueId());
            if (shouldReturnToSelectedQueue) {
                shouldReturnToSelectedQueue = false;
                index = currentSelectedQueueSongIndex;
            }
            if (index >= 0 && index <= currentQueue.size() && index > 0) currentPlayingItem = currentQueue.get(index - 1);
            else currentPlayingItem = currentQueue.get(currentQueue.size() - 1);
            return true;
        }
        return false;
    }

    public void addSongToSelectedQueueByIndex(int index) {
        if (index >= 0 && index < defaultQueue.size()) {
            if (isPlayingDefaultQueue) {
                isPlayingDefaultQueue = false;
                if (currentPlayingItem != null) selectedQueue.add(0, currentPlayingItem);
            }

            MediaSessionCompat.QueueItem selectedItem = defaultQueue.get(index);
            selectedQueue.add(selectedItem);

            if (currentPlayingItem == null) {
                currentPlayingItem = selectedItem;
                listener.onNowPlayingChanged(currentPlayingItem);
            }
        }
    }

    private void emptySelectedQueue() {
        if (selectedQueue.size() > 0) selectedQueue.clear();
    }

    public void updateMetadata() {
        Log.d(TAG, "Updating queue metadata");
        MediaSessionCompat.QueueItem currentMusic = getCurrentMusic();
        if (currentMusic == null) {
            listener.onMetadataRetrieveError();
            return;
        }

        final String mediaId = currentMusic.getDescription().getMediaId();
        MediaMetadataCompat metadata = musicProvider.getTrackById(mediaId);
        if (metadata == null) throw new IllegalArgumentException("Invalid mediaId " + mediaId);

        listener.onMetadataChanged(metadata);
        if (isPlayingDefaultQueue) listener.onQueueUpdated(Constants.DEFAULT_QUEUE, defaultQueue);
        else listener.onQueueUpdated(Constants.SELECTED_QUEUE, selectedQueue);
        listener.onQueuePositionChanged(QueueHelper.getItemIndexOnQueue(defaultQueue, currentPlayingItem.getQueueId()));

        // handle artwork change (metadata.getDescription().getIconBitmap())
    }

    public MediaSessionCompat.QueueItem getCurrentMusic() {
        Log.d(TAG, "Getting current music");
        return currentPlayingItem;
    }

    public void initializeQueueWithFirstItem() {
        if (currentPlayingItem == null && defaultQueue.size() > 0) currentPlayingItem = defaultQueue.get(0);
    }

    private List<MediaSessionCompat.QueueItem> getCurrentQueue() {
        if (isPlayingDefaultQueue) return defaultQueue;
        return selectedQueue;
    }

    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);
        void onNowPlayingChanged(MediaSessionCompat.QueueItem nowPlaying);
        void onQueuePositionChanged(int position);
        void onPauseRequest();
    }
}
