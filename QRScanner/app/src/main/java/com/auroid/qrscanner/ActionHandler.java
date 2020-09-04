package com.auroid.qrscanner;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.Toast;

import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.ContactWrapper;
import com.auroid.qrscanner.serializable.EventWrapper;
import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.auroid.qrscanner.utils.TypeSelector;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.Barcode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActionHandler {

    private static final String TAG = "ActionHandler";

    private Context mContext;
    private BarcodeWrapper mBarcodeWrapper;
    private FirebaseAnalytics mFirebaseAnalytics;

    public ActionHandler(Context context, BarcodeWrapper barcodeWrapper) {
        this.mBarcodeWrapper = barcodeWrapper;
        this.mContext = context;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void openBrowser() {
        mFirebaseAnalytics.logEvent("action_open_browser", null);
        String url = mBarcodeWrapper.url;

        Uri webUri = Uri.parse(url);
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            webUri = Uri.parse("http://" + url);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(webUri);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.error_open_browser, Toast.LENGTH_LONG).show();
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Open browser failed", e);
        }
    }

    public void openDialer() {
        mFirebaseAnalytics.logEvent("action_open_dialer", null);
        String number = mBarcodeWrapper.phoneNumber;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        mContext.startActivity(intent);
    }

    public void openMaps() {
        mFirebaseAnalytics.logEvent("action_open_maps", null);
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
        mFirebaseAnalytics.logEvent("action_add_to_calender", null);
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
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "addToCalender: Date parsing failed", e);
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
        mFirebaseAnalytics.logEvent("action_add_to_contacts", null);
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

    public void connectToWifi() {
        mFirebaseAnalytics.logEvent("action_connect_to_wifi", null);
        String ssid = mBarcodeWrapper.wifiWrapper.ssid;
        String pass = mBarcodeWrapper.wifiWrapper.password;

        Toast.makeText(mContext, "Connecting to: " + ssid + "..", Toast.LENGTH_LONG).show();
        WifiManager wifiManager =
                (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int encryption = mBarcodeWrapper.wifiWrapper.encryptionType;
            if (encryption == Barcode.WiFi.TYPE_WPA) {
                // TODO: Need to find a proper way for API 29 (Q) and above
                WifiNetworkSuggestion networkSuggestion = new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(pass)
                        .build();

                List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
                suggestionsList.add(networkSuggestion);
                wifiManager.addNetworkSuggestions(suggestionsList);
            } else if (encryption == Barcode.WiFi.TYPE_WEP) {
                Toast.makeText(mContext, R.string.unsupported_encryption, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = String.format("\"%s\"", ssid);
            wifiConfiguration.preSharedKey = String.format("\"%s\"", pass);
            int wifiID = wifiManager.addNetwork(wifiConfiguration);
            wifiManager.enableNetwork(wifiID, true);
        }
    }

    public void copyToClipboard() {
        mFirebaseAnalytics.logEvent("action_copy_to_clipboard", null);
        ClipboardManager clipboardManager =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRCode", mBarcodeWrapper.displayValue);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clip);

            Toast.makeText(mContext, R.string.confirm_copy_to_clipboard,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void webSearch() {
        mFirebaseAnalytics.logEvent("action_web_search", null);
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, mBarcodeWrapper.displayValue);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.error_web_search, Toast.LENGTH_LONG).show();
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Web search failed", e);
        }
    }

    public String getFormattedEventDetails() {
        EventWrapper calEvent = mBarcodeWrapper.eventWrapper;

        return "Title: " + calEvent.summary + "\n"
                + "Location: " + calEvent.location + "\n"
                + "Organizer: " + calEvent.organizer + "\n"
                + "Start: " + calEvent.start + "\n"
                + "End: " + calEvent.end + "\n"
                + "Status: " + calEvent.status + "\n"
                + "Description: " + calEvent.description;
    }

    public SpannableStringBuilder getFormattedContactDetails() {
        ContactWrapper contact = mBarcodeWrapper.contactWrapper;

        int cursor = 0;

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        String nameHeading = "Name:\n";
        ssb.append(nameHeading);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += nameHeading.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ssb.append(contact.formattedName);
        cursor += contact.formattedName.length();

        ssb.append("\n\n");
        cursor += 2;

        if (!contact.title.equals("")) {
            String titleHeading = "Title:\n";
            ssb.append(titleHeading);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += titleHeading.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.append(contact.title);
            cursor += contact.title.length();

            ssb.append("\n\n");
            cursor += 2;
        }

        if (!contact.organization.equals("")) {
            String orgHeading = "Company:\n";
            ssb.append(orgHeading);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += orgHeading.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.append(contact.organization);
            cursor += contact.organization.length();

            ssb.append("\n\n");
            cursor += 2;
        }

        for (int i = 0; i < contact.phones.length; i++) {
            if (i == 0) {
                String contactHeading = "Contact Numbers:\n";
                ssb.append(contactHeading);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += contactHeading.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            String phoneType = TypeSelector.phoneTypeAsString(contact.phones[i].type);
            ssb.append(phoneType);
            ssb.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor += phoneType.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.append(": ");
            cursor += 2;

            String phoneNum = PhoneNumberUtils.formatNumber(contact.phones[i].number, "US");
            if (phoneNum == null) {
                phoneNum = contact.phones[i].number.replaceAll("[-,+]","");
            }
            ssb.append(phoneNum);
            // Inserting a space to the end to avoid unintended behaviours when scrolling
            ssb.append(" ");
            // adding 1 to count the additional space character
            cursor += phoneNum.length() + 1;

            ssb.append("\n");
            cursor += 1;
        }

        ssb.append("\n");
        cursor += 1;

        for (int i = 0; i < contact.emails.length; i++) {
            if (i == 0) {
                String emailHeading = "Email Addresses:\n";
                ssb.append(emailHeading);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += emailHeading.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            String emailType = TypeSelector.emailTypeAsString(contact.emails[i].type);
            ssb.append(emailType);
            ssb.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor += emailType.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.append(": ");
            cursor += 2;

            String emailAddr = contact.emails[i].address;
            ssb.append(emailAddr);
            // Inserting a space to the end to avoid unintended behaviours when scrolling
            ssb.append(" ");
            // adding 1 to count the additional space character
            cursor += emailAddr.length() + 1;

            ssb.append("\n");
            cursor += 1;
        }

        ssb.append("\n");
        cursor += 1;

        for (int i = 0; i < contact.urls.length; i++) {
            if (i == 0) {
                String webHeading = "Websites:\n";
                ssb.append(webHeading);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += webHeading.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            String url = contact.urls[i];
            ssb.append(url);
            // Inserting a space to the end to avoid unintended behaviours when scrolling
            ssb.append(" ");
            // adding 1 to count the additional space character
            cursor += url.length() + 1;

            ssb.append("\n");
            cursor += 1;
        }

        ssb.append("\n");
        cursor += 1;

        for (int i = 0; i < contact.addresses.length; i++) {
            if (i == 0) {
                String addrHeading = "Addresses:\n";
                ssb.append(addrHeading);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor += addrHeading.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (i > 0) {
                ssb.append("\n\n");
                cursor += 2;
            }
            String addressType = TypeSelector.addressTypeAsString(contact.addresses[i].type);
            ssb.append(addressType);
            ssb.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor += addressType.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssb.append(":\n");
            cursor += 2;

            String address = contact.addresses[i].addressLines[0];
            ssb.append(address);
            // Inserting a space to the end to avoid unintended behaviours when scrolling
            ssb.append(" ");
            // adding 1 to count the additional space character
            cursor += address.length() + 1;
        }

        Linkify.addLinks(ssb, Linkify.ALL);
        return ssb;
    }

    public String getFormattedWiFiDetails() {
        WiFiWrapper wifiParams = mBarcodeWrapper.wifiWrapper;

        String encryption;
        assert wifiParams != null;
        switch (wifiParams.encryptionType) {
            case Barcode.WiFi.TYPE_OPEN:
                encryption = "None";
                break;

            case Barcode.WiFi.TYPE_WPA:
                encryption = "WPA";
                break;

            case Barcode.WiFi.TYPE_WEP:
                encryption = "WEP";
                break;

            default:
                encryption = "Unknown";
        }

        return "Wi-Fi Network\n" +
                "\nName: " + wifiParams.ssid +
                "\nPassword: " + wifiParams.password +
                "\nEncryption: " + encryption;
    }
}
