# Green Shop API

Secure NestJS API for admin access requests and delivery-partner applications. Supabase remains the identity provider; this server verifies access tokens using the project's public JWKS and stores authorization in `trusted_role_assignments`.

## Configure

1. Copy `.env.example` to `.env` and set the Supabase URL, publishable key, and a TLS PostgreSQL connection string from the Supabase **Connect** panel. This checkout already contains a git-ignored `.env` for project `peyydzxmlgskeptkncmr`. For the shared transaction pooler, use its host, port `6543`, and pooler username from the Connect panel; percent-encode reserved characters in the database password. If TLS verification fails with the system trust store, download the project's root certificate from Supabase **Database Settings** and set `DATABASE_SSL_CA_PATH` to its file path, or set `DATABASE_SSL_CA` to the complete PEM on a hosted platform. The API keeps certificate verification enabled. The publishable key lets the API validate shared-secret/legacy user tokens with Supabase Auth; never use a service-role or secret key for this fallback.
2. Generate and review a forward migration from `../supabase/schemas/roles_and_applications.sql` with the Supabase CLI, then apply it to the intended project.
3. Use a dedicated least-privilege database login in production. Grant only the tables/sequences required by this API.
4. Install pinned dependencies with `npm install`, run `npm run typecheck`, `npm test`, and `npm run build`.
5. Deploy behind HTTPS and set Android `API_BASE_URL` to the deployed origin.

## Local Android development OTP

The fixed OTP endpoint is `POST /api/v1/dev-auth/login`. It is enabled only when both
`NODE_ENV=development` and `DEV_OTP_ENABLED=true`. Development identities and sessions are held
in memory and are cleared whenever the API restarts; they never create or modify Supabase users.
For local testing, use the same `DEV_OTP_CODE` and role-specific phone numbers in `backend/.env`
and the app's root `.env`; the example files use OTP `123456` and synthetic numbers ending in
`0001` (customer), `0002` (admin), and `0003` (delivery partner). Set the API's `DEV_OTP_ENABLED=true`
only for local development. In the Android app, `DEV_FIXED_OTP_ENABLED=true` enables this debug-only
login flow; the release build always disables it. Development users are isolated synthetic identities;
this flow does not create Supabase Auth users or database rows.

- Android emulator: `API_BASE_URL=http://10.0.2.2:3000/api/v1/`
- USB-connected physical phone: set `API_BASE_URL=http://127.0.0.1:3000/api/v1/`, then run
  `adb reverse tcp:3000 tcp:3000` before opening the app.
- Wi-Fi physical phone: set `API_BASE_URL=http://<computer-LAN-IP>:3000/api/v1/`, keep both devices
  on the same network, and permit inbound TCP 3000 in the development computer's firewall.

The API binds to `HOST=0.0.0.0` for emulator/LAN access. Cleartext HTTP is allowed only by the
Android debug manifest. Release builds disable fixed OTP and must use a deployed HTTPS API.

From the repository root, start the API with `Set-Location backend; npm run start:dev`.
Set `API_BASE_URL` in the root `.env` to the emulator or phone URL before building with
`.\gradlew.bat assembleDebug`. The APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## Delivery partner dashboard troubleshooting

The rider routes are under the global `/api/v1` prefix, for example
`GET /api/v1/rider/me` and `GET /api/v1/rider/deliveries`. Dashboard totals, active assignment,
recent deliveries, and notifications are queried independently; a failed optional section is
reported in the response's `unavailable` list while the other sections remain available. A missing
partner profile remains an explicit not-found response. Database connection failures are logged
with the route and partner ID and returned as HTTP 503; check `/api/v1/health/database` and the
database TLS setup below rather than changing delivery-role authorization.

## Admin dashboard database troubleshooting

`GET /api/v1/admin/dashboard` logs each failed query with its stack trace and returns the other
dashboard sections when one query fails. The Android dashboard labels failed metrics/sections as
unavailable and offers retry rather than presenting failed values as zero. `/api/v1/health/database`
returns 503 when the database cannot be reached; the underlying error is logged by the API.
If logs report a TLS certificate-chain error, download the project's root certificate from Supabase
Database Settings and set `DATABASE_SSL_CA_PATH` in `backend/.env`. Certificate validation remains
enabled; do not work around this by disabling TLS verification.

The Android delivery screen also keeps the partner profile and working sections visible when an
optional dashboard query fails, marks unavailable totals with a dash, and provides an in-place retry.

## Initial administrator bootstrap

No default admin is created. After verifying the initial administrator's Supabase user UUID through a trusted operational process, insert the initial `trusted_role_assignments` row from a protected deployment console/database session. Record the operator and ticket in `audit_logs` in the same short transaction. Never expose this operation as a public endpoint.

## Routes

- `POST /api/v1/admin/access-requests`
- `GET /api/v1/admin/access-requests/me`
- `POST /api/v1/delivery-partners/applications`
- `GET /api/v1/delivery-partners/applications/me`
- `GET /api/v1/admin/applications/:kind` (`admin` or `delivery_partner`)
- `PATCH /api/v1/admin/applications/:kind/:id`

Every route requires a valid Supabase bearer token. Review routes additionally require a current active trusted admin assignment.
