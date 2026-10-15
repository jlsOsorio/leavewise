# REST API (`/api`, media-type versioned)

> All milestones. Conventions (nouns only, pagination, media type) and the error format are in `SPEC.md` §5. Access legend: **P** public, **U** authenticated (own data only; another user's or company's resource → 404), **A** ADMIN. Every list endpoint is paged (`Page` below = SPEC §5 paged shape).

## Auth and public

| Method | Path                           | Access | Notes                                                                             |
| ------ | ------------------------------ | ------ | --------------------------------------------------------------------------------- |
| POST   | /auth/registration             | P      | `{name,email,password,nif?,contact?,companyId,companyLocationId}` → 201 TokenPair |
| POST   | /auth/login                    | P      | `{email,password}` → 200 TokenPair                                                |
| POST   | /auth/refresh                  | P      | `{refreshToken}` → 200 TokenPair (rotates)                                        |
| POST   | /auth/logout                   | P      | `{refreshToken}` → 204 (the refresh token is the credential)                      |
| GET    | /auth/companies?q              | P      | Page `{id,name}` for the register form; `q` = case-insensitive contains on name   |
| GET    | /auth/companies/{id}/locations | P      | Page `{id,name}`                                                                  |

`TokenPair` = `{accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, user:{id,name,email,role,companyId,companyLocationId}}`.

Pickers: `q` matches the company name only (a blank `q` lists all companies); locations are not searchable. Both sort by `name` only (default `name,asc`). An unknown company id on `/locations` → 404 `NOT_FOUND`.

Registration checks, in order: unknown `companyId` → 404 `NOT_FOUND`; location missing or in another company → 422 `LOCATION_COMPANY_MISMATCH` (BR-1); then `EMAIL_TAKEN`, `NIF_TAKEN` (BR-3).

## Me

| Method      | Path               | Access | Notes                                                                                            |
| ----------- | ------------------ | ------ | ------------------------------------------------------------------------------------------------ |
| GET / PATCH | /me                | U      | PATCH: `name, contact, nif`                                                                      |
| PUT         | /me/password       | U      | `{currentPassword,newPassword}` → 204; revokes refresh tokens                                    |
| GET         | /me/company        | U      | company, location + municipality, work schedule, current-year policy                             |
| GET         | /me/holidays?year= | U      | Page of applicable holidays (BR-6)                                                               |
| GET         | /me/entitlements   | U      | Page                                                                                             |
| GET         | /me/balance?year=  | U      | `{year,annualDays,carriedOverDays,carriedOverExpiryDate,acceptedDays,pendingDays,availableDays}` |

## Administration

| Method    | Path                                                       | Access         | Notes                                                                     |
| --------- | ---------------------------------------------------------- | -------------- | ------------------------------------------------------------------------- |
| CRUD      | /companies[/{id}]                                          | A (GET: U own) |                                                                           |
| CRUD      | /companies/{id}/locations, /locations/{id}                 | A              |                                                                           |
| CRUD      | /municipalities[/{id}]                                     | A (GET: U)     |                                                                           |
| GET / PUT | /companies/{id}/work-schedule                              | A (GET: U own) | PUT `{name, days:[{dayOfWeek,isWorkingDay}]}`, exactly 7 entries (upsert) |
| CRUD      | /companies/{id}/vacation-policies, /vacation-policies/{id} | A (GET: U own) |                                                                           |
| CRUD      | /holidays[/{id}]?year&municipalityId&type                  | A              |                                                                           |
| POST      | /holidays/generation?year=                                 | A              | → 200 `{created}`                                                         |
| CRUD      | /users[/{id}]?companyId&role&active&q                      | A              | PUT may change role, company, location, isActive                          |
| PUT       | /users/{id}/password                                       | A              | reset → 204; revokes refresh tokens                                       |
| CRUD      | /users/{id}/entitlements, /entitlements/{id}               | A              | unique (user, year)                                                       |

## Vacations

| Method | Path                                                  | Access | Notes                                                    |
| ------ | ----------------------------------------------------- | ------ | -------------------------------------------------------- |
| GET    | /vacations?year&status&userId                         | U / A  | Page; U sees own only; `userId` filter A only            |
| GET    | /vacations/{id}                                       | U / A  |                                                          |
| GET    | /vacations/preview?startDate&endDate&excludeId&userId | U / A  | `{days, availableDays, problems:[code]}`, no persistence |
| POST   | /vacations                                            | U / A  | `{startDate,endDate,userId?}`; `userId` A only → 201     |
| PUT    | /vacations/{id}                                       | U / A  | dates only; PENDING only                                 |
| DELETE | /vacations/{id}                                       | U / A  | PENDING only → 204                                       |
| POST   | /vacations/{id}/cancellation                          | U / A  | ACCEPTED → CANCELLED; → 200 VacationDto                  |
| POST   | /vacations/{id}/approval                              | A      | PENDING → ACCEPTED; → 200 VacationDto                    |
| POST   | /vacations/{id}/rejection                             | A      | PENDING → REJECTED; → 200 VacationDto                    |

## Suggestions

| Method | Path                          | Access | Notes                                                                       |
| ------ | ----------------------------- | ------ | --------------------------------------------------------------------------- |
| POST   | /suggestions/generation?year= | U      | → 200 `{created}` (replaces SUGGESTED rows for the year; then GET the list) |
| GET    | /suggestions?year&status      | U      | Page                                                                        |
| POST   | /suggestions/{id}/acceptance  | U      | → 201 VacationDto                                                           |
| POST   | /suggestions/{id}/dismissal   | U      | → 200 SuggestionDto                                                         |

## Imports

| Method | Path                 | Access | Notes                                       |
| ------ | -------------------- | ------ | ------------------------------------------- |
| POST   | /imports             | U / A  | multipart `file` → 202 ImportDto            |
| GET    | /imports             | U / A  | Page; U own, A all                          |
| GET    | /imports/{id}        | U / A  | ImportDto with `errorCount`                 |
| GET    | /imports/{id}/errors | U / A  | Page `{rowNumber, message}`, by `rowNumber` |
| GET    | /imports/{id}/events | U / A  | `text/event-stream` (default version)       |
| GET    | /imports/template    | U / A  | xlsx (default version)                      |
