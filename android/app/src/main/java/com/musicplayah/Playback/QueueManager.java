package com.musicplayah.Playback;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.musicplayah.Constants;
import com.musicplayah.MusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {
    private static final String TAG = Constants.TAG;

    private MusicProvider musicProvider;
    private Context context;
    private Resources resources;
    private List<MediaSessionCompat.QueueItem> defaultQueue;
    private List<MediaSessionCompat.QueueItem> selectedQueue;
    private MetadataUpdateListener listener;

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

    public List<MediaSessionCompat.QueueItem> getCurrentQueue() {
            return defaultQueue;
    }

    private List<MediaSessionCompat.QueueItem> getRandomQueue(MusicProvider musicProvider, int numberOfSongs) {
        List<MediaMetadataCompat> result = new ArrayList<>(numberOfSongs);

        MediaMetadataCompat randomlyChosenTrack;
        for (int i = 0 ; i < numberOfSongs ; i++) {
            randomlyChosenTrack = musicProvider.getRandomSongFromAllSongsOnDevice();
            result.add(randomlyChosenTrack);
            if (randomlyChosenTrack == null) {
                Log.d(TAG, "randomlyChosenTrack is NULL");
            } else {
                Log.d(TAG, "randomly chosen track: " + randomlyChosenTrack.getDescription().getTitle());
            }
        }

        Log.d(TAG, "getRandomQueue: result.size=" + result.size());

        return convertToQueue(result);
    }

    public void fillQueueWithAllSongs() {
        int currentQueueSize = defaultQueue.size();
        Log.d(TAG, "fillQueueWithAllSongs, current size = " + defaultQueue.size());

        if (currentQueueSize < 10) // TODO: Should this value come from somewhere ??
        {
            ArrayList<MediaMetadataCompat> allSongsOnDevice = musicProvider.getAllSongs();

            List<MediaSessionCompat.QueueItem> newTracks = convertToQueue(allSongsOnDevice);

            Log.d(TAG, "Adding " +  newTracks.size() + " new songs to the queue");
            defaultQueue.addAll(newTracks);
        }

        if (currentPlayingItem == null) currentPlayingItem = defaultQueue.get(0);

        listener.onQueueUpdated("AlbumTitle", defaultQueue);
    }

    public void setCurrentQueueItem(long queueId) {
        int index = getItemIndexOnQueue(defaultQueue, queueId);
        if (index >= 0 && index < defaultQueue.size()) {
            currentPlayingItem = defaultQueue.get(index);
            listener.onNowPlayingChanged(currentPlayingItem);
        }
    }

    public static int getItemIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue, long queueId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (queueId == item.getQueueId()) return index;
            index++;
        }
        return -1;
    }

    public void fillRandomQueue() {
        int currentQueueSize = defaultQueue.size();
        Log.d(TAG, "fillRandomQueue, current size = " + defaultQueue.size());

        if (currentQueueSize < 10) // TODO: Should this value come from somewhere ??
        {
            List<MediaSessionCompat.QueueItem> newTracks = getRandomQueue(musicProvider, 10); // Same ?

            Log.d(TAG, "Adding " +  newTracks.size() + " new songs to the queue");
            defaultQueue.addAll(newTracks);
        }

        if (currentPlayingItem == null) currentPlayingItem = defaultQueue.get(0);

        listener.onQueueUpdated("AlbumTitle", defaultQueue);
    }

    public boolean goToNextSong() {
        Log.d(TAG, "Skipping song, queue size => " + defaultQueue.size());
        if (defaultQueue.size() > 0) {
            int index = getItemIndexOnQueue(defaultQueue, currentPlayingItem.getQueueId());
            Log.d(TAG, "Index " + index + " queue size " +  defaultQueue.size());
            if (index >= 0 && index <= (defaultQueue.size() - 1)) {
                if (index < (defaultQueue.size() - 1)) {
                    currentPlayingItem = defaultQueue.get(index + 1);
                } else currentPlayingItem = defaultQueue.get(0);
            }
            return true;
        }
        return false;
    }

    public boolean goToPreviousSong() {
        Log.d(TAG, "Skipping song, queue size => " + defaultQueue.size());
        if (defaultQueue.size() > 0) {
            int index = getItemIndexOnQueue(defaultQueue, currentPlayingItem.getQueueId());
            if (index >= 0 && index <= defaultQueue.size()) {
                if (index > 0) {
                    currentPlayingItem = defaultQueue.get(index - 1);
                } else currentPlayingItem = defaultQueue.get(defaultQueue.size() - 1);
            } // TODO: else throw custom not found exception
            return true;
        }
        return false;
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

        // handle artwork change (metadata.getDescription().getIconBitmap())
    }

    public static int count = 0;

    private static List<MediaSessionCompat.QueueItem> convertToQueue(Iterable<MediaMetadataCompat> tracks) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();

        for (MediaMetadataCompat track : tracks) {
            if (track != null) {
                MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.getDescription().getMediaId())
                        .build();

                MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(trackCopy.getDescription(), count++);
                queue.add(item);
            }
        }
        return queue;
    }

    public MediaSessionCompat.QueueItem getCurrentMusic() {
        return currentPlayingItem;
    }

    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);
        void onNowPlayingChanged(MediaSessionCompat.QueueItem nowPlaying);
        void onPauseRequest();
    }
}
