# QR-Reader-Android
A robust, hassle-free Barcode reader for Android powered by [Google ML Kit's Barcode API](https://developers.google.com/ml-kit/vision/barcode-scanning/).

<a href="https://play.google.com/store/apps/details?id=com.auroid.qrscanner" rel="noopener noreferrer" target="_blank">
<img src="google-play-badge.png" style="float:left" width="180" /></a>



## Functions

- Go to a website, copy to clipboard or search scanned content on the web
- Scan a QR code on a business card (vCard) then add to contacts
- Open maps to navigate to a location embedded in a QR code
- Open dialer to call or SMS to a number scanned from a QR code
- Connect to a WiFi network by scanning a QR code
- Add an event to the calender by scanning a QR code
- Save scan history and do above actions even from the history
- Ability to use torch/flasher to scan in dark places
- Track and display a dot in the center of the decoded QR code/barcode in real-time
- Scan a QR code or a barcode saved in the device gallery

## Prerequisites

Android API 21 (Android 5.0 - Lollipop) or greater

## How to setup

* Clone or [download](https://github.com/amila93/QR-Reader-Android/archive/master.zip) the repository and open "**QRScanner**" project using Android Studio

* [Create a Firebase project in the Firebase console, if you don't already have one](https://firebase.google.com/docs/android/setup) (Using Firebase only for Analytics)
* Add a new Android app into your Firebase project with package name **com.auroid.qrscanner**
* Download the config file (google-services.json) from the newly added app and move it into the module folder (i.e. [app/](./QRScanner/app/))
* Build and run

## Implementation using Mobile Vision Barcode API (Depricated)

Implementation using the [older Google Mobile Vision Barcode API](https://developers.google.com/vision) can be found on **OldMobileVisionAPI** branch. Since Google will wind down Mobile Vision Barcode API, migrated to Google ML Kit. Both API's are almost the same. 

