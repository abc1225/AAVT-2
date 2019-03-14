package com.wuwang.aavt.media.player;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
public abstract class BaseAudioDecoder implements IDecoder,ITimeObserver {

    protected IAudioProcessor processor;
    protected MediaExtractor extractor;
    protected MediaFormat format;
    protected long seekToTimeUs = -1;
    protected ITimeObserver observer;
    public boolean loop = false;

    public void setAudioProcessor(IAudioProcessor processor){
        this.processor = processor;
    }

    public void setTimeObserver(ITimeObserver observer){
        this.observer = observer;
    }

    public void init(MediaExtractor extractor, MediaFormat codec)  {
        this.extractor = extractor;
        this.format = codec;
    }

    @Override
    public void seekTo(long timeUs) {
        this.seekToTimeUs = timeUs;
    }

    public void setLoop(boolean loop){
        this.loop = loop;
    }

    @Override
    public boolean update(long time) {
        if(observer != null){
            observer.update(time);
        }
        return true;
    }
}
