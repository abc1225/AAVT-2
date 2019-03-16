package com.wuwang.aavt.media.player;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.egl.EGLConfigAttrs;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EglHelper;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.gl.LazyFilter;
import com.wuwang.aavt.gl.OesFilter;
import com.wuwang.aavt.media.WrapRenderer;
import com.wuwang.aavt.utils.GpuUtils;
import com.wuwang.aavt.utils.MatrixUtils;

/**
 * @author wuwang
 * @version 1.00 , 2018/09/29
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoPlayer implements Runnable,ITextureProcessor{

    private boolean threadFlag = false;
    private Thread thread;
    private SurfaceTexture surfaceTexture;
    private int surfaceTextureId;
    private Object surface;
    private boolean surfaceChanged = false;
    private boolean textureUpdate = false;
    private final Object SURFACE_LOCK = new Object();
    private final Object THREAD_LOCK = new Object();
    private final Object RENDER_LOCK = new Object();
    private int width;
    private int height;
    private Renderer renderer;
    private boolean isRendererChanged = false;
    private boolean isSurfaceSizeChanged = false;

    public VideoPlayer(){

    }

    public void setSurface(Object surface){
        if(surface instanceof Surface || surface instanceof SurfaceTexture || surface instanceof SurfaceHolder){
            synchronized (SURFACE_LOCK){
                if(this.surface != surface){
                    this.surface = surface;
                    surfaceChanged = true;
                    SURFACE_LOCK.notifyAll();
                }
            }
        }
    }

    public void destroySurface(){
        synchronized (SURFACE_LOCK){
            if(this.surface != null){
                surface = null;
                surfaceChanged = true;
            }
        }
    }

    public void setSurfaceSize(int width,int height){
        this.width = width;
        this.height = height;
        this.isSurfaceSizeChanged = true;
    }

    public void init(){
        synchronized (THREAD_LOCK){
            threadFlag = true;
            thread = new Thread(this);
            thread.start();
            try {
                THREAD_LOCK.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setRenderer(Renderer renderer){
        this.renderer = renderer;
        this.isRendererChanged = true;
    }

    public SurfaceTexture getInputSurfaceTexture(){
        return surfaceTexture;
    }

    @Override
    public int process(int textureId) {
        synchronized (RENDER_LOCK){
            textureUpdate = true;
            RENDER_LOCK.notifyAll();
        }
        return 0;
    }

    public void requestRender(){
        synchronized (RENDER_LOCK){
            RENDER_LOCK.notifyAll();
        }
    }

    @Override
    public void release(){
        threadFlag = false;
        synchronized (SURFACE_LOCK){
            SURFACE_LOCK.notifyAll();
        }
        synchronized (RENDER_LOCK){
            RENDER_LOCK.notifyAll();
        }
    }

    private WrapRenderer checkWrapRenderer(WrapRenderer renderer){
        if(isRendererChanged){
            isRendererChanged = false;
            renderer.destroy();
            renderer = new WrapRenderer(this.renderer);
            renderer.create();
            renderer.sizeChanged(width,height);
        }
        return renderer;
    }

    @Override
    public void run() {
        SurfaceTexture temp = new SurfaceTexture(0);
        EglHelper helper = new EglHelper();
        helper.createGLESWithSurface(new EGLConfigAttrs(),new EGLContextAttrs(),temp);
        surfaceTextureId = GpuUtils.createTextureID(true);
        surfaceTexture = new SurfaceTexture(surfaceTextureId);
        synchronized (THREAD_LOCK){
            THREAD_LOCK.notifyAll();
        }
        synchronized (SURFACE_LOCK){
            if(surface == null || !surfaceChanged){
                try {
                    SURFACE_LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                surfaceChanged = false;
            }
        }
        if(!threadFlag){
            return;
        }
        EGLSurface showSurface = helper.createWindowSurface(surface);
        if(showSurface == EGL14.EGL_NO_SURFACE){
            //todo report error
            return;
        }

        FrameBuffer buffer = new FrameBuffer();
        LazyFilter matrixFilter = new LazyFilter();
        MatrixUtils.flip(matrixFilter.getVertexMatrix(),false,true);
        matrixFilter.create();
        matrixFilter.sizeChanged(width,height);

        WrapRenderer renderer = new WrapRenderer(null);
        renderer.create();

        while (threadFlag){
            synchronized (RENDER_LOCK){
                try {
                    RENDER_LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!threadFlag){
                    break;
                }
                if(textureUpdate){
                    surfaceTexture.updateTexImage();
                    textureUpdate = false;
                }
            }

            renderer = checkWrapRenderer(renderer);

            if(surfaceChanged && surface!=null){
                helper.destroySurface(showSurface);
                showSurface = helper.createWindowSurface(surface);
                surfaceChanged = false;
            }
            if(isSurfaceSizeChanged){
                isSurfaceSizeChanged = false;
                renderer.sizeChanged(width,height);
            }
            helper.makeCurrent(showSurface);
            GLES20.glViewport(0,0,width,height);
            GLES20.glClearColor(0,1,0,1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            surfaceTexture.getTransformMatrix(renderer.getTextureMatrix());
            buffer.bindFrameBuffer(width,height);
            renderer.draw(surfaceTextureId);
            buffer.unBindFrameBuffer();
            matrixFilter.draw(buffer.getCacheTextureId());

            helper.swapBuffers(showSurface);
        }
        renderer.destroy();
        surfaceTexture.release();
        temp.release();
        helper.destroyGLES(helper.getDefaultSurface(),helper.getDefaultContext());
    }
}
