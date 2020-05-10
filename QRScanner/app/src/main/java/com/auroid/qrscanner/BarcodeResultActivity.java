package com.auroid.qrscanner;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BarcodeResultActivity extends AppCompatActivity {

    private String mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        Intent intent = getIntent();
        mResult = intent.getStringExtra("barcode");

        TextView barcodeResult = findViewById(R.id.barcode_result);
        barcodeResult.setText(mResult);
    }

    public void back (View v) {
        finish();
    }

    public void openBrowser (View v) {
        boolean isValidURL =
                Patterns.WEB_URL.matcher(mResult).matches();

        if (isValidURL) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mResult));
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.invalid_url),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void copyToClipboard(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRCode", mResult);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clip);

            Toast.makeText(this, getString(R.string.confirm_copy_to_clipboard),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void webSearch(View view) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, mResult);
        startActivity(intent);
    }
}
