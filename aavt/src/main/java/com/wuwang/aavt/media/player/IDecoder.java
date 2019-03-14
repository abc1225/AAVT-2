package com.wuwang.aavt.media.player;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
public interface IDecoder {

    /**
     * seek到指定位置再解码
     * @param timeUs 时间us
     */
    void seekTo(long timeUs);

    /**
     * 开始解码
     */
    void start();

    /**
     * 暂停解码
     */
    void pause();

    /**
     * 停止解码
     */
    void stop();

}
