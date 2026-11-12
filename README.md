# Snapdoc

**Offline-first Android document scanner, PDF toolkit and lightweight editor.**

## Snapdoc v0.4.0
- Configurable PDF split: inclusive start page, stop page and pages per output part, with validation and suggested corrections
- Editor dashboard with PDF Editor and Image Editor workspaces
- PDF Editor: scrolling page previews, reorder pages, remove pages, append pages from another PDF, rotate a page and add a text overlay
- Image Editor: preview, rotate 90°, center crop to square, grayscale and save a separate edited copy
- Files library refreshes after editor saves; outputs remain local and can be opened, shared or exported
- Existing scanner, image-to-PDF, merge, split and PDF optimization tools remain available

## Editing boundaries — read before use
- PDF text overlay adds new text to a page. It does **not** edit or replace existing PDF text, fonts, paragraphs or layout.
- Page reorder, delete, append and rotate are real PDF page operations and preserve the existing page content.
- Image Editor currently supports rotate, center square crop and grayscale. It is not a full layer-based image editor; freeform crop, object removal, text layers and advanced retouching are not included.
- PDF preview is rendered locally on the device. Large PDFs and high-resolution images use memory; very large files may be slow or exceed device memory.
- PDF optimization rasterizes pages and may flatten searchable text, links, forms, vectors and accessibility information. Keep the original.

## Download a test APK without opening Android Studio
Pushes to `feature/android-v1` run GitHub Actions. Open the **Actions** tab, select the latest **Android build** run, and download the `Snapdoc-debug-apk` artifact. Extract the ZIP and install `app-debug.apk`.

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
