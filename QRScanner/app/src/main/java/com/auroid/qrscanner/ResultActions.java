package com.auroid.qrscanner;

import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.util.ArrayList;
import java.util.List;

/** The same (at most three) actions are used by result buttons and history menus. */
public final class ResultActions {
    private ResultActions() {}

    public enum Action {
        OPEN_URL(R.string.action_web, R.drawable.ic_public_black_44dp),
        COPY_URL(R.string.copy_url), SHARE_URL(R.string.share_link, R.drawable.ic_share),
        DIAL(R.string.action_phone, R.drawable.ic_phone_black_38dp),
        SMS(R.string.send_sms, R.drawable.ic_message), COPY_NUMBER(R.string.copy_number),
        MAP(R.string.action_geo, R.drawable.ic_location_on_black_38dp),
        COPY_COORDINATES(R.string.copy_coordinates), SHARE_LOCATION(R.string.share_location, R.drawable.ic_share),
        CONTACT(R.string.action_contact, R.drawable.ic_person_add_black_38dp),
        CALENDAR(R.string.action_calender, R.drawable.ic_calender_black_38dp),
        COPY_DETAILS(R.string.copy_details),
        EMAIL(R.string.compose_email, R.drawable.ic_email), COPY_ADDRESS(R.string.copy_address),
        COMPOSE_SMS(R.string.compose_sms, R.drawable.ic_message), COPY_MESSAGE(R.string.copy_message),
        CONNECT(R.string.action_wifi, R.drawable.ic_wifi_black_38),
        COPY_SSID(R.string.wifi_copy_ssid), COPY_PASSWORD(R.string.wifi_copy_password),
        COPY(R.string.action_copy), SEARCH(R.string.action_web_search, R.drawable.ic_search_black_44dp);

        public final int label;
        public final int icon;
        Action(int label) { this(label, R.drawable.ic_content_copy_black_36dp); }
        Action(int label, int icon) { this.label = label; this.icon = icon; }
    }

    public static List<Action> forBarcode(BarcodeWrapper b) {
        ResultContent.restoreMessageFields(b);
        List<Action> actions = new ArrayList<>();
        switch (b.valueFormat) {
            case Barcode.TYPE_URL:
                if (ResultContent.has(b.url)) add(actions, Action.OPEN_URL, Action.COPY_URL, Action.SHARE_URL);
                break;
            case Barcode.TYPE_PHONE:
                if (ResultContent.has(b.phoneNumber)) add(actions, Action.DIAL, Action.SMS, Action.COPY_NUMBER);
                break;
            case Barcode.TYPE_GEO:
                if (b.geoWrapper != null && Double.isFinite(b.geoWrapper.lat) && Double.isFinite(b.geoWrapper.lng)
                        && Math.abs(b.geoWrapper.lat) <= 90 && Math.abs(b.geoWrapper.lng) <= 180)
                    add(actions, Action.MAP, Action.COPY_COORDINATES, Action.SHARE_LOCATION);
                break;
            case Barcode.TYPE_CONTACT_INFO:
                if (ResultContent.has(ResultContent.contactDetails(b.contactWrapper))) add(actions, Action.CONTACT, Action.COPY_DETAILS);
                break;
            case Barcode.TYPE_CALENDAR_EVENT:
                if (b.eventWrapper != null) {
                    if (ResultContent.parseDate(b.eventWrapper.start) != null) actions.add(Action.CALENDAR);
                    if (ResultContent.has(ResultContent.eventDetails(b.eventWrapper))) actions.add(Action.COPY_DETAILS);
                }
                break;
            case Barcode.TYPE_EMAIL:
                if (ResultContent.has(b.recipient)) add(actions, Action.EMAIL, Action.COPY_ADDRESS);
                break;
            case Barcode.TYPE_SMS:
                if (ResultContent.has(b.recipient)) add(actions, Action.COMPOSE_SMS, Action.COPY_NUMBER);
                if (ResultContent.has(b.message)) actions.add(Action.COPY_MESSAGE);
                break;
            case Barcode.TYPE_WIFI:
                if (b.wifiWrapper != null) {
                    if (ResultContent.has(b.wifiWrapper.ssid)) add(actions, Action.CONNECT, Action.COPY_SSID);
                    if (b.wifiWrapper.encryptionType != Barcode.WiFi.TYPE_OPEN && ResultContent.has(b.wifiWrapper.password))
                        actions.add(Action.COPY_PASSWORD);
                }
                break;
            default:
                if (ResultContent.has(b.displayValue)) add(actions, Action.COPY, Action.SEARCH);
        }
        return actions;
    }

    private static void add(List<Action> actions, Action... values) {
        java.util.Collections.addAll(actions, values);
    }

    public static int typeLabel(int type) {
        switch (type) {
            case Barcode.TYPE_URL: return R.string.type_web;
            case Barcode.TYPE_PHONE: return R.string.type_phone;
            case Barcode.TYPE_GEO: return R.string.type_geo;
            case Barcode.TYPE_CONTACT_INFO: return R.string.type_contact;
            case Barcode.TYPE_CALENDAR_EVENT: return R.string.type_calender;
            case Barcode.TYPE_WIFI: return R.string.type_wifi;
            case Barcode.TYPE_EMAIL: return R.string.type_email;
            case Barcode.TYPE_SMS: return R.string.type_sms;
            case Barcode.TYPE_PRODUCT: return R.string.type_product;
            case Barcode.TYPE_ISBN: return R.string.type_isbn;
            default: return R.string.type_text;
        }
    }
}
