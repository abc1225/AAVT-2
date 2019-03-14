package com.wuwang.aavt.media.player;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.wuwang.aavt.R;
import com.wuwang.aavt.core.Renderer;

import java.io.IOException;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/30
 */
public class EffectMediaView extends RelativeLayout {

    public static int TYPE_SURFACE = 0;
    public static int TYPE_TEXTURE = 1;

    private int type = TYPE_TEXTURE;
    private ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_INSIDE;
    private String path;
    private boolean isResume;


    private EffectMediaPlayer player;
    private LayoutParams screenLayoutParams;
    private View screenView;
    private boolean isUserWantStart = false;

    public EffectMediaView(Context context) {
        this(context,null);
    }

    public EffectMediaView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public EffectMediaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs){
        if(attrs != null){
            TypedArray tArray = context.obtainStyledAttributes(attrs, R.styleable.EffectMediaView);
            type = tArray.getInt(R.styleable.EffectMediaView_aavt_media_surface_type,0);
            tArray.recycle();
        }

        player = new EffectMediaPlayer();
        screenLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        screenLayoutParams.addRule(CENTER_IN_PARENT);
    }

    private void invalidatePlayerScreen(){
        if(!isResume || player.getVideoWidth() <= 0 || player.getVideoHeight() <= 0){
            return;
        }
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if(scaleType == ImageView.ScaleType.CENTER_INSIDE && viewWidth != 0 && viewHeight != 0){
            float viewRatio = viewWidth/(float)viewHeight;
            float videoRatio = player.getVideoWidth()/(float)player.getVideoHeight();
            screenLayoutParams.width = viewWidth;
            screenLayoutParams.height = (int) (viewWidth/videoRatio);
        }
        if(screenView != null){
            screenView.setLayoutParams(screenLayoutParams);
            screenView.invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        invalidatePlayerScreen();
    }

    private void useTextureViewAsScreen(){
        TextureView texture = new TextureView(getContext());
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                player.setSurface(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                player.setSurface(surface);
                player.setSurfaceSize(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                player.destroySurface();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        texture.setBackgroundColor(0xFF880088);
        addView(texture,screenLayoutParams);
        screenView = texture;
    }

    private void useSurfaceViewAsScreen(){
        SurfaceView surface = new SurfaceView(getContext());
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                player.setSurface(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                player.setSurfaceSize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                player.destroySurface();
            }
        });
        addView(surface,screenLayoutParams);
        screenView = surface;
    }

    public void setRenderer(Renderer renderer){
        player.setRenderer(renderer);
    }

    private void tryToInit(){
        if(player.getVideoWidth() < 0 || player.getVideoHeight() < 0){
            return;
        }
        if(TYPE_SURFACE == type){
            useSurfaceViewAsScreen();
        }else if(TYPE_TEXTURE == type){
            useTextureViewAsScreen();
        }
        player.prepare();
    }

    public void setDataSource(String path){
        this.path = path;
        try {
            player.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tryToInit();
        invalidatePlayerScreen();
    }

    public void start(){
        player.start();
        isUserWantStart = true;
    }

    public void stop(){
        player.stop();
        isUserWantStart = false;
    }

    public void pause(){
        player.pause();
        isUserWantStart = false;
    }

    public void seekTo(long timeMs){
        player.seekTo(timeMs * 1000);
    }

    public long getDuration(){
        return player.getDuration();
    }

    public void onResume(){
        isResume = true;
        invalidatePlayerScreen();
        if(isUserWantStart){
            player.start();
        }
    }

    public void onPause(){
        isResume = false;
        player.pause();
    }

    public void onDestroy(){
        player.stop();
        player.release();
    }

}
