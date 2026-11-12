# Security policy

- Store working scans in app-private storage by default.
- Use content URIs and temporary grants for import/export.
- Avoid broad storage permissions; use Storage Access Framework.
- Request camera permission only when scanning begins.
- Never log document contents, OCR text, paths or sensitive filenames.
- Treat imported files as untrusted; bound size, dimensions, pages and processing duration.
- Handle malformed/encrypted PDFs without crashing.
- Review network permissions and SDKs before release.
- Publish an accurate privacy policy and Play Data safety declaration.