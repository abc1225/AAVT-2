package com.wuwang.aavt.media.player;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import com.wuwang.aavt.media.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NormalVideoDecoder extends BaseVideoDecoder implements Runnable {

    private boolean threadFlag = false;
    private Thread thread;
    private int timeout = 1000;
    private boolean pause;
    private long currentPtsTime;
    private long currentReferenceTime;
    private final Object CONTROL_LOCK = new Object();
    private final Object DECODE_LOCK = new Object();
    private final Object SEEK_LOCK = new Object();
    private boolean loop = true;
    private boolean isFirstFrameEmpty = true;
    private final long SEEK_LIMIT_TIME = 400000;
    private long tryToSeekTo = -1;

    public NormalVideoDecoder(){

    }

    @Override
    public void init(MediaExtractor extractor, MediaFormat codec, SurfaceTexture texture) {
        super.init(extractor, codec, texture);
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
            synchronized (CONTROL_LOCK){
                pause = false;
                CONTROL_LOCK.notifyAll();
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
        synchronized (CONTROL_LOCK){
            this.pause = true;
        }
    }

    @Override
    public void stop() {
        threadFlag = false;
        synchronized (CONTROL_LOCK){
            pause = false;
            CONTROL_LOCK.notifyAll();
        }
        synchronized (DECODE_LOCK){
            DECODE_LOCK.notifyAll();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        currentPtsTime = 0;
        currentReferenceTime = 0;
    }

    @Override
    public void seekTo(long timeUs) {
        synchronized (CONTROL_LOCK){
            super.seekTo(timeUs);
            //当前如果是暂停状态需要跳过暂停
            CONTROL_LOCK.notifyAll();
        }

        //当前如果是等待解码状态需要跳过等待，seek后更新图像
        synchronized (DECODE_LOCK){
            DECODE_LOCK.notifyAll();
        }
        isFirstFrameEmpty = true;
    }

    private void checkAndDoSeek(MediaCodec codec){
        if(seekToTimeUs >= 0 && tryToSeekTo == -1){
            synchronized (CONTROL_LOCK){
                codec.flush();
                extractor.seekTo(seekToTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                tryToSeekTo = seekToTimeUs;
                seekToTimeUs = -1;
            }
        }
    }

    private boolean checkAndDoPause(){
        if(pause && !isFirstFrameEmpty){
            synchronized (CONTROL_LOCK){
                try {
                    CONTROL_LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    private void codecInput(MediaCodec codec){
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
    }

    @Override
    public void run() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaCodec codec;
        try {
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        codec.configure(format,surface,null,0);
        codec.start();
        //todo 处理帧率非常高，图像处理又比较复杂比较耗时的情况。
        while (threadFlag){
            checkAndDoSeek(codec);
            if(checkAndDoPause()){
                continue;
            }
            codecInput(codec);
            while(true){
                int outputIndex = codec.dequeueOutputBuffer(info,timeout);
                if (outputIndex >= 0) {
                    currentPtsTime = info.presentationTimeUs;
                    //处理为精准seek，如果seek到的位置解码还未到目标位置，不更新texture，继续下一帧解码
                    if(tryToSeekTo != -1){
                        synchronized (CONTROL_LOCK){
                            if(currentPtsTime < tryToSeekTo){
                                codec.releaseOutputBuffer(outputIndex, false);
                                continue;
                            }
                        }
                        synchronized (SEEK_LOCK){
                            tryToSeekTo = -1;
                            SEEK_LOCK.notifyAll();
                        }
                    }
                    //精准seek完成，或者是正常的音频同步解码，需要更新输出纹理
                    codec.releaseOutputBuffer(outputIndex, true);
                    if(isFirstFrameEmpty){
                        //如果是seek完成的第一帧，需要立即处理
                        if(processor != null){
                            processor.process(0);
                        }
                        isFirstFrameEmpty = false;
                    }else{
                        //音频同步解码流程中，如果视频解码到了音频前面，则需要等待音频解码
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0
                                && currentPtsTime > currentReferenceTime) {
                            synchronized (DECODE_LOCK){
                                if(threadFlag){
                                    try {
                                        DECODE_LOCK.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        if(processor != null && currentReferenceTime - currentPtsTime < SEEK_LIMIT_TIME){
                            processor.process(0);
                        }
                    }
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if(loop) {
                            seekTo(0);
                        }else{
                            threadFlag = false;
                        }
                        break;
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = codec.getOutputFormat();

                } else if(outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }
            }
        }
        codec.flush();
        codec.stop();
        codec.release();
    }

    @Override
    public boolean update(long time) {
        if(tryToSeekTo != -1){
            synchronized (SEEK_LOCK){
                try {
                    SEEK_LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //ITimeObserver的接口，通常由音频解码器或者播放器调用，以达到音视频同步的目的
        currentReferenceTime = time;
        long interval = currentPtsTime - currentReferenceTime;
        if(interval > SEEK_LIMIT_TIME){
            if(seekToTimeUs == -1 && tryToSeekTo == -1){
                //视频解码太前时，seek回音频播放处，主要用于处理视频总长度大于音频的情况
                seekTo(currentReferenceTime);
                synchronized (DECODE_LOCK){
                    DECODE_LOCK.notifyAll();
                }
            }
        }else if(interval < 0){
            //视频时间戳小于音频时间戳时，通知处理并继续解码下一帧
            synchronized (DECODE_LOCK){
                DECODE_LOCK.notifyAll();
            }
        }
        return true;
    }

}
