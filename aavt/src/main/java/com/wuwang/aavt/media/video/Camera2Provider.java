package com.wuwang.aavt.media.video;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @author wuwang
 * @version 1.00 , 2018/11/14
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Provider extends CameraDevice.StateCallback implements ITextureProvider {

    private CameraDevice camera;
    private CameraManager manager;
    private String[] ids;
    private String[] cameraId;
    private Size[] previewSize;
    private int minWidth = 720;
    private float rate = 1.67f;
    private int nowCamera = 1;
    private SurfaceTexture surface;
    private HandlerThread thread;
    private Handler cameraHandler;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private Semaphore mFrameSem;

    public Camera2Provider(Context context){
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraId = new String[2];
        previewSize = new Size[2];
        thread = new HandlerThread("camera handler thread");
        thread.start();
        cameraHandler = new Handler(thread.getLooper());
    }

    public void switchCamera(){
        this.nowCamera ^= 1;
        close();
        open(surface);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Point open(SurfaceTexture surface) {
        try {
            mFrameSem=new Semaphore(0);
            ids = manager.getCameraIdList();
            for (String id:ids){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                int cameraIndex = -1;
                if(facing != null){
                    cameraIndex = facing;
                }
                if(cameraIndex != 0 && cameraIndex != 1){
                    return null;
                }
                cameraId[cameraIndex] = id;
                //选择合适的预览尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null){
                    return null;
                }
                Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
                List<Size> sizes = Arrays.asList(previewSizes);
                Collections.sort(sizes,new Comparator<Size>() {
                    @Override
                    public int compare(Size o1, Size o2) {
                        return o1.getWidth() - o2.getWidth();
                    }
                });
                for (Size s : sizes) {
                    if (s.getHeight() >= minWidth) {
                        boolean isSuit = (rate >1.4f && s.getWidth() / (float) s.getHeight() > 1.4)
                                || (rate < 1.4f &&s.getWidth() / (float) s.getHeight() < 1.4);
                        if (isSuit) {
                            previewSize[cameraIndex] = s;
                            break;
                        }
                    }
                }
                if(previewSize[cameraIndex] == null){
                    previewSize[cameraIndex] = previewSizes[0];
                }
            }
            if(cameraId[0] == null && cameraId[1] == null){
                return new Point(0,0);
            }else if(cameraId[0] == null){
                cameraId[0] = cameraId[1];
            }else if(cameraId[1] == null){
                cameraId[1] = cameraId[0];
            }
            surface.setDefaultBufferSize(previewSize[nowCamera].getWidth(),previewSize[nowCamera].getHeight());
            manager.openCamera(cameraId[nowCamera],this,cameraHandler);
            this.surface = surface;
            surface.setOnFrameAvailableListener(frameListener);
            return new Point(previewSize[nowCamera].getHeight(),previewSize[nowCamera].getWidth());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        mFrameSem.drainPermits();
        mFrameSem.release();
        if (captureSession != null){
            captureSession.close();
            captureSession = null;
        }
        if (camera != null){
            camera.close();
            camera = null;
        }
        if(cameraHandler != null && cameraHandler.getLooper() != null){
            cameraHandler.getLooper().quitSafely();
            cameraHandler = null;
        }
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
        return false;
    }

    private SurfaceTexture.OnFrameAvailableListener frameListener=new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mFrameSem.drainPermits();
            mFrameSem.release();
        }

    };


    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        this.camera = camera;
        try {
            Surface target = new Surface(surface);
            captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            captureRequestBuilder.addTarget(target);
            /* //获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
             builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));*/
            camera.createCaptureSession(Collections.singletonList(target),captureSessionStateCallback,cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        camera.close();
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        camera.close();
    }

    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            try {
                session.setRepeatingRequest(captureRequestBuilder.build(),captureCallback,cameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            session.close();
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }
    };

}
