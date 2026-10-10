# Business Rules

> Milestones: M1 (BR-1..3), M2 (BR-8, 13..15), M3 (BR-6), M4 (BR-4..12, 16). Rule IDs are referenced in code comments, tests and other spec files. Depends on: `docs/data-model.md`.

| ID    | Rule                                                                                                                                                                                                                                                              |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR-1  | Self-registration always creates role USER with `is_active=true`. Company and location come from public lists, and the location must belong to the company (`LOCATION_COMPANY_MISMATCH`). Only an ADMIN changes a user's role, company, location or active state. |
| BR-2  | Startup seeding creates the first ADMIN plus the company, location and municipality it needs. Details in `security.md` §Seeding.                                                                                                                                  |
| BR-3  | `nif` is optional. If present: 9 digits with a valid Portuguese check digit (mod 11), unique (`NIF_TAKEN`). `email` is mandatory, lowercased and unique (`EMAIL_TAKEN`).                                                                                          |
| BR-4  | A vacation must lie within a single calendar year (`MULTI_YEAR_RANGE`). `end_date >= start_date` (`VALIDATION_ERROR`).                                                                                                                                            |
| BR-5  | `days` is always computed server-side (client values ignored): dates in range that are working days per the company's `work_schedule_day` rows (default Mon–Fri when there's no schedule) and not applicable holidays (BR-6). Must be > 0 (`NO_WORKING_DAYS`).    |
| BR-6  | A holiday applies to a user when `municipality_id IS NULL` or equals the municipality of the user's location; `type` doesn't affect applicability. Type/municipality consistency: NATIONAL has none, MUNICIPAL requires one, OTHER is optional (`VALIDATION_ERROR`). One holiday per date and municipality (`DUPLICATE`). |
| BR-7  | No overlap with the same user's PENDING or ACCEPTED vacations (`VACATION_OVERLAP`), excluding the vacation being edited.                                                                                                                                          |
| BR-8  | An entitlement must exist for the year (`NO_ENTITLEMENT`). ADMIN creates entitlements manually; the UI prefills `annual_days` from the company policy for that year.                                                                                              |
| BR-9  | Balance: request fails with `INSUFFICIENT_BALANCE` when `days > available` (formula below).                                                                                                                                                                       |
| BR-10 | USER: `start_date` must be after today (`PAST_DATE`). ADMIN may use past dates.                                                                                                                                                                                   |
| BR-11 | Status machine (below). Other transitions fail with `INVALID_STATUS_TRANSITION`. Owners act only on their own vacations; ADMIN acts on any.                                                                                                                       |
| BR-12 | Vacations created by a USER (UI, import or suggestion) start PENDING. Vacations created or imported by an ADMIN (for anyone, themselves included) start ACCEPTED.                                                                                                 |
| BR-13 | One `vacation_policy` per company per year (`DUPLICATE`). Entitlements are not auto-synced from it.                                                                                                                                                               |
| BR-14 | Deleting a user (ADMIN): first delete their imports, files (storage + rows) and refresh tokens, then the user. ADMIN cannot delete or deactivate themselves or the last active ADMIN (`LAST_ADMIN`).                                                              |
| BR-15 | Deleting a company, location or municipality still referenced under RESTRICT returns 409 `RESOURCE_IN_USE`.                                                                                                                                                       |
| BR-16 | Creating, updating (old and new date/municipality), deleting or generating holidays recomputes `days` (BR-5) of PENDING and ACCEPTED vacations **not yet ended** (`end_date >= today`) that contain an affected date, for users the holiday applies to (BR-6). Same transaction; never fails the holiday change (balance may go negative). Ended vacations stay frozen. If the result would be 0 days, the vacation becomes CANCELLED and keeps its previous `days`. |

## BR-9 balance formula

For year Y: `used` = Σ`days` of the user's PENDING and ACCEPTED vacations in Y (excluding the one being edited). From the entitlement: `annual`, `carried`, `exp` = `carried_over_expiry_date`.

- If `exp` is null, or the new vacation's `end_date <= exp`: `available = annual + carried − used`.
- Otherwise: `carriedConsumed = min(carried, Σdays of those vacations with end_date <= exp)` and `available = annual − (used − carriedConsumed)`.

`/me/balance` returns `availableDays` computed as if for a vacation ending after `exp` when today is past `exp`, otherwise as if before it.

## BR-11 status transitions

| From                | Action        | To                  | Who                                            |
| ------------------- | ------------- | ------------------- | ---------------------------------------------- |
| PENDING             | edit / delete | PENDING / (deleted) | owner while `start_date > today`; ADMIN always |
| PENDING             | approve       | ACCEPTED            | ADMIN                                          |
| PENDING             | reject        | REJECTED            | ADMIN                                          |
| ACCEPTED            | cancel        | CANCELLED           | owner while `start_date > today`; ADMIN always |
| PENDING, ACCEPTED   | holiday change leaves 0 days (BR-16) | CANCELLED | system                              |
| REJECTED, CANCELLED | —             | terminal            | —                                              |

## Implementation notes

- Put BR-4..10 in one reusable `VacationRules` (or validator) component used by manual creation, editing, preview, import and suggestion acceptance. Never re-implement these checks elsewhere.
- `WorkingDayCalculator` (BR-5/6) and the balance calculator (BR-9) are pure classes, unit-tested without Spring.
- Preview (`/vacations/preview`) runs the same checks and returns all failing codes in `problems` instead of throwing.
- BR-16: the holiday feature publishes `HolidaysChangedEvent(dates, municipalityId)`; the vacation feature handles it in a synchronous `@EventListener` (same transaction) with `WorkingDayCalculator`. Holiday never depends on vacation. Until M4 the event has no listener.

## Tests

- Unit: `WorkingDayCalculator` (weekends, custom schedule, national and municipal holidays), balance formula (with and without expiry, before and after expiry, editing), status machine (every allowed and forbidden transition).
- IT: one test per 422 code, BR-12 initial status by actor, BR-14 and BR-15 delete behavior, BR-16 (ended vacation untouched, upcoming recalculated, zero days → CANCELLED).
