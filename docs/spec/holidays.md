# Holidays (Portugal)

> Milestone: M3. Depends on: BR-6 and BR-16 (`business-rules.md`), `holiday` table (`docs/data-model.md`), holiday endpoints (`api.md`).

## Generator

- `PortugueseHolidayCalendar.holidaysOf(year)`: pure class, returns the 13 national holidays below. Easter comes from `EasterCalculator` (Anonymous Gregorian algorithm).
- `HolidayServiceImpl.generate(year)` persists them as NATIONAL with `municipality_id = NULL` and returns `{created}`. It is idempotent: load the year's national dates in one query, `saveAll` only the missing ones. `year` must be 2021–2100 (`VALIDATION_ERROR`; matches the `year > 2020` checks in the data model).

| Date        | Name                         |
| ----------- | ---------------------------- |
| Jan 1       | Ano Novo                     |
| Easter − 2  | Sexta-feira Santa            |
| Easter      | Páscoa                       |
| Apr 25      | Dia da Liberdade             |
| May 1       | Dia do Trabalhador           |
| Easter + 60 | Corpo de Deus                |
| Jun 10      | Dia de Portugal              |
| Aug 15      | Assunção de Nossa Senhora    |
| Oct 5       | Implantação da República     |
| Nov 1       | Todos os Santos              |
| Dec 1       | Restauração da Independência |
| Dec 8       | Imaculada Conceição          |
| Dec 25      | Natal                        |

Carnaval is **not** generated; an ADMIN may add it as OTHER.

## Triggers

All call `HolidayServiceImpl.generate(year)`; years come from the `Clock` bean.

- On startup: an `ApplicationRunner` (after Flyway) ensures the current and next year.
- `@Scheduled(cron = "0 0 3 1 12 *", zone = "Europe/Lisbon")` (needs `@EnableScheduling`): generates the next year.
- `POST /holidays/generation?year=` (ADMIN) → 200 `{created}`.

## CRUD

- ADMIN CRUD; type/municipality consistency and uniqueness per BR-6.
- Every create, update, delete and generation publishes `HolidaysChangedEvent(dates, municipalityId)` for BR-16 (for an update: old and new date/municipality).

## Tests

- Unit: `EasterCalculator` for known years (2025-04-20, 2026-04-05, 2027-03-28); `PortugueseHolidayCalendar` (13 dates); generation idempotency (Mockito); BR-6 type/municipality validation.
- IT: generation endpoint (a second call returns `{created: 0}`; year out of range → 400); duplicate holiday → 409 `DUPLICATE`; `/me/holidays` is paged and returns national + the user's municipal holidays only (BR-6). BR-16 recalculation ITs live with the vacation feature (M4).
