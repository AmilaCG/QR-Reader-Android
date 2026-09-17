package com.auroid.qrscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import com.google.mlkit.vision.barcode.common.Barcode;

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
        com.auroid.qrscanner.utils.Utils.applySystemBarInsets(this);

        mGraphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
        // Isolate CLEAR blending so the cutout only erases the overlay
        mGraphicOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
        mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                && rc != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }

        // Only run at the first time activity launches
        if (savedInstanceState == null) {
            AppRater.appLaunched(this);
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
        if (mCameraHandler == null
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else if (mCameraHandler != null && mCameraHandler.isReady()) {
            mWorkflowModel.setWorkflowState(WorkflowState.DETECTING);
        } else if (mWorkflowModel.workflowState.getValue() == WorkflowState.CAMERA_UNAVAILABLE) {
            mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);
        }
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
        int id = view.getId();
        if (id == R.id.settings_button) {
            mSettingsButton.setEnabled(false);
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.history_button) {
            mHistoryButton.setEnabled(false);
            startActivity(new Intent(this, ScanHistoryActivity.class));
            mFirebaseAnalytics.logEvent("open_history", null);
        } else if (id == R.id.flash_button) {
            if (mCameraHandler != null) {
                boolean enabled = !mFlashButton.isSelected();
                mFlashButton.setSelected(enabled);
                mCameraHandler.enableTorch(enabled);
            }
        } else if (id == R.id.gallery_button) {
            mGalleryButton.setEnabled(false);
            Utils.openImagePicker(this);
        }
    }

    private void setupCamera() {
        if (mCameraHandler != null) return;

        mWorkflowModel.setWorkflowState(WorkflowState.NOT_STARTED);
        try {
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
        } catch (IllegalStateException | SecurityException e) {
            Log.w(TAG, "Unable to initialize camera", e);
            mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);
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

                    boolean cameraReady = mCameraHandler != null && mCameraHandler.isReady();
                    mGraphicOverlay.setVisibility(cameraReady ? View.VISIBLE : View.GONE);
                    mFlashButton.setVisibility(cameraReady && mCameraHandler.hasFlash()
                            ? View.VISIBLE : View.GONE);

                    switch (workflowState) {
                        case CAMERA_UNAVAILABLE:
                            mGraphicOverlay.clear();
                            mGuideChip.setVisibility(View.VISIBLE);
                            mGuideChip.setText(R.string.camera_unavailable);
                            mWorkflowModel.markCameraFrozen();
                            break;

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
                            mWorkflowModel.markCameraFrozen();
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
                            intent.putExtra("FORMAT", barcode.getFormat());
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                setupCamera();
            } else {
                mWorkflowModel.setWorkflowState(WorkflowState.CAMERA_UNAVAILABLE);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.no_camera_permission)
                        .setPositiveButton(R.string.ok, null)
                        .setNeutralButton(R.string.activity_label_settings, (dialog, id) ->
                                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getPackageName()))))
                        .show();

                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
            }
        } else {
            Log.e(TAG, "Got unexpected permission result: " + requestCode);
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

        Snackbar.make(findViewById(R.id.main_layout), R.string.request_camera_permission,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }
}
