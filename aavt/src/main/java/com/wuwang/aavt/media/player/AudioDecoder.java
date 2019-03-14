package com.wuwang.aavt.media.player;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.nio.ByteBuffer;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/27
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class AudioDecoder implements Runnable {

    private MediaExtractor extractor;
    private MediaCodec codec;

    private boolean threadFlag = false;
    private Thread thread;
    private long seekTime;
    private final Object SEEK_LOCK = new Object();
    private int timeout = 1000;
    private MediaCodec.BufferInfo info;
    private IAudioProcessor processor;

    AudioDecoder(MediaExtractor extractor, MediaCodec codec){
        this.extractor = extractor;
        this.codec = codec;
        this.info = new MediaCodec.BufferInfo();
    }

    public void setAudioFormatChanged(IAudioProcessor processor){
        this.processor = processor;
    }

    public void init(){
        threadFlag = true;
        thread = new Thread(this);
        thread.start();
    }

    public void seekTo(long time){
        synchronized (SEEK_LOCK){
            this.seekTime = time;
        }
    }

    private void checkAndDoSeek(){
        if(seekTime >= 0){
            synchronized (SEEK_LOCK){
                if(seekTime >=0 ){
                    extractor.seekTo(seekTime,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
        }
    }

    private ByteBuffer getInputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        }else{
            return codec.getOutputBuffers()[index];
        }
    }

    public void release(){
        threadFlag = false;
    }

    @Override
    public void run() {
        while (threadFlag){
            checkAndDoSeek();
            int inputIndex = codec.dequeueInputBuffer(timeout);
            if(inputIndex >= 0){
                ByteBuffer inputBuffer = getInputBuffer(codec,inputIndex);
                inputBuffer.position(0);
                int size = extractor.readSampleData(inputBuffer,0);
                if(size == -1){
                    //todo 最后一帧处理
                    codec.queueInputBuffer(inputIndex,0,0,extractor.getSampleTime(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else{
                    codec.queueInputBuffer(inputIndex,0,size,extractor.getSampleTime(),extractor.getSampleFlags());
                }
            }
            while(true){
                int outputIndex = codec.dequeueOutputBuffer(info,timeout);
                if (outputIndex >= 0) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        ByteBuffer outputBuffer = getOutputBuffer(codec,outputIndex);
                        if(processor != null){
                            ByteBufferData data = new ByteBufferData();
                            data.data = outputBuffer;
                            data.length = info.size;
                            data.offset = info.offset;
                            data.timeUs = info.presentationTimeUs;
                            data.flag = info.flags;
                            processor.processAudioData(data);
                        }
                        codec.releaseOutputBuffer(outputIndex, false);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            //todo 最后一帧处理
                            break;
                        }
                    } else {
                        codec.releaseOutputBuffer(outputIndex, false);
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = codec.getOutputFormat();
                    if(processor != null){
                        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        int channelCount  = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        processor.onAudioFormatChanged(sampleRate,channelCount);
                    }
                } else if(outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }
            }
        }
    }
}
