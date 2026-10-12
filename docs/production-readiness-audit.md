# The Green Shop production-readiness audit

Audit date: 2026-10-12

## Blocker verification snapshot

### Backend and deployment

- Registered locally: `GET /api/v1/health`, `GET /api/v1/health/database`,
  `GET /api/v1/catalog/categories`, `GET /api/v1/catalog/products`, and
  `GET /api/v1/catalog/promotions`.
- Local HTTP verification against the configured PostgreSQL database returned HTTP 200 for all
  five routes. The three catalog bodies were `{ "items": [] }`.
- Automated result: 16 test files and 80 tests passed, including five HTTP route tests. Typecheck,
  NestJS build, and `npm audit` passed; npm reports zero vulnerabilities.
- Production verification on 2026-10-12 after deployment: health, database health, categories,
  products, and promotions all returned HTTP 200. Categories returned two records, products
  returned five records, and promotions returned an empty `items` array.
- Recovery commit `402af56` was pushed to GitHub `origin/main`. The public catalog route changed
  from HTTP 404 to HTTP 200 at 19:12 IST, confirming that Render deployed the updated backend.
- `render.yaml` uses root directory `backend`, `npm ci && npm run typecheck && npm run build`,
  `npm start`, and `/api/v1/health`. Those commands are correct for the updated backend.
- The deployed behavior proves that the recovery code is active. The exact Render commit metadata
  still cannot be read from its public HTTP API because no authenticated Render dashboard/API
  access or version endpoint is configured.

### Database

- Before seeding, read-only inspection confirmed that `public.categories`, `public.products`, and
  `public.promotions` existed with counts 0, 0, and 0 respectively.
- `supabase_migrations.schema_migrations` is absent, so migration history/status cannot be proven
  from that database. The repository uses declarative schemas in `supabase/schemas`.
- After explicit user approval, `supabase/seed/initial_catalog.sql` was run once. Counts are now two
  categories, five products, zero promotions, and five inventory rows. It uses stable slugs/SKUs
  and `ON CONFLICT DO NOTHING`, did not update existing admin-managed values, and created no users,
  orders, payments, analytics, or promotions.

### Google Maps and Android

- Android application ID: `com.aistudio.greenshop.mkvqlt`.
- `MAPS_API_KEY` remains a placeholder; no key value was printed or committed. Maps SDK enablement
  and Cloud Console restrictions require owner access and remain unverified.
- The debug/release fingerprints and exact Android-key restrictions are documented in
  `docs/google-maps-setup.md`. The backend currently needs no Google server key.
- `API_BASE_URL` is the intended Render HTTPS URL with `/api/v1` exactly once. Build logic
  normalizes duplicate suffixes and rejects localhost/non-HTTPS release URLs.
- Android unit tests, lint, and debug APK build passed. The release build was deliberately rejected
  with the actionable error `MAPS_API_KEY must be a real Android-restricted Google Maps key for
  release builds`.

### End-to-end status

- The debug app is buildable and catalog parsing/error/empty-state behavior is implemented.
- Real categories and products are verified through the deployed API. Offers correctly return an
  empty list because no promotion was authorized. Device-level UI and Maps remain unverified
  because no emulator/physical-device session is attached and the Maps key is not configured.
- Authentication, authorization, inventory validation, payment verification, cart, orders, and
  existing UI architecture were preserved. No production mutation or deployment was performed.

## Existing architecture

- Mobile: native Android, Kotlin, Jetpack Compose. This repository is not a Flutter project and does not contain an iOS target.
- API: NestJS 11 on Node.js 22+, versioned under `/api/v1`.
- Data: PostgreSQL hosted by Supabase, accessed by the API through `pg`. The project uses declarative schemas in `supabase/schemas`; Prisma is not used.
- Authentication: Supabase phone OTP in normal builds. A separate backend-issued fixed-OTP session exists only when both `NODE_ENV=development` and `DEV_OTP_ENABLED=true`.
- Authorization: API guards look up trusted admin and approved delivery-partner records server-side. Client-selected roles are not trusted.
- Deployment: one Render web service for the NestJS API. PostgreSQL health is independently exposed. Redis is optional and not implemented.
- Payments: server-created Razorpay orders, signature verification, idempotent webhook receipt, COD validation, and transactional stock reservation exist in the backend.
- Maps: Android location and Maps SDK integrations exist. Real delivery location upload and authorized customer tracking endpoints exist.

## Verified working foundations

- Strict DTO validation with unknown fields rejected.
- Global request throttling plus tighter development-auth and location limits.
- Production requires TLS for PostgreSQL and retains certificate verification.
- Admin and delivery APIs enforce authentication and trusted-role guards.
- Delivery assignment ownership and state transitions are checked server-side.
- Checkout recalculates prices from products, checks stock under a transaction, reserves inventory, and uses an idempotency key.
- Razorpay webhook signatures and captured amounts are verified server-side.
- Public API and database health endpoints currently return HTTP 200.
- Declarative schemas enable RLS and contain ownership policies for customer order and payment reads.
- Android release builds require HTTPS, disable the fixed OTP path, and require explicit signing credentials.

## Incomplete or non-production behavior

### P0: customer transaction path

- Render has not yet deployed the new `/catalog` routes; the public route currently returns 404.
- Android checkout is intentionally disabled and is not wired to `/payments/checkout`.
- Customer order history/tracking is not loaded from a customer API.
- Addresses, wallet actions, and some order helpers remain in-memory UI state.
- Cart and favorites are not persisted across process death or merged after login.
- Catalog pagination is not yet consumed by the Android client.

Acceptance gate: a real product can be browsed, added, checked out with an authenticated session, safely reserved, and returned through an ownership-scoped order endpoint without any locally fabricated order state.

### P1: customer experience and catalog depth

- The app requires customer login before browsing.
- Landing and recipe screens still reference `SampleData`; these must not become production fallbacks.
- Categories have no real subcategory/variant model in the current schema.
- Product media uses either packaged images or an HTTPS `image_path`; managed upload/storage workflows are absent.
- Search has no recent-search persistence, server pagination, or offline cache.
- Cancellation, refunds, substitutions, serviceability, taxes, and coupon application are incomplete.

Acceptance gate: browsing works as a guest, production screens contain no fake commercial state, media has an authorized storage workflow, and checkout eligibility is entirely server-derived.

### P1: fulfilment and operational reliability

- Reservation expiry/release workers are absent.
- Redis health reports either `not_configured` or `not_implemented`; BullMQ/background workers do not exist.
- Picking, packing, barcode/SKU verification, substitutions, refund operations, and inventory reconciliation are not complete workflows.
- Push notifications and FCM device registration/event delivery are absent.

Acceptance gate: abandoned/failed checkouts release stock automatically, retries are idempotent, and real backend events drive operational/customer notifications.

### P2: administration and delivery

- Admin functionality is embedded in the Android application rather than a dedicated web dashboard.
- Several admin sections are list-oriented and do not yet expose complete create/edit/export/refund workflows.
- Delivery has availability, assignments, transitions, locations, earnings, payouts, notifications, and support APIs, but device-level background tracking and notification delivery need end-to-end verification.

Acceptance gate: every privileged action is guard-protected and audited, and partner workflows are verified on a physical device under permission denial and network interruption.

### P2: release and operations

- There is no normal CI workflow that runs Android and backend checks; `.github` only contains modernization tooling.
- No staging service, Redis service, worker process, monitoring/error reporting, or backup runbook is configured here.
- Android release signing credentials are intentionally absent.
- No iOS project exists, so an iOS build requires a separately approved product decision and macOS toolchain.
- Production SMS provider, Razorpay live mode, Maps restrictions, FCM, storage policies, and webhook dashboard configuration require external verification.

## Security findings

- No service-role key is present in the Android configuration templates.
- Production fixed OTP is prevented by both Android build configuration and backend startup validation.
- RLS ownership predicates use `auth.uid()` and do not rely on editable user metadata.
- Public catalog access is mediated by the NestJS API. Direct Supabase Data API policies currently target authenticated users only.
- `inventory` is directly readable by any authenticated Supabase user under the current RLS policy. The mobile app does not require direct table access, so this grant/policy should be narrowed after checking all consumers.
- The Razorpay webhook secret is used by code but missing from `render.yaml`; it must be added as a non-synced Render secret before webhook testing.
- Render health currently checks application liveness rather than database readiness. This avoids rollback loops but requires a separate readiness/monitoring policy.

## Implementation order

1. Deploy and verify catalog API; add API contract tests and Android catalog parsing/error tests.
2. Add authenticated, ownership-scoped customer address and order APIs using existing tables; wire real order history.
3. Connect Android checkout to server quoting/order creation, initially COD and Razorpay test mode; remove local order simulation.
4. Persist guest cart locally and merge quantities safely after authentication.
5. Add reservation expiry/release logic and a worker architecture; configure Redis only when the worker is ready.
6. Add product media storage/upload policy and admin media management.
7. Complete fulfilment, cancellation/refund, coupon, notification, and support workflows.
8. Add CI, staging, monitoring, release signing, and end-to-end device verification.

No destructive database operation or production seed is authorized by this plan.
