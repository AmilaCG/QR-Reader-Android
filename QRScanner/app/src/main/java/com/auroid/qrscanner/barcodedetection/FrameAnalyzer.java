package com.auroid.qrscanner.barcodedetection;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.auroid.qrscanner.camera.CameraReticleAnimator;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.WorkflowModel;
import com.auroid.qrscanner.consts.CommonDefines;
import com.auroid.qrscanner.utils.BitmapUtils;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.Objects;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "FrameAnalyzer";

    private final BarcodeScanner scanner =
            BarcodeScanning.getClient(CommonDefines.barcodeScannerOptions);
    private final GraphicOverlay mGraphicOverlay;
    private final CameraReticleAnimator mCameraReticleAnimator;
    private final WorkflowModel mWorkflowModel;
    private InputImage mInputImage;

    public FrameAnalyzer(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        mGraphicOverlay = graphicOverlay;
        mCameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        mWorkflowModel = workflowModel;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        /*TODO: Using fromMediaImage to process is the recommended way but it is temporary commented
         * since it is not working
         */
//        @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
//        if (mediaImage == null) {
//            return;
//        }
//        mInputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees);

//        long startMs = SystemClock.elapsedRealtime();
        @SuppressLint("UnsafeExperimentalUsageError")
        Bitmap input = BitmapUtils.getBitmap(imageProxy);
        if (input == null) {
            return;
        }
        mInputImage = InputImage.fromBitmap(input, rotationDegrees);
//        long endMs = SystemClock.elapsedRealtime();
//        Log.d(TAG, "Latency is: " + (endMs - startMs));

        scanner.process(mInputImage)
                .addOnSuccessListener(
                        barcodes -> {
                            if (barcodes.size() > 0) {
                                Log.d(TAG, "analyze: barcode detected");
                            }
                            processBarcode(barcodes);
                        })
                .addOnFailureListener(
                        e -> Log.e(TAG, "Barcode scanning failed", e)
                )
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void processBarcode(List<Barcode> results) {
        if (!mWorkflowModel.isCameraLive()) {
            return;
        }
        mGraphicOverlay.setAnalyzerInfo(mInputImage.getWidth(), mInputImage.getHeight());

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        Barcode barcodeInCenter = null;
        for (Barcode barcode : results) {
            RectF box = mGraphicOverlay.translateRect(Objects.requireNonNull(barcode.getBoundingBox()));
            if (box.contains(mGraphicOverlay.getWidth() / 2f, mGraphicOverlay.getHeight() / 2f)) {
                barcodeInCenter = barcode;
                break;
            }
        }

        mGraphicOverlay.clear();
        if (barcodeInCenter == null) {
            mCameraReticleAnimator.start();
            mGraphicOverlay.add(new BarcodeReticleGraphic(mGraphicOverlay, mCameraReticleAnimator));
            mWorkflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING);
        } else {
            mCameraReticleAnimator.cancel();
            mGraphicOverlay.add(new BarcodeTrackerGraphic(mGraphicOverlay, barcodeInCenter));
            mWorkflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED);
            mWorkflowModel.detectedBarcode.setValue(barcodeInCenter);
        }
        mGraphicOverlay.invalidate();
    }
}