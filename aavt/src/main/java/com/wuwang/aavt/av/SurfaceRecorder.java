package com.wuwang.aavt.av;

import android.content.Context;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.media.video.Camera2Provider;
import com.wuwang.aavt.media.video.ITextureProvider;
import com.wuwang.aavt.media.SoundRecorder;
import com.wuwang.aavt.media.SurfaceEncoder;
import com.wuwang.aavt.media.SurfaceShower;
import com.wuwang.aavt.media.VideoSurfaceProcessor;
import com.wuwang.aavt.media.av.AvException;
import com.wuwang.aavt.media.hard.IHardStore;
import com.wuwang.aavt.media.hard.StrengthenMp4MuxStore;

/**
 * @author wuwang
 * @version 1.00 , 2018/11/14
 */
public class SurfaceRecorder {

    private Context context;
    private VideoSurfaceProcessor mTextureProcessor;
    private SurfaceShower mShower;
    private SurfaceEncoder mSurfaceStore;
    private IHardStore mMuxer;

    private ITextureProvider mProvider;
    private SoundRecorder mSoundRecord;

    private int mPreviewWidth = 720;
    private int mPreviewHeight = 1280;
    private int mRecordWidth = 720;
    private int mRecordHeight = 1280;
    private String path;
    private Renderer mRender;
    private Object mShowSurface;

    public SurfaceRecorder(Context context){
        this.context = context;
    }

    public void setTextureProvider(ITextureProvider provider){
        this.mProvider = provider;
    }

    public void setPreviewSize(int width,int height){
        this.mPreviewWidth = width;
        this.mPreviewHeight = height;
    }

    public void setRecordSize(int width,int height){
        this.mRecordWidth = width;
        this.mRecordHeight = height;
    }

    public void setSurface(Object surface){
        this.mShowSurface = surface;
    }

    /**
     * 设置录制的输出路径
     * @param path 输出路径
     */
    public void setOutputPath(String path){
        this.path = path;
    }

    public void setRenderer(Renderer renderer){
        this.mRender = renderer;
    }

    public void open(){
        if(mMuxer == null){
            mMuxer=new StrengthenMp4MuxStore(true);
            mMuxer.setOutputPath(path);
        }

        if(mProvider == null){
            mProvider=new Camera2Provider(context);
        }

        if(mShower == null){
            //用于预览图像
            mShower=new SurfaceShower();
            mShower.setOutputSize(mPreviewWidth,mPreviewHeight);
            mShower.setSurface(mShowSurface);
        }

        if(mSurfaceStore == null){
            //用于编码图像
            mSurfaceStore=new SurfaceEncoder();
            mSurfaceStore.setOutputSize(mRecordWidth,mRecordHeight);
            mSurfaceStore.setStore(mMuxer);
        }

        if(mSoundRecord == null){
            //用于音频
            mSoundRecord=new SoundRecorder(mMuxer);
        }

        if(mTextureProcessor == null){
            //用于处理视频图像
            mTextureProcessor=new VideoSurfaceProcessor();
            mTextureProcessor.setTextureProvider(mProvider);
            mTextureProcessor.addObserver(mShower);
            mTextureProcessor.addObserver(mSurfaceStore);
            mTextureProcessor.setRenderer(mRender);
        }
        mTextureProcessor.start();
    }

    public void close(){
        if(mTextureProcessor != null){
            mTextureProcessor.stop();
        }
    }

    public void startPreview(){
        if(mShower != null){
            mShower.open();
        }
    }

    public void stopPreview(){
        if(mShower != null){
            mShower.close();
        }
    }

    /**
     * 开始录制
     */
    public void startRecord(){
        if(mSurfaceStore != null && mSoundRecord !=null){
            mSurfaceStore.open();
            mSoundRecord.start();
        }
    }

    /**
     * 关闭录制
     */
    public void stopRecord(){
        if(mSoundRecord != null && mSurfaceStore != null && mMuxer != null){
            mSoundRecord.stop();
            mSurfaceStore.close();
            mMuxer.close();
        }
    }

}
