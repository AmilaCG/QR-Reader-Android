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
import com.auroid.qrscanner.utils.TypeSelector;
import com.auroid.qrscanner.wifi.WifiConnectionHelper;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class BarcodeResultActivity extends AppCompatActivity implements View.OnClickListener,
        WifiConnectionHelper.Host {

    private static final String TAG = "BarcodeResultActivity";

    private int mResultType;

    private BarcodeWrapper mBarcodeWrapper;
    private int mBarcodeFormat = -1;
    private WifiConnectionHelper mWifiConnectionHelper;

    @Override
    public WifiConnectionHelper getWifiConnectionHelper() {
        return mWifiConnectionHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiConnectionHelper = new WifiConnectionHelper(this);
        setContentView(R.layout.activity_barcode_result);
        com.auroid.qrscanner.utils.Utils.applySystemBarInsets(this);

        findViewById(R.id.back_button).setOnClickListener(this);
        findViewById(R.id.top_action_button).setVisibility(View.GONE);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String barcodeJson = bundle.getString("RESULT");
            mBarcodeFormat = bundle.getInt("FORMAT", Barcode.FORMAT_UNKNOWN);
            Gson gson = new Gson();
            try {
                mBarcodeWrapper = gson.fromJson(barcodeJson, BarcodeWrapper.class);
            } catch (JsonSyntaxException e) {
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: json is not a valid representation for BarcodeWrapper", e);
                FirebaseCrashlytics.getInstance().recordException(e);
                finish();
                return;
            }
            if (mBarcodeWrapper == null) {
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: mBarcodeWrapper is null");
                finish();
                return;
            }
        } else {
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onCreate: Intent bundle is null");
            finish();
            return;
        }

        TextView tvBarcodeFormat = findViewById(R.id.barcode_format);
        if (mBarcodeWrapper.barcodeFormat != null) {
            mBarcodeFormat = mBarcodeWrapper.barcodeFormat;
        }
        if (mBarcodeFormat > 0) {
            tvBarcodeFormat.setText(TypeSelector.barcodeFormatAsString(mBarcodeFormat));
        } else {
            tvBarcodeFormat.setVisibility(View.GONE);
        }
        showResult();
    }

    private void showResult() {
        mResultType = mBarcodeWrapper.valueFormat;
        ActionHandler handler = new ActionHandler(this, mBarcodeWrapper);
        ((TextView) findViewById(R.id.result_type)).setText(ResultActions.typeLabel(mResultType));
        TextView details = findViewById(R.id.barcode_result);
        details.setText(handler.getDetails());
        if (mResultType == Barcode.TYPE_CONTACT_INFO || mResultType == Barcode.TYPE_CALENDAR_EVENT
                || mResultType == Barcode.TYPE_EMAIL || mResultType == Barcode.TYPE_SMS) {
            details.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            details.setTextSize(18);
        }
        details.setMovementMethod(mResultType == Barcode.TYPE_CONTACT_INFO
                ? LinkMovementMethod.getInstance() : android.text.method.ScrollingMovementMethod.getInstance());

        java.util.List<ResultActions.Action> actions = ResultActions.forBarcode(mBarcodeWrapper);
        int[] buttons = {R.id.ib_action, R.id.ib_copy, R.id.ib_search};
        int[] labels = {R.id.txt_action, R.id.txt_copy, R.id.txt_search};
        for (int i = 0; i < buttons.length; i++) {
            ImageButton button = findViewById(buttons[i]);
            TextView label = findViewById(labels[i]);
            boolean visible = i < actions.size();
            button.setVisibility(visible ? View.VISIBLE : View.GONE);
            label.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                ResultActions.Action action = actions.get(i);
                button.setImageResource(action.icon);
                button.setContentDescription(getString(action.label));
                label.setText(action.label);
                button.setOnClickListener(v -> handler.perform(action));
            }
        }
        // Keep the existing button chain evenly spaced when one or two actions are present.
        androidx.constraintlayout.widget.ConstraintLayout layout =
                (androidx.constraintlayout.widget.ConstraintLayout) findViewById(R.id.ib_copy).getParent();
        androidx.constraintlayout.widget.ConstraintSet constraints = new androidx.constraintlayout.widget.ConstraintSet();
        constraints.clone(layout);
        constraints.connect(R.id.barcode_result, androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
                actions.isEmpty() ? androidx.constraintlayout.widget.ConstraintSet.PARENT_ID : buttons[0],
                actions.isEmpty() ? androidx.constraintlayout.widget.ConstraintSet.BOTTOM
                        : androidx.constraintlayout.widget.ConstraintSet.TOP, 16);
        for (int button : buttons) {
            constraints.clear(button, androidx.constraintlayout.widget.ConstraintSet.START);
            constraints.clear(button, androidx.constraintlayout.widget.ConstraintSet.END);
        }
        if (actions.size() == 1) {
            constraints.connect(buttons[0], androidx.constraintlayout.widget.ConstraintSet.START,
                    androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START);
            constraints.connect(buttons[0], androidx.constraintlayout.widget.ConstraintSet.END,
                    androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END);
        } else if (actions.size() > 1) {
            int[] visibleButtons = java.util.Arrays.copyOf(buttons, actions.size());
            constraints.createHorizontalChainRtl(
                    androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
                    androidx.constraintlayout.widget.ConstraintSet.START,
                    androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
                    androidx.constraintlayout.widget.ConstraintSet.END,
                    visibleButtons,
                    null, androidx.constraintlayout.widget.ConstraintSet.CHAIN_SPREAD);
        }
        constraints.applyTo(layout);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_button) {
            onBackPressed();
        }
    }
}
