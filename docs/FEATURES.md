# Feature specification

## Verified v0.4.0 baseline
- Offline camera capture and multi-page image-to-PDF
- Original, grayscale and black-and-white scan presets
- PDF merge and configurable inclusive page-range split with pages-per-part
- PDF optimization by rasterizing pages (source is preserved)
- Local Files actions: open, share, export and delete
- PDF Editor: scrolling previews, page reorder/removal, append pages, rotate selected page and add text overlay
- Image Editor: import, preview, rotate 90 degrees, center crop to square, grayscale and save a separate image

## v0.5.0 development branch — implemented slice
- OCR Text Extractor workspace in the Editor dashboard
- Select an image or multi-page PDF and extract recognized text locally on the device
- Multi-page PDF OCR output is separated by page number
- Recognized text can be selected and saved as a UTF-8 TXT file in the local Files library
- Bundled ML Kit Latin text-recognition model; OCR does not require document upload or a first-run model download

## v0.5 planned capabilities (not yet implemented)
- CameraX live scanner, automatic edge detection, manual corner correction and perspective correction
- Searchable PDF export with invisible OCR text layer and page-level search
- PDF annotation tools: highlight, underline, strike-through, freehand, shapes, text boxes and signature marks
- Expanded page operations: blank-page insertion, duplicate, extraction, image-to-page insertion, watermarks and page numbering
- Supported AcroForm filling and password-protected PDF export/opening
- Image editor freeform crop, perspective correction, adjustments, flip, text/shapes and undo/redo
- Memory-aware lazy PDF rendering, progress/cancellation and expanded file management

## Known v0.5 OCR boundaries
- The currently integrated recognizer is the Latin-script model. Gujarati and other unsupported scripts are not promised by this build.
- OCR quality depends on image resolution, focus, lighting, skew and language.
- OCR currently extracts text to a selectable result and TXT file; it does not yet create a searchable PDF.
- PDF OCR rasterizes pages for recognition; very large PDFs can take time and consume memory.
- The current OCR output is not a faithful layout reconstruction and does not preserve tables, columns or exact typography.

## Editing and document safety boundaries
- Text overlay adds new text; it does not edit or replace arbitrary existing PDF text, fonts, paragraphs or layout.
- Page reorder, delete, append and rotate are real PDF page operations. Keep originals before destructive changes.
- PDF signatures, form fields, annotations, links, bookmarks and accessibility structures may not survive some page rebuild operations.
- A visual white cover-up is not secure redaction. Never label it redaction or claim underlying content was removed.
- Do not claim cryptographic digital-signature certification without certificate-based signing and verification.
- PDF optimization rasterizes pages and may flatten searchable text, links, forms, vectors and accessibility information.

Never label a not-yet-implemented capability as available. Preserve source files and explain destructive transformations.
