# Delivery partner upgrade status

The repository is native Android with Kotlin and Jetpack Compose, not Flutter. The rider upgrade therefore extends the existing client and NestJS `/api/v1` backend without replacing customer or admin flows.

## Implemented

- Existing Supabase phone OTP remains the only sign-in mechanism.
- Four-step partner application UI with real backend submission, duplicate prevention, persisted status, and honest payout-provider state.
- Trusted backend guard requiring both an active `delivery_partner` role and an approved application.
- Approval provisions a protected partner profile and server-owned availability record.
- Admin assignment creates one authoritative delivery assignment and an in-app notification.
- Rider dashboard with real profile, availability, active assignment, completion and earnings summaries, history, notifications, support, and payout records.
- Optimistic concurrency for availability and delivery actions.
- Server-enforced assignment ownership and lifecycle: assigned → accepted → arrived at pickup → picked up → out for delivery → delivered.
- Acceptance requires server-confirmed online state. Offline mode does not cancel an active assignment.
- Delivery completion requires configured server-side OTP verification and an idempotency key; the client cannot declare success by itself.
- Foreground GPS acquisition and location upload only during picked-up/out-for-delivery states, with batching limits, stale-point rejection, and endpoint throttling.
- External navigation via Android geo intent; no fabricated route or ETA.
- Partner-owned earnings, payouts, notifications, delivery history, location data, and support tables with RLS and indexes.

## Required before production deployment

1. Generate and review a forward-only migration from the declarative schemas with a current Supabase CLI. The CLI is not installed in this workspace.
2. Provision the private `partner-documents` bucket through a reviewed versioned data migration/bootstrap step. Current Supabase guidance excludes bucket-row DML from declarative schema files.
3. Connect document upload UI to that private bucket and provide authorized reviewer signed-URL access.
4. Define server-generated customer delivery OTP issuance and deliver it through the customer order API before enabling completion in production.
5. Configure a payout provider before collecting payout details or initiating payouts.
6. Configure and test FCM or another push provider. In-app notifications work from stored records; push is not claimed.
7. Background location is intentionally not enabled. The current implementation shares foreground fixes only after pickup; add a disclosed foreground service and background permission only if the operating model genuinely requires it.

No available-job marketplace, dynamic pricing, fake earnings, fake GPS data, or simulated payouts were introduced.
