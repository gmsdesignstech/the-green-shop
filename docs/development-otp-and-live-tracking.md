# Development OTP and live tracking

## Development OTP

The Android app always submits phone OTPs to Supabase Auth. It does not contain or compare the
fixed code. To test with `123456`, create three distinct test users in an isolated development or
staging Supabase project and configure that code using Supabase Auth's provider-side test-phone
facility:

- `DEV_CUSTOMER_TEST_PHONE_NUMBER`
- `DEV_ADMIN_TEST_PHONE_NUMBER`
- `DEV_DELIVERY_PARTNER_TEST_PHONE_NUMBER`

Also set `DEV_SUPABASE_URL`, `DEV_SUPABASE_PUBLISHABLE_KEY`, and the existing
`DEV_TEST_PHONE_NUMBER` gate to one valid E.164 development number. The development project URL
must differ from production or the debug build refuses to enable its test-mode indicator. Release
builds always set `DEV_TEST_OTP_ENABLED=false`.

Provision the admin and delivery test users out of band. The admin needs an active
`trusted_role_assignments` admin row. The partner needs an approved application, partner profile,
and active delivery role. A valid OTP session without those records is rejected from the relevant
dashboard. Never put service-role keys or database credentials in the APK.

## Live tracking contract

- Rider upload: `POST /api/v1/rider/deliveries/{assignmentId}/locations`
- Customer snapshot: `GET /api/v1/tracking/orders/{orderId}`
- Admin monitor snapshot: `GET /api/v1/tracking/admin/active-deliveries`

Rider uploads are allowed only for the assigned partner while an assignment is picked up or out
for delivery. Coordinates, accuracy, speed, timestamps, role, and assignment ownership are checked
server-side. Customer snapshots require order ownership; admin snapshots require the active admin
guard. A location older than two minutes is marked stale. Clients should poll snapshots with
backoff when WebSockets are unavailable and must never display a stale point as current.

Google Maps for Android reads `MAPS_API_KEY` through the manifest placeholder. Restrict the key to
package `com.aistudio.greenshop.mkvqlt` and the signing certificate SHA-1, enable Maps SDK for
Android, and keep server-side Routes API credentials on the backend. Routes and ETA are not
implemented without a configured routing provider; no fabricated route or ETA should be shown.

Location history remains authoritative in Postgres. Schedule a reviewed server-side retention job
to delete `partner_location_updates` older than the approved business/privacy period (for example,
30 days). The `received_at` index supports bounded cleanup. Do not add a client-callable cleanup
function.
