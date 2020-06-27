/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.auroid.qrscanner.barcodedetection;

import android.graphics.RectF;
import android.util.Log;
import androidx.annotation.MainThread;
import com.google.android.gms.tasks.Task;
import com.auroid.qrscanner.camera.CameraReticleAnimator;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.common.InputImage;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.WorkflowModel;
import com.auroid.qrscanner.camera.WorkflowModel.WorkflowState;
import com.auroid.qrscanner.camera.FrameProcessorBase;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import java.util.List;
import java.util.Objects;

/** A processor to run the barcode detector. */
public class BarcodeProcessor extends FrameProcessorBase<List<Barcode>> {

    private static final String TAG = "BarcodeProcessor";

    private final BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_CODE_128)
            .build();

    private final BarcodeScanner scanner = BarcodeScanning.getClient(options);
    private final WorkflowModel workflowModel;
    private final CameraReticleAnimator cameraReticleAnimator;

    public BarcodeProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        this.workflowModel = workflowModel;
        this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
    }

    @Override
    protected Task<List<Barcode>> detectInImage(InputImage image) {
        return scanner.process(image);
    }

    @MainThread
    @Override
    protected void onSuccess(
            InputImage image,
            List<Barcode> results,
            GraphicOverlay graphicOverlay) {
        if (!workflowModel.isCameraLive()) {
            return;
        }

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        Barcode barcodeInCenter = null;
        for (Barcode barcode : results) {
            RectF box = graphicOverlay.translateRect(Objects.requireNonNull(barcode.getBoundingBox()));
            if (box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f)) {
                barcodeInCenter = barcode;
                break;
            }
        }

        graphicOverlay.clear();
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start();
            graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
            workflowModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            cameraReticleAnimator.cancel();
            graphicOverlay.add(new BarcodeTrackerGraphic(graphicOverlay, barcodeInCenter));
            workflowModel.setWorkflowState(WorkflowState.DETECTED);
            workflowModel.detectedBarcode.setValue(barcodeInCenter);
        }
        graphicOverlay.invalidate();
    }

    @Override
    protected void onFailure(Exception e) {
        Log.e(TAG, "Barcode detection failed!", e);
    }

    @Override
    public void stop() {
        scanner.close();
    }
}
