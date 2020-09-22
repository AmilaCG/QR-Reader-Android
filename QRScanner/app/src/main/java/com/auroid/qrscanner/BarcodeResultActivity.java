package com.auroid.qrscanner;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.auroid.qrscanner.serializable.BarcodeWrapper;

import com.auroid.qrscanner.utils.AppRater;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class BarcodeResultActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "BarcodeResultActivity";

    private int mResultType;

    private BarcodeWrapper mBarcodeWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        findViewById(R.id.back_button).setOnClickListener(this);
        findViewById(R.id.top_action_button).setVisibility(View.GONE);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String barcodeJson = bundle.getString("RESULT");
            Gson gson = new Gson();
            try {
                mBarcodeWrapper = gson.fromJson(barcodeJson, BarcodeWrapper.class);
            } catch (JsonSyntaxException e) {
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: json is not a valid representation for BarcodeWrapper", e);
                FirebaseCrashlytics.getInstance().recordException(e);
                finish();
            }
            if (mBarcodeWrapper == null) {
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: mBarcodeWrapper is null");
                finish();
            }
        } else {
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onCreate: Intent bundle is null");
            finish();
        }

        AppRater.showRateDialog(this);

        String result = mBarcodeWrapper.displayValue;
        String rawValue = mBarcodeWrapper.rawValue;
        mResultType = mBarcodeWrapper.valueFormat;

        TextView tvBarcodeResult = findViewById(R.id.barcode_result);
        tvBarcodeResult.setText(result);

        TextView tvAction = findViewById(R.id.txt_action);
        ImageButton ibAction = findViewById(R.id.ib_action);

        switch (mResultType) {
            case Barcode.TYPE_URL:
                Log.d(TAG, "URL");
                tvBarcodeResult.setText(rawValue);
                tvAction.setText(R.string.action_web);
                ibAction.setImageResource(R.drawable.ic_public_black_44dp);
                break;

            case Barcode.TYPE_PHONE:
                Log.d(TAG, "PHONE");
                tvAction.setText(R.string.action_phone);
                ibAction.setImageResource(R.drawable.ic_phone_black_38dp);
                break;

            case Barcode.TYPE_GEO:
                Log.d(TAG, "GEO");
                tvAction.setText(R.string.action_geo);
                ibAction.setImageResource(R.drawable.ic_location_on_black_38dp);
                break;

            case Barcode.TYPE_CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                ActionHandler actionEvent = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionEvent.getFormattedEventDetails());
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                tvAction.setText(R.string.action_calender);
                ibAction.setImageResource(R.drawable.ic_calender_black_38dp);
                break;

            case Barcode.TYPE_CONTACT_INFO:
                Log.d(TAG, "CONTACT_INFO");
                ActionHandler actionContact = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionContact.getFormattedContactDetails());
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                tvBarcodeResult.setTextSize(18);
                tvBarcodeResult.setMovementMethod(LinkMovementMethod.getInstance());

                tvAction.setText(R.string.action_contact);
                ibAction.setImageResource(R.drawable.ic_person_add_black_38dp);
                break;

            case Barcode.TYPE_WIFI:
                Log.d(TAG, "WIFI");
                ActionHandler actionWifi = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionWifi.getFormattedWiFiDetails());

                tvAction.setText(R.string.action_wifi);
                ibAction.setImageResource(R.drawable.ic_wifi_black_38);
                break;

            default:
                Log.d(TAG, "default");
                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);
                break;
        }
    }

    public void doAction(View v) {
        ActionHandler actionHandler = new ActionHandler(this, mBarcodeWrapper);

        switch (mResultType) {
            case Barcode.TYPE_URL:
                actionHandler.openBrowser();
                break;

            case Barcode.TYPE_PHONE:
                actionHandler.openDialer();
                break;

            case Barcode.TYPE_CALENDAR_EVENT:
                actionHandler.addToCalender();
                break;

            case Barcode.TYPE_GEO:
                actionHandler.openMaps();
                break;

            case Barcode.TYPE_CONTACT_INFO:
                actionHandler.addToContacts();
                break;

            case Barcode.TYPE_WIFI:
                actionHandler.connectToWifi();
                break;
        }
    }

    public void copyToClipboard(View view) {
        ActionHandler actionHandler = new ActionHandler(this, mBarcodeWrapper);
        actionHandler.copyToClipboard();
    }

    public void webSearch(View view) {
        ActionHandler actionHandler = new ActionHandler(this, mBarcodeWrapper);
        actionHandler.webSearch();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_button) {
            onBackPressed();
        }
    }
}
