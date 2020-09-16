package com.auroid.qrscanner;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.mlkit.vision.barcode.Barcode;

import com.auroid.qrscanner.camera.CameraHandler;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.WorkflowModel;
import com.auroid.qrscanner.camera.WorkflowModel.WorkflowState;
import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.utils.Utils;
import com.auroid.qrscanner.utils.PreferenceUtils;
import com.auroid.qrscanner.utils.AppRater;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int RC_HANDLE_CAMERA_PERM = 24;
    private static final int READ_EXT_STORAGE_PERM = 25;
    private static final int RC_PHOTO_LIBRARY = 26;

    private CameraHandler mCameraHandler;

    private GraphicOverlay mGraphicOverlay;
    private View mSettingsButton;
    private View mHistoryButton;
    private View mFlashButton;
    private View mGalleryButton;
    private Chip mGuideChip;

    private WorkflowModel mWorkflowModel;
    private WorkflowState mCurrentWorkflowState;

    private AudioHandler mAudioHandler;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGraphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
        mGraphicOverlay.setOnClickListener(this);

        mGuideChip = findViewById(R.id.guide_chip);

        mFlashButton = findViewById(R.id.flash_button);
        mFlashButton.setOnClickListener(this);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mHistoryButton = findViewById(R.id.history_button);
        mHistoryButton.setOnClickListener(this);
        mGalleryButton = findViewById(R.id.gallery_button);
        mGalleryButton.setOnClickListener(this);

        mAudioHandler = new AudioHandler(this);
        mAudioHandler.setupAudioBeep();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setUpWorkflowModel();

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            requestCameraPermission();
        }

        // Only run at the first time activity launches
        if (savedInstanceState == null) {
            AppRater.app_launched(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGraphicOverlay.clear();
        mSettingsButton.setEnabled(true);
        mHistoryButton.setEnabled(true);
        mGalleryButton.setEnabled(true);
        mFlashButton.setSelected(false);
        mCurrentWorkflowState = WorkflowState.NOT_STARTED;
        mWorkflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCurrentWorkflowState = WorkflowState.NOT_STARTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraHandler != null) {
            mCameraHandler.release();
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
                mFirebaseAnalytics.logEvent("open_history", null);
                break;

            case R.id.flash_button:
                if (mFlashButton.isSelected()) {
                    mFlashButton.setSelected(false);
                    mCameraHandler.enableTorch(false);
                } else {
                    mFlashButton.setSelected(true);
                    mCameraHandler.enableTorch(true);
                }
                break;

            case R.id.gallery_button:
                mGalleryButton.setEnabled(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED) {
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions, READ_EXT_STORAGE_PERM);
                    } else {
                        Utils.openImagePicker(this);
                    }
                } else {
                    Utils.openImagePicker(this);
                }
                break;
        }
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        PreviewView previewView = findViewById(R.id.view_finder);
        mCameraHandler = new CameraHandler(
                cameraProviderFuture,
                previewView,
                this,
                mGraphicOverlay,
                mWorkflowModel);
        cameraProviderFuture.addListener(mCameraHandler, ContextCompat.getMainExecutor(this));
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

                    switch (workflowState) {
                        case DETECTING:
                            mGuideChip.setVisibility(View.VISIBLE);
                            mGuideChip.setText(R.string.prompt_point_at_a_barcode);
                            mWorkflowModel.markCameraLive();
                            break;
                        case DETECTED:
                            mGuideChip.setVisibility(View.GONE);
                            mAudioHandler.playAudioBeep();
                            mWorkflowModel.markCameraFrozen();
                            break;
                        default:
                            mGuideChip.setVisibility(View.GONE);
                            break;
                    }
                });

        mWorkflowModel.detectedBarcode.observe(
                this,
                barcode -> {
                    if (barcode != null) {
                        ResultHandler resultHandler = new ResultHandler(this);
                        resultHandler.pushToDatabase(barcode);

                        boolean openInBrowser = PreferenceUtils.shouldOpenDirectlyInBrowser(this);
                        int barcodeValueType = barcode.getValueType();
                        if (openInBrowser && barcodeValueType == Barcode.TYPE_URL) {
                            BarcodeWrapper barcodeWrapper = new BarcodeWrapper(
                                    barcodeValueType,
                                    barcode.getDisplayValue(),
                                    barcode.getRawValue());
                            barcodeWrapper.url =
                                    java.util.Objects.requireNonNull(barcode.getUrl()).getUrl();

                            ActionHandler actionHandler = new ActionHandler(this, barcodeWrapper);
                            actionHandler.openBrowser();
                        } else {
                            Intent intent = new Intent(this, BarcodeResultActivity.class);
                            intent.putExtra("RESULT", resultHandler.getResultJson());
                            startActivity(intent);
                        }
                        mFirebaseAnalytics.logEvent("scan_barcode", null);
                        resultHandler.release();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == RC_PHOTO_LIBRARY && data != null) {
            Intent intent = new Intent(this, ImageScanningActivity.class);
            intent.setData(data.getData());
            startActivity(intent);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_HANDLE_CAMERA_PERM: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted");
                    setupCamera();
                } else {
                    DialogInterface.OnClickListener listener = (dialog, id) -> finish();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.app_name)
                            .setMessage(R.string.no_camera_permission)
                            .setPositiveButton(R.string.ok, listener)
                            .setCancelable(false)
                            .show();

                    Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                            " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                }
                break;
            }
            case READ_EXT_STORAGE_PERM: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Gallery permission granted");
                    Utils.openImagePicker(this);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Image Scanning")
                            .setMessage(R.string.no_gallery_permission)
                            .show();

                    Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                            " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                }
                break;
            }
            default: {
                Log.e(TAG, "Got unexpected permission result: " + requestCode);
                break;
            }
        }
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
