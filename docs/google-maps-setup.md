# Google Maps setup

The Android application ID is `com.aistudio.greenshop.mkvqlt`. This repository has no iOS target.

## Google Cloud Console

1. Select the Google Cloud project that owns the mobile app and ensure billing is active.
2. Enable **Maps SDK for Android**. The current app uses Android's `Geocoder` and does not call the Google Places, Routes, Directions, or Geocoding web APIs.
3. Create a dedicated Android key. Set its application restriction to **Android apps** and add one entry per signing certificate:
   - Package name: `com.aistudio.greenshop.mkvqlt`
   - Debug SHA-1: `35:74:14:D3:28:0B:B9:AA:36:5B:2D:55:71:AD:1D:5B:49:01:B1:33`
   - Release/upload SHA-1: `15:13:EB:85:56:2E:D9:0C:D8:41:80:0C:5D:46:C5:51:A5:45:D8:9D`
   - Re-run `./gradlew signingReport` (`gradlew.bat signingReport` on Windows) whenever a signing key changes.
4. Restrict the key's API restrictions to **Maps SDK for Android**.
5. Do not reuse a browser key or a backend/server key in the Android app.

Google Play App Signing uses a different certificate from the local upload key. After enrolling, add the **App signing key certificate** SHA-1 shown in Play Console under **Setup > App integrity** to the same Android key restriction.

## Local Android configuration

Put the Android-restricted key in the ignored root `.env` file:

```properties
MAPS_API_KEY=replace_with_the_android_restricted_key
```

The Secrets Gradle Plugin supplies this value to the manifest placeholder. Release builds fail when the key is missing or still a placeholder. Debug builds show a manual-address fallback instead of opening a broken map.

Verify locally:

```powershell
.\gradlew.bat signingReport
.\gradlew.bat testDebugUnitTest assembleDebug
```

Test the map on a Google-Play-enabled emulator or physical Android device. Verify map tiles, map taps, current-location behavior, denied permission, permanently denied permission, disabled location services, and manual address entry.

## Render/backend configuration

The current backend does not call Google Maps Platform APIs, so no Google server key is required on Render. If a future backend feature calls Places, Routes, or Geocoding, create a separate server-restricted key and store it only as `GOOGLE_MAPS_SERVER_API_KEY` in Render. Restrict that key to the exact server APIs; never return it to the Android client.
