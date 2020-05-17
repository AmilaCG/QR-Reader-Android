package com.auroid.qrscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import com.auroid.qrscanner.camera.CameraSource;
import com.auroid.qrscanner.camera.PreviewFrameSetListener;
import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultViewModel;
import com.auroid.qrscanner.serializable.BarcodeWrapper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "QRScanner";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private static final int FULL_SCALE = 100;

    private static final int RELEASE_BARCODE_DETECTOR = 10;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private Size mReqPreviewSize;
    private Context mContext;

    private CameraSource mCameraSource;
    private BarcodeDetector mBarcodeDetector;
    private BarcodeTrackerView mTrackerView;
    public static Barcode mDetectedBarcode;

    private boolean mUseFlash;

    private Handler mTaskHandler;
    private HandlerThread mHandlerThread;

    private SoundPool mSoundPool;
    private int mBeep;

    private SharedPreferences mSharedPrefs;

    private ViewModelStoreOwner mViewModelStoreOwner;

    private TextView mTvScanGuide;

    public static class CameraState {
        static final int INIT_CAMERA = 0;
        static final int START_CAMERA = 1;
        static final int STOP_CAMERA = 2;
        static final int RELEASE_CAMERA = 3;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        mUseFlash = false;
        mCameraSource = null;
        mBarcodeDetector = null;
        mSoundPool = null;
        mViewModelStoreOwner = this;

        mTvScanGuide = findViewById(R.id.txt_scan_guide);

        setupBottomAppBar();
        setupAudioBeep();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        setupViewfinder();

        mTrackerView = findViewById(R.id.barcode_tracker_view);

        setPreviewSize();

        mHandlerThread = new HandlerThread("QRRead");
        mHandlerThread.start();
        setupTaskHandler();

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            initBarcodeDetector();
            mTaskHandler.sendEmptyMessage(CameraState.INIT_CAMERA);
            mSurfaceView.setVisibility(View.VISIBLE);
        } else {
            requestCameraPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if (mBarcodeDetector == null) initBarcodeDetector();
        mTaskHandler.sendEmptyMessage(CameraState.INIT_CAMERA);
        mSurfaceView.setVisibility(View.VISIBLE);
        mTvScanGuide.setVisibility(View.VISIBLE);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        mTrackerView.clearView();
        mTaskHandler.sendEmptyMessage(CameraState.STOP_CAMERA);
        mTvScanGuide.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskHandler.sendEmptyMessage(RELEASE_BARCODE_DETECTOR);
        mTaskHandler.sendEmptyMessage(CameraState.RELEASE_CAMERA);

        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    private void setupTaskHandler() {
        mTaskHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case CameraState.INIT_CAMERA:
                        Log.d(TAG,"INIT_CAMERA");
                        if (mCameraSource == null) initCamera(mUseFlash);
                        break;

                    case CameraState.START_CAMERA:
                        Log.d(TAG,"START_CAMERA");
                        startCamera();
                        break;

                    case CameraState.STOP_CAMERA:
                        Log.d(TAG,"STOP_CAMERA");
                        if (mCameraSource != null) mCameraSource.stop();
                        break;

                    case CameraState.RELEASE_CAMERA:
                        Log.d(TAG, "RELEASE_CAMERA");
                        if (mCameraSource != null) {
                            mCameraSource.release();
                            mCameraSource = null;
                        }
                        break;

                    case RELEASE_BARCODE_DETECTOR:
                        Log.d(TAG,"RELEASE_BARCODE_DETECTOR");
                        if (mBarcodeDetector != null) {
                            mBarcodeDetector.release();
                            mBarcodeDetector = null;
                        }
                        break;
                }
            }
        };
    }

    private void setupViewfinder() {
        mSurfaceView = findViewById(R.id.camera_viewfinder);

        mSurfaceView.setVisibility(View.GONE);
        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
                mTaskHandler.sendEmptyMessage(CameraState.START_CAMERA);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");

                mTaskHandler.sendEmptyMessage(RELEASE_BARCODE_DETECTOR);
                mTaskHandler.sendEmptyMessage(CameraState.RELEASE_CAMERA);
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
            }

            String currentBarcode = null;
            int confirmCounter = 0;
            final static int CONFIRM_VALUE = 3;
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (detections.getDetectedItems().size() != 0) {
                    mDetectedBarcode = detections.getDetectedItems().valueAt(0);
                    if (currentBarcode != null && currentBarcode.equals(mDetectedBarcode.displayValue)) {
                        confirmCounter++;
                        if (confirmCounter >= CONFIRM_VALUE){
                            confirmCounter = 0;

                            if (mSharedPrefs.getBoolean("audio_beep", true)) {
                                mSoundPool.play(mBeep, 1, 1, 0, 0, 1);
                            }
                            mTrackerView.updateView(mDetectedBarcode.cornerPoints);

                            mTaskHandler.sendEmptyMessage(CameraState.STOP_CAMERA);

                            AsyncTask.execute(() -> {
                                boolean openInBrowser =
                                        mSharedPrefs.getBoolean("open_browser", false);

                                if (openInBrowser && mDetectedBarcode.valueFormat == Barcode.URL) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(mDetectedBarcode.url.url));
                                    startActivity(intent);

                                    BarcodeWrapper barcode = new BarcodeWrapper(mDetectedBarcode.valueFormat,
                                                    mDetectedBarcode.displayValue);
                                    barcode.url = mDetectedBarcode.url.url;
                                    Gson gson = new Gson();
                                    String resultJson = gson.toJson(barcode);

                                    // Insert result to the database
                                    ResultViewModel resultViewModel =
                                            new ViewModelProvider(mViewModelStoreOwner).get(ResultViewModel.class);
                                    Result result = new Result(resultJson, new Date());
                                    resultViewModel.insert(result);
                                } else {
                                    startActivity(new Intent(mContext, BarcodeResultActivity.class));
                                }
                            });

                            mTaskHandler.sendEmptyMessage(RELEASE_BARCODE_DETECTOR);
                        }
                    } else {
                        currentBarcode = mDetectedBarcode.displayValue;
                        confirmCounter = 0;
                    }
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

            DialogInterface.OnClickListener listener = (dialog, id) -> finish();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Dependencies not found")
                    .setMessage(R.string.dependencies_not_found)
                    .setPositiveButton("OK", listener)
                    .setCancelable(false)
                    .show();
        }

        if (mCameraSource == null) {
            PreviewFrameSetListener surfaceSizeCalc =
                    new SurfaceSizeCalculator(mContext, mSurfaceView, mTrackerView);

            mCameraSource = new CameraSource.Builder(mContext, mBarcodeDetector, surfaceSizeCalc)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(mReqPreviewSize.getWidth(), mReqPreviewSize.getHeight())
                    .setRequestedFps(20.0f)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                    .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                    .build();
        } else {
            Log.e(TAG, "initCamera: Init camera failed. mCameraSource is not null");
        }
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
            try {
                mCameraSource.start(mSurfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
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

        DialogInterface.OnClickListener listener = (dialog, id) -> finish();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code Reader")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton("OK", listener)
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

        View.OnClickListener listener = view -> {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
        };

        findViewById(R.id.main_layout).setOnClickListener(listener);
        Snackbar.make(findViewById(R.id.main_layout), R.string.request_camera_permission,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", listener)
                .show();
    }

    private void setupBottomAppBar() {
        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        bottomAppBar.setOnMenuItemClickListener((item) -> {
            int id = item.getItemId();
            switch(id) {
                case R.id.action_torch:
                    if(mUseFlash) {
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        item.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_flash_off_white_24dp));
                        mUseFlash = !mUseFlash;
                    } else {
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        item.setIcon(ContextCompat.getDrawable(mContext, R.drawable.ic_flash_on_white_24dp));
                        mUseFlash = !mUseFlash;
                    }
                    return true;

                case R.id.action_history:
                    startActivity(new Intent(this, ScanHistoryActivity.class));
                    return true;

                case R.id.action_settings:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;

                default:
                    return false;
            }
        });
    }

    private void setupAudioBeep() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        mBeep = mSoundPool.load(this, R.raw.beep, 1);
    }

    private void setPreviewSize() {
        int scaleFactor = 65;

        Rect displaySize = new Rect();
        getWindowManager().getDefaultDisplay().getRectSize(displaySize);

        mReqPreviewSize =
                new Size(displaySize.width() * scaleFactor / FULL_SCALE, displaySize.height() * scaleFactor / FULL_SCALE);
        displaySize = null;

        if (mReqPreviewSize.getHeight() > mReqPreviewSize.getWidth()) {
            mReqPreviewSize = new Size(mReqPreviewSize.getHeight(), mReqPreviewSize.getWidth());
        }
        Log.d(TAG, "mReqPreviewSize width: " + mReqPreviewSize.getWidth() + ", height: " + mReqPreviewSize.getHeight());
    }
}
