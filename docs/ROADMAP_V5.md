# Snapdoc v0.5.0 — Advanced Document Studio

## Release objective

Turn Snapdoc from a basic offline scanner/PDF utility into a capable offline-first document studio. Every tool shown in the UI must complete a real operation, preserve the source file, report failures clearly, and save a usable output.

- Android application version: 0.5.0
- versionCode: 5
- Development branch: feature/android-v5
- Base: feature/android-v1 (verified v0.4.0)
- Processing model: on-device/offline by default; no account or document upload.

## v0.4 gaps that v0.5 is intended to close

- Automatic document edge detection, perspective correction and manual crop controls.
- OCR, text extraction, copy/search within scanned documents, and searchable PDF output.
- PDF annotations beyond a single text overlay.
- Image editing beyond rotate, square crop and grayscale.
- PDF form filling and common document finishing tools.
- Better handling of large files, progress, cancellation, errors and undo/source preservation.

## v0.5 capability targets

### 1. Advanced scanner
- In-app CameraX capture flow with a clear capture/retake/accept experience.
- Document boundary detection with manual corner adjustment.
- Perspective correction / de-skew, rotate and crop before adding a page.
- Multi-page capture session, page reorder/delete/retake, scan presets and batch export.
- Keep the original captured image until the user confirms the processed page.

Acceptance: a user can photograph a paper document, correct its edges, add multiple pages and produce a readable PDF without leaving Snapdoc.

### 2. OCR and searchable documents
- On-device OCR using a bundled/offline text-recognition model.
- Extract recognized text per page and show it in a selectable/copyable result view.
- Create searchable PDFs by placing an invisible text layer aligned to the page image.
- Search recognized text across pages and jump to the matching page.
- Handle OCR failure, unsupported scripts and low-confidence results honestly.

Acceptance: a scanned image-only PDF can be converted to a searchable PDF and text can be copied/searched without uploading the document. OCR accuracy depends on image quality and supported languages.

### 3. PDF annotation studio
- Add text boxes with configurable size, color and placement.
- Highlight, underline and strike-through selected regions.
- Freehand pen and eraser; rectangle, circle, line and arrow shapes.
- Insert an image or signature mark on a page.
- Undo/redo annotation actions before saving.
- Save annotations as a new PDF while keeping the source unchanged.

Acceptance: annotations are visible in common PDF readers and the original file remains available.

### 4. Expanded PDF page tools
- Insert blank pages and selected images as PDF pages.
- Duplicate, extract selected pages, reorder, rotate and delete pages.
- Add page numbers and a text/image watermark.
- Add or edit PDF document metadata where supported.
- Batch split by ranges and fixed page counts; retain v0.4 inclusive-range validation.

Acceptance: page operations produce correct page counts/order and separate output files with clear names.

### 5. PDF forms and protection
- Detect and display fillable AcroForm fields; allow text/check-box selection where supported.
- Save a filled copy and preserve the unfilled original.
- Password-protect exported PDFs and open password-protected PDFs when the user supplies the password.
- Clearly report unsupported encryption, malformed files and unsupported form widgets.

Acceptance: supported form fields can be filled and saved; protected exports require a password to open. Do not claim digital-signature certification.

### 6. Advanced image editor
- Freeform crop and aspect-ratio presets.
- Perspective correction / straighten.
- Brightness, contrast, exposure and sharpness adjustments.
- Flip horizontal/vertical, rotate and practical scan filters.
- Add text, arrows, shapes and signature marks.
- Undo/redo and export JPEG/PNG while retaining the source.

Acceptance: every control changes the preview and the exported image matches the visible edit.

### 7. Files, reliability and usability
- Search/sort/filter files and show file size, type and modified date.
- Multi-select files for share/export/delete and batch PDF actions.
- Progress indicators and cancellation for long-running work.
- Confirm destructive actions; preserve source files by default.
- Memory-aware page rendering (render visible pages on demand instead of keeping every full preview bitmap).
- Clear errors for invalid PDFs, oversized images, unsupported formats and insufficient storage.
- Add unit tests for page ranges, page ordering, image transforms and export naming; add instrumentation tests for core flows where feasible.

## Important technical truth: editing existing PDF text

Arbitrary editing of existing PDF text is not equivalent to editing a Word document. PDFs may store text as positioned glyphs, vector outlines, embedded subsets of fonts or scanned pixels. v0.5 must not pretend a white rectangle plus new text is a secure edit.

- Provide reliable annotation/text overlay first.
- For supported AcroForm fields, edit the actual form values.
- Investigate safe text replacement only for PDFs whose text/font/layout can be reconstructed reliably; otherwise explain the limitation and offer annotation or OCR-based reconstruction into a new document.
- A visual cover-up is not secure redaction. Do not label it redaction or promise removal of underlying content.
- Do not claim cryptographic digital signatures unless a proper certificate-based signing implementation and verification flow exists.

## Implementation sequence

1. **Foundation and safety:** branch from verified v0.4; bump version only after the first coherent feature slice; add tests, source-preserving output rules, memory-safe rendering and progress/error handling.
2. **Scanner + image pipeline:** CameraX capture, crop/edge correction, perspective transform, image editing pipeline and undo/redo.
3. **OCR pipeline:** bundled offline recognizer, text extraction, searchable PDF generation and page search.
4. **PDF annotation + page studio:** annotation model, canvas tools, undo/redo, page insertion/extraction/duplicate and watermark/page numbering.
5. **Forms + protection + polish:** supported AcroForm controls, password-protected exports/opening, file management and regression tests.

Each phase must compile and pass CI before the next phase is merged. Do not add dead UI. If a capability cannot be implemented safely with the existing library, research and test a suitable library before exposing the feature.

## Release verification checklist

- GitHub Actions unit tests pass and debug APK artifact exists.
- Install and test on a physical Android device.
- Verify OCR works in airplane mode after installation.
- Verify searchable PDF in a second PDF reader.
- Verify annotation and page operations preserve source files.
- Verify encrypted PDF requires the chosen password.
- Test multi-page, large, rotated, image-only, form-based and password-protected PDFs.
- Confirm no network permission or cloud upload is introduced.
- Keep the v0.4 branch and successful APK as a rollback baseline.
