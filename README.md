# Snapdoc

Offline-first Android document scanner and PDF toolkit.

## V1 scope
- Camera document capture and multi-page scanning
- Crop, rotate, grayscale and contrast enhancement
- Convert images to PDF; merge and split PDFs
- Local document library, share and export
- Core workflows run without an internet connection

See `docs/` for product, architecture, offline policy, security and roadmap.

## Build
Open in Android Studio, sync Gradle, and run the `app` configuration. Min SDK 26; compile/target SDK 36. Validate the Gradle build locally before treating an APK as release-ready.

## Privacy
No account and no automatic document uploads. Documents remain local unless the user explicitly exports or shares them.
