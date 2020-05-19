package com.auroid.qrscanner;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultViewModel;
import com.auroid.qrscanner.serializable.AddressWrapper;
import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.ContactWrapper;
import com.auroid.qrscanner.serializable.EmailWrapper;
import com.auroid.qrscanner.serializable.EventWrapper;
import com.auroid.qrscanner.serializable.GeoWrapper;
import com.auroid.qrscanner.serializable.PhoneWrapper;
import com.auroid.qrscanner.utils.TypeSelector;

import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BarcodeResultActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeResultActivity";

    private String mResult;
    private int mResultType;

    private Barcode.CalendarEvent mCalEvent;
    private String mEventStart;
    private String mEventEnd;

    private Barcode.ContactInfo mContact;

    //TODO: Convert Barcode into BarcodeWrapper and make ActionHandler common
    private Barcode mDetectedBarcode = MainActivity.mDetectedBarcode;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        mResult = mDetectedBarcode.displayValue;
        mResultType = mDetectedBarcode.valueFormat;

        TextView tvBarcodeResult = findViewById(R.id.barcode_result);
        tvBarcodeResult.setText(mResult);

        TextView tvAction = findViewById(R.id.txt_action);
        ImageButton ibAction = findViewById(R.id.ib_action);

        // Create BarcodeWrapper object which will be stored in the database
        BarcodeWrapper barcodeWrapper =
                new BarcodeWrapper(mResultType, mResult);

        switch (mResultType) {
            case Barcode.URL:
                Log.d(TAG, "URL");
                tvAction.setText(R.string.action_web);
                ibAction.setImageResource(R.drawable.ic_public_black_44dp);

                barcodeWrapper.url = mDetectedBarcode.url.url;
                insertToDb(barcodeWrapper);
                break;

            case Barcode.PHONE:
                Log.d(TAG, "PHONE");
                tvAction.setText(R.string.action_phone);
                ibAction.setImageResource(R.drawable.ic_phone_black_38dp);

                barcodeWrapper.phoneNumber = mDetectedBarcode.phone.number;
                insertToDb(barcodeWrapper);
                break;

            case Barcode.GEO:
                Log.d(TAG, "GEO");
                tvAction.setText(R.string.action_geo);
                ibAction.setImageResource(R.drawable.ic_location_on_black_38dp);

                barcodeWrapper.geoWrapper
                        = new GeoWrapper(mDetectedBarcode.geoPoint.lat, mDetectedBarcode.geoPoint.lng);
                insertToDb(barcodeWrapper);
                break;

            case Barcode.CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                mCalEvent = mDetectedBarcode.calendarEvent;

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

                barcodeWrapper.eventWrapper = new EventWrapper(
                        mCalEvent.description,
                        mCalEvent.location,
                        mCalEvent.organizer,
                        mCalEvent.status,
                        mCalEvent.summary,
                        mEventStart,
                        mEventEnd);
                insertToDb(barcodeWrapper);
                break;

            case Barcode.CONTACT_INFO:
                Log.d(TAG, "CONTACT_INFO");
                mContact = mDetectedBarcode.contactInfo;

                tvAction.setText(R.string.action_contact);
                ibAction.setImageResource(R.drawable.ic_person_add_black_38dp);

                StringBuilder phoneNumbers = new StringBuilder();
                for (int i = 0; i < mContact.phones.length; i++) {
                    if (i == 0) phoneNumbers.append("Contact Numbers:\n");
                    phoneNumbers.append(TypeSelector.phoneTypeAsString(mContact.phones[i].type));
                    phoneNumbers.append(": ");
                    phoneNumbers.append(PhoneNumberUtils.formatNumber(mContact.phones[i].number, "US"));
                    phoneNumbers.append("\n");
                }

                StringBuilder emailAddresses = new StringBuilder();
                for (int i = 0; i < mContact.emails.length; i++) {
                    if (i == 0) emailAddresses.append("Email Addresses:\n");
                    emailAddresses.append(TypeSelector.emailTypeAsString(mContact.emails[i].type));
                    emailAddresses.append(": ");
                    emailAddresses.append(mContact.emails[i].address);
                    emailAddresses.append("\n");
                }

                StringBuilder websites = new StringBuilder();
                for (int i = 0; i < mContact.urls.length; i++) {
                    if (i == 0) websites.append("Websites:\n");
                    websites.append(mContact.urls[i]);
                    websites.append("\n");
                }

                StringBuilder addresses = new StringBuilder();
                for (int i = 0; i < mContact.addresses.length; i++) {
                    if (i == 0) addresses.append("Addresses:\n");
                    if (i > 0) addresses.append("\n\n");
                    addresses.append(TypeSelector.addressTypeAsString(mContact.addresses[i].type));
                    addresses.append(":\n");
                    addresses.append(mContact.addresses[i].addressLines[0]);
                }

                String strContact = "Name: " + mContact.name.formattedName + "\n"
                        + "Title: " + mContact.title + "\n"
                        + "Company: " + mContact.organization + "\n\n"
                        + phoneNumbers + "\n"
                        + emailAddresses + "\n"
                        + websites + "\n"
                        + addresses;

                tvBarcodeResult.setText(strContact);
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                tvBarcodeResult.setTextSize(18);

                AsyncTask.execute(() -> {
                    int numOfPhones = mContact.phones.length;
                    PhoneWrapper[] phoneWrappers = new PhoneWrapper[numOfPhones];
                    for (int i = 0; i < numOfPhones; i++) {
                        phoneWrappers[i] = new PhoneWrapper(
                                mContact.phones[i].number,
                                mContact.phones[i].type);
                    }

                    int numOfEmails = mContact.emails.length;
                    EmailWrapper[] emailWrappers = new EmailWrapper[numOfEmails];
                    for (int i = 0; i < numOfEmails; i++) {
                        emailWrappers[i] = new EmailWrapper(
                                mContact.emails[i].address,
                                mContact.emails[i].type);
                    }

                    int numOfAddresses = mContact.addresses.length;
                    AddressWrapper[] addressWrappers = new AddressWrapper[numOfAddresses];
                    for (int i = 0; i < numOfAddresses; i++) {
                        addressWrappers[i] = new AddressWrapper(
                                mContact.addresses[i].addressLines,
                                mContact.addresses[i].type);
                    }

                    barcodeWrapper.contactWrapper = new ContactWrapper(
                            mContact.name.formattedName,
                            mContact.organization,
                            mContact.title,
                            mContact.urls,
                            phoneWrappers,
                            emailWrappers,
                            addressWrappers
                    );
                    insertToDb(barcodeWrapper);
                });
                break;

            case Barcode.WIFI:
                Log.d(TAG, "WIFI");
                Barcode.WiFi wifiParams = mDetectedBarcode.wifi;

                String encryption;
                switch (wifiParams.encryptionType) {
                    case Barcode.WiFi.OPEN:
                        encryption = "None";
                        break;

                    case Barcode.WiFi.WPA:
                        encryption = "WPA";
                        break;

                    case Barcode.WiFi.WEP:
                        encryption = "WEP";
                        break;

                    default:
                        encryption = "Unknown";
                }

                String wifiInfo = "Wi-Fi Network\n" +
                        "\nName: " + wifiParams.ssid +
                        "\nPassword: " + wifiParams.password +
                        "\nEncryption: " + encryption;
                tvBarcodeResult.setText(wifiInfo);

                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);

                // No additional params to set, directly inserting to the DB
                insertToDb(barcodeWrapper);
                break;

            default:
                Log.d(TAG, "default");
                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);

                // No additional params to set, directly inserting to the DB
                insertToDb(barcodeWrapper);
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

            case Barcode.CONTACT_INFO:
                addToContacts();
                break;
        }
    }

    // TODO: Move this into ActionHandler
    private void openBrowser() {
        String url = mDetectedBarcode.url.url;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    // TODO: Move this into ActionHandler
    private void openDialer() {
        String number = mDetectedBarcode.phone.number;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        startActivity(intent);
    }

    // TODO: Move this into ActionHandler
    private void openMaps() {
        double lat = mDetectedBarcode.geoPoint.lat;
        double lng = mDetectedBarcode.geoPoint.lng;
        String geo = "geo:0,0?q=" + lat + "," + lng + "(Location)";
        Uri coordinates = Uri.parse(geo);

        Intent intent = new Intent(Intent.ACTION_VIEW, coordinates);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    // TODO: Move this into ActionHandler
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

    // TODO: Move this into ActionHandler
    private void addToContacts() {
        Barcode.ContactInfo contactInfo = mDetectedBarcode.contactInfo;

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, contactInfo.name.formattedName);
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, contactInfo.organization);
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contactInfo.title);

        ArrayList<ContentValues> data = new ArrayList<>();
        // Adding URL's
        for (int i = 0; i < mContact.urls.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Website.URL, mContact.urls[i]);
            data.add(row);
        }

        // Adding phone numbers
        for (int i = 0; i < mContact.phones.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, mContact.phones[i].number);

            row.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    TypeSelector.selectPhoneType(mContact.phones[i].type));

            data.add(row);
        }
        // Adding email addressees
        for (int i = 0; i < mContact.emails.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Email.ADDRESS, mContact.emails[i].address);

            row.put(ContactsContract.CommonDataKinds.Email.TYPE,
                    TypeSelector.selectEmailType(mContact.emails[i].type));

            data.add(row);
        }
        // Due to some unknown reason, addresses are not passing to contacts in some devices.
        // Therefore temporarily commented out below code and used an alternative method to set a
        // single address. This issue will be fixed in the future.
        // Adding addressees
        /*for (int i = 0; i < mContact.addresses.length; i++) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);

            StringBuilder postalAddress = new StringBuilder();
            for (int j = 0; j < mContact.addresses[i].addressLines.length; j++) {
                postalAddress.append(mContact.addresses[i].addressLines[j]);
                if (j > 0) postalAddress.append(" ");
            }
            row.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                    postalAddress.toString());

            row.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                    TypeSelector.selectAddressType(mContact.addresses[i].type));
            data.add(row);
        }*/
        if (mContact.addresses.length != 0) {
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, mContact.addresses[0].addressLines[0]);
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE,
                    TypeSelector.selectAddressType(mContact.addresses[0].type));
        }

        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        startActivity(intent);
    }

    // TODO: Move this into ActionHandler
    public void copyToClipboard(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRCode", mResult);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clip);

            Toast.makeText(this, getString(R.string.confirm_copy_to_clipboard),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // TODO: Move this into ActionHandler
    public void webSearch(View view) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, mResult);
        startActivity(intent);
    }

    public void back(View v) {
        finish();
    }

    private void insertToDb(BarcodeWrapper barcode) {
        Gson gson = new Gson();
        String resultJson = gson.toJson(barcode);

        ResultViewModel resultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);
        Result result = new Result(resultJson, new Date());
        resultViewModel.insert(result);
    }
}
