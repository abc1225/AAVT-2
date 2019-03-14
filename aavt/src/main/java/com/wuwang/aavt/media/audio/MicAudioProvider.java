package com.wuwang.aavt.media.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.wuwang.aavt.media.audio.ISoundProvider;
import com.wuwang.aavt.media.av.AvException;

/**
 * @author wuwang
 * @version 1.00 , 2019/03/13
 */
public class MicAudioProvider implements ISoundProvider {

    private AudioRecord record;
    private int recordBufferSize=0;
    private int sampleRateInHz=48000;   //音频采样率
    private int channelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int audioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit

    public MicAudioProvider(){
        setProperty(sampleRateInHz,channelConfig,audioFormat);
    }

    public MicAudioProvider(int sampleRateInHz, int channelConfig, int audioFormat){
        setProperty(sampleRateInHz, channelConfig, audioFormat);
    }

    public void setProperty(int sampleRateInHz, int channelConfig, int audioFormat){
        this.sampleRateInHz = sampleRateInHz;
        this.channelConfig = channelConfig;
        this.audioFormat = audioFormat;
        this.recordBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) * 2;
    }

    @Override
    public void open() {
        close();
        record = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRateInHz,channelConfig,audioFormat,recordBufferSize);
        record.startRecording();
    }

    @Override
    public void close() {
        if(record != null){
            record.stop();
            record.release();
            record = null;
        }
    }

}
