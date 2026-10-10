# The Green Shop multi-role implementation plan

## Repository baseline

- Native Android application built with Kotlin and Jetpack Compose (not Flutter).
- A single activity selects Compose screens using `GreenShopViewModel`; Navigation Compose is not enabled.
- Supabase Auth is used only for persistent customer phone OTP sessions.
- Catalog, cart, checkout totals, addresses, orders, wallet activity, delivery OTP, rider details, and tracking are local sample/client-generated data.
- Google Maps/location libraries and permission handling exist, but there is no production delivery assignment or tracking backend.
- No application backend, database schemas, migrations, RLS policies, admin role, delivery-partner role, storage policy, payments integration, or notification provider exists in this workspace.

## Principal risks

1. Operational screens cannot be labelled production-ready until sample data is replaced by authoritative APIs.
2. Admin or delivery access must not be activated before trusted server-side role resolution and endpoint authorization exist.
3. Current client-generated orders, delivery OTPs, totals, and rider details must never be submitted as authoritative production values.
4. A Flutter rewrite would replace the existing application and is therefore outside the preservation constraint; implementation should continue in Compose unless a separate migration is explicitly approved.
5. There is no Git metadata in this workspace, so commits/checkpoints are unavailable.

## Controlled delivery sequence

1. **Role entry (implemented):** branded three-role first screen; preserve customer OTP; block unbacked privileged entry with an honest configuration state.
2. **Backend foundation:** add the existing production backend repository or create an explicitly approved service; define `/v1`, JWT verification, trusted roles, validation, rate limits, idempotency, audit logging, and error contracts.
3. **Database and RLS:** add forward-only migrations for profiles, trusted role assignments, access requests, partner applications/documents, assignments/history, availability, earnings/payout records, and audit events. Add constraints, indexes, private storage, least-privilege grants, and RLS tests.
4. **Role resolution:** resolve active/suspended/pending role server-side after Supabase authentication. Route only from the trusted response, never from client metadata.
5. **Admin access:** connect the request form, duplicate prevention, verification, approval/rejection, and audit events. Provision the initial administrator out of band.
6. **Partner onboarding:** connect multi-step application, private uploads, approval status, and eligibility checks. Add payout-provider onboarding only when a provider is selected.
7. **Customer data replacement:** replace `SampleData` repository-by-repository with catalog, address, quote, checkout, payment, order-history, and tracking APIs while retaining offline/error states.
8. **Admin operations:** connect dashboard metrics, products/categories, inventory adjustments, orders, assignments, customers, promotions, reports, and settings to real endpoints.
9. **Delivery operations:** connect availability, assigned deliveries, atomic claim/assignment rules, validated state transitions, navigation, proof, location batching, history, and reconciled earnings.
10. **Notifications and release QA:** connect only configured providers; add authorization/integration tests, UI size/accessibility tests, migration validation, release build/signing verification, and customer-data regression coverage.

## Required inputs for the next secure phase

- Backend source repository and confirmed API prefix, or approval to create a backend.
- Supabase schema/migrations or read-only project access for inspection.
- Role and permission policy, including the initial trusted administrator process.
- Single-store confirmation and delivery assignment model (admin-assigned or offer/accept).
- Payment, payout, notification, document-retention, and verification-provider decisions.
