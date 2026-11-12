# Snapdoc

**Offline-first Android document scanner and PDF toolkit.**

## Implemented in this branch
- Full-resolution camera capture through Android's camera app and a private FileProvider URI
- Multi-page scan session
- Import multiple images from the device
- Convert captured/imported images into a multi-page PDF
- Merge selected PDF files
- Split a PDF into one-page PDF files
- Local private file listing
- No account, cloud upload or INTERNET permission in the app manifest

## Download a test APK without opening Android Studio
Pushes to `feature/android-v1` run GitHub Actions. Open the **Actions** tab, select the latest **Android build** run, and download the `Snapdoc-debug-apk` artifact. Extract the ZIP and install `app-debug.apk`.

See [GitHub build instructions](docs/BUILD_FROM_GITHUB.md).

## Project
- Kotlin + Jetpack Compose
- Android SDK 36, minimum Android 8.0 (API 26)
- Android PdfDocument for image-to-PDF
- PDFBox-Android for merge and split
- App-private file storage and Android Storage Access Framework pickers

## Testing status
The source and CI workflow are committed, but a successful GitHub Actions build and real-device test must complete before this APK can be considered validated. The debug APK is for testing, not Play Store distribution.

Not included yet: PDF compression, automatic edge detection/perspective correction, OCR, manual crop/enhancement controls, and a custom in-app camera preview.

## Privacy
Document processing is local. The camera is launched through Android's camera app; selected input files are read through Android document pickers. Documents are stored in Snapdoc's private app storage.
