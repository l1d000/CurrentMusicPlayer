package com.htc.my.mymusicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.htc.my.files.AllSongs;
import com.htc.my.files.PlayerConstants;
import com.htc.my.service.MyMusicPlayerService;


import com.htc.my.files.MySong;


import java.util.ArrayList;

/**
 * Created by Lidong Zhou on 18-2-12.
 */

public class MusicInfoActivity extends AppCompatActivity {
    private static AppCompatImageView play_pause_imageView; // 播放当前音乐
    private AppCompatImageView next_song_imageView;         // 切换至下一首音乐
    private AppCompatImageView last_song_imageView;         // 切换至上一首音乐
    private AppCompatImageView favorite_song_imageView;     // 标记喜欢歌曲
    private AppCompatImageView play_model_imageView;        // 播放模式
    private AppCompatSeekBar music_process_seek_bar;        // 音乐播放进度条
    private TextView music_title;                           // 音乐标题
    private TextView music_author;                          // 音乐作者
    private TextView music_current_time;                    // 当前音乐播放时间
    private TextView music_end_time;                        // 音乐时长
    private RadioButton music_back_list;                        // 音乐时长

    private int music_amount = 0 ;
    private ArrayList<MySong> allSongs;

    private ShadowImageView shadowImageView;

    private boolean isPlaying = false;  // 正在播放
    private boolean isStart = true;     // 刚刚开始
    private boolean isPause=false;      // 暂停
    private boolean isEnd=false;        // 结束
    private boolean serviceSate=false;  // 音乐后台状态
    private Intent interactionIntent;   // 与MainActivity进行信息的交互

    private String log_name = "qingsong";                   // 监听事件者
    private MyMusicPlayerService.MusicPlayerServiceBinder music_binder;
    private MySong currentSong;
    private MySong mainSong;
    private static int currentSong_id=-1;
    private int currentPositon= 0;
    private SeekBarThread seekBarThread = new SeekBarThread();
    private boolean seekThreadFlag = true;
    private int play_music_model = 0;
    private MediaSessionManager  mMediaSessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_music);
        Log.i(log_name," on create");
        this.createLinkWithPlayerService();
        overridePendingTransition(R.anim.enter_dapanzi_from_bottom,R.anim.exit_dapanzi_to_bottom);
        mMediaSessionManager =  MediaSessionManager.getInstance();
        seekBarThread.start();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        music_binder.setSongIdOnService(currentSong_id);
        Log.i(log_name,log_name+" on destroy");
        unbindService(connection);
        seekThreadFlag = false;
        this.finish();

    }

    // 获取所有的音乐列表
    private void getAllSongs(){
        interactionIntent = getIntent();
        allSongs = music_binder.getAllSongs();
        music_amount = allSongs.size();
        mainSong = music_binder.getSongOnService();
        currentSong_id = music_binder.getSongIdOnService();
        play_music_model = music_binder.getPlayMusicModel();
    }


    // 建立与PlayerService的连接
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            music_binder = (MyMusicPlayerService.MusicPlayerServiceBinder) service;
            getAllSongs();
            findAllViewById();

            currentSong=music_binder.getSongOnService();
            serviceSate=music_binder.isPlayingOnService();
            if(currentSong_id ==-1) currentSong_id=music_binder.getSongIdOnService();
            if(currentSong==null) currentSong=allSongs.get(currentSong_id);
            else {
                currentPositon = music_binder.getMusicCurrentPosition();
            }
            Log.i(log_name,log_name + currentSong);

            setAllViewListener();
            initAllView();
        }
    };
    // 建立与PlayerService的连接
    private void createLinkWithPlayerService(){
        Intent bindIntent = new Intent(this, MyMusicPlayerService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    // 根据currentSong初始化界面
    private void initAllView(){
        music_title.setText(currentSong.getTitle());
        music_author.setText(currentSong.getAuthor());
        music_current_time.setText(AllSongs.formatTime(currentPositon));
        music_end_time.setText(AllSongs.formatTime(currentSong.getDuration()));
        Log.i(log_name,log_name+serviceSate);
        Log.i(log_name,"thread" + currentSong.getTitle());

        if(serviceSate) {
            isStart=false;
            isPlaying=true;
            isPause=false;
            play_pause_imageView.setImageResource(R.drawable.ic_pause);
            shadowImageView.resumeRotateAnimation();
        }else if(music_binder.isStartOnService()){
            music_binder.seekMusicPosition(currentPositon);
        }
        initSeekBar(currentPositon,currentSong.getDuration());

        if(play_music_model == 0) {
            play_model_imageView.setImageResource(R.drawable.ic_play_mode_loop);
        }else if (play_music_model == 1){
            play_model_imageView.setImageResource(R.drawable.ic_play_mode_shuffle);
        }else if(play_music_model == 2){
            play_model_imageView.setImageResource(R.drawable.ic_play_mode_single);
        }

    }

    // 初始化进度条
    private void initSeekBar(int start,Long end){
        music_process_seek_bar.setMax(end.intValue());
        music_process_seek_bar.setProgress(start);
        music_process_seek_bar.setEnabled(true);

    }

    // 实现进度条的动态控制
    private class SeekBarThread extends Thread{
        @Override
        public void run(){
            while(seekThreadFlag){
                handler.sendMessage(new Message());
                try{
                    Thread.sleep(100);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            if (music_binder.isPlayingOnService()) {
                long currentPosition = music_binder.getMusicCurrentPosition();
                String text_current = AllSongs.formatTime(currentPosition);
                music_current_time.setText(text_current);
                int progress = new Long(currentPosition).intValue();
                music_process_seek_bar.setProgress(progress);
                if(music_binder.isEndOnService()) {
                    shadowImageView.pauseRotateAnimation();
                    play_pause_imageView.setImageResource(R.drawable.ic_play);
                    Log.i(log_name,"tread in seek");
                    currentSong=music_binder.getSongOnService();
                    currentSong_id=music_binder.getSongIdOnService();
                    music_title.setText(currentSong.getTitle());
                    music_author.setText(currentSong.getAuthor());
                    music_current_time.setText(AllSongs.formatTime(currentPositon));
                    music_end_time.setText(AllSongs.formatTime(currentSong.getDuration()));
                    play_pause_imageView.setImageResource(R.drawable.ic_pause);
                    shadowImageView.resumeRotateAnimation();
                    initSeekBar(currentPositon,currentSong.getDuration());
                }
            }
        };

    };

    // 获取所有的组件
    private void findAllViewById(){
        //获取界面上的控件
        play_pause_imageView = (AppCompatImageView)findViewById(R.id.button_play_toggle);
        play_model_imageView = (AppCompatImageView)findViewById(R.id.button_play_mode_toggle);
        next_song_imageView = (AppCompatImageView)findViewById(R.id.button_play_next);
        last_song_imageView = (AppCompatImageView)findViewById(R.id.button_play_last);
        favorite_song_imageView = (AppCompatImageView)findViewById(R.id.button_favorite_toggle);
        shadowImageView = (ShadowImageView)findViewById(R.id.image_view_album);
        music_process_seek_bar = (AppCompatSeekBar)findViewById(R.id.seek_bar);
        music_title = (TextView)findViewById(R.id.text_view_name);
        music_author = (TextView)findViewById(R.id.text_view_artist);
        music_current_time = (TextView)findViewById(R.id.text_view_progress);
        music_end_time = (TextView)findViewById(R.id.text_view_duration);
        music_back_list = (RadioButton)findViewById(R.id.music_list_back);
        mMediaSessionManager.MediaSessionInitMusicInfo(play_pause_imageView,shadowImageView);
    }

    // 设置所有组件的监听事件
    private void setAllViewListener(){
        // 设置"大盘子"界面中关于音乐文件的控制事件
        play_pause_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isStart){
                    my_music_start_play();
                }else if(isPlaying){
                    my_music_pause();
                }else if(isPause){
                    my_music_continue();
                }else{
                    Log.i(log_name,"My Music produce a unkown problem!");
                }
            }
        });

        next_song_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                my_music_next();
            }
        });

        last_song_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                my_music_last();
            }
        });

        // (1)当音乐在播放时,滑动进度条则先改变,再切换至播放位置
        // (2)当音乐暂停时,则改变滑动进度条,当再次播放时,再切换至播放位置
        music_process_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(fromUser) {
                    music_binder.seekMusicPosition(progress);
                    music_current_time.setText(AllSongs.formatTime(progress));
                    Log.i(log_name,progress+"");
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                music_binder.musicSeekTo();
                Log.i(log_name,"stop touch");
            }
        });

        play_model_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent=new Intent(DaPanZiActivity.this, MyMusicPlayerService.class);
//                intent.putExtra("song",currentSong);
//                intent.putExtra("MSG",PlayerConstants.MSG_PLAY);
//                startService(intent);
                  if(play_music_model == 0) {
                      play_music_model = 1;
                      play_model_imageView.setImageResource(R.drawable.ic_play_mode_shuffle);
                      music_binder.setPlayMusicModel(1);
                  }else if (play_music_model == 1){
                      play_music_model = 2;
                      play_model_imageView.setImageResource(R.drawable.ic_play_mode_single);
                      music_binder.setPlayMusicModel(2);
                  }else if(play_music_model == 2){
                      play_music_model = 0;
                      play_model_imageView.setImageResource(R.drawable.ic_play_mode_loop);
                      music_binder.setPlayMusicModel(0);
                  }
            }
        });

        music_back_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // 开始播放音乐
    private void my_music_start_play(){
        // 将音乐播放功能交给后台
        Intent intent=new Intent(MusicInfoActivity.this, MyMusicPlayerService.class);
        intent.putExtra("song",currentSong);
        intent.putExtra("MSG", PlayerConstants.MSG_PLAY);
        startService(intent);
        currentPositon = 0;
        isPause=false;
        isPlaying=true;
        isStart=false;
        music_binder.setSongIdOnService(currentSong_id);
        play_pause_imageView.setImageResource(R.drawable.ic_pause);
        mMediaSessionManager.updateMetaData(currentSong);
        // 开始"大盘子"的运动
        if(shadowImageView!=null) shadowImageView.startRotateAnimation();
        Log.i(log_name , log_name + " start playing music.");
    }

    //音乐暂停
    private void my_music_pause(){
        isPlaying=false;
        isPause = true;

        // 将音乐暂停功能交给后台
        Intent intent=new Intent(MusicInfoActivity.this, MyMusicPlayerService.class);
        intent.putExtra("MSG",PlayerConstants.MSG_PAUSE);
        startService(intent);

        play_pause_imageView.setImageResource(R.drawable.ic_play);
        // 暂停"大盘子"的活动
        shadowImageView.pauseRotateAnimation();
        Log.i(log_name,log_name+ " pause playing music.");
    }

    //音乐继续播放
    private void my_music_continue(){
        isPlaying=true;
        isPause=false;


        // 将音乐暂停功能交给后台
        Intent intent=new Intent(MusicInfoActivity.this, MyMusicPlayerService.class);
        intent.putExtra("MSG",PlayerConstants.MSG_CONTINUE);
        startService(intent);

        play_pause_imageView.setImageResource(R.drawable.ic_pause);
        // 恢复"大盘子"的活动
        shadowImageView.resumeRotateAnimation();
        Log.i(log_name,log_name + " continue playing music.");

    }

    // 播放下一首音乐
    private void my_music_next(){
        if(play_music_model == 0) {
            Log.i(log_name, log_name + " go to next " + ((currentSong_id + 1) % music_amount));
            ++currentSong_id;
        }else if(play_music_model == 1){
            currentSong_id = music_binder.rand_music_id();
        }
        currentSong=allSongs.get((currentSong_id)%music_amount);
        this.initAllView();
        my_music_start_play();
    }

    // 播放前一首音乐
    private void my_music_last(){
        if(play_music_model == 0) {
            int last = currentSong_id+music_amount;
            last = (--last)%music_amount;
            currentSong_id = last;
        }else if(play_music_model == 1){
            currentSong_id = music_binder.rand_music_id();
        }

        Log.i(log_name,log_name + " go to last " + currentSong_id);
        currentSong=allSongs.get(currentSong_id);
        this.initAllView();
        my_music_start_play();
    }

//    // 产生随机数
//    private int rand_music_id(){
//        Random rand = new Random();
//        int temp = rand.nextInt(music_amount)%music_amount;
//        while(temp==currentSong_id && music_amount >=2){
//            temp = rand.nextInt(music_amount)%music_amount;
//        }
//        return temp;
//    }

}
