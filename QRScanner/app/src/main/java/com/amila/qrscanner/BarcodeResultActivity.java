package com.amila.qrscanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.amila.qrscanner.resultdb.Result;
import com.amila.qrscanner.resultdb.ResultViewModel;

public class BarcodeResultActivity extends AppCompatActivity {

    private TextView mBarcodeResult;
    private String mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        Intent intent = getIntent();
        mResult = intent.getStringExtra("barcode");

        mBarcodeResult = findViewById(R.id.barcode_result);
        mBarcodeResult.setText(mResult);

        ResultViewModel resultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);
        Result result = new Result(mResult);
        resultViewModel.insert(result);
    }

    public void back (View v) {
        finish();
    }

    public void openBrowser (View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(mResult));
        startActivity(intent);
    }

    public void copyToClipboard(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRCode", mResult);
        if (clipboardManager != null) clipboardManager.setPrimaryClip(clip);
    }

    public void showHistory(View view) {
        Intent intent = new Intent(this, ScanHistory.class);
        startActivity(intent);
        finish();
    }
}
