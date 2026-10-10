# Google Sign-In production setup

The Android client uses Supabase Auth's browser OAuth flow with PKCE. The callback is
`greenshop://login-callback`; the app accepts only that exact scheme and host. Phone OTP remains
enabled and uses the same Supabase session store. Admin and delivery access is resolved by the
NestJS API from `trusted_role_assignments` and approved applications, never from client metadata.

## Google Cloud

1. Configure the OAuth consent screen with the production app name, support email, privacy policy,
   terms URL, authorized domains, and the `openid`, `email`, and `profile` scopes. Publish or add
   explicit test users while the consent screen is in testing.
2. Create the Web application OAuth client used by Supabase. Add the callback URL shown on the
   Supabase **Authentication > Providers > Google** page. It has the form
   `https://<project-ref>.supabase.co/auth/v1/callback` (or the matching custom Auth domain).
3. The current browser-based PKCE integration does not require an Android OAuth client, SHA
   fingerprint, `google-services.json`, or a client secret in the APK. If the app later switches to
   native Google Credential Manager, create separate Android clients for
   `com.aistudio.greenshop.mkvqlt` and every signing certificate (debug, upload, and Play App
   Signing), and pass the resulting ID token to Supabase. Do not mix that flow into this one.

## Supabase

1. Enable Google under **Authentication > Providers**, then enter the Google Web client ID and
   client secret. The secret stays in Supabase.
2. Add `greenshop://login-callback` to **Authentication > URL Configuration > Redirect URLs**.
   Keep web development and production URLs explicit; do not use a broad wildcard in production.
3. Keep email confirmation/provider defaults appropriate to the environment. Google supplies a
   verified identity plus the requested profile fields. Supabase, not the Android client, owns
   identity verification and session refresh.
4. Review Supabase identity-linking behavior before launch. Never implement an application-level
   merge by comparing email strings. Existing phone-only users should link a Google identity only
   from an authenticated account-recovery/linking flow; preserve the existing auth user ID so
   orders and ownership records do not move. If the project's provider behavior cannot enforce
   that policy, disable the rollout until a server-mediated linking flow is deployed.
5. Apply a generated forward-only migration from `supabase/schemas/*.sql`. The new
   `user_profiles` projection is idempotent, backfills current users, stores Google name/avatar,
   and gives users column-level write access only to those two public profile fields. Privileged
   roles remain in the server-only `trusted_role_assignments` table.

## Application and backend configuration

Android `.env` (client-safe values only):

```dotenv
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_PUBLISHABLE_KEY=<publishable-key>
API_BASE_URL=https://<api-host>/api/v1/
GOOGLE_OAUTH_ENABLED=true
```

NestJS environment:

```dotenv
SUPABASE_URL=https://<project-ref>.supabase.co
DATABASE_URL=<server-only pooled postgres connection>
DATABASE_SSL=true
```

`GOOGLE_OAUTH_ENABLED` is a release gate, not a credential. Leave it false until the provider,
redirect allow-list, backend, schema, and both authorized/rejected staff scenarios have been tested.
Never add a Google client secret, Supabase secret/service-role key, database URL, or tokens to the
Android environment or logs.

## Release verification

- Customer: new Google login creates one auth user and one profile; repeat login reuses both.
- Existing phone user: no automatic application-level email merge; secure linking preserves ID.
- Admin: active assignment reaches the admin dashboard; an unassigned Google user is denied.
- Delivery partner: both approved application and active assignment are required; another
  partner's assignments remain inaccessible.
- Cancellation, offline callback, expired/revoked session, duplicate taps, logout/back navigation,
  and retry all fail closed without exposing tokens.
- Re-run Android unit tests and `assembleDebug`, backend tests/typecheck, database lint/advisors, and
  RLS tests against a disposable Supabase project. iOS is not part of this native Android repo.
