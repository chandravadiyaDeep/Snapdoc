# Snapdoc

**Offline-first Android document scanner and PDF toolkit.**

## Snapdoc v0.2.0 — added in this version
- Original, grayscale and black-and-white scan presets when creating a PDF
- Offline PDF optimization: rasterizes PDF pages at a maximum 1600-pixel edge and rebuilds a lighter PDF
- Keeps the source PDF unchanged and saves a separate optimized copy
- Existing camera capture, multi-page scan session, image import, image-to-PDF, PDF merge and PDF split

## Download a test APK without opening Android Studio
Pushes to `feature/android-v1` run GitHub Actions. Open the **Actions** tab, select the latest **Android build** run, and download the `Snapdoc-debug-apk` artifact. Extract the ZIP and install `app-debug.apk`.

See [GitHub build instructions](docs/BUILD_FROM_GITHUB.md).

## Project
- Kotlin + Jetpack Compose
- Android SDK 36, minimum Android 8.0 (API 26)
- Android PdfDocument and PdfRenderer for local PDF workflows
- PDFBox-Android for merge and split
- App-private file storage and Android Storage Access Framework pickers
- No account, cloud upload or INTERNET permission

## Important optimization note
PDF optimization converts pages into images. It can reduce the size of scanned PDFs, but searchable text, selectable text, vector graphics, links, forms and document accessibility information may be flattened or lost. Keep the original if those properties matter. Compression ratio depends on the input file.

## Testing status
GitHub Actions build #5 completed successfully and produced the `Snapdoc-debug-apk` artifact for the previous v0.1 code. v0.2 changes are being built by the next workflow run. A successful CI build confirms compilation and unit tests, not real-device camera or PDF behavior. The debug APK is for testing, not Play Store distribution.

## Current limitations
- Camera capture opens the Android camera app; there is not yet a custom live camera preview
- Automatic document-edge detection and perspective correction are not yet implemented
- OCR and editable crop/rotation controls are not yet implemented

## Privacy
Document processing is local. Camera capture uses Android's camera app and imported files are selected through Android document pickers. Documents are stored in Snapdoc's private app storage.
