package com.amila.qrscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.amila.qrscanner.camera.CameraSource;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = "QRScanner";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private static final int FULL_SCALE = 100;

    private SurfaceView mSurfaceView;
    private boolean mIsSurfaceAvailable;
    private SurfaceHolder mSurfaceHolder;

    private Size mPreviewSize;
    private Context mContext;

    private CameraSource mCameraSource;
    private BarcodeDetector mBarcodeDetector;
    private BarcodeTrackerView mTrackerView;

    private boolean mUseFlash;

    private Handler mTaskHandler;

    public static class CameraState {
        static final int INIT_CAMERA = 0;
        static final int START_CAMERA = 1;
        static final int RELEASE_CAMERA = 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        setupTaskHandler();

        mUseFlash = false;
        mIsSurfaceAvailable = false;
        mCameraSource = null;
        mBarcodeDetector = null;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSurfaceView = findViewById(R.id.camera_viewfinder);
        mSurfaceView.setVisibility(View.GONE);
        mSurfaceHolder = mSurfaceView.getHolder();
        setupViewfinder();

        mTrackerView = findViewById(R.id.barcode_tracker_view);

        setPreviewSize();

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            initBarcodeDetector();
            mTaskHandler.sendEmptyMessage(CameraState.INIT_CAMERA);
            mSurfaceView.setVisibility(View.VISIBLE);
        } else {
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            requestCameraPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if (mBarcodeDetector == null) initBarcodeDetector();
        mTaskHandler.sendEmptyMessage(CameraState.INIT_CAMERA);
        mTaskHandler.sendEmptyMessage(CameraState.START_CAMERA);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        mTrackerView.clearView();
        if (mCameraSource != null) mCameraSource.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mTaskHandler.sendEmptyMessage(CameraState.RELEASE_CAMERA);
            mCameraSource = null;
        }
    }

    private void setupTaskHandler() {
        mTaskHandler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case CameraState.INIT_CAMERA:
                        if (mCameraSource == null) initCamera(mUseFlash);
                        break;

                    case CameraState.START_CAMERA:
                        startCamera();
                        break;

                    case CameraState.RELEASE_CAMERA:
                        Log.d(TAG, "RELEASE_CAMERA called");
                        if (mCameraSource != null) mCameraSource.release();
                        mCameraSource = null;
                        break;
                }
            }
        };
    }

    private void setupViewfinder() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                mIsSurfaceAvailable = true;
                mTaskHandler.sendEmptyMessage(CameraState.START_CAMERA);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                mIsSurfaceAvailable = false;
                if (mCameraSource != null) mCameraSource.stop();
            }
        });
    }

    private void initBarcodeDetector() {
        mBarcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE | Barcode.DATA_MATRIX | Barcode.AZTEC)
                .build();

        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Log.d(TAG,"barcodeDetector Processor release");
                mBarcodeDetector = null;
                mTaskHandler.sendEmptyMessage(CameraState.RELEASE_CAMERA);
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (detections.getDetectedItems().size() != 0) {
                    Barcode detectedBarcode = detections.getDetectedItems().valueAt(0);

                    mTrackerView.updateView(detectedBarcode.cornerPoints);

                    String result = detectedBarcode.displayValue;
                    Log.d(TAG,"Barcode decoded: " + result);

                    Intent intent = new Intent(mContext, BarcodeResultActivity.class);
                    intent.putExtra("barcode", result);
                    startActivity(intent);

                    mBarcodeDetector.release();
                } else {
                    mTrackerView.clearView();
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void initCamera(boolean useFlash) {
        if (mBarcodeDetector == null) {
            Log.e(TAG, "Barcode detector is null");
            return;
        }

        if (!mBarcodeDetector.isOperational()) {
            Log.e(TAG, "Detector dependencies are not available");

            File cacheDir = getCacheDir();
            // Check for low storage
            if (cacheDir.getFreeSpace() * 100 / cacheDir.getTotalSpace() <= 10) {
                Toast.makeText(mContext, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            } else {
                Log.e(TAG, getString(R.string.unknown_download_error));
            }
        }

        mCameraSource = new CameraSource.Builder(mContext, mBarcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight())
                .setRequestedFps(20.0f)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();

        ToggleButton tbFlasher = findViewById(R.id.flasher);
        tbFlasher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mUseFlash = true;
                } else {
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mUseFlash = false;
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startCamera() {
        // Check that the device has play services available
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            if (mIsSurfaceAvailable) {
                try {
                    mCameraSource.start(mSurfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
            mTaskHandler.sendEmptyMessage(CameraState.INIT_CAMERA);
            mSurfaceView.setVisibility(View.VISIBLE);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Reader")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton("OK", listener)
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

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(R.id.main_layout).setOnClickListener(listener);
        Snackbar.make(findViewById(R.id.main_layout), R.string.request_camera_permission,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", listener)
                .show();
    }

    private void setPreviewSize() {
        int scaleFactor = 65;

        Rect displaySize = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(displaySize);

        mPreviewSize = new Size(displaySize.width() * scaleFactor / FULL_SCALE, displaySize.height() * scaleFactor / FULL_SCALE);
        displaySize = null;

        if (mPreviewSize.getHeight() > mPreviewSize.getWidth()) {
            mPreviewSize = new Size(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
        Log.d(TAG, "mPreviewSize width: " + mPreviewSize.getWidth() + ", height: " + mPreviewSize.getHeight());
    }
}
