package com.auroid.qrscanner;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Objects;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.WorkflowModel;
import com.auroid.qrscanner.camera.WorkflowModel.WorkflowState;
import com.auroid.qrscanner.camera.CameraSource;
import com.auroid.qrscanner.camera.CameraSourcePreview;
import com.auroid.qrscanner.barcodedetection.BarcodeProcessor;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "LiveBarcodeActivity";

    private static final int RC_HANDLE_CAMERA_PERM = 24;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private View mSettingsButton;
    private View mHistoryButton;
    private View mFlashButton;
    private Chip mGuideChip;
    private AnimatorSet mPromptChipAnimator;
    private WorkflowModel mWorkflowModel;
    private WorkflowState mCurrentWorkflowState;
    private AudioHandler mAudioHandler;

    public static FirebaseVisionBarcode mDetectedBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreview = findViewById(R.id.camera_preview);
        mGraphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
        mGraphicOverlay.setOnClickListener(this);
        mCameraSource = new CameraSource(mGraphicOverlay);

        mGuideChip = findViewById(R.id.guide_chip);
        mPromptChipAnimator =
                (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.guide_chip_enter);
        mPromptChipAnimator.setTarget(mGuideChip);

        mFlashButton = findViewById(R.id.flash_button);
        mFlashButton.setOnClickListener(this);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mHistoryButton = findViewById(R.id.history_button);
        mHistoryButton.setOnClickListener(this);

        mAudioHandler = new AudioHandler(this);
        mAudioHandler.setupAudioBeep();

        setUpWorkflowModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWorkflowModel.markCameraFrozen();
        mSettingsButton.setEnabled(true);
        mHistoryButton.setEnabled(true);
        mCurrentWorkflowState = WorkflowState.NOT_STARTED;
        mCameraSource.setFrameProcessor(new BarcodeProcessor(mGraphicOverlay, mWorkflowModel));
        mWorkflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCurrentWorkflowState = WorkflowState.NOT_STARTED;
        stopCameraPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
        if (mAudioHandler != null) {
            mAudioHandler.release();
            mAudioHandler = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.settings_button:
                // Sets as disabled to prevent the user from clicking on it too fast.
                mSettingsButton.setEnabled(false);
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.history_button:
                // Sets as disabled to prevent the user from clicking on it too fast.
                mHistoryButton.setEnabled(false);
                startActivity(new Intent(this, ScanHistoryActivity.class));
                break;

            case R.id.flash_button:
                if (mFlashButton.isSelected()) {
                    mFlashButton.setSelected(false);
                    mCameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    mFlashButton.setSelected(true);
                    try {
                        mCameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    } catch (RuntimeException e) {
                        mFlashButton.setSelected(false);
                        Toast.makeText(this, getText(R.string.flasher_fail), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void startCameraPreview() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            startPreview();
        } else {
            requestCameraPermission();
        }
    }

    private void startPreview() {
        if (!mWorkflowModel.isCameraLive() && mCameraSource != null) {
            try {
                mWorkflowModel.markCameraLive();
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Failed to start camera preview!", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void stopCameraPreview() {
        if (mWorkflowModel.isCameraLive()) {
            mWorkflowModel.markCameraFrozen();
            mFlashButton.setSelected(false);
            mPreview.stop();
        }
    }

    private void setUpWorkflowModel() {
        mWorkflowModel = new ViewModelProvider(this).get(WorkflowModel.class);

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        mWorkflowModel.workflowState.observe(
                this,
                workflowState -> {
                    if (workflowState == null || Objects.equal(mCurrentWorkflowState, workflowState)) {
                        return;
                    }

                    mCurrentWorkflowState = workflowState;
                    Log.d(TAG, "Current workflow state: " + mCurrentWorkflowState.name());

                    boolean wasPromptChipGone = (mGuideChip.getVisibility() == View.GONE);

                    switch (workflowState) {
                        case DETECTING:
                            mGuideChip.setVisibility(View.VISIBLE);
                            mGuideChip.setText(R.string.prompt_point_at_a_barcode);
                            startCameraPreview();
                            break;
                        case CONFIRMING:
                            mGuideChip.setVisibility(View.VISIBLE);
                            mGuideChip.setText(R.string.prompt_move_camera_closer);
                            startCameraPreview();
                            break;
                        case SEARCHING:
                            mGuideChip.setVisibility(View.VISIBLE);
                            mGuideChip.setText(R.string.prompt_searching);
                            mAudioHandler.playAudioBeep();
                            stopCameraPreview();
                            break;
                        case DETECTED:
                        case SEARCHED:
                            mGuideChip.setVisibility(View.GONE);
                            mAudioHandler.playAudioBeep();
                            stopCameraPreview();
                            break;
                        default:
                            mGuideChip.setVisibility(View.GONE);
                            break;
                    }

                    boolean shouldPlayPromptChipEnteringAnimation =
                            wasPromptChipGone && (mGuideChip.getVisibility() == View.VISIBLE);
                    if (shouldPlayPromptChipEnteringAnimation && !mPromptChipAnimator.isRunning()) {
                        mPromptChipAnimator.start();
                    }
                });

        mWorkflowModel.detectedBarcode.observe(
                this,
                barcode -> {
                    if (barcode != null) {
                        mDetectedBarcode = barcode;
                        startActivity(new Intent(this, BarcodeResultActivity.class));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted");
            startPreview();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = (dialog, id) -> finish();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Reader")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .setCancelable(false)
                .show();
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;
        View.OnClickListener listener = view ->
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);

        findViewById(R.id.main_layout).setOnClickListener(listener);
        Snackbar.make(findViewById(R.id.main_layout), R.string.request_camera_permission,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }
}
