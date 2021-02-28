package com.musicplayah.Playback;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.musicplayah.Utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {
    private static final String TAG = Constants.TAG;

    private final MusicProvider musicProvider;
    private final Context context;
    private Resources resources;
    private final List<MediaSessionCompat.QueueItem> defaultQueue;
    private List<MediaSessionCompat.QueueItem> selectedQueue;
    private boolean isPlayingDefaultQueue = true;
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
        int currentQueueSize = defaultQueue.size();
        Log.d(TAG, "fillQueueWithAllSongs, current size = " + defaultQueue.size());

        if (currentQueueSize < 10)
        {
            ArrayList<MediaMetadataCompat> allSongsOnDevice = musicProvider.getAllSongs();

            List<MediaSessionCompat.QueueItem> newTracks = QueueHelper.convertToQueue(allSongsOnDevice);

            Log.d(TAG, "Adding " +  newTracks.size() + " new songs to the queue");
            defaultQueue.addAll(newTracks);
        }

        listener.onQueueUpdated("AlbumTitle", defaultQueue);
    }

    public void setCurrentQueueItem(long queueId) {
        int index = QueueHelper.getItemIndexOnQueue(defaultQueue, queueId);
        if (index >= 0 && index < defaultQueue.size()) {
            if (!isPlayingDefaultQueue) {
                isPlayingDefaultQueue = true;
                emptySelectedQueue();
            }
            currentPlayingItem = defaultQueue.get(index);
            listener.onNowPlayingChanged(currentPlayingItem);
        }
    }

    public boolean goToNextSong() {
        List<MediaSessionCompat.QueueItem> currentQueue = getCurrentQueue();
        Log.d(TAG, "Skipping song, queue size => " + currentQueue.size());
        if (currentQueue.size() > 0) {
            if (currentPlayingItem == null) currentPlayingItem = defaultQueue.get(0);
            int index = QueueHelper.getItemIndexOnQueue(currentQueue, currentPlayingItem.getQueueId());
            Log.d(TAG, "Index " + index + " queue size " +  currentQueue.size());
            if (index >= 0 && index <= (currentQueue.size() - 1)) {
                if (index < (currentQueue.size() - 1)) currentPlayingItem = currentQueue.get(index + 1);
                else currentPlayingItem = currentQueue.get(0);
            }
            return true;
        }
        return false;
    }

    public boolean goToPreviousSong() {
        List<MediaSessionCompat.QueueItem> currentQueue = getCurrentQueue();
        Log.d(TAG, "Skipping song, queue size => " + currentQueue.size());
        if (currentQueue.size() > 0) {
            if (currentPlayingItem == null) currentPlayingItem = defaultQueue.get(0);
            int index = QueueHelper.getItemIndexOnQueue(currentQueue, currentPlayingItem.getQueueId());
            if (index >= 0 && index <= currentQueue.size()) {
                if (index > 0)  currentPlayingItem = currentQueue.get(index - 1);
                else currentPlayingItem = currentQueue.get(currentQueue.size() - 1);
            }
            return true;
        }
        return false;
    }

    public void addSongToSelectedQueueByIndex(int index) {
        Log.d(TAG, "setSongToSelectedQueueByIndex " + index);
        if (index >= 0 && index < defaultQueue.size()) {

            if (isPlayingDefaultQueue) {
                isPlayingDefaultQueue = false;

                if (currentPlayingItem != null) selectedQueue.add(0, currentPlayingItem);
            }

            MediaSessionCompat.QueueItem selectedItem = defaultQueue.get(index);
            selectedQueue.add(selectedItem);
            Log.d(TAG, "Queue size " + selectedQueue.size());
            Log.d(TAG, "currentPlayingItem " + currentPlayingItem);

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
        listener.onQueueUpdated("AlbumTitle", defaultQueue);
        listener.onQueuePositionChanged(QueueHelper.getItemIndexOnQueue(defaultQueue, currentPlayingItem.getQueueId()));

        // handle artwork change (metadata.getDescription().getIconBitmap())
    }

    public MediaSessionCompat.QueueItem getCurrentMusic() {
        if (currentPlayingItem == null && defaultQueue.size() > 0) currentPlayingItem = defaultQueue.get(0);

        return currentPlayingItem;
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
