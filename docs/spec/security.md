# Security

> Milestones: M0 (seeding), M1. Depends on: `refresh_token` table (`docs/data-model.md`), BR-1, BR-3 (`business-rules.md`), endpoints in `api.md` (Auth and public, Me). How to implement: backend `spring-boot-best-practices` skill §Security; frontend `.claude/rules/nextjs.md` §Auth.

## Tokens

- **Access token:** JWT HS256 signed with `JWT_SECRET` (base64, ≥ 256 bit), TTL `JWT_ACCESS_TTL` (default `PT15M`). Claims: `sub` = userId, `email`, `role`, `companyId`, `iat`, `exp`. Issued with `NimbusJwtEncoder`, validated by the OAuth2 Resource Server; `role` maps to `ROLE_<role>`.
- **Refresh token:** opaque 256-bit random value (base64url), TTL `JWT_REFRESH_TTL` (default `P7D`). Only its SHA-256 hex hash is stored in `refresh_token`.
  - **Rotation:** every refresh revokes the old row (`revoked_at`) and issues a new pair.
  - **Reuse detection:** a revoked token presented more than **10 s** after its `revoked_at` revokes all of that user's tokens → 401 `INVALID_REFRESH_TOKEN`. Within 10 s (concurrent refreshes from the proxy) it returns the same 401 without revoking the others.
- **Revocation:** logout revokes the presented refresh token; it is idempotent (unknown or already revoked tokens also get 204). A password change, or an ADMIN change to a user's role, company, location or active state, revokes all of that user's refresh tokens. Access tokens are never checked against the database: they stay valid until they expire (≤ `JWT_ACCESS_TTL`), accepted by design.
- Inactive users cannot log in or refresh (`ACCOUNT_DISABLED`).
- Passwords: BCrypt (`DelegatingPasswordEncoder`). Policy: at least 8 characters, at most 72 UTF-8 bytes (BCrypt's limit).

## Backend access

- Public: the **P** endpoints in `api.md` and `/actuator/health` (`publicPaths` bean). The rest of `/api/**` requires authentication (`privatePaths`); anything else is denied.
- Stateless; no cookies, CORS or CSRF on the backend (only Next.js calls it, with a bearer token).
- Roles via `@PreAuthorize`; ownership in services (SPEC §4).
- Unauthenticated → 401 `UNAUTHENTICATED`; wrong role → 403 `FORBIDDEN`; both as ProblemDetail.

## Seeding (BR-2)

`SeedRunner` (`ApplicationRunner`, idempotent) runs after Flyway:

1. Ensure the municipality (concelho) `SEED_MUNICIPALITY` in district `SEED_DISTRICT` and country `SEED_COUNTRY` (optional; blank or unset defaults to `Portugal` in code, not in the schema, so other countries need no migration).
2. Ensure the company `SEED_COMPANY_NAME` and its location `SEED_LOCATION_NAME` in that municipality.
3. Ensure an ADMIN with `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` in that company and location. Never overwrite an existing user's password.

Seed values are validated at startup (`SeedProperties`): a missing required value, an invalid `ADMIN_EMAIL` or an `ADMIN_PASSWORD` outside the password policy (§Tokens) fails the startup. An existing user with `ADMIN_EMAIL` is never changed (no password reset, no promotion of a USER).

## Frontend (BFF)

| Cookie  | Content       | Flags                                                                            |
| ------- | ------------- | -------------------------------------------------------------------------------- |
| `lw_at` | access JWT    | httpOnly, SameSite=Lax, Secure when `COOKIE_SECURE=true`, path `/`, maxAge = TTL |
| `lw_rt` | refresh token | httpOnly, SameSite=Lax, Secure when `COOKIE_SECURE=true`, path `/`, maxAge = TTL |

- Server Actions in `src/lib/auth/actions.ts` (login, registration, logout) call the backend and set or clear both cookies through `cookies()`. Login and registration return a result and the form navigates to the safe `next` (`safeNext`: same-site paths only); logout redirects to `/login`.
- `src/proxy.ts` runs on all routes except `_next` and static assets:
  - **CSRF:** cookies are SameSite=Lax; the proxy rejects non-GET/HEAD requests to `/api/*` whose `Origin` doesn't match the host (403), the only check `/api/auth/*` gets. Server Actions, including the auth actions, are origin-checked by Next.js (`Origin` vs `Host`/`X-Forwarded-Host`).
  - **Refresh:** when `lw_at` is missing or expires within 60 s and `lw_rt` exists, decodes `exp` without verification, calls the backend `/auth/refresh`, sets both cookies on the response and forwards the updated `Cookie` header. If the refresh fails while `lw_at` is still valid, continues without touching the cookies. Refreshes are single-flight per refresh token (in memory, keyed by its SHA-256): concurrent requests share one backend call and a successful result is reused for 10 s (the backend grace window), so racing requests all get the new pair.
  - **Unauthenticated** (no usable `lw_at`): clears both cookies; page requests are redirected to `/login?next=…`, `/api/*` requests get 401 ProblemDetail `UNAUTHENTICATED`. Public pages: `/login`, `/register`.
  - GET `/login` or `/register` with a usable session redirects to the safe `next` (or `/`); without one, clears both cookies. `/login?session=expired` (sent by `serverApi()` on a backend 401) always clears them: the backend rejected a token the proxy would still accept, so honouring it would loop. POSTs to these pages are the auth actions and keep the cookies they set.
  - Redirects non-ADMIN users away from `/admin/**` (role decoded from the JWT).
- 401 handling in `serverApi()`, Server Actions and client callers: `.claude/rules/nextjs.md` §Auth.

## Tests

- Unit: token service (issue, rotation, reuse inside and outside the grace window, expiry), password policy, NIF validator (BR-3).
- IT: register/login/refresh/logout flows, inactive user, reuse after the grace window revokes all, role change revokes refresh tokens, every **P** path reachable without a token and other paths 401, 401 vs 403 vs 404, seeding idempotency.
- Vitest: `proxy.ts` (refresh decision, failed refresh with valid vs expired `lw_at`, Origin rejection, `/api/*` 401 vs page redirect, signed-in redirect from `/login` and `/register`, `session=expired` clears, POST `/login` keeps cookies), auth actions (cookies set or cleared, backend errors, validation), auth form schemas (password byte limit).
