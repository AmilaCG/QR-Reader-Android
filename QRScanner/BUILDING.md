# Building QR Code Reader

The project uses Android Gradle Plugin 9.4.0, Gradle 9.7.1, and Android SDK 37.
Use Android Studio Quail 4 (2026.1.4) or newer, with its bundled JDK selected as
the Gradle JDK. Install Android SDK Platform 37 through the SDK Manager.

The app now requires Android 7.0 (API 24), because the latest Google Play services
OSS licenses library requires it. The compile and target SDK are both 37.

For a PowerShell build using the default Android Studio installation:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug -x :app:uploadCrashlyticsMappingFileRelease
```

The exclusion skips uploading release mapping files to Firebase during local
verification. Omit it when building a release whose mapping should be uploaded.
The release APK is unsigned; configure your existing release signing credentials
when preparing an app for distribution.

With an emulator or device connected, run:

```powershell
./gradlew.bat :app:connectedDebugAndroidTest
```

APK outputs are in `app/build/outputs/apk/`. Lint and test reports are in
`app/build/reports/`.

Dependencies were checked against Google Maven and Maven Central on September 13,
2026 and pinned to stable releases. Firebase libraries share BoM 34.19.0. The image
viewer remains at 3.10.0 because that is still its latest published stable version.
The retired monolithic Play Core dependency was replaced by Play In-App Review.

Reference release information:

- [Android Gradle plugin compatibility](https://developer.android.com/build/releases/about-agp)
- [AndroidX stable releases](https://developer.android.com/jetpack/androidx/versions)
- [Firebase Android releases](https://firebase.google.com/support/release-notes/android)

The migration also updates ML Kit imports, CameraX opt-in annotations, resource
ID comparisons, and manifest declarations. Image selection uses the system
document picker without broad storage permission. App screens handle system bar
insets for the current target SDK. Database version 4 and saved barcode JSON
field names remain unchanged.
