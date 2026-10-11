# Frontend

> Milestones: M1 (auth pages, layout), M2 (admin), M4 (calendar, vacations, approvals), M5 (imports), M6 (suggestions), M7 (Playwright). Depends on: BFF and cookies (`security.md`), endpoints (`api.md`), error codes (`SPEC.md` §5).

## Page tree

```
src/app/(auth)/login, (auth)/register
src/app/(app)/calendar (home), vacations, imports, imports/[id], suggestions, profile
src/app/(app)/admin/{companies, municipalities, users, holidays, policies, entitlements, schedules, approvals}
src/app/api/auth/{login,registration,logout}/route.ts, api/auth/companies/route.ts (picker search)
src/app/api/imports/route.ts (upload), api/imports/[id]/events/route.ts (SSE proxy), api/imports/template/route.ts
src/lib/api/{server.ts, errors.ts, media-type.ts}  src/lib/auth/  src/lib/schemas/  src/lib/dates.ts
src/components/{ui (shadcn), calendar, forms, layout}
src/proxy.ts
src/test/{setup.ts, utils.tsx (custom render, when needed), msw/{handlers.ts, server.ts}}
```

## Layout

Sidebar: Calendar, Vacations, Suggestions, Imports, Profile, plus an Admin section for ADMIN. Header: user name, company, logout.

## Pages

- **Login / Register:** register has a company picker (searchable Combobox, `.claude/rules/nextjs.md`), then a plain location select loaded for that company; Zod validation includes the NIF check digit (BR-3), the email pattern and the password policy. The credentials are email + password.
- **Calendar (home):**
  - Year selector; month grid on the shadcn `Calendar` in range mode.
  - Legend: non-working days (muted), holidays (tooltip with name), vacations by status (PENDING amber, ACCEPTED green, REJECTED/CANCELLED struck through), suggestions (dashed outline).
  - Balance card: annual, carried over, accepted, pending, available.
  - Selecting a range opens a create dialog that calls `/vacations/preview` (debounced) and shows days and problems. Clicking a vacation opens a dialog with edit, delete or cancel, enabled per BR-11.
- **Vacations:** table with year and status filters.
- **Suggestions:** "Generate for {year}" button; cards with the dates to request, days, the block of days off (start–end, length), reason, Accept and Dismiss. This should be triggered by the user, not automatically.
- **Imports:** dropzone (xlsx, 5 MB), template download, history table. Detail page: live status (SSE), counters, paged error table (row, message).
- **Profile:** edit name, contact, NIF; change password.
- **Admin:** paginated data tables with create/edit dialogs per entity; Approvals (PENDING vacations, approve/reject); Holidays with "Generate national holidays for year"; entitlement form prefilled from the policy (BR-8); work-schedule editor with 7 toggles; companies management; users management.

## Patterns

Data fetching, mutations, backend headers and error handling: `.claude/rules/nextjs.md`.

## Tests

- Vitest: Zod schemas (NIF, email, password, date ranges), date utilities, error mapper, calendar day classification, key components, company combobox (one request after typing stops; stale responses ignored).
- Playwright (compose stack, seeded admin):
  1. Register → login → create a vacation from the calendar.
  2. Admin approves → user sees ACCEPTED.
  3. Upload xlsx → import detail shows COMPLETED with counters.
  4. USER cannot open `/admin`.
  5. Generate and accept a suggestion.
