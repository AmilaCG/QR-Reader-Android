package com.amila.qrscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private static final String TAG = "QRScanner";

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private SurfaceView mSurfaceView;
    private Size mPreviewSize;
    private Context mContext;
    private CameraSource mCameraSource;
    private boolean mUseFlash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSurfaceView = findViewById(R.id.camera_viewfinder);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            initCamera(mUseFlash);
            setupViewfinder();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");

    }

    private void initCamera(boolean useFlash) {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Log.d(TAG,"barcodeDetector Processor release");
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if(detections.getDetectedItems().size() > 0) {
                    String result = detections.getDetectedItems().valueAt(0).rawValue;
                    Log.d(TAG,"Barcode decoded: " + result);

                    Intent intent = new Intent(mContext, BarcodeResultActivity.class);
                    intent.putExtra("barcode", result);
                    startActivity(intent);
                }
            }
        });

        if (!barcodeDetector.isOperational()) {
            Log.w(TAG, "Detector dependencies are not available");

            File cacheDir = getCacheDir();
            // Check for low storage
            if (cacheDir.getFreeSpace() * 100 / cacheDir.getTotalSpace() <= 10) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            } else {
                Log.e(TAG, getString(R.string.unknown_download_error));
            }
        }

        mCameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                //.setRequestedPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight())
                .build();
        //TODO: Need to implement a method to use flasher
    }

    private void setupViewfinder() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG,"surfaceCreated");
                try {
                    mCameraSource.start(mSurfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG,"surfaceDestroyed");
                mCameraSource.stop();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted");
            initCamera(mUseFlash);
            setupViewfinder();
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

    public void toggleFlash(View view) {
        mUseFlash = !mUseFlash;
    }
}
