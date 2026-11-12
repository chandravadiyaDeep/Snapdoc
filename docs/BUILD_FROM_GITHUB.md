# Build and download Snapdoc without a local development setup

Every push to `feature/android-v1` or `main`, and every pull request to `main`, triggers GitHub Actions.

1. Open the repository's **Actions** tab.
2. Select **Android build**.
3. Open the latest run and wait for the green success result.
4. Under **Artifacts**, download **Snapdoc-debug-apk**.
5. Extract the ZIP and install `app-debug.apk` on an Android phone.

The debug APK is for testing. It is not the signed Play Store release. For a Play release, configure a private signing key in GitHub Actions secrets and generate a signed Android App Bundle (AAB). Never commit signing keys or passwords.

If the workflow fails, open the failed job and inspect the first Gradle error. Fix that before distributing the APK.
