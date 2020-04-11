package com.amila.qrscanner;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "QRScanner";

    private TextureView mTextureView;
    private Size mPreviewSize;
    private String mCameraId;

    private TextureView.SurfaceTextureListener mSurfaceTextureListner = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createTextureView();
    }

    private void createTextureView() {
        mTextureView = findViewById(R.id.texture_view);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mTextureView.isAvailable()) {
            mSurfaceTextureListner.onSurfaceTextureAvailable(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
        }else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListner);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Iterating through camera id's to get back camera's stream configuration map
            for(String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = streamConfigMap.getOutputSizes(SurfaceTexture.class);
                mPreviewSize = getPreferredPreviewSize(sizes, width, height);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> sizesCollector = new ArrayList();
        for(Size option : mapSizes) {
            if(height > width) {
                if(option.getWidth() > height && option.getHeight() > width) {
                    sizesCollector.add(option);
                }
            } else {
                if(option.getWidth() > width && option.getHeight() > height) {
                    sizesCollector.add(option);
                }
            }
        }
        if(sizesCollector.size() > 0) {
            return Collections.min(sizesCollector, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return mapSizes[0];
    }
}
