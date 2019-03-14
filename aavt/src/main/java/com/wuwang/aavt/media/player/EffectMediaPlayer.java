package com.wuwang.aavt.media.player;

import android.annotation.TargetApi;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;

import com.wuwang.aavt.core.Renderer;

import java.io.IOException;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/27
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class EffectMediaPlayer {

    private static final String YES = "yes";

    private String path;
    private Object surface;
    private BaseAudioDecoder audioDecoder;
    private BaseVideoDecoder videoDecoder;
    private MediaExtractor audioExtractor;
    private MediaExtractor videoExtractor;
    private MediaFormat videoFormat;
    private MediaFormat audioFormat;

    private long durationUs = 0;

    private int videoWidth = -1;
    private int videoHeight = -1;

    private AudioPlayer audioPlayer;
    private VideoPlayer videoPlayer;

    public EffectMediaPlayer(){
        audioPlayer = new AudioPlayer();
        audioDecoder = new NormalAudioDecoder();
        audioDecoder.setLoop(true);
        audioDecoder.setAudioProcessor(audioPlayer);
        videoPlayer = new VideoPlayer();
        videoDecoder = new NormalVideoDecoder();
        videoDecoder.setTextureProcessor(videoPlayer);
        //时钟同步
        audioPlayer.setTimeObserver(videoDecoder);
    }

    public void setDataSource(String filePath) throws IOException {
        this.path = filePath;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(!TextUtils.isEmpty(duration) && TextUtils.isDigitsOnly(duration)){
            durationUs = Long.parseLong(duration);
        }
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(filePath);
        videoWidth = 0;
        videoHeight = 0;
        for (int i=0;i<extractor.getTrackCount();i++){
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if(mime.startsWith("video")){
                videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(path);
                videoExtractor.selectTrack(i);
                videoFormat = format;
                int degrees = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format.containsKey(MediaFormat.KEY_ROTATION)) {
                    degrees = format.getInteger(MediaFormat.KEY_ROTATION);
                }
                if(degrees%180 == 0){
                    videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                    videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                }else{
                    videoHeight = format.getInteger(MediaFormat.KEY_WIDTH);
                    videoWidth = format.getInteger(MediaFormat.KEY_HEIGHT);
                }
            }else if(mime.startsWith("audio")){
                audioExtractor = new MediaExtractor();
                audioExtractor.setDataSource(path);
                audioExtractor.selectTrack(i);
                audioFormat = format;
                if(audioFormat.containsKey(MediaFormat.KEY_DURATION)){
                    durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION);
                }
            }
        }

    }

    public int getVideoWidth(){
        return videoWidth;
    }

    public int getVideoHeight(){
        return videoHeight;
    }

    public void setSurface(Object surface){
        this.surface = surface;
        videoPlayer.setSurface(surface);
        videoPlayer.requestRender();
    }

    public void destroySurface(){
        videoPlayer.destroySurface();
    }

    public void setSurfaceSize(int width,int height){
        videoPlayer.setSurfaceSize(width, height);
    }

    public void prepare(){
        videoPlayer.init();
        audioDecoder.init(audioExtractor,audioFormat);
        videoDecoder.init(videoExtractor,videoFormat,videoPlayer.getInputSurfaceTexture());
    }

    public void setRenderer(Renderer renderer){
        videoPlayer.setRenderer(renderer);
    }

    public void pause() {
        if(audioDecoder != null){
            audioDecoder.pause();
        }
        if(videoDecoder != null){
            videoDecoder.pause();
        }
    }

    public void start() {
        if(audioDecoder != null){
            audioDecoder.start();
        }
        if(videoDecoder != null){
            videoDecoder.start();
        }
    }

    public void stop() {
        if(audioDecoder != null){
            audioDecoder.stop();
        }
        if(videoDecoder != null){
            videoDecoder.stop();
        }
    }

    public void release(){
        videoPlayer.release();
    }

    public void seekTo(long timeUs) {
        if(audioDecoder != null){
            audioDecoder.seekTo(timeUs);
        }
        if(videoDecoder != null){
            videoDecoder.seekTo(timeUs);
        }
    }

    public long getDuration(){
        return durationUs;
    }

}
