package com.wuwang.aavt.media.player;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import com.wuwang.aavt.media.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 正常流程的音频解码
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NormalAudioDecoder extends BaseAudioDecoder implements Runnable {

    private boolean threadFlag = false;
    private Thread thread;
    private int timeout = 1000;
    private MediaCodec codec;
    private MediaCodec.BufferInfo info;
    private boolean pause;
    private final Object LOCK = new Object();

    public NormalAudioDecoder(){
        info = new MediaCodec.BufferInfo();
    }

    @Override
    public void init(MediaExtractor extractor, MediaFormat codec) {
        super.init(extractor, codec);
        if(seekToTimeUs == -1){
            seekTo(0);
        }
        pause = true;
        threadFlag = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void start() {
        if(threadFlag){
            synchronized (LOCK){
                pause = false;
                LOCK.notifyAll();
            }
        }else{
            if(seekToTimeUs == -1){
                seekTo(0);
            }
            pause = false;
            threadFlag = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void pause() {
        synchronized (LOCK){
            this.pause = true;
        }
    }

    @Override
    public void stop() {
        threadFlag = false;
        synchronized (LOCK){
            pause = false;
            LOCK.notifyAll();
        }
        if(thread != null){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void seekTo(long timeUs) {
        synchronized (LOCK){
            super.seekTo(timeUs);
            //当前如果是暂停状态需要跳过暂停
            LOCK.notifyAll();
        }
    }

    private void checkAndDoSeek(){
        if(seekToTimeUs >= 0){
            synchronized (LOCK){
                extractor.seekTo(seekToTimeUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                seekToTimeUs = -1;
            }
        }
    }

    private boolean checkAndDoPause(){
        if(pause){
            synchronized (LOCK){
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        try {
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        codec.configure(format,null,null,0);
        codec.start();
        while (threadFlag){
            checkAndDoSeek();
            if(checkAndDoPause()){
                continue;
            }
            int inputIndex = codec.dequeueInputBuffer(timeout);
            if(inputIndex >= 0){
                ByteBuffer inputBuffer = CodecUtil.getInputBuffer(codec,inputIndex);
                inputBuffer.position(0);
                int size = extractor.readSampleData(inputBuffer,0);
                if(size == -1){
                    //todo 最后一帧处理
                    codec.queueInputBuffer(inputIndex,0,0,extractor.getSampleTime(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else{
                    codec.queueInputBuffer(inputIndex,0,size,extractor.getSampleTime(),extractor.getSampleFlags());
                }
                extractor.advance();
            }
            while(true){
                int outputIndex = codec.dequeueOutputBuffer(info,timeout);
                if (outputIndex >= 0) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        ByteBuffer outputBuffer = CodecUtil.getOutputBuffer(codec,outputIndex);
                        if(processor != null){
                            ByteBufferData data = new ByteBufferData();
                            data.data = outputBuffer;
                            data.length = info.size;
                            data.offset = info.offset;
                            data.timeUs = info.presentationTimeUs;
                            data.flag = info.flags;
                            processor.processAudioData(data);
                            update(data.timeUs);
                        }
                        codec.releaseOutputBuffer(outputIndex, false);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            //todo 最后一帧处理
                            if(loop){
                                seekTo(0);
                                codec.flush();
                            }else{
                                threadFlag = false;
                            }
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
        codec.stop();
        codec.release();
    }
}
