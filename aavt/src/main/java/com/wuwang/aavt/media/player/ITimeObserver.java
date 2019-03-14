package com.wuwang.aavt.media.player;

/**
 * @author wuwang
 * @version 1.00 , 2018/10/10
 */
public interface ITimeObserver {

    /**
     * 通知观察者当前时间
     * @param time 时间戳
     * @return 观察者是否处理完成
     */
    boolean update(long time);

}
