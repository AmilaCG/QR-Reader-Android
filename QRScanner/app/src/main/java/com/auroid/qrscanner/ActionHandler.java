package com.auroid.qrscanner;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.ContactWrapper;
import com.auroid.qrscanner.utils.TypeSelector;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public void addToContacts() {
        ContactWrapper contactWrapper = mBarcodeWrapper.contactWrapper;

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, contactWrapper.formattedName);
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, contactWrapper.organization);
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contactWrapper.title);

        ArrayList<ContentValues> data = new ArrayList<>();
        // Adding URL's
        for (int i = 0; i < contactWrapper.urls.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Website.URL, contactWrapper.urls[i]);
            data.add(row);
        }

        // Adding phone numbers
        for (int i = 0; i < contactWrapper.phones.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactWrapper.phones[i].number);

            row.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    TypeSelector.selectPhoneType(contactWrapper.phones[i].type));

            data.add(row);
        }
        // Adding email addressees
        for (int i = 0; i < contactWrapper.emails.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactWrapper.emails[i].address);

            row.put(ContactsContract.CommonDataKinds.Email.TYPE,
                    TypeSelector.selectEmailType(contactWrapper.emails[i].type));

            data.add(row);
        }
        // Due to some unknown reason, addresses are not passing to contacts in some devices.
        // Therefore temporarily commented out below code and used an alternative method to set a
        // single address. This issue will be fixed in the future.
        // Adding addressees
        /*for (int i = 0; i < contactWrapper.addresses.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);

            StringBuilder postalAddress = new StringBuilder();
            for (int j = 0; j < contactWrapper.addresses[i].addressLines.length; j++) {
                postalAddress.append(contactWrapper.addresses[i].addressLines[j]);
                if (j > 0) postalAddress.append(" ");
            }
            row.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                    postalAddress.toString());

            row.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                    TypeSelector.selectAddressType(contactWrapper.addresses[i].type));
            data.add(row);
        }*/
        if (contactWrapper.addresses.length != 0) {
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL,
                    contactWrapper.addresses[0].addressLines[0]);
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE,
                    TypeSelector.selectAddressType(contactWrapper.addresses[0].type));
        }

        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
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
