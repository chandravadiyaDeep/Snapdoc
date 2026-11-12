# Snapdoc

**Offline-first Android document scanner, PDF toolkit and lightweight editor.**

## v0.5.0 development build
- New OCR Text Extractor: choose an image or multi-page PDF and recognize text locally
- Select/copy recognized text and save it as a TXT file in the local Files library
- OCR pages are labelled separately for multi-page PDFs
- The current OCR model is Latin-script; Gujarati OCR and searchable PDF export are not included yet

## v0.4.0 baseline features
- Configurable PDF split: inclusive start page, stop page and pages per output part, with validation and suggested corrections
- PDF Editor: preview, reorder, remove, append, rotate and add text overlay
- Image Editor: rotate, center crop to square, grayscale and save a separate edited copy
- Scanner, image-to-PDF, merge, split, PDF optimization and local file actions

## v0.5 development roadmap
The v0.5 work is isolated on feature/android-v5. See [Snapdoc v0.5 roadmap](docs/ROADMAP_V5.md) for scanner upgrades, OCR/searchable PDF, PDF annotation, image editing, forms, protection, reliability and release checks.

## Important limitations
- OCR currently extracts text to a selectable result and TXT file; it does not yet create a searchable PDF.
- Existing PDF text is not directly editable; the v0.4 text overlay only adds new text.
- The image editor does not yet have freeform crop, perspective correction, layers or advanced adjustments.
- Arbitrary PDF text reconstruction and secure redaction are not implemented.
- Large PDFs and high-resolution images can consume substantial memory.

## Download a test APK without opening Android Studio
The v0.4 baseline build is available from [GitHub Actions run #22](https://github.com/chandravadiyaDeep/Snapdoc/actions/runs/43229038761). For v0.5, open the Actions tab, select the latest Android build run on feature/android-v5, and download the Snapdoc-debug-apk artifact. Extract the ZIP and install app-debug.apk.

See [GitHub build instructions](docs/BUILD_FROM_GITHUB.md).

## Project and privacy
- Kotlin + Jetpack Compose, Android SDK 36, minimum Android 8.0 (API 26)
- Android PdfDocument, PdfRenderer, PDFBox-Android and bundled ML Kit text recognition
- App-private file storage and Android document pickers
- No account, cloud upload or INTERNET permission; document OCR runs on-device

GitHub Actions unit tests and APK assembly verify compilation and tests, not every real-device workflow. Debug APKs are for testing, not Play Store distribution.
