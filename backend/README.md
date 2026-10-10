# Green Shop API

Secure NestJS API for admin access requests and delivery-partner applications. Supabase remains the identity provider; this server verifies access tokens using the project's public JWKS and stores authorization in `trusted_role_assignments`.

## Configure

1. Copy `.env.example` to `.env` and set the Supabase URL, publishable key, and a TLS PostgreSQL connection string from the Supabase **Connect** panel. This checkout already contains a git-ignored `.env` for project `peyydzxmlgskeptkncmr`; replace `YOUR_DATABASE_PASSWORD` before starting the API. The publishable key lets the API validate shared-secret/legacy user tokens with Supabase Auth; never use a service-role or secret key for this fallback.
2. Generate and review a forward migration from `../supabase/schemas/roles_and_applications.sql` with the Supabase CLI, then apply it to the intended project.
3. Use a dedicated least-privilege database login in production. Grant only the tables/sequences required by this API.
4. Install pinned dependencies with `npm install`, run `npm run typecheck`, `npm test`, and `npm run build`.
5. Deploy behind HTTPS and set Android `API_BASE_URL` to the deployed origin.

## Local Android development OTP

The fixed OTP endpoint is `POST /api/v1/dev-auth/login`. It is enabled only when both
`NODE_ENV=development` and `DEV_OTP_ENABLED=true`. Development identities and sessions are held
in memory and are cleared whenever the API restarts; they never create or modify Supabase users.

- Android emulator: `API_BASE_URL=http://10.0.2.2:3000/api/v1/`
- USB-connected physical phone: set `API_BASE_URL=http://127.0.0.1:3000/api/v1/`, then run
  `adb reverse tcp:3000 tcp:3000` before opening the app.
- Wi-Fi physical phone: set `API_BASE_URL=http://<computer-LAN-IP>:3000/api/v1/`, keep both devices
  on the same network, and permit inbound TCP 3000 in the development computer's firewall.

The API binds to `HOST=0.0.0.0` for emulator/LAN access. Cleartext HTTP is allowed only by the
Android debug manifest. Release builds disable fixed OTP and must use a deployed HTTPS API.

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
