package com.auroid.qrscanner;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.auroid.qrscanner.serializable.BarcodeWrapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActionHandler {

    private static final String TAG = "ActionHandler";

    private Context mContext;
    private BarcodeWrapper mBarcodeWrapper;

    public ActionHandler(Context context, BarcodeWrapper mBarcodeWrapper) {
        this.mBarcodeWrapper = mBarcodeWrapper;
        this.mContext = context;
    }

    public void openBrowser() {
        String url = mBarcodeWrapper.url;
        boolean isValidURL = Patterns.WEB_URL.matcher(url).matches();

        if (isValidURL) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            mContext.startActivity(intent);
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_url),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void openDialer() {
        String number = mBarcodeWrapper.phoneNumber;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        mContext.startActivity(intent);
    }

    public void openMaps() {
        double lat = mBarcodeWrapper.geoWrapper.lat;
        double lng = mBarcodeWrapper.geoWrapper.lng;
        String geo = "geo:0,0?q=" + lat + "," + lng + "(Location)";
        Uri coordinates = Uri.parse(geo);

        Intent intent = new Intent(Intent.ACTION_VIEW, coordinates);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(intent);
        }
    }

    public void addToCalender() {
        long startDate;
        long endDate;

        Date dateStart = null;
        Date dateEnd = null;
        try {
            dateStart = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                    .parse(mBarcodeWrapper.eventWrapper.start);
            dateEnd = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                    .parse(mBarcodeWrapper.eventWrapper.end);
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

        intent.putExtra(CalendarContract.Events.TITLE, mBarcodeWrapper.eventWrapper.summary);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, mBarcodeWrapper.eventWrapper.location);
        intent.putExtra(CalendarContract.Events.ORGANIZER, mBarcodeWrapper.eventWrapper.organizer);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate);
        intent.putExtra(CalendarContract.Events.STATUS, mBarcodeWrapper.eventWrapper.status);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, mBarcodeWrapper.eventWrapper.description);

        mContext.startActivity(intent);
    }

    public void copyToClipboard() {
        ClipboardManager clipboardManager =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRCode", mBarcodeWrapper.displayValue);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clip);

            Toast.makeText(mContext, mContext.getString(R.string.confirm_copy_to_clipboard),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void webSearch() {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, mBarcodeWrapper.displayValue);
        mContext.startActivity(intent);
    }
}
