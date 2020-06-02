# QR-Reader-Android
A robust, minimalist QR code reader for Android powered by [Firebase ML Kit's Barcode API](https://firebase.google.com/docs/ml-kit/read-barcodes).

<a href="https://play.google.com/store/apps/details?id=com.auroid.qrscanner" rel="noopener noreferrer" target="_blank">
<img src="google-play-badge.png" style="float:left" width="180" /></a>



## Demo

![demo gif](demo.gif)

## Functions

- Go to a website, copy to clipboard or search scanned content on the web
- Option to directly open scanned URL in the browser
- Scan a QR code on a business card (vCard) then add to contacts
- Open maps to navigate to a location embedded in a QR code
- Open dialer to call or SMS to a number scanned from a QR code
- Add an event to the calender by scanning a QR code
- Save scan history and do above actions even from the history
- Ability to use torch/flasher to scan in dark places
- Delete individual items from scan history and undo mistaken deletions
- Display corners of the decoded QR code in real-time

## Prerequisites

Android API 21 (Android 5.0 - Lollipop) or greater

## How to setup

* Clone or [download](https://github.com/amila93/QR-Reader-Android/archive/master.zip) the repository and open "**QRScanner**" project using Android Studio
* [Create a Firebase project in the Firebase console, if you don't already have one](https://firebase.google.com/docs/android/setup)
* Add a new Android app into your Firebase project with package name **com.auroid.qrscanner**
* Download the config file (google-services.json) from the new added app and move it into the module folder (i.e. [app/](./QRScanner/app/))
* Build and run

## Implementation using Google Vision Barcode API

Implementation using the older Google Mobile Vision Barcode API can be found on **OldMobileVisionAPI** branch. Since Google will wind down Mobile Vision Barcode API, moved to Firebase ML Kit. Both API's are almost same. 

## Note

Google Play Services will automatically download and install required dependencies on the first run of this app. If the app is not detecting any QR code, then turn on Wifi or Mobile Data on your device and wait a couple of minutes. This is a one time process.
