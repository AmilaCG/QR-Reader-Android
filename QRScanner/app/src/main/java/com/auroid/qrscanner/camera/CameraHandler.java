package com.auroid.qrscanner.camera;

import android.util.Log;
import android.util.Size;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.auroid.qrscanner.barcodedetection.FrameAnalyzer;
import com.auroid.qrscanner.camera.WorkflowModel.WorkflowState;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraHandler implements Runnable {

    private static final String TAG = "CameraHandler";
    private static final int CAMERA_PREVIEW_WIDTH = 720;
    private static final int CAMERA_PREVIEW_HEIGHT = 1280;

    private final ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private ProcessCameraProvider mCameraProvider;
    private final PreviewView mPreviewView;
    private Camera mCamera;
    private ExecutorService mCameraExecutor;
    private final LifecycleOwner mLifecycleOwner;
    private final GraphicOverlay mGraphicOverlay;
    private final WorkflowModel mWorkflowModel;
    private boolean mReleased;

    public CameraHandler(ListenableFuture<ProcessCameraProvider> cpf,
                         PreviewView previewView,
                         LifecycleOwner lifecycleOwner,
                         GraphicOverlay graphicOverlay,
                         WorkflowModel workflowModel) {
        mCameraProviderFuture = cpf;
        mPreviewView = previewView;
        mLifecycleOwner = lifecycleOwner;
        mGraphicOverlay = graphicOverlay;
        mWorkflowModel = workflowModel;
    }

    @Override
    public void run() {
        if (mReleased) return;

        try {
            mCameraProvider = mCameraProviderFuture.get();
            bindPreview();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onCameraUnavailable(e);
        } catch (ExecutionException | CameraInfoUnavailableException
                 | IllegalArgumentException | IllegalStateException | SecurityException e) {
            onCameraUnavailable(e);
        }
    }

    private void onCameraUnavailable(Exception e) {
        Log.w(TAG, "Camera unavailable; gallery scanning is still available", e);
        release();
        mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);
    }

    private void bindPreview() throws CameraInfoUnavailableException {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        if (!mCameraProvider.hasCamera(cameraSelector)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            if (!mCameraProvider.hasCamera(cameraSelector)) {
                mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);
                return;
            }
        }

        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mCamera = mCameraProvider.bindToLifecycle(
                mLifecycleOwner, cameraSelector, preview, imageAnalyzer);

        mCameraExecutor = Executors.newSingleThreadExecutor();
        imageAnalyzer.setAnalyzer(
                mCameraExecutor,
                new FrameAnalyzer(mGraphicOverlay, mWorkflowModel));

        mWorkflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    public boolean isReady() {
        return mCamera != null;
    }

    public boolean hasFlash() {
        return mCamera != null && mCamera.getCameraInfo().hasFlashUnit();
    }

    public void enableTorch(boolean state) {
        if (hasFlash()) {
            mCamera.getCameraControl().enableTorch(state);
        }
    }

    public void release() {
        mReleased = true;
        mCamera = null;
        if (mCameraExecutor != null) mCameraExecutor.shutdown();
    }
}
