# Green Shop backend contract

The Android client now supports real Supabase phone OTP authentication. The remaining production
features require an authenticated backend that does not exist in this workspace. This document
defines the minimum contract needed to replace `SampleData` without inventing client-side success.

## Configuration

Provide these values through the local `.env` file or CI secret injection:

- `SUPABASE_URL`: HTTPS project URL.
- `SUPABASE_PUBLISHABLE_KEY`: client-safe publishable key. Never use a secret or service-role key.
- `API_BASE_URL`: HTTPS application API URL when the catalog/order backend is available.

Every protected API request must use `Authorization: Bearer <Supabase access token>`. The backend
must verify the JWT, derive the user ID from it, and resolve authorization roles from protected
server-side data (`app_metadata` or a database role mapping), never from request fields.

## Required customer endpoints

- `GET /v1/catalog/categories`
- `GET /v1/catalog/products?categoryId=&query=&cursor=&limit=`
- `GET /v1/catalog/products/{productId}`
- `GET /v1/me/addresses`
- `POST /v1/me/addresses`
- `PATCH /v1/me/addresses/{addressId}`
- `DELETE /v1/me/addresses/{addressId}`
- `POST /v1/orders/quote` — authoritative stock, discounts, fees, taxes, and total.
- `POST /v1/orders` — idempotent order creation from a valid server quote.
- `GET /v1/me/orders?cursor=&limit=`
- `GET /v1/me/orders/{orderId}` — ownership check required.
- `POST /v1/payments/intents` — creates a provider payment intent for an owned quote/order.
- `GET /v1/payments/{paymentId}` — returns provider-confirmed status.

## Required admin endpoints

All endpoints require a server-verified `admin` role.

- `GET /v1/admin/dashboard`
- CRUD/deactivation endpoints under `/v1/admin/products` and `/v1/admin/categories`
- `GET /v1/admin/inventory` and `POST /v1/admin/inventory/adjustments`
- `GET /v1/admin/orders` and `PATCH /v1/admin/orders/{orderId}/status`
- `POST /v1/admin/orders/{orderId}/assign-rider`
- `GET /v1/admin/customers`, `/v1/admin/riders`, `/v1/admin/payments`, `/v1/admin/reports`

## Required rider endpoints

All endpoints require a server-verified `rider` role and assignment ownership checks.

- `GET /v1/rider/me`
- `PATCH /v1/rider/me/availability`
- `GET /v1/rider/deliveries?status=&cursor=&limit=`
- `POST /v1/rider/deliveries/{orderId}/accept`
- `POST /v1/rider/deliveries/{orderId}/picked-up`
- `POST /v1/rider/deliveries/{orderId}/start`
- `POST /v1/rider/deliveries/{orderId}/arrived`
- `POST /v1/rider/deliveries/{orderId}/complete` — server-side OTP/POD validation required.
- `POST /v1/rider/deliveries/{orderId}/locations` — rate-limited batched coordinates.

## Realtime authorization

The backend must publish order and delivery changes only to channels authorized for the current
customer, assigned rider, or admin. Rider coordinates must stop after delivery completion and must
never be exposed through a public channel.

## Status vocabulary

The backend must publish a documented state machine. Minimum order states are `placed`,
`confirmed`, `preparing`, `rider_assigned`, `out_for_delivery`, `delivered`, and `cancelled`.
Transitions must be validated server-side and unsafe arbitrary status patches must be rejected.
