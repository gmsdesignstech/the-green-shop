# Admin upgrade implementation status

The client is native Android (Kotlin/Jetpack Compose), not Flutter, so the admin experience was added to the existing application rather than introducing a second client stack.

## Implemented

- Server-verified Supabase JWT authentication and active trusted-admin guard on every `/api/v1/admin/*` route.
- Live dashboard metrics, daily sales series, recent orders, best sellers, low-stock records, partner-application count, and recent audit activity.
- Cursor-paginated product, inventory, order, payment, notification, and audit feeds with search/status/date filters where applicable.
- Product create/update/archive services, including validation and price-change audit events.
- Atomic inventory adjustments that cannot reduce on-hand stock below reservations, with reason and movement history.
- Version-checked order transitions and partner assignment with an explicit lifecycle and audit history.
- Declarative Supabase schema for catalog, inventory, orders, payments, promotions, notifications, support, and store settings; all exposed tables have RLS, least-privilege grants, constraints, and operational indexes.
- Mobile-first Compose admin navigation, dashboard cards, searchable operational lists, loading/empty/error/retry states, and honest “Not configured” labels for unfinished sections.

## Deployment requirements

1. Generate and review a forward-only migration from `supabase/schemas/*.sql`, then apply it to the target Supabase project. Never reset production.
2. Provision the initial administrator out of band in `trusted_role_assignments`; public registration never grants admin access.
3. Configure backend `SUPABASE_URL` and `DATABASE_URL`, then deploy it over HTTPS and set Android `API_BASE_URL`.
4. Configure a payment provider and verified webhook flow before enabling refunds. Refunds are intentionally not simulated.
5. Configure private product-image storage, push/email/WhatsApp providers, and report-download storage before enabling those controls.

Categories, customers, promotions, reports, support, and store settings remain visibly unavailable in the mobile navigation until their mutation/detail screens are connected. Their foundational tables are present; no fake data or fake success states are used.
