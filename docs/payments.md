# Payments deployment checklist

The backend payment API is disabled safely until Razorpay and store settings are configured.

## Environment

Set only on the NestJS server:

```env
RAZORPAY_KEY_ID=rzp_test_or_live_key
RAZORPAY_KEY_SECRET=server_only_secret
RAZORPAY_WEBHOOK_SECRET=independent_webhook_secret
```

Do not put either secret in the Android project. The safe checkout key is returned by the authenticated checkout endpoint only after an order is created.

## Endpoints

- `POST /api/v1/payments/checkout` creates or reuses an idempotent attempt from trusted product IDs and quantities.
- `POST /api/v1/payments/attempts/:id/verify` verifies the mobile callback signature but leaves the result pending until trusted capture confirmation.
- `GET /api/v1/payments/attempts/:id` returns an owned attempt's status.
- `POST /api/v1/payments/webhooks/razorpay` verifies the raw request body and processes each event once.

Configure the Razorpay webhook URL as `https://YOUR_API_HOST/api/v1/payments/webhooks/razorpay` and subscribe at minimum to `payment.captured` and `payment.failed`. Use a webhook secret different from the API key secret. Enable auto-capture in the Razorpay dashboard if that is the chosen business model.

## Required database/store setup

1. Generate and review a forward migration from `supabase/schemas/store_operations.sql`.
2. Deploy it without resetting existing data.
3. Set `store_settings.supported_payment_methods` to the methods genuinely enabled, for example `{'razorpay','cod'}`.
4. For COD, set `operating_hours.cod_max_order_value` to a numeric limit. A missing or zero value means no amount cap, so production should set this explicitly.
5. Test with Razorpay Test Mode and verify callback, captured webhook, duplicate webhook, and app-restart status polling before using live keys.

The current customer app still uses local sample product IDs. Do not enable its Pay button until catalog/cart synchronization supplies the numeric backend product IDs expected by checkout.
