package com.auroid.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageScanningActivity extends AppCompatActivity {

    private static final String TAG = "ImageScanningActivity";

    private ImageView mImagePreview;
    private ProgressBar progressBar;
    private final ExecutorService imageDecodeExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_scanning);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mImagePreview = findViewById(R.id.image_preview);
        progressBar = findViewById(R.id.progressBar);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("IMAGE")) {
            Uri pickedImageUri = Uri.parse(extras.getString("IMAGE"));
            convertAndDecode(pickedImageUri);
        }
    }

    private void convertAndDecode(Uri uri) {
        final Bitmap[] bitmap = new Bitmap[1];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), uri);
            imageDecodeExecutor.execute(() -> {
                try {
                    bitmap[0] = ImageDecoder.decodeBitmap(source);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> {
                    decodeImage(bitmap[0]);
                    progressBar.setVisibility(View.GONE);
                });
            });
        } else {
            imageDecodeExecutor.execute(() -> {
                try {
                    bitmap[0] = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> {
                    decodeImage(bitmap[0]);
                    progressBar.setVisibility(View.GONE);
                });
            });
        }
    }

    private void decodeImage(Bitmap bitmap) {
        if (bitmap != null) {
            mImagePreview.setImageBitmap(bitmap);

            ImageScanner imageScanner = new ImageScanner(bitmap);
            imageScanner.decode();
            imageScanner.release();
        } else {
            Log.e(TAG, "onActivityResult: Something is wrong with the selected image");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        this.finish();
        return true;
    }
}