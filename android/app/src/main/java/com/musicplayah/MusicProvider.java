package com.musicplayah;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import java.util.ArrayList;
import java.util.List;

import static com.musicplayah.Constants.TAG;

public class MusicProvider {

    private Context context;

    public static String CUSTOM_METADATA_TRACK_SOURCE = "CUSTOM_METADATA_TRACK_SOURCE";

    public MusicProvider(Context context) {
        Log.d(TAG, "Inside MusicProvider constructor!");
        this.context = context;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getAllSongs() {
        Log.d(TAG, "Getting all songs...");
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Media.ARTIST;
        final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ARTIST_ID, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;

        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaBrowserCompat.MediaItem> tracks = new ArrayList<>();
        MediaBrowserCompat.MediaItem mediaItem;

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                long longId = tracksCursor.getLong(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String artist_id = tracksCursor.getString(3);
                String album = tracksCursor.getString(4);
                Long durationInMs = tracksCursor.getLong(5);
                Long trackNo = tracksCursor.getLong(6);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, longId);
                Log.d(TAG, "Track " + id + " uri= " + contentUri.toString());
                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                        .setTitle(title)
                        .setSubtitle(artist)
                        .setMediaId(id)
                        .setMediaUri(contentUri)
                        .build();

                mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                tracks.add(mediaItem);
            }
        } finally {
            tracksCursor.close();
        }

        Log.d(TAG, "All songs retrieved " + tracks.size());

        return tracks;
    }

    public MediaMetadataCompat getTrackById(String mediaId) {
        Log.d(TAG, "getTrackById");
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;
        if (mediaId != null && !mediaId.isEmpty()) {
            selection = MediaStore.Audio.Media._ID + "=?";
            selectionArgs = new String [1];
            selectionArgs[0] = mediaId;
        }
        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);

        MediaMetadataCompat mediaItem = null;

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                long longId = tracksCursor.getLong(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                Long durationInMs = tracksCursor.getLong(4);
                Long trackNumber = tracksCursor.getLong(5);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, longId);
                Log.d(TAG, "Track " + id + " uri= " + contentUri.toString());
                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                        .setTitle(title)
                        .setSubtitle(artist)
                        .setMediaId(id)
                        .setMediaUri(contentUri)
                        .build();

                Uri trackUri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));

                mediaItem = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationInMs) // in ms
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, trackUri.toString())
                        .build();
            }
        } finally {
            tracksCursor.close();
        }

        if (mediaItem != null) {
            Log.d(TAG, "Media => " + mediaItem.toString());
        } else Log.d(TAG, "MediaItem not found");

        return mediaItem;
    }

    public MediaMetadataCompat getRandomSongFromAllSongsOnDevice() {
        long randomSongId = 0;

        ContentResolver resolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.Audio.Media._ID };

        Cursor musicCursor = resolver.query(musicUri, projection, null, null, "RANDOM() LIMIT 1");
        if (musicCursor != null && musicCursor.moveToFirst()) {
            randomSongId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));
            musicCursor.close();
        }

        return getTrackById(Long.toString(randomSongId));
    }
}
