package com.wuwang.aavt.media.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
public class AudioPlayer implements IAudioProcessor,ITimeObserver{

    private AudioTrack track;
    private float volume = 1.0f;
    private int channelCount;
    private int sampleRate;
    private float tempo = 1.0f;
    private float pitchSemi = 1.0f;
    private float rate = 1.0f;
    private int audioTrackMinBuffer;
    private int writtenPcmByteCount;
    private int currentTimeUs;
//    private SoundTouch soundTouch;
    private byte[] soundTouchBuffer;
    private ITimeObserver observer;
    private boolean resetTimeWhenRepeat = true;
    private long lastTimePts;

    public void setVolume(float volume){
        if(track != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(volume);
            }else{
                track.setStereoVolume(volume,volume);
            }
        }
        this.volume = volume;
    }

    public void setTimeObserver(ITimeObserver observer){
        this.observer = observer;
    }

    public void init(int sampleRate, int channelCount){
        int channelFormat = channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
        this.channelCount = channelCount;
        this.sampleRate = sampleRate;
        this.audioTrackMinBuffer = AudioTrack.getMinBufferSize(sampleRate, channelFormat, AudioFormat.ENCODING_PCM_16BIT);
        if (audioTrackMinBuffer <= 0) {
            audioTrackMinBuffer = sampleRate * channelCount * 2 * 100 / 1000;
        }
        track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelFormat, AudioFormat.ENCODING_PCM_16BIT,
                audioTrackMinBuffer, AudioTrack.MODE_STREAM);
        setVolume(volume);
        track.play();
    }

    private long pcmBufferSizeToDurationUs(long bufferSize) {
        final int frameSize = 2 * channelCount;
        return (long) (bufferSize / frameSize * 1000000 * tempo / sampleRate);
    }

    private void writePcmToAudioTrack(ByteBufferData data) {
        if (pitchSemi == 1.0f && rate == 1.0f && tempo == 1.0f) {
            int offset = 0;
            int leftSize = data.length;
            long dataPlayTime = pcmBufferSizeToDurationUs(data.length);
            while (true) {
                int currentSize = Math.min(leftSize,audioTrackMinBuffer);
                update(data.timeUs + dataPlayTime - (long)(leftSize/(float)data.length * dataPlayTime));
                if (track != null && track.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        track.write(data.data,currentSize,AudioTrack.WRITE_BLOCKING);
                    }else{
                        track.write(data.data.array(),data.offset+offset,currentSize);
                    }
                    writtenPcmByteCount += currentSize;
                    currentTimeUs += pcmBufferSizeToDurationUs(currentSize);
                }
                offset += currentSize;
                leftSize -= currentSize;
                if (leftSize <= 0) {
                    break;
                }
            }
        }
//        else {
//            if(soundTouch == null){
//                soundTouch = new SoundTouch(0, channelCount, sampleRate,
//                        2, tempo, pitchSemi);
//                soundTouch.setRate(rate);
//                soundTouch.setup();
//                soundTouch.setTempo(tempo);
//            }
//            byte[] t = new byte[data.length];
//            data.data.get(t,data.offset,data.length);
//            soundTouch.putBytes(t);
//            if (soundTouchBuffer == null) {
//                soundTouchBuffer = new byte[audioTrackMinBuffer];
//            }
//            if (soundTouch != null) {
//                int bytesReceived = soundTouch.getBytes(soundTouchBuffer);
//                while (bytesReceived > 0) {
//                    if (track != null) {
//                        track.write(soundTouchBuffer, 0, bytesReceived);
//                        writtenPcmByteCount += bytesReceived;
//                        currentTimeUs += pcmBufferSizeToDurationUs(bytesReceived);
//                        //todo 计算精准时间
//                        update(data.timeUs);
//                    }
//                    bytesReceived = soundTouch.getBytes(soundTouchBuffer);
//                }
//            }
//        }
    }

    public long getCurrentTime(){
        return currentTimeUs;
    }

    @Override
    public void onAudioFormatChanged(int sampleRate, int channelCount) {
        init(sampleRate,channelCount);
    }

    @Override
    public ByteBufferData processAudioData(ByteBufferData data) {
        if(resetTimeWhenRepeat && data.timeUs < lastTimePts){
            currentTimeUs = 0;
        }
        lastTimePts = data.timeUs;
        writePcmToAudioTrack(data);
        return data;
    }

    @Override
    public void release() {
        if(track != null){
            track.stop();
            track.release();
            track = null;
        }
    }

    @Override
    public boolean update(long time) {
        return observer == null || observer.update(time);
    }
}
