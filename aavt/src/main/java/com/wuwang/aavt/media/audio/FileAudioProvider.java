package com.wuwang.aavt.media.audio;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.wuwang.aavt.media.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author wuwang
 * @version 1.00 , 2019/03/13
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FileAudioProvider extends MediaCodec.Callback implements ISoundProvider {

    private MediaExtractor extractor;
    private MediaCodec codec;
    private int audioTrack = -1;
    private MediaFormat format;
    private final long TIME_US = 1000;

    private FileAudioProvider(){}

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        ByteBuffer buffer = CodecUtil.getInputBuffer(codec,index);
        buffer.clear();
        int size = extractor.readSampleData(buffer,0);
        codec.queueInputBuffer(index,0,size ,extractor.getSampleTime(),extractor.getSampleFlags());
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        ByteBuffer buffer = CodecUtil.getOutputBuffer(codec,index);
        codec.releaseOutputBuffer(index,false);
    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

    }

    private boolean setPath(String path){
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(path);
            int count = extractor.getTrackCount();
            if(count <= 0){
                return false;
            }
            for (int i=0;i<count;i++){
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("audio")){
                    audioTrack = i;
                    extractor.selectTrack(audioTrack);
                    codec = MediaCodec.createDecoderByType(mime);
                    codec.setCallback(this);
                    this.format = format;
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static FileAudioProvider create(String path){
        FileAudioProvider provider = new FileAudioProvider();
        return provider.setPath(path)?provider:null;
    }

    @Override
    public void open() {
        codec.configure(format,null,null,0);
        codec.start();
    }

    public void codecStep(){
        int inputId = codec.dequeueInputBuffer(TIME_US);
        if(inputId>=0){
            ByteBuffer buffer = CodecUtil.getInputBuffer(codec,inputId);
            buffer.clear();

        }
    }

    @Override
    public void close() {
        codec.stop();
        codec.release();
    }
}
