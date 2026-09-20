package com.auroid.qrscanner;

import com.auroid.qrscanner.serializable.*;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared display values and compatibility with history saved before email/SMS fields existed. */
public final class ResultContent {
    private ResultContent() {}

    public static boolean has(String value) { return value != null && !value.isEmpty(); }

    public static String safe(String value) { return value == null ? "" : value; }

    public static void restoreMessageFields(BarcodeWrapper barcode) {
        String raw = safe(barcode.rawValue);
        String lower = raw.toLowerCase(Locale.ROOT);
        String recipient = null, subject = null, message = null;
        if (barcode.valueFormat == Barcode.TYPE_EMAIL) {
            if (lower.startsWith("mailto:")) {
                String[] parts = raw.substring(7).split("\\?", 2);
                recipient = decode(parts[0]);
                if (parts.length > 1) {
                    subject = query(parts[1], "subject");
                    message = query(parts[1], "body");
                    if (!has(recipient)) recipient = query(parts[1], "to");
                }
            } else if (lower.startsWith("matmsg:")) {
                recipient = field(raw.substring(7), "TO");
                subject = field(raw.substring(7), "SUB");
                message = field(raw.substring(7), "BODY");
            } else if (lower.startsWith("smtp:")) {
                String[] parts = raw.substring(5).split(":", 3);
                recipient = parts[0];
                if (parts.length > 1) subject = parts[1];
                if (parts.length > 2) message = parts[2];
            } else if (raw.matches("[^\\s@]+@[^\\s@]+")) {
                recipient = raw;
            }
        } else if (barcode.valueFormat == Barcode.TYPE_SMS) {
            if (lower.startsWith("smsto:") || lower.startsWith("mmsto:")) {
                String[] parts = raw.substring(6).split(":", 2);
                recipient = parts[0];
                if (parts.length > 1) message = parts[1];
            } else if (lower.startsWith("sms:") || lower.startsWith("mms:")) {
                String[] parts = raw.substring(4).split("\\?", 2);
                recipient = decode(parts[0]);
                if (parts.length > 1) message = query(parts[1], "body");
            }
        }
        if (barcode.recipient == null) barcode.recipient = recipient;
        if (barcode.subject == null) barcode.subject = subject;
        if (barcode.message == null) barcode.message = message;
    }

    private static String decode(String text) {
        try {
            // A literal + in an address or phone number is not a space.
            return URLDecoder.decode(text.replace("+", "%2B"), StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException | IllegalArgumentException e) {
            return text;
        }
    }

    private static String query(String query, String name) {
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && name.equalsIgnoreCase(decode(pair[0]))) return decode(pair[1]);
        }
        return null;
    }

    private static String field(String raw, String name) {
        StringBuilder part = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i <= raw.length(); i++) {
            char c = i == raw.length() ? ';' : raw.charAt(i);
            if (escaped) { part.append(c); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == ';') {
                String prefix = name + ":";
                if (part.toString().regionMatches(true, 0, prefix, 0, prefix.length()))
                    return part.substring(prefix.length());
                part.setLength(0);
            } else part.append(c);
        }
        return null;
    }

    public static Date parseDate(String value) {
        if (!has(value)) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date date = format.parse(value, position);
        return position.getIndex() == value.length() ? date : null;
    }

    public static String displayDate(String value) {
        Date date = parseDate(value);
        return date == null ? safe(value)
                : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date);
    }

    public static String coordinates(BarcodeWrapper barcode) {
        return barcode.geoWrapper == null ? ""
                : barcode.geoWrapper.lat + "," + barcode.geoWrapper.lng;
    }

    public static String address(String[] lines) {
        StringBuilder result = new StringBuilder();
        if (lines != null) for (String line : lines) {
            if (has(line)) {
                if (result.length() > 0) result.append('\n');
                result.append(line);
            }
        }
        return result.toString();
    }

    static void append(StringBuilder text, String label, String value) {
        if (has(value)) {
            if (text.length() > 0) text.append('\n');
            text.append(label).append(": ").append(value);
        }
    }

    public static String contactDetails(ContactWrapper c) {
        if (c == null) return "";
        StringBuilder text = new StringBuilder();
        append(text, "Name", c.formattedName);
        append(text, "Company", c.organization);
        append(text, "Title", c.title);
        if (c.phones != null) for (PhoneWrapper p : c.phones) if (p != null) append(text, "Phone", p.number);
        if (c.emails != null) for (EmailWrapper e : c.emails) if (e != null) append(text, "Email", e.address);
        if (c.urls != null) for (String url : c.urls) append(text, "Website", url);
        if (c.addresses != null) for (AddressWrapper a : c.addresses) if (a != null) append(text, "Address", address(a.addressLines));
        return text.toString();
    }

    public static String eventDetails(EventWrapper e) {
        if (e == null) return "";
        StringBuilder text = new StringBuilder();
        append(text, "Title", e.summary);
        append(text, "Location", e.location);
        append(text, "Organizer", e.organizer);
        append(text, "Start", displayDate(e.start));
        append(text, "End", displayDate(e.end));
        append(text, "Status", e.status);
        append(text, "Description", e.description);
        return text.toString();
    }

    public static String messageDetails(BarcodeWrapper b) {
        restoreMessageFields(b);
        StringBuilder text = new StringBuilder();
        append(text, "To", b.recipient);
        append(text, "Subject", b.subject);
        append(text, "Message", b.message);
        return text.length() == 0 ? safe(b.displayValue) : text.toString();
    }

    public static String summary(BarcodeWrapper b) {
        restoreMessageFields(b);
        String value = null;
        switch (b.valueFormat) {
            case Barcode.TYPE_WIFI:
                // Never expose a password in the list preview.
                return b.wifiWrapper != null && has(b.wifiWrapper.ssid) ? b.wifiWrapper.ssid : "Wi-Fi";
            case Barcode.TYPE_CONTACT_INFO:
                if (b.contactWrapper != null) {
                    value = has(b.contactWrapper.formattedName) ? b.contactWrapper.formattedName : b.contactWrapper.organization;
                    if (!has(value)) value = contactDetails(b.contactWrapper);
                }
                break;
            case Barcode.TYPE_CALENDAR_EVENT:
                if (b.eventWrapper != null) value = safe(b.eventWrapper.summary)
                        + (has(b.eventWrapper.start) ? "\n" + displayDate(b.eventWrapper.start) : "");
                break;
            case Barcode.TYPE_EMAIL:
                value = safe(b.recipient) + (has(b.subject) ? "\n" + b.subject : "");
                break;
            case Barcode.TYPE_SMS:
                value = safe(b.recipient) + (has(b.message) ? "\n" + b.message : "");
                break;
            case Barcode.TYPE_URL: value = b.url; break;
            case Barcode.TYPE_PHONE: value = b.phoneNumber; break;
            case Barcode.TYPE_GEO: value = coordinates(b); break;
        }
        return has(value) ? value : safe(b.displayValue);
    }
}
