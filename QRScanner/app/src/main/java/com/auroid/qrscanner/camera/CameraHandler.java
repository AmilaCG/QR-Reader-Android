package com.auroid.qrscanner.camera;

import android.util.Size;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.auroid.qrscanner.barcodedetection.FrameAnalyzer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraHandler implements Runnable {

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
        try {
            mCameraProvider = mCameraProviderFuture.get();
            bindPreview();
        } catch (ExecutionException | InterruptedException e) {
            // No errors need to be handled for this Future.
            // This should never be reached.
        }
    }

    private void bindPreview() {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        mCameraExecutor = Executors.newSingleThreadExecutor();
        imageAnalyzer.setAnalyzer(
                mCameraExecutor,
                new FrameAnalyzer(mGraphicOverlay, mWorkflowModel));

        mCamera = mCameraProvider.bindToLifecycle(
                mLifecycleOwner, cameraSelector, preview, imageAnalyzer);
    }

    public void enableTorch(boolean state) {
        mCamera.getCameraControl().enableTorch(state);
    }

    public void release() {
        if (mCameraExecutor != null) mCameraExecutor.shutdown();
    }
}
