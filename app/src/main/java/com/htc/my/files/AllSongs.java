package com.htc.my.files;

/**
 * Created by lidongzhou on 18-2-13.
 */

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AllSongs {


    public static List<MySong> getAllSongs(Context context){
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,null,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        List<MySong> allSongs = new ArrayList<MySong>();
        for(int i=0; i<cursor.getCount(); ++i){
            cursor.moveToNext();
            MySong song = new MySong();
            Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String author = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            Long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            Long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            String music_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            Log.i("allsong","allsong " + title + isMusic);
            Log.i("allsong","allsong " + title);
            song.setAuthor(author);
            song.setSize(size);
            song.setDuration(duration);
            song.setMusicPath(music_path);
            song.setId(id);
            song.setTitle(title);
            allSongs.add(song);
        }

        return allSongs;
    }

    public static List<HashMap<String, String>> getMusicMaps(List<MySong> songs) { // {{{
        List<HashMap<String, String>> allSongs = new ArrayList<HashMap<String, String>>();
        for (Iterator<MySong> iterator = songs.iterator(); iterator.hasNext();) {
            MySong song = (MySong) iterator.next();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("title", song.getTitle());
            map.put("Artist", song.getAuthor());
            map.put("duration", formatTime(song.getDuration()));
            map.put("size", String.valueOf(song.getSize()));
            map.put("url", song.getMusicPath());
            allSongs.add(map);
        }
        return allSongs;
    }

    public static String formatTime(long time) { // {{{
        String min = time / (1000 * 60) + "";
        String sec = time % (1000 * 60) + "";
        if (min.length() < 2) {
            min = "0" + time / (1000 * 60) + "";
        } else {
            min = time / (1000 * 60) + "";
        }
        if (sec.length() == 4) {
            sec = "0" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 3) {
            sec = "00" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 2) {
            sec = "000" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 1) {
            sec = "0000" + (time % (1000 * 60)) + "";
        }
        return min + ":" + sec.trim().substring(0, 2);
    }
}
