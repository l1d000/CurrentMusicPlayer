package com.htc.my.mymusicplayer;

/**
 * Created by lidongzhou on 18-2-13.
 */

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.my.files.AllSongs;
import com.htc.my.files.PlayerConstants;
import com.htc.my.service.MyMusicPlayerService;
import com.htc.my.files.HtcSong;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HTC-M MainActivity";
    private Toolbar main_toolbar;

    private RecyclerView recyclerView;               // 音乐列表
    private ArrayList<HtcSong> allSongs;              // 储存所有的音乐
    private ArrayList<Boolean> allSongs_state = new ArrayList<Boolean>();   // 标记所有音乐列表的状态
    private MyMusicPlayerService.MusicPlayerServiceBinder music_binder;     // 与musicService进行交互的通道
    private HtcSong current_song_OnService = null;                           // 标记当前正在musicService上播放的音乐
    private ProgressBar progressBar;
    private AppCompatImageView image_view_play_toggle;
    private ImageView image_view_logo;
    private TextView music_text_name;
    private TextView music_text_author;
    private boolean current_song_state = false;
    private RelativeLayout layout_root;
    private MediaSessionManager  mMediaSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaSessionManager =  MediaSessionManager.createInstance(MainActivity.this);
        requestReadExternalPermission();
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.i("main","on create");

    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(music_binder!=null&&main_toolbar!=null && music_binder.getSongOnService()!=null) {
            HtcSong temp = music_binder.getSongOnService();
            main_toolbar.setTitle(temp.getTitle());
            music_text_name.setText(temp.getTitle());
            music_text_author.setText(temp.getAuthor());
            mMediaSessionManager.updateMetaData(temp);
            Log.e(TAG,"onActivityResult");

        }

        Log.i("main","requestCode: "+requestCode);
        Log.i("main","result: "+resultCode);
        // 此处应判别传回消息的Actitvity为videoActivity是才执行
        if(requestCode==2 && music_binder!=null){
            if(music_binder.getSongOnService()!=null){
                music_binder.continueSongOnService();
            }
        }

        if(music_binder!=null && music_binder.isPlayingOnService()) {
            image_view_play_toggle.setImageResource(R.drawable.ic_pause_1);
            current_song_state = true;
        }else{
            image_view_play_toggle.setImageResource(R.drawable.ic_play_1);
            current_song_state = false;
        }

    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        mMediaSessionManager.release();
        unbindService(connection);
        handler.removeMessages((new Message()).what=1);
        this.finish();
    }

    // 获取所有的组件
    private void  findAllViewById(){
        main_toolbar = (Toolbar) findViewById(com.htc.my.mymusicplayer.R.id.main_toolbar);
        main_toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(main_toolbar);

        recyclerView = (RecyclerView)findViewById(com.htc.my.mymusicplayer.R.id.recycler_view);
        progressBar = (ProgressBar)findViewById(com.htc.my.mymusicplayer.R.id.progress_bar);

        image_view_play_toggle = (AppCompatImageView) findViewById(com.htc.my.mymusicplayer.R.id.image_view_play_toggle);
        music_text_name = (TextView) findViewById(com.htc.my.mymusicplayer.R.id.music_text_view_name);
        music_text_author = (TextView) findViewById(com.htc.my.mymusicplayer.R.id.music_text_view_artist);
        layout_root = (RelativeLayout) findViewById(com.htc.my.mymusicplayer.R.id.layout_root);
        image_view_logo =(ImageView)findViewById(R.id.image_view_logo);
        mMediaSessionManager.MediaSessionInitMainActivity(image_view_play_toggle);
        Message msg = new Message();
        msg.what = 2;
        handler.sendMessage(msg);
    }

    // 设置所有组件的监听事件
    private void setAllViewListener(){

        image_view_play_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(music_binder.isPlayingOnService()){
                    music_binder.pauseSongOnService();
                    image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_play_1);
                }else{
                    HtcSong temp = music_binder.getSongOnService();

                    if(temp != null){
                        Intent service_intent = new Intent(MainActivity.this, MyMusicPlayerService.class);
                        service_intent.putExtra("MSG", PlayerConstants.MSG_CONTINUE);
                        startService(service_intent);
                        current_song_state = true;
                        image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_pause_1);
                    }else{
                        Intent service_intent = new Intent(MainActivity.this, MyMusicPlayerService.class);
                        service_intent.putExtra("song", allSongs.get(0));
                        service_intent.putExtra("MSG", PlayerConstants.MSG_PLAY);
                        music_binder.seekMusicPosition(-1);
                        startService(service_intent);
                        allSongs_state.set(0, true);
                        current_song_state = true;
                        image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_pause_1);
                    }
                }
            }
        });

        layout_root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = 0;
                current_song_OnService = music_binder.getSongOnService();
                allSongs_state.set(position,music_binder.isPlayingOnService());
                Log.i("main",""+allSongs_state.get(position));


//                main_toolbar.setSubtitle(allSongs.get(position).getAuthor());
                if(current_song_OnService==null){
                    Intent service_intent = new Intent(MainActivity.this, MyMusicPlayerService.class);
                    service_intent.putExtra("song", allSongs.get(position));
                    service_intent.putExtra("MSG", PlayerConstants.MSG_PLAY);
                    music_binder.seekMusicPosition(-1);
                    startService(service_intent);
                    allSongs_state.set(position, true);
                    current_song_state = true;
                    image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_pause_1);
                } else{
                    position = music_binder.getSongIdOnService();
                    Intent to_dapanzi_intent;                // 与"大盘子" 进行信息交互
                    to_dapanzi_intent = new Intent(MainActivity.this, MusicInfoActivity.class);
                    to_dapanzi_intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    to_dapanzi_intent.putParcelableArrayListExtra("allsongs",allSongs);
                    to_dapanzi_intent.putExtra("main_song",current_song_OnService);
                    to_dapanzi_intent.putExtra("song_id",position);
                    startActivityForResult(to_dapanzi_intent,1);
                }
            }
        });

    }

    // 导入音乐文件
    private void loadMusicFiles(){
        MusicPlayerAdapter musicPlayerAdapter;   // 设置音乐列表中每一列的状态
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(musicPlayerAdapter=new MusicPlayerAdapter());
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        musicPlayerAdapter.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(View view, int position)
            {
            /*    Toast.makeText(MainActivity.this, allSongs.get(position).getTitle(),
                        Toast.LENGTH_SHORT).show();*/
                current_song_OnService = music_binder.getSongOnService();
                allSongs_state.set(position,music_binder.isPlayingOnService());

                HtcSong temp = allSongs.get(position);
                main_toolbar.setTitle(temp.getTitle());
                music_text_name.setText(temp.getTitle());
                music_text_author.setText(temp.getAuthor());
                mMediaSessionManager.updateMetaData(temp);
                Log.e(TAG,"onItemClick");

//                main_toolbar.setSubtitle(allSongs.get(position).getAuthor());
                if(current_song_OnService!=null){
                    if(!current_song_OnService.getTitle().equals(allSongs.get(position).getTitle())){
                        allSongs_state.set(position,false);
                        current_song_OnService = allSongs.get(position);
                    }else {
                        allSongs_state.set(position,true);
                    }
                }
                Log.i("main",""+allSongs_state.get(position));
                music_binder.setSongIdOnService(position);
                if(!allSongs_state.get(position)) {
                    Intent service_intent = new Intent(MainActivity.this, MyMusicPlayerService.class);
                    service_intent.putExtra("song", allSongs.get(position));
                    service_intent.putExtra("MSG", PlayerConstants.MSG_PLAY);
                    music_binder.seekMusicPosition(-1);
                    startService(service_intent);
                    allSongs_state.set(position, true);
                    current_song_state = true;
                    image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_pause_1);
                }else{
                    Intent to_dapanzi_intent;                // 与"大盘子" 进行信息交互
                    to_dapanzi_intent = new Intent(MainActivity.this, MusicInfoActivity.class);
                    to_dapanzi_intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    to_dapanzi_intent.putParcelableArrayListExtra("allsongs",allSongs);
                    to_dapanzi_intent.putExtra("main_song",current_song_OnService);
                    to_dapanzi_intent.putExtra("song_id",position);
                    startActivityForResult(to_dapanzi_intent,1);
                }
            }

            @Override
            public void onItemLongClick(View view, int position)
            {
                Toast.makeText(MainActivity.this, allSongs.get(position).getAuthor(),
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    // 自定义RecycleView内的点击事件(1)
    public interface OnItemClickListener
    {
        void onItemClick(View view, int position);
        void onItemLongClick(View view , int position);
    }

    class MusicPlayerAdapter extends RecyclerView.Adapter<MusicPlayerAdapter.MyMusicPlayerViewHolder>
    {
        // 自定义RecycleView内的点击事件(2)
        private OnItemClickListener mOnItemClickListener;

        // 自定义RecycleView内的点击事件(3)
        public void setOnItemClickListener(OnItemClickListener mOnItemClickListener)
        {
            this.mOnItemClickListener = mOnItemClickListener;
        }

        @Override
        public MyMusicPlayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            MyMusicPlayerViewHolder holder = new MyMusicPlayerViewHolder(LayoutInflater.from(
                    MainActivity.this).inflate(com.htc.my.mymusicplayer.R.layout.my_music_lists_itme, parent,
                    false));
            Log.i("main","hello");

            return holder;
        }

        @Override
        public void onBindViewHolder(final MyMusicPlayerViewHolder holder, int position)
        {

            HtcSong HtcSong = allSongs.get(position);
            holder.my_music_title.setText(HtcSong.getTitle());
            holder.my_music_author.setText(HtcSong.getAuthor());
            holder.my_music_duration.setText(AllSongs.formatTime(HtcSong.getDuration()));

            // 自定义RecycleView内的点击事件(4)
            // 如果设置了回调，则设置点击事件
            if (mOnItemClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickListener.onItemClick(holder.itemView, pos);
                    }
                });

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickListener.onItemLongClick(holder.itemView, pos);
                        return false;
                    }
                });
            }

        }

        @Override
        public int getItemCount()
        {
            return allSongs.size();
        }


        class MyMusicPlayerViewHolder extends RecyclerView.ViewHolder
        {

            TextView my_music_title;
            TextView my_music_author;
            TextView my_music_duration;

            public MyMusicPlayerViewHolder(View view)
            {
                super(view);
                my_music_title = (TextView) view.findViewById(com.htc.my.mymusicplayer.R.id.text_view_name);
                my_music_author = (TextView) view.findViewById(com.htc.my.mymusicplayer.R.id.text_view_artist);
                my_music_duration = (TextView) view.findViewById(com.htc.my.mymusicplayer.R.id.text_view_duration);
            }
        }


    }

    // 建立与PlayerService的连接
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("main","connect");
            music_binder = (MyMusicPlayerService.MusicPlayerServiceBinder) service;
            if(allSongs == null) allSongs = music_binder.getAllSongs();
            for(int i=0; i<allSongs.size(); ++i) allSongs_state.add(false);        // 初始化音乐播放状态
            findAllViewById();
            setAllViewListener();
            loadMusicFiles();

            if(music_binder.isPlayingOnService()) image_view_play_toggle.setImageResource(com.htc.my.mymusicplayer.R.drawable.ic_pause_1);

            if (allSongs.size()>0) {
                HtcSong temp = allSongs.get(0);
                main_toolbar.setTitle(temp.getTitle());
                music_text_name.setText(temp.getTitle());
                music_text_author.setText(temp.getAuthor());
                mMediaSessionManager.updateMetaData(temp);
                Log.e(TAG,"onServiceConnected");


            }
            progressBar.setVisibility(ProgressBar.GONE);

            if(music_binder.getSongOnService()!=null) {
                HtcSong temp = music_binder.getSongOnService();
                main_toolbar.setTitle(temp.getTitle());
                music_text_name.setText(temp.getTitle());
                music_text_author.setText(temp.getAuthor());
                mMediaSessionManager.updateMetaData(temp);
                Log.e(TAG,"onServiceConnected");

            }
        }
    };
    // 建立与PlayerService的连接
    private void createLinkWithPlayerService(){
        Intent bindIntent = new Intent(this, MyMusicPlayerService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }


    @SuppressLint("NewApi")
    private void requestReadExternalPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                // 0 是自己定义的请求coude
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        } else {
            this.createLinkWithPlayerService();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 0: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    // request successfully, handle you transactions
                    this.createLinkWithPlayerService();

                } else {

                    // permission denied
                    // request failed
                }

                return;
            }
            default:
                break;

        }
    }


/*    private class NextThread extends Thread{
        @Override
        public void run(){
            while (true){

                handler.sendMessage(new Message());
                break;
            }
        }
    }*/

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            //旋转动画：围绕x轴旋转
            switch (msg.what) {

                case 1:
             /*       //旋转动画：围绕x轴旋转
                    ObjectAnimator animator = ObjectAnimator.ofFloat(image_view_logo, "rotationX", 0, 360, 0);
                    animator.setDuration(3000);
                    animator.start();
                    message.what = 2;
                    handler.sendMessageDelayed(message,10000);*/
                    break;

                case 2:
                    //旋转动画:围绕z轴旋转
                    ObjectAnimator animator = ObjectAnimator.ofFloat(image_view_logo, "rotation", 0, 720);
                    animator.setDuration(3000);
                    animator.start();
                    Message message = new Message();
                    message.what = 2;
                    handler.sendMessageDelayed(message,10000);
                    break;
                default:
                    break;
            }
        }

    };
/*    // 集中管理线程的启动
    public void startAllThread(){
        if(nextThread == null) nextThread = new NextThread();
        if(nextThread.getState() == Thread.State.TERMINATED || nextThread.getState() == Thread.State.NEW) {
            nextThread.start();
        }
    }*/


}
