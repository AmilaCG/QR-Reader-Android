package com.auroid.qrscanner.barcodedetection;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.WorkflowModel;
import com.auroid.qrscanner.consts.CommonDefines;
import com.auroid.qrscanner.utils.BitmapUtils;

import com.auroid.qrscanner.utils.PreferenceUtils;
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
    private final WorkflowModel mWorkflowModel;
    private InputImage mInputImage;
    private final int mCropPercentage;

    public FrameAnalyzer(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        mGraphicOverlay = graphicOverlay;
        mWorkflowModel = workflowModel;
        mCropPercentage =
                PreferenceUtils.getCropPrecentages(graphicOverlay.getContext()).getWidth() + 10;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        //TODO: Find a method to crop and feed only the reticle box area for processing, without
        // converting to a bitmap
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            return;
        }
        mediaImage.getPlanes();
        mInputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees);

//        long startMs = SystemClock.elapsedRealtime();
//        Bitmap input = cropImage(imageProxy);
//        if (input == null) {
//            return;
//        }
//        mInputImage = InputImage.fromBitmap(input, rotationDegrees);
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
            mGraphicOverlay.add(new BarcodeGraphicBase(mGraphicOverlay) {
                @Override
                protected void draw(Canvas canvas) {
                    super.draw(canvas);
                }
            });
            mWorkflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING);
        } else {
            mGraphicOverlay.add(new BarcodeTrackerGraphic(mGraphicOverlay, barcodeInCenter));
            mWorkflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED);
            mWorkflowModel.detectedBarcode.setValue(barcodeInCenter);
        }
        mGraphicOverlay.invalidate();
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private Bitmap cropImage(ImageProxy imageProxy) {
        Bitmap bitmap = BitmapUtils.getBitmap(imageProxy);
        if (bitmap == null) {
            Log.e(TAG, "cropImage: Bitmap conversion failed");
            return null;
        }
        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();

        int boxSideLength;
        if (imageWidth <= imageHeight) {
            boxSideLength = (int) imageWidth * mCropPercentage / 100;
        } else {
            boxSideLength = (int) imageHeight * mCropPercentage / 100;
        }

        int cx = imageWidth / 2;
        int cy = imageHeight / 2;

        int left = cx - boxSideLength / 2;
        int top = cy - boxSideLength / 2;
        int right = cx + boxSideLength / 2;
        int bottom = cy + boxSideLength / 2;

        return Bitmap.createBitmap(bitmap, left, top, boxSideLength, boxSideLength);
    }

    private Rect getCropRect(Image mediaImage) {
        int imageWidth = mediaImage.getWidth();
        int imageHeight = mediaImage.getHeight();

        int boxSideLength;
        if (imageWidth <= imageHeight) {
            boxSideLength = (int) imageWidth * mCropPercentage / 100;
        } else {
            boxSideLength = (int) imageHeight * mCropPercentage / 100;
        }

        int cx = imageWidth / 2;
        int cy = imageHeight / 2;

        int left = cx - boxSideLength / 2;
        int top = cy - boxSideLength / 2;
        int right = cx + boxSideLength / 2;
        int bottom = cy + boxSideLength / 2;

        return new Rect(left, top, right, bottom);
    }
}
