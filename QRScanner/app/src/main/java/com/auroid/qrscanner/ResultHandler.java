package com.auroid.qrscanner;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

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

import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.Barcode;

import java.util.Date;
import java.util.Objects;

class ResultHandler {

    private static final String TAG = "ResultHandler";
    private ViewModelStoreOwner mViewModelStoreOwner;
    private String mResultJson;

    public ResultHandler(ViewModelStoreOwner viewModelStoreOwner) {
        this.mViewModelStoreOwner = viewModelStoreOwner;
    }

    public String getResultJson() {
        return mResultJson;
    }

    @SuppressLint("DefaultLocale")
    public void pushToDatabase(Barcode barcode) {
        String result = barcode.getDisplayValue();
        String rawValue = barcode.getRawValue();
        int resultType = barcode.getValueType();

        // Create BarcodeWrapper object which will be stored in the database
        BarcodeWrapper barcodeWrapper = new BarcodeWrapper(resultType, result, rawValue);

        switch (resultType) {
            case Barcode.TYPE_URL:
                Log.d(TAG, "URL");
                barcodeWrapper.url = Objects.requireNonNull(barcode.getUrl()).getUrl();
                break;

            case Barcode.TYPE_PHONE:
                Log.d(TAG, "PHONE");
                barcodeWrapper.phoneNumber = Objects.requireNonNull(barcode.getPhone()).getNumber();
                break;

            case Barcode.TYPE_GEO:
                Log.d(TAG, "GEO");
                barcodeWrapper.geoWrapper = new GeoWrapper(
                        Objects.requireNonNull(barcode.getGeoPoint()).getLat(),
                        barcode.getGeoPoint().getLng());
                break;

            case Barcode.TYPE_CALENDAR_EVENT:
                Log.d(TAG, "CALENDAR_EVENT");
                Barcode.CalendarEvent calEvent = barcode.getCalendarEvent();

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

                barcodeWrapper.eventWrapper = new EventWrapper(
                        calEvent.getDescription(),
                        calEvent.getLocation(),
                        calEvent.getOrganizer(),
                        calEvent.getStatus(),
                        calEvent.getSummary(),
                        eventStart,
                        eventEnd);
                break;

            case Barcode.TYPE_CONTACT_INFO:
                Log.d(TAG, "CONTACT_INFO");
                Barcode.ContactInfo contact = barcode.getContactInfo();

                assert contact != null;
                int numOfUrls = contact.getUrls().size();
                String[] urls = new String[numOfUrls];
                for (int i = 0; i < numOfUrls; i++) {
                    urls[i] = contact.getUrls().get(i);
                }

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

                barcodeWrapper.contactWrapper = new ContactWrapper(
                        Objects.requireNonNull(contact.getName()).getFormattedName(),
                        contact.getOrganization(),
                        contact.getTitle(),
                        urls,
                        phoneWrappers,
                        emailWrappers,
                        addressWrappers
                );
                break;

            case Barcode.TYPE_WIFI:
                Log.d(TAG, "WIFI");
                barcodeWrapper.wifiWrapper = new WiFiWrapper(
                        Objects.requireNonNull(barcode.getWifi()).getEncryptionType(),
                        barcode.getWifi().getPassword(),
                        barcode.getWifi().getSsid()
                );
                break;

            default:
                Log.d(TAG, "default");
                // No additional params to set, directly inserting to the DB
                break;
        }

        insertToDb(barcodeWrapper);
    }

    public void release() {
        mViewModelStoreOwner = null;
        mResultJson = null;
    }

    private void insertToDb(BarcodeWrapper barcode) {
        Gson gson = new Gson();
        mResultJson = gson.toJson(barcode);

        ResultViewModel resultViewModel =
                new ViewModelProvider(mViewModelStoreOwner).get(ResultViewModel.class);
        Result result = new Result(mResultJson, new Date());
        resultViewModel.insert(result);
    }
}
