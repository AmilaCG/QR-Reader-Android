package com.amila.qrscanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

public class BarcodeResultActivity extends Activity {

    private TextView mBarcodeResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int width = displayMetrics.widthPixels;
//        int height = displayMetrics.heightPixels;
//
//        getWindow().setLayout((int)(width * 0.8), (int)(height * 0.6));

        Intent intent = getIntent();
        String result = intent.getStringExtra("barcode");

        mBarcodeResult = findViewById(R.id.barcode_result);
        mBarcodeResult.setText(result);
    }

    public void back (View v) {
        finish();
    }
}
