package com.wuwang.aavt.media.player;

/**
 * 处理音频数据接口
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
public interface IAudioProcessor {

    /**
     * 音频信息发生改变
     * @param sampleRate 音频采样率
     * @param channelCount 音频通道数
     */
    void onAudioFormatChanged(int sampleRate, int channelCount);


    /**
     * 处理音频数据
     * @param data 输入的音频数据
     *  @return 处理后的数据
     */
    ByteBufferData processAudioData(ByteBufferData data);

    /**
     * 销毁音频处理相关资源
     */
    void release();

}
