<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/77df8be5-1ffa-43b8-a1f6-69c2d3ba4800

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `SUPABASE_URL` and
   `SUPABASE_PUBLISHABLE_KEY` from the Supabase Connect panel (see `.env.example`). Never use a
   service-role or secret key in the Android application.
5. Set `MAPS_API_KEY` in `.env` to an Android-restricted Google Maps Platform key. Enable the
   **Maps SDK for Android** for that Google Cloud project and restrict the key to this app's
   package name and SHA-1 signing certificate.
6. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
7. Run the app on an emulator or physical device. In checkout, tap the delivery-address card to
   open the map picker, move the pin, review the resolved address, and confirm it.
8. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

The production API contract still required by catalog, checkout, admin, and rider features is
documented in [`docs/backend-contract.md`](docs/backend-contract.md).
