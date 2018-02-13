package com.htc.my.service;

/**
 * Created by lidongzhou on 18-2-13.
 */


import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.htc.my.files.AllSongs;
import com.htc.my.files.MySong;
import com.htc.my.files.PlayerConstants;

import java.util.ArrayList;
import java.util.Random;


public class MyMusicPlayerService extends Service {

    private static final String TAG = "HTC-M PlayerService";
    private static MediaPlayer mediaPlayer = null;
    private String music_path = "";
    private boolean isPause = false;
    private boolean isStart = false;
    private boolean isPlaying = false;
    private boolean isEnd = false;

    private MusicPlayerServiceBinder music_binder = new MusicPlayerServiceBinder();
    private MySong current_song = null;
    private int seek_position = -1;   // 标记上次暂停播放音乐时,已经播放到的位置
    private int song_id = 0;
    private int play_song_model = 0;  // 0 顺序 1 随机 2 单曲
    private static ArrayList<MySong> allsongs;
    private int music_amount = 0;
    private ArrayList<Integer> randNumbers = new ArrayList<Integer>();

    @Override
    public IBinder onBind(Intent arg0){
        return music_binder;
    }

    public class MusicPlayerServiceBinder extends Binder {
        // 获取当前在后台上播放的音乐的所有信息
        public MySong getSongOnService() {
            return current_song;
        }

        // 判断后台上的音乐是否在播放
        public boolean isPlayingOnService() {
            return isPlaying;
        }

        // 判断后台的音乐是否已经开始
        public boolean isStartOnService() {
            return isStart;
        }

        // 判断后台音乐是否播放结束
        public boolean isEndOnService() {
            boolean temp = isEnd;
            isEnd = false;
            return temp;
        }

        // 获取当前音乐播放到的位置
        public int getMusicCurrentPosition() {
            int position = 0;
            if (mediaPlayer != null) {
                position = mediaPlayer.getCurrentPosition();
            }
            return position;
        }

        // 设定当前音乐位置
        public void seekMusicPosition(int position) {
            seek_position = position;
        }

        // 将指定当前音乐播放的位置
        public void musicSeekTo() {
            if (mediaPlayer != null) {

                mediaPlayer.seekTo(seek_position);
                if (isPlaying) {
                    seek_position = -1;
                }
            }
        }

        public void setSongIdOnService(int id) {
            song_id = id;
        }

        public int getSongIdOnService() {
            return song_id;
        }

        public ArrayList<MySong> getAllSongs(){
            return allsongs;
        }

        public void pauseSongOnService() {
            if (current_song != null) {
                my_music_pause();
            }
        }

        public void continueSongOnService() {
            if (current_song != null){
                my_music_continue();
            }
        }

        public void setPlayMusicModel(int model){
            play_song_model = model;
        }

        // 产生随机数
        public int rand_music_id(){
            Random rand = new Random();

            int temp = rand.nextInt(music_amount)%music_amount;
            while(randNumbers.contains(temp)){
                temp = rand.nextInt(music_amount)%music_amount;
            }
            randNumbers.add(temp);

            if(randNumbers.size() == music_amount) {
                randNumbers.clear();
            }
            return temp;
        }

        public int getPlayMusicModel(){
            return play_song_model;
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();
        if(allsongs==null) {
            allsongs = (ArrayList<MySong>) AllSongs.getAllSongs(MyMusicPlayerService.this);
            music_amount = allsongs.size();
        }
        Log.i(TAG," on create");
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){

        if(intent != null) {

            if (mediaPlayer == null) mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (play_song_model == 0) {
                        ++song_id;
                    } else if (play_song_model == 1) {
                        song_id = music_binder.rand_music_id();
                    }

                    song_id = (song_id) % allsongs.size();
                    current_song = allsongs.get(song_id);
                    music_path = current_song.getMusicPath();
                    my_music_play();
                    my_music_play();
                    isEnd = true;
                }
            });
            Log.i(TAG, " on start");
            int music_play_operation = intent.getIntExtra("MSG", 0);

            // 从头开始播放音乐
            if (music_play_operation == PlayerConstants.MSG_PLAY) {

                current_song = intent.getParcelableExtra("song");
                music_path = current_song.getMusicPath();
                my_music_play();
                Log.i(TAG, "MSG_PLAY");

            }
            // 暂停播放音乐
            else if (music_play_operation == PlayerConstants.MSG_PAUSE) {

                my_music_pause();
                Log.i(TAG, "MSG_PAUSE");

            }
            // 从暂停中恢复播放音乐
            else if (music_play_operation == PlayerConstants.MSG_CONTINUE) {

                my_music_continue();
                Log.i(TAG, "MSG_CONTINUE");

            } else {
                Log.i(TAG, " has strange things happened.");
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }

    private void my_music_play(){
        try{
            mediaPlayer.reset();    // 可以使播放器从Error状态中恢复过来，重新会到Idle状态
            mediaPlayer.setDataSource(music_path);
            mediaPlayer.prepare();  // 准备(同步)
            if(seek_position>=0){
                mediaPlayer.start();
//                mediaPlayer.pause();
                mediaPlayer.seekTo(seek_position);
                Log.i("seek_position",seek_position+"");
                seek_position=-1;
            }else{
                mediaPlayer.start();
                Log.i("start seek_position",seek_position+"");
            }
            isPause = false;
            isPlaying = true;
            isStart = true;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void my_music_pause(){
        if (mediaPlayer!=null) {
            mediaPlayer.pause();

            isPause=true;
            isPlaying = false;
        }
    }

    private void my_music_continue(){
        if(mediaPlayer!=null){
            if(seek_position>=0){
                mediaPlayer.seekTo(seek_position);
                mediaPlayer.start();
                Log.i("seek_position",seek_position+"");
                seek_position=-1;
            }else{
                mediaPlayer.start();

            }
            isPause = false;
            isPlaying = true;
        }
    }


    @Override
    public void onDestroy(){
        if (mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }





}
