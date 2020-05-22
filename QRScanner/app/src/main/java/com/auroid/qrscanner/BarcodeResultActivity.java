package com.auroid.qrscanner;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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

import java.util.Date;

public class BarcodeResultActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeResultActivity";

    private int mResultType;

    private Barcode.ContactInfo mContact;

    private Barcode mDetectedBarcode = MainActivity.mDetectedBarcode;

    private BarcodeWrapper mBarcodeWrapper;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        String result = mDetectedBarcode.displayValue;
        String rawValue = mDetectedBarcode.rawValue;
        mResultType = mDetectedBarcode.valueFormat;

        TextView tvBarcodeResult = findViewById(R.id.barcode_result);
        tvBarcodeResult.setText(result);

        TextView tvAction = findViewById(R.id.txt_action);
        ImageButton ibAction = findViewById(R.id.ib_action);

        // Create BarcodeWrapper object which will be stored in the database
        mBarcodeWrapper = new BarcodeWrapper(mResultType, result, rawValue);

        switch (mResultType) {
            case Barcode.URL:
                Log.d(TAG, "URL");
                tvAction.setText(R.string.action_web);
                ibAction.setImageResource(R.drawable.ic_public_black_44dp);
                tvBarcodeResult.setText(rawValue);

                mBarcodeWrapper.url = mDetectedBarcode.url.url;
                insertToDb(mBarcodeWrapper);
                break;

            case Barcode.PHONE:
                Log.d(TAG, "PHONE");
                tvAction.setText(R.string.action_phone);
                ibAction.setImageResource(R.drawable.ic_phone_black_38dp);

                mBarcodeWrapper.phoneNumber = mDetectedBarcode.phone.number;
                insertToDb(mBarcodeWrapper);
                break;

            case Barcode.GEO:
                Log.d(TAG, "GEO");
                tvAction.setText(R.string.action_geo);
                ibAction.setImageResource(R.drawable.ic_location_on_black_38dp);

                mBarcodeWrapper.geoWrapper
                        = new GeoWrapper(mDetectedBarcode.geoPoint.lat, mDetectedBarcode.geoPoint.lng);
                insertToDb(mBarcodeWrapper);
                break;

            case Barcode.CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                Barcode.CalendarEvent calEvent = mDetectedBarcode.calendarEvent;

                String eventStart = calEvent.start.year + "/" + calEvent.start.month + "/" + calEvent.start.day
                        + " " + String.format("%02d", calEvent.start.hours)
                        + ":" + String.format("%02d", calEvent.start.minutes);

                String eventEnd = calEvent.end.year + "/" + calEvent.end.month + "/" + calEvent.end.day
                        + " " + String.format("%02d", calEvent.end.hours)
                        + ":" + String.format("%02d", calEvent.end.minutes);

                String strEvent =
                        "Title: " + calEvent.summary + "\n"
                        + "Location: " + calEvent.location + "\n"
                        + "Organizer: " + calEvent.organizer + "\n"
                        + "Start: " + eventStart + "\n"
                        + "End: " + eventEnd + "\n"
                        + "Status: " + calEvent.status + "\n"
                        + "Description: " + calEvent.description;

                tvBarcodeResult.setText(strEvent);
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                tvAction.setText(R.string.action_calender);
                ibAction.setImageResource(R.drawable.ic_calender_black_38dp);

                mBarcodeWrapper.eventWrapper = new EventWrapper(
                        calEvent.description,
                        calEvent.location,
                        calEvent.organizer,
                        calEvent.status,
                        calEvent.summary,
                        eventStart,
                        eventEnd);
                insertToDb(mBarcodeWrapper);
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

                    mBarcodeWrapper.contactWrapper = new ContactWrapper(
                            mContact.name.formattedName,
                            mContact.organization,
                            mContact.title,
                            mContact.urls,
                            phoneWrappers,
                            emailWrappers,
                            addressWrappers
                    );
                    insertToDb(mBarcodeWrapper);
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
                insertToDb(mBarcodeWrapper);
                break;

            default:
                Log.d(TAG, "default");
                tvAction.setVisibility(View.GONE);
                ibAction.setVisibility(View.GONE);

                // No additional params to set, directly inserting to the DB
                insertToDb(mBarcodeWrapper);
                break;
        }
    }

    public void doAction(View v) {
        ActionHandler actionHandler = new ActionHandler(this, mBarcodeWrapper);

        switch (mResultType) {
            case Barcode.URL:
                actionHandler.openBrowser();
                break;

            case Barcode.PHONE:
                actionHandler.openDialer();
                break;

            case Barcode.CALENDAR_EVENT:
                actionHandler.addToCalender();
                break;

            case Barcode.GEO:
                actionHandler.openMaps();
                break;

            case Barcode.CONTACT_INFO:
                actionHandler.addToContacts();
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
