package com.wuwang.aavt.examples;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.wuwang.aavt.gl.CandyFilter;
import com.wuwang.aavt.gl.GrayFilter;
import com.wuwang.aavt.media.player.EffectMediaView;

/**
 * @author wuwang
 * @version 1.00 , 2019/03/13
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private EffectMediaView playerView;
    private String videoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        playerView = (EffectMediaView) findViewById(R.id.playerView);
        videoPath = getIntent().getStringExtra("videoPath");
        playerView.setRenderer(new CandyFilter(getResources()));
        playerView.setDataSource(videoPath);
        playerView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerView.onDestroy();
    }

    public void onPlay(View view){
        playerView.start();
    }

    public void onStop(View view){
        playerView.stop();
    }

    public void onPause(View view){
        playerView.pause();
    }
}
