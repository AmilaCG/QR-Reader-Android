package com.auroid.qrscanner;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.Toast;

import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.ContactWrapper;
import com.auroid.qrscanner.serializable.EventWrapper;
import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.auroid.qrscanner.utils.TypeSelector;
import com.auroid.qrscanner.wifi.WifiConnectionHelper;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.ArrayList;
import java.util.Date;

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
        if (!ResultContent.has(url)) return;

        Uri webUri = Uri.parse(url);
        if (webUri.getScheme() == null) {
            webUri = Uri.parse("https://" + url);
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
        intent.setData(Uri.fromParts("tel", number, null));
        launch(intent);
    }

    public void openMaps() {
        mFirebaseAnalytics.logEvent("action_open_maps", null);
        double lat = mBarcodeWrapper.geoWrapper.lat;
        double lng = mBarcodeWrapper.geoWrapper.lng;
        String geo = "geo:0,0?q=" + lat + "," + lng + "(Location)";
        Uri coordinates = Uri.parse(geo);

        Intent intent = new Intent(Intent.ACTION_VIEW, coordinates);
        launch(intent);
    }

    public void addToCalender() {
        mFirebaseAnalytics.logEvent("action_add_to_calender", null);
        EventWrapper event = mBarcodeWrapper.eventWrapper;
        if (event == null) return;
        Date start = ResultContent.parseDate(event.start);
        Date end = ResultContent.parseDate(event.end);
        if (start == null) return;
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.item/event");

        intent.putExtra(CalendarContract.Events.TITLE, mBarcodeWrapper.eventWrapper.summary);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, mBarcodeWrapper.eventWrapper.location);
        intent.putExtra(CalendarContract.Events.ORGANIZER, mBarcodeWrapper.eventWrapper.organizer);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.getTime());
        if (end != null && !end.before(start)) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.getTime());
        intent.putExtra(CalendarContract.Events.STATUS, mBarcodeWrapper.eventWrapper.status);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, mBarcodeWrapper.eventWrapper.description);

        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.error_open_calender, Toast.LENGTH_LONG).show();
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Open calender failed", e);
        }
    }

    public void addToContacts() {
        mFirebaseAnalytics.logEvent("action_add_to_contacts", null);
        ContactWrapper contactWrapper = mBarcodeWrapper.contactWrapper;
        if (contactWrapper == null) return;

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, contactWrapper.formattedName);
        intent.putExtra(ContactsContract.Intents.Insert.COMPANY, contactWrapper.organization);
        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contactWrapper.title);

        ArrayList<ContentValues> data = new ArrayList<>();
        // Adding URL's
        for (int i = 0; contactWrapper.urls != null && i < contactWrapper.urls.length; i++) {
            if (!ResultContent.has(contactWrapper.urls[i])) continue;
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Website.URL, contactWrapper.urls[i]);
            data.add(row);
        }

        // Adding phone numbers
        for (int i = 0; contactWrapper.phones != null && i < contactWrapper.phones.length; i++) {
            if (contactWrapper.phones[i] == null || !ResultContent.has(contactWrapper.phones[i].number)) continue;
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contactWrapper.phones[i].number);

            row.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    TypeSelector.selectPhoneType(contactWrapper.phones[i].type));

            data.add(row);
        }
        // Adding email addressees
        for (int i = 0; contactWrapper.emails != null && i < contactWrapper.emails.length; i++) {
            if (contactWrapper.emails[i] == null || !ResultContent.has(contactWrapper.emails[i].address)) continue;
            ContentValues row = new ContentValues();
            row.put(ContactsContract.RawContacts.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);

            row.put(ContactsContract.CommonDataKinds.Email.ADDRESS, contactWrapper.emails[i].address);

            row.put(ContactsContract.CommonDataKinds.Email.TYPE,
                    TypeSelector.selectEmailType(contactWrapper.emails[i].type));

            data.add(row);
        }
        // Keep the existing single-address insertion path, preserving every address line.
        if (contactWrapper.addresses != null) {
            for (com.auroid.qrscanner.serializable.AddressWrapper address : contactWrapper.addresses) {
                if (address == null || !ResultContent.has(ResultContent.address(address.addressLines))) continue;
                intent.putExtra(ContactsContract.Intents.Insert.POSTAL, ResultContent.address(address.addressLines));
                intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE,
                        TypeSelector.selectAddressType(address.type));
                break;
            }
        }

        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        launch(intent);
    }

    public void connectToWifi() {
        mFirebaseAnalytics.logEvent("action_connect_to_wifi", null);
        WifiConnectionHelper.connect(mContext,
                mBarcodeWrapper == null ? null : mBarcodeWrapper.wifiWrapper);
    }

    public void copyToClipboard() {
        mFirebaseAnalytics.logEvent("action_copy_to_clipboard", null);
        copyText("QRCode", mBarcodeWrapper.displayValue, false);
    }

    public boolean hasWifiPassword() {
        WiFiWrapper wifi = mBarcodeWrapper == null ? null : mBarcodeWrapper.wifiWrapper;
        return wifi != null && wifi.encryptionType != Barcode.WiFi.TYPE_OPEN
                && wifi.password != null && !wifi.password.isEmpty();
    }

    public void copyWifiSsid() {
        WiFiWrapper wifi = mBarcodeWrapper == null ? null : mBarcodeWrapper.wifiWrapper;
        if (wifi == null || wifi.ssid == null || wifi.ssid.isEmpty()) {
            return;
        }
        mFirebaseAnalytics.logEvent("action_copy_wifi_ssid", null);
        copyText(mContext.getString(R.string.wifi_copy_ssid), wifi.ssid, false);
    }

    public void copyWifiPassword() {
        if (!hasWifiPassword()) {
            return;
        }
        mFirebaseAnalytics.logEvent("action_copy_wifi_password", null);
        copyText(mContext.getString(R.string.wifi_copy_password),
                mBarcodeWrapper.wifiWrapper.password, true);
    }

    private void copyText(String label, String value, boolean sensitive) {
        ClipboardManager clipboardManager =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, value);
        if (sensitive) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clip.getDescription().setExtras(extras);
        }
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
        return ResultContent.eventDetails(mBarcodeWrapper.eventWrapper);
    }

    public SpannableStringBuilder getFormattedContactDetails() {
        SpannableStringBuilder text = new SpannableStringBuilder(
                ResultContent.contactDetails(mBarcodeWrapper.contactWrapper));
        java.util.regex.Matcher headings = java.util.regex.Pattern.compile(
                "(?m)^(Name|Company|Title|Phone|Email|Website|Address):").matcher(text);
        while (headings.find()) {
            text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    headings.start(), headings.end(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        Linkify.addLinks(text, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);
        return text;
    }

    public CharSequence getDetails() {
        switch (mBarcodeWrapper.valueFormat) {
            case Barcode.TYPE_CONTACT_INFO: return getFormattedContactDetails();
            case Barcode.TYPE_CALENDAR_EVENT: return getFormattedEventDetails();
            case Barcode.TYPE_WIFI: return getFormattedWiFiDetails();
            case Barcode.TYPE_EMAIL:
            case Barcode.TYPE_SMS: return ResultContent.messageDetails(mBarcodeWrapper);
            default: return ResultContent.summary(mBarcodeWrapper);
        }
    }

    public void perform(ResultActions.Action action) {
        // Recheck missing data even if the caller retained an old action list.
        if (!ResultActions.forBarcode(mBarcodeWrapper).contains(action)) return;
        switch (action) {
            case OPEN_URL: openBrowser(); break;
            case DIAL: openDialer(); break;
            case MAP: openMaps(); break;
            case CONTACT: addToContacts(); break;
            case CALENDAR: addToCalender(); break;
            case CONNECT: connectToWifi(); break;
            case COPY_SSID: copyWifiSsid(); break;
            case COPY_PASSWORD: copyWifiPassword(); break;
            case COPY: copyToClipboard(); break;
            case SEARCH: webSearch(); break;
            case SMS:
            case COMPOSE_SMS:
            case EMAIL: launch(createComposeIntent(action)); break;
            case SHARE_URL:
            case SHARE_LOCATION:
                Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, action == ResultActions.Action.SHARE_URL
                        ? mBarcodeWrapper.url : "https://maps.google.com/?q=" + ResultContent.coordinates(mBarcodeWrapper));
                launch(Intent.createChooser(share, mContext.getString(action.label)));
                break;
            default:
                copyText(mContext.getString(action.label), copyValue(action), false);
        }
    }

    String copyValue(ResultActions.Action action) {
        switch (action) {
            case COPY_URL: return mBarcodeWrapper.url;
            case COPY_NUMBER: return mBarcodeWrapper.valueFormat == Barcode.TYPE_SMS
                    ? mBarcodeWrapper.recipient : mBarcodeWrapper.phoneNumber;
            case COPY_COORDINATES: return ResultContent.coordinates(mBarcodeWrapper);
            case COPY_ADDRESS: return mBarcodeWrapper.recipient;
            case COPY_MESSAGE: return mBarcodeWrapper.message;
            case COPY_DETAILS: return getDetails().toString();
            default: throw new IllegalArgumentException("Not a field copy action");
        }
    }

    Intent createComposeIntent(ResultActions.Action action) {
        ResultContent.restoreMessageFields(mBarcodeWrapper);
        if (action == ResultActions.Action.EMAIL) {
            String uri = "mailto:" + Uri.encode(mBarcodeWrapper.recipient, "@,+")
                    + "?subject=" + Uri.encode(ResultContent.safe(mBarcodeWrapper.subject))
                    + "&body=" + Uri.encode(ResultContent.safe(mBarcodeWrapper.message));
            return new Intent(Intent.ACTION_SENDTO, Uri.parse(uri))
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{mBarcodeWrapper.recipient})
                    .putExtra(Intent.EXTRA_SUBJECT, mBarcodeWrapper.subject)
                    .putExtra(Intent.EXTRA_TEXT, mBarcodeWrapper.message);
        }
        String number = action == ResultActions.Action.SMS
                ? mBarcodeWrapper.phoneNumber : mBarcodeWrapper.recipient;
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null));
        if (action == ResultActions.Action.COMPOSE_SMS) intent.putExtra("sms_body", mBarcodeWrapper.message);
        return intent;
    }

    private void launch(Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(mContext, R.string.error_action_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    public String getFormattedWiFiDetails() {
        WiFiWrapper wifiParams = mBarcodeWrapper.wifiWrapper;
        if (wifiParams == null) {
            return mContext.getString(R.string.wifi_invalid_credentials);
        }

        String encryption;
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

        return mContext.getString(R.string.wifi_details,
                wifiParams.ssid == null ? "" : wifiParams.ssid,
                wifiParams.password == null ? "" : wifiParams.password, encryption);
    }
}
