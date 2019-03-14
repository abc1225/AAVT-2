package com.wuwang.aavt.media.player;

import android.graphics.SurfaceTexture;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/28
 */
public abstract class BaseVideoDecoder implements IDecoder,ITimeObserver {

    protected ITextureProcessor processor;
    protected MediaExtractor extractor;
    protected MediaFormat format;
    protected long seekToTimeUs = -1;
    protected Surface surface;

    public BaseVideoDecoder(){

    }

    public void init(MediaExtractor extractor, MediaFormat codec, SurfaceTexture texture){
        this.extractor = extractor;
        this.format = codec;
        this.surface = new Surface(texture);
    }

    public void setTextureProcessor(ITextureProcessor processor){
        this.processor = processor;
    }

    @Override
    public void seekTo(long timeUs) {
        this.seekToTimeUs = timeUs;
    }

}
