# Snapdoc

**Offline-first Android document scanner, PDF toolkit and lightweight editor.**

## Current stable baseline: v0.4.0
- Configurable PDF split: inclusive start page, stop page and pages per output part, with validation and suggested corrections
- Editor dashboard with PDF Editor and Image Editor workspaces
- PDF Editor: scrolling page previews, reorder, remove, append, rotate and text overlay
- Image Editor: preview, rotate 90°, center crop to square, grayscale and save a separate edited copy
- Local Files actions: open, share, export and delete
- Offline processing; no account or document upload

## v0.4 limitations being addressed in v0.5
- Existing PDF text is not directly editable; v0.4 text overlay only adds new text.
- No OCR or searchable-text generation yet.
- Image editor has no freeform crop, perspective correction, layers or advanced adjustments.
- No PDF annotation suite, form filling, password-protected export or advanced scanner capture yet.
- PDF preview currently renders all pages and can use substantial memory for large documents.

## v0.5 development
The v0.5 work is isolated on feature/android-v5, branched from the verified v0.4 baseline. See [Snapdoc v0.5 roadmap](docs/ROADMAP_V5.md) for the complete feature targets, technical boundaries, implementation sequence and release checks.

## Download a test APK without opening Android Studio
The v0.4 baseline build is available from [GitHub Actions run #22](https://github.com/chandravadiyaDeep/Snapdoc/actions/runs/43229038761). For future v0.5 builds, open the Actions tab, select the latest Android build run on feature/android-v5, and download the Snapdoc-debug-apk artifact. Extract the ZIP and install app-debug.apk.

See [GitHub build instructions](docs/BUILD_FROM_GITHUB.md).

## Project
- Kotlin + Jetpack Compose
- Android SDK 36, minimum Android 8.0 (API 26)
- Android PdfDocument and PdfRenderer for local PDF workflows and previews
- PDFBox-Android for merge, page editing and split
- App-private file storage and Android Storage Access Framework pickers
- No account, cloud upload or INTERNET permission

## Verification
GitHub Actions runs unit tests and assembles a debug APK. A successful CI build confirms compilation and unit tests, not all real-device behavior. Test large files, unusual PDFs, export destinations and page ranges on-device. Debug APKs are for testing, not Play Store distribution.

## Privacy
Document processing is local. Camera capture uses Android's camera app and imported files are selected through Android document pickers. Documents are stored in Snapdoc's private app storage.
