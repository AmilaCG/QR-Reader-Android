package com.auroid.qrscanner;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.vision.barcode.Barcode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BarcodeResultActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeResultActivity";

    private String mResult;
    private int mResultType;

    private Barcode.CalendarEvent mCalEvent;
    private String mEventStart;
    private String mEventEnd;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        mResult = MainActivity.mDetectedBarcode.displayValue;
        mResultType = MainActivity.mDetectedBarcode.valueFormat;

        TextView tvBarcodeResult = findViewById(R.id.barcode_result);
        tvBarcodeResult.setText(mResult);

        TextView tvAction = findViewById(R.id.txt_action);
        ImageButton ibAction = findViewById(R.id.ib_action);

        switch (mResultType) {
            case Barcode.URL:
                Log.d(TAG, "URL");
                tvAction.setText(R.string.action_web);
                ibAction.setImageResource(R.drawable.ic_public_black_44dp);
                break;

            case Barcode.TEXT:
                Log.d(TAG, "TEXT");
                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);
                break;

            case Barcode.PHONE:
                Log.d(TAG, "PHONE");
                tvAction.setText(R.string.action_phone);
                ibAction.setImageResource(R.drawable.ic_phone_black_38dp);
                break;

            case Barcode.GEO:
                Log.d(TAG, "GEO");
                tvAction.setText(R.string.action_geo);
                ibAction.setImageResource(R.drawable.ic_location_on_black_38dp);
                break;

            case Barcode.CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                mCalEvent = MainActivity.mDetectedBarcode.calendarEvent;

                mEventStart = mCalEvent.start.year + "/" + mCalEvent.start.month + "/" + mCalEvent.start.day
                        + " " + String.format("%02d", mCalEvent.start.hours)
                        + ":" + String.format("%02d", mCalEvent.start.minutes);

                mEventEnd = mCalEvent.end.year + "/" + mCalEvent.end.month + "/" + mCalEvent.end.day
                        + " " + String.format("%02d", mCalEvent.end.hours)
                        + ":" + String.format("%02d", mCalEvent.end.minutes);

                String strEvent =
                        "Title: " + mCalEvent.summary + "\n"
                        + "Location: " + mCalEvent.location + "\n"
                        + "Organizer: " + mCalEvent.organizer + "\n"
                        + "Start: " + mEventStart + "\n"
                        + "End: " + mEventEnd + "\n"
                        + "Status: " + mCalEvent.status + "\n"
                        + "Description: " + mCalEvent.description;

                tvBarcodeResult.setText(strEvent);
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                tvAction.setText(R.string.action_calender);
                ibAction.setImageResource(R.drawable.ic_calender_black_38dp);
                break;

            default:
                Log.d(TAG, "default");
                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);
                break;
        }
    }

    public void doAction(View v) {
        switch (mResultType) {
            case Barcode.URL:
                openBrowser();
                break;

            case Barcode.PHONE:
                openDialer();
                break;

            case Barcode.CALENDAR_EVENT:
                addToCalender();
                break;

            case Barcode.GEO:
                openMaps();
                break;
        }
    }

    private void openBrowser() {
        String url = MainActivity.mDetectedBarcode.url.url;
        boolean isValidURL = Patterns.WEB_URL.matcher(url).matches();

        if (isValidURL) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.invalid_url),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialer() {
        String number = MainActivity.mDetectedBarcode.phone.number;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        startActivity(intent);
    }

    private void openMaps() {
        double lat = MainActivity.mDetectedBarcode.geoPoint.lat;
        double lng = MainActivity.mDetectedBarcode.geoPoint.lng;
        String geo = "geo:0,0?q=" + lat + "," + lng + "(Location)";
        Uri coordinates = Uri.parse(geo);

        Intent intent = new Intent(Intent.ACTION_VIEW, coordinates);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void addToCalender() {
        long startDate;
        long endDate;

        Date dateStart = null;
        Date dateEnd = null;
        try {
            dateStart = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).parse(mEventStart);
            dateEnd = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).parse(mEventEnd);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (dateStart == null || dateEnd == null) {
            Log.e(TAG, "addToCalender: date is null");
            return;
        }

        startDate = dateStart.getTime();
        endDate = dateEnd.getTime();

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.item/event");

        intent.putExtra(CalendarContract.Events.TITLE, mCalEvent.summary);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, mCalEvent.location);
        intent.putExtra(CalendarContract.Events.ORGANIZER, mCalEvent.organizer);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate);
        intent.putExtra(CalendarContract.Events.STATUS, mCalEvent.status);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, mCalEvent.description);

        startActivity(intent);
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

    public void back(View v) {
        finish();
    }
}
