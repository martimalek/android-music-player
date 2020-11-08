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

    public MusicProvider(Context context) {
        Log.d(TAG, "Inside MusicProvider constructor!");
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public List<Audio> getAudioList() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
        };

        Cursor audioCursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        Log.d(TAG, "Cursor created");

        List<Audio> audios = new ArrayList<>();

        try {
            if (audioCursor != null && audioCursor.moveToFirst()) {
                Log.d(TAG, "Cursor valid " + audioCursor.toString());

                int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int dataColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int displayNameColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                do {
                    Audio audio = new Audio(
                            audioCursor.getInt(idColumn),
                            audioCursor.getString(artistColumn),
                            audioCursor.getString(titleColumn),
                            audioCursor.getString(dataColumn),
                            audioCursor.getString(displayNameColumn),
                            audioCursor.getInt(durationColumn)
                    );
                    audios.add(audio);
                } while (audioCursor.moveToNext());

                Log.d(TAG, "Size: " + String.valueOf(audios.size()));
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception occurred " + e.getMessage());
        }
        return audios;
    }

    public ArrayList<MediaItem> getAllSongs() {
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
        ArrayList<MediaItem> tracks = new ArrayList<>();

        MediaItem mediaItem;

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
//                Log.d(TAG, "Track " + id + " uri= " + contentUri.toString());
//                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
//                        .setTitle(title)
//                        .setSubtitle(artist)
//                        .setMediaId(id)
//                        .setMediaUri(contentUri)
//                        .build();

                MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                        .setTitle(title)
                        .build();

                mediaItem = new MediaItem.Builder()
                        .setMediaId(id)
                        .setUri(contentUri)
                        .setMediaMetadata(mediaMetadata)
                        .build();

                //                mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                tracks.add(mediaItem);
            }
        } finally {
            tracksCursor.close();
        }

        Log.d(TAG, "All songs retrieved " + tracks.size());

        return tracks;
    }

    public MediaItem getTrackById(String mediaId) {
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

        MediaItem mediaItem = null;

        try {
            if (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                Long durationInMs = tracksCursor.getLong(4);
                Long trackNo = tracksCursor.getLong(5);

                Uri mediaUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
                MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                    .setTitle(title)
                    .build();

                Log.d(TAG, "URI => " + mediaUri.toString());

                mediaItem = new MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(mediaUri)
                        .setMediaMetadata(mediaMetadata)
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
}
