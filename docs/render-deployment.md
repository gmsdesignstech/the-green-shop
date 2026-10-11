# Render production deployment

This repository contains a native Android application and a standalone npm/NestJS backend. It is
not a Flutter or pnpm workspace.

## Render service settings

- Root Directory: `backend`
- Runtime: Node
- Build Command: `npm ci && npm run typecheck && npm run build`
- Start Command: `npm start`
- Health Check Path: `/api/v1/health` during database diagnosis. Restore
  `/api/v1/health/database` after PostgreSQL health succeeds so production readiness includes the database.

The root `render.yaml` contains the same settings. Connect the GitHub repository in Render and use
**New > Blueprint** to apply it, or create a Web Service manually with the values above. Render
supplies `PORT`; the API binds it on `0.0.0.0` and serves routes under `/api/v1`.

## Required Render environment variables

- `NODE_ENV=production`
- `HOST=0.0.0.0`
- `DATABASE_URL`: the TLS pooler connection string from Supabase **Connect**. Use a dedicated,
  least-privilege database login.
- `DATABASE_SSL=true`
- `DATABASE_SSL_CA`: complete database root certificate PEM from Supabase **Database Settings →
  SSL Configuration**. Store it as a Render secret; do not commit the certificate.
- `SUPABASE_URL`: the Supabase project HTTPS URL.
- `SUPABASE_PUBLISHABLE_KEY`: the public/publishable key, never a secret or service-role key.
- `CORS_ORIGINS`: comma-separated HTTPS browser origins, only if a browser frontend calls this API.
  Native Android does not require CORS, so this may be blank.
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET`: required when live
  Razorpay payments/webhooks are enabled.

`REDIS_URL` is not required by the current backend because no order, inventory, reservation, or
queue code uses Redis yet. `/api/v1/health/redis` reports that state; it is not a readiness check.
Development OTP variables must not be added to the production service.

## Deploy and verify

1. Push reviewed changes to the configured GitHub `origin` on the deployment branch.
2. In Render, connect GitHub, select the repository and branch, then apply `render.yaml`.
3. Enter every secret marked `sync: false` in the Render dashboard and deploy.
4. Copy the real HTTPS service URL shown by Render only after deployment succeeds.
5. Verify `<service-url>/api/v1/health` returns HTTP 200, then verify
   `<service-url>/api/v1/health/database`. Do not continue to a production APK if database health
   is not HTTP 200.
6. Exercise login, catalog, inventory, cart/order, and payment flows against the deployed service.

## Android release build

This is a Gradle/Compose app, so Flutter's `--dart-define` does not apply. Supply the verified URL
as a Gradle property or environment variable. The build normalizes exactly one `/api/v1` suffix and
rejects missing, non-HTTPS, localhost, emulator, query-string, and fragment values in release.

PowerShell:

```powershell
.\gradlew.bat assembleRelease -PAPI_BASE_URL=https://YOUR-REAL-RENDER-HOST/api/v1
```

Release signing accepts either CI environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD`) or an ignored root `key.properties` file:

```properties
storeFile=C:/secure/android/the-green-shop-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=upload
keyPassword=YOUR_KEY_PASSWORD
```

If no upload key exists and the application has never been published, generate one outside the
repository with Android Studio's **Build > Generate Signed Bundle / APK** flow or `keytool`, then
back it up securely. Never replace the signing identity of a published app without explicit
approval. Replace the example API host only with the real Render URL after both health endpoints
have been tested.
