/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuwang.aavt.media.video;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.wuwang.aavt.log.AvLog;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * CameraProvider 相机数据
 *
 * @author wuwang
 * @version v1.0 2017:10:26 18:09
 */
public class CameraProvider implements ITextureProvider {

    private Camera mCamera;
    private int cameraId=0;
    private int minWidth = 720;
    private float rate = 1.67f;
    private Semaphore mFrameSem;
    private String tag=getClass().getSimpleName();

    @Override
    public Point open(final SurfaceTexture surface) {
        final Point size=new Point();
        try {
            mFrameSem=new Semaphore(0);
            mCamera=Camera.open(cameraId);
            onCameraOpened(mCamera);
            mCamera.setPreviewTexture(surface);
            surface.setOnFrameAvailableListener(frameListener);
            Camera.Size s=mCamera.getParameters().getPreviewSize();
            mCamera.startPreview();
            size.x=s.height;
            size.y=s.width;
            AvLog.i(tag,"Camera Opened");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * Camera Open后，Preview前被调用的函数
     * @param camera 打开的Camera
     */
    protected void onCameraOpened(Camera camera){
        //小米5、5s这类奇葩手机不调用setParameters这句会导致图像预览方向错误
        Camera.Parameters param = camera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(android.hardware.Camera.Size o1, android.hardware.Camera.Size o2) {
                return o1.width - o2.width;
            }
        });
        for (android.hardware.Camera.Size s : sizes) {
            if (s.width >= minWidth) {
                if (rate > 1.4) {
                    if (s.width / (float) s.height > 1.4) {
                        param.setPreviewSize(s.width, s.height);
                        param.set("video-size", s.width + "x" + s.height);
                        break;
                    }
                } else {
                    if (s.width / (float) s.height < 1.4) {
                        param.setPreviewSize(s.width, s.height);
                        param.set("video-size", s.width + "x" + s.height);
                        break;
                    }
                }
            }
        }
        List<String> mode = param.getSupportedFocusModes();
        if(mode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(param);
    }

    public void setCameraSize(int minWidth,float rate){
        this.minWidth = minWidth;
        this.rate = rate;
    }

    public void switchCamera(){
        cameraId^=1;
    }

    public void setDefaultCamera(int id){
        cameraId = id;
    }

    @Override
    public void close() {
        mFrameSem.drainPermits();
        mFrameSem.release();

        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }

    @Override
    public boolean frame() {
        try {
            mFrameSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long getTimeStamp() {
        return -1;
    }

    @Override
    public boolean isLandscape() {
        return true;
    }

    private SurfaceTexture.OnFrameAvailableListener frameListener=new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            AvLog.d(tag,"onFrameAvailable");
            mFrameSem.drainPermits();
            mFrameSem.release();
        }

    };

}
