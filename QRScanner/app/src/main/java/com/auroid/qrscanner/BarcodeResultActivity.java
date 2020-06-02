package com.auroid.qrscanner;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
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
import com.auroid.qrscanner.serializable.WiFiWrapper;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.gson.Gson;

import java.util.Date;
import java.util.Objects;

public class BarcodeResultActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeResultActivity";

    private int mResultType;

    private FirebaseVisionBarcode mDetectedBarcode = MainActivity.mDetectedBarcode;

    private BarcodeWrapper mBarcodeWrapper;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        String result = mDetectedBarcode.getDisplayValue();
        String rawValue = mDetectedBarcode.getRawValue();
        mResultType = mDetectedBarcode.getValueType();

        TextView tvBarcodeResult = findViewById(R.id.barcode_result);
        tvBarcodeResult.setText(result);

        TextView tvAction = findViewById(R.id.txt_action);
        ImageButton ibAction = findViewById(R.id.ib_action);

        // Create BarcodeWrapper object which will be stored in the database
        mBarcodeWrapper = new BarcodeWrapper(mResultType, result, rawValue);

        switch (mResultType) {
            case FirebaseVisionBarcode.TYPE_URL:
                Log.d(TAG, "URL");
                tvBarcodeResult.setText(rawValue);

                tvAction.setText(R.string.action_web);
                ibAction.setImageResource(R.drawable.ic_public_black_44dp);

                mBarcodeWrapper.url = Objects.requireNonNull(mDetectedBarcode.getUrl()).getUrl();
                insertToDb(mBarcodeWrapper);
                break;

            case FirebaseVisionBarcode.TYPE_PHONE:
                Log.d(TAG, "PHONE");
                tvAction.setText(R.string.action_phone);
                ibAction.setImageResource(R.drawable.ic_phone_black_38dp);

                mBarcodeWrapper.phoneNumber =
                        Objects.requireNonNull(mDetectedBarcode.getPhone()).getNumber();
                insertToDb(mBarcodeWrapper);
                break;

            case FirebaseVisionBarcode.TYPE_GEO:
                Log.d(TAG, "GEO");
                tvAction.setText(R.string.action_geo);
                ibAction.setImageResource(R.drawable.ic_location_on_black_38dp);

                mBarcodeWrapper.geoWrapper = new GeoWrapper(
                        Objects.requireNonNull(mDetectedBarcode.getGeoPoint()).getLat(),
                        mDetectedBarcode.getGeoPoint().getLng());
                insertToDb(mBarcodeWrapper);
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                FirebaseVisionBarcode.CalendarEvent calEvent = mDetectedBarcode.getCalendarEvent();

                assert calEvent != null;
                String eventStart = Objects.requireNonNull(calEvent.getStart()).getYear() + "/"
                        + calEvent.getStart().getMonth() + "/"
                        + calEvent.getStart().getDay()
                        + " " + String.format("%02d", calEvent.getStart().getHours())
                        + ":" + String.format("%02d", calEvent.getStart().getMinutes());

                String eventEnd = Objects.requireNonNull(calEvent.getEnd()).getYear() + "/"
                        + calEvent.getEnd().getMonth() + "/"
                        + calEvent.getEnd().getDay()
                        + " " + String.format("%02d", calEvent.getEnd().getHours())
                        + ":" + String.format("%02d", calEvent.getEnd().getMinutes());

                mBarcodeWrapper.eventWrapper = new EventWrapper(
                        calEvent.getDescription(),
                        calEvent.getLocation(),
                        calEvent.getOrganizer(),
                        calEvent.getStatus(),
                        calEvent.getSummary(),
                        eventStart,
                        eventEnd);

                ActionHandler actionEvent = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionEvent.getFormattedEventDetails());
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                tvAction.setText(R.string.action_calender);
                ibAction.setImageResource(R.drawable.ic_calender_black_38dp);

                insertToDb(mBarcodeWrapper);
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                Log.d(TAG, "CONTACT_INFO");
                FirebaseVisionBarcode.ContactInfo contact = mDetectedBarcode.getContactInfo();

                assert contact != null;
                int numOfPhones = contact.getPhones().size();
                PhoneWrapper[] phoneWrappers = new PhoneWrapper[numOfPhones];
                for (int i = 0; i < numOfPhones; i++) {
                    phoneWrappers[i] = new PhoneWrapper(
                            contact.getPhones().get(i).getNumber(),
                            contact.getPhones().get(i).getType());
                }

                int numOfEmails = contact.getEmails().size();
                EmailWrapper[] emailWrappers = new EmailWrapper[numOfEmails];
                for (int i = 0; i < numOfEmails; i++) {
                    emailWrappers[i] = new EmailWrapper(
                            contact.getEmails().get(i).getAddress(),
                            contact.getEmails().get(i).getType());
                }

                int numOfAddresses = contact.getAddresses().size();
                AddressWrapper[] addressWrappers = new AddressWrapper[numOfAddresses];
                for (int i = 0; i < numOfAddresses; i++) {
                    addressWrappers[i] = new AddressWrapper(
                            contact.getAddresses().get(i).getAddressLines(),
                            contact.getAddresses().get(i).getType());
                }

                mBarcodeWrapper.contactWrapper = new ContactWrapper(
                        Objects.requireNonNull(contact.getName()).getFormattedName(),
                        contact.getOrganization(),
                        contact.getTitle(),
                        contact.getUrls(),
                        phoneWrappers,
                        emailWrappers,
                        addressWrappers
                );

                ActionHandler actionContact = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionContact.getFormattedContactDetails());
                tvBarcodeResult.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                tvBarcodeResult.setTextSize(18);
                tvBarcodeResult.setMovementMethod(LinkMovementMethod.getInstance());

                tvAction.setText(R.string.action_contact);
                ibAction.setImageResource(R.drawable.ic_person_add_black_38dp);

                insertToDb(mBarcodeWrapper);
                break;

            case FirebaseVisionBarcode.TYPE_WIFI:
                Log.d(TAG, "WIFI");

                mBarcodeWrapper.wifiWrapper = new WiFiWrapper(
                        Objects.requireNonNull(mDetectedBarcode.getWifi()).getEncryptionType(),
                        mDetectedBarcode.getWifi().getPassword(),
                        mDetectedBarcode.getWifi().getSsid()
                );

                ActionHandler actionWifi = new ActionHandler(this, mBarcodeWrapper);
                tvBarcodeResult.setText(actionWifi.getFormattedWiFiDetails());

                tvAction.setText(R.string.action_wifi);
                ibAction.setImageResource(R.drawable.ic_wifi_black_38);

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
            case FirebaseVisionBarcode.TYPE_URL:
                actionHandler.openBrowser();
                break;

            case FirebaseVisionBarcode.TYPE_PHONE:
                actionHandler.openDialer();
                break;

            case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
                actionHandler.addToCalender();
                break;

            case FirebaseVisionBarcode.TYPE_GEO:
                actionHandler.openMaps();
                break;

            case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                actionHandler.addToContacts();
                break;

            case FirebaseVisionBarcode.TYPE_WIFI:
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

    private void insertToDb(BarcodeWrapper barcode) {
        Gson gson = new Gson();
        String resultJson = gson.toJson(barcode);

        ResultViewModel resultViewModel = new ViewModelProvider(this).get(ResultViewModel.class);
        Result result = new Result(resultJson, new Date());
        resultViewModel.insert(result);
    }

    @Override
    public boolean onSupportNavigateUp() {
        this.finish();
        return true;
    }
}
