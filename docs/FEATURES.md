# Feature specification

## Implemented in v0.4.0
- Offline camera capture and multi-page image-to-PDF
- Original, grayscale and black-and-white scan presets
- PDF merge and configurable inclusive page-range split with pages-per-part
- PDF optimization by rasterizing pages (source is preserved)
- Local Files actions: open, share, export and delete
- PDF Editor: scrolling rendered previews, page reorder, page removal, append pages from another PDF, rotate selected page, add text overlay and save a new PDF
- Image Editor: import, preview, rotate 90 degrees, center crop to square, grayscale and save a new image

## Explicit editing limitations
- Text overlay is an added text layer/content stream; editing existing text and reflowing PDF layout is not implemented.
- PDF signatures, form fields, annotations, links, bookmarks and accessibility structures may not survive some page rebuild operations. Test copies and preserve originals.
- Image editor does not yet support freeform crop, layers, object erasing, background removal or advanced retouching.
- No OCR, handwriting recognition or searchable-text generation yet.
- Large PDFs may use substantial memory because the editor renders page thumbnails for the preview.

## Next candidates
- Freeform crop and perspective correction
- OCR and searchable PDF
- Existing-text editing with font/layout preservation (requires a dedicated text reconstruction workflow)
- Image layers, text, shapes and selective adjustments
- PDF annotations, signatures and form editing with explicit preservation tests

Never label a not-yet-implemented capability as available. Preserve source files and explain destructive transformations.
