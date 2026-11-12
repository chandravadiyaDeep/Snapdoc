# Architecture

## Stack
Kotlin, Android SDK 36, Jetpack Compose, CameraX, Room, DataStore, Android PdfDocument, PDFBox-Android and OpenCV.

## Layers
- `features/`: screen UI and ViewModels
- `core/`: navigation, permissions, theme and shared utilities
- `domain/`: models and use-case contracts
- `data/`: Room entities/DAOs, repositories and file operations
- `engine/`: scanner, image and PDF processing

## Storage
Store working scans in app-private files. Room stores metadata and relative file IDs. Use Storage Access Framework for user import/export. Write to a temporary file and atomically finalize exports.

## Reliability
Run processing off the main thread. Bound image dimensions, PDF pages, file size and processing time. Keep captures immutable; edits create derived files. Verify dependency versions, licenses and API 36 compatibility before release.