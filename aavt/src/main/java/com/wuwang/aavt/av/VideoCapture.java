package com.wuwang.aavt.av;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseArray;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.media.video.CameraProvider;
import com.wuwang.aavt.media.video.ITextureProvider;
import com.wuwang.aavt.media.video.Mp4Provider;
import com.wuwang.aavt.media.SurfaceEncoder;
import com.wuwang.aavt.media.SurfaceShower;
import com.wuwang.aavt.media.VideoSurfaceProcessor;
import com.wuwang.aavt.media.hard.IHardStore;
import com.wuwang.aavt.media.hard.StrengthenMp4MuxStore;

/**
 * @author wuwang
 * @version 1.00 , 2019/03/13
 */
public class VideoCapture {

    public static final int KEY_CAMERA_MIN_WIDTH = 1;
    public static final int KEY_CAMERA_RATE = 2;
    public static final int KEY_OUTPUT_WIDTH = 3;
    public static final int KEY_OUTPUT_HEIGHT = 4;
    public static final int KEY_OUTPUT_PATH = 5;
    public static final int KEY_PREVIEW_WIDTH = 6;
    public static final int KEY_PREVIEW_HEIGHT = 7;

    private Context context;
    private ITextureProvider provider;
    private SurfaceShower shower;
    private SurfaceEncoder encoder;
    private VideoSurfaceProcessor processor;
    private IHardStore muxer;
    private SparseArray<Float> propFloat;
    private SparseArray<String> propStr;
    private Renderer renderer;
    private Object showSurface;

    @SuppressLint("SdCardPath")
    private static final String DEFAULT_OUTPUT_PATH = "/mnt/sdcard/test.mp4";

    public VideoCapture(Context context){
        this.context = context;
        propFloat = new SparseArray<>();
        propStr = new SparseArray<>();
    }

    public void setRenderer(Renderer renderer){
        this.renderer = renderer;
        if(processor != null){
            processor.setRenderer(renderer);
        }
    }

    public void setProperty(int key,float value){
        propFloat.put(key,value);
    }

    public void setProperty(int key,String value){
        propStr.put(key,value);
    }

    @SuppressLint("SdCardPath")
    public void open(int id){
        provider = new CameraProvider();
        ((CameraProvider) provider).setDefaultCamera(id);
        ((CameraProvider) provider).setCameraSize((int)(float)(propFloat.get(KEY_CAMERA_MIN_WIDTH,720f)),propFloat.get(KEY_CAMERA_RATE,1.7f));

        shower = new SurfaceShower();
        shower.setOutputSize((int)(float)(propFloat.get(KEY_PREVIEW_WIDTH,720.0f)),
                (int)(float)(propFloat.get(KEY_PREVIEW_HEIGHT,1280.0f)));
        
        muxer = new StrengthenMp4MuxStore(false);
        muxer.setOutputPath(propStr.get(KEY_OUTPUT_PATH,DEFAULT_OUTPUT_PATH));

        encoder = new SurfaceEncoder();
        encoder.setStore(muxer);
        encoder.setOutputSize((int)(float)(propFloat.get(KEY_OUTPUT_WIDTH,368.0f)),
                (int)(float)(propFloat.get(KEY_OUTPUT_HEIGHT,640.f)));

        processor = new VideoSurfaceProcessor();
        processor.setTextureProvider(provider);
        processor.addObserver(shower);
        processor.addObserver(encoder);

        processor.start();
    }

    public void open(String path){
        muxer = new StrengthenMp4MuxStore(false);
        muxer.setOutputPath(propStr.get(KEY_OUTPUT_PATH,DEFAULT_OUTPUT_PATH));

        provider = new Mp4Provider();
        ((Mp4Provider) provider).setInputPath(path);
        ((Mp4Provider) provider).setStore(muxer);

        shower = new SurfaceShower();
        shower.setOutputSize((int)(float)(propFloat.get(KEY_OUTPUT_WIDTH,720.0f)),
                (int)(float)(propFloat.get(KEY_OUTPUT_HEIGHT,1280.0f)));

        encoder = new SurfaceEncoder();
        encoder.setStore(muxer);

        processor = new VideoSurfaceProcessor();
        processor.setTextureProvider(provider);
        processor.addObserver(shower);
        processor.addObserver(encoder);
        setRenderer(renderer);

        processor.start();
    }

    public void setPreviewSurface(Object surface){
        this.showSurface = surface;
        if(shower != null){
            shower.setSurface(surface);
        }
    }

    public void startPreview(){
        setPreviewSurface(showSurface);
        shower.open();
    }

    public void stopPreview(){
        shower.close();
    }

    public void startRecord(){
        encoder.open();
    }

    public void stopRecord(){
        encoder.close();
    }


    public void close(){
        processor.stop();
    }

}
