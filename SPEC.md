# SPEC — LeaveWise (core)

> Always loaded. Cross-cutting rules only. Feature detail lives in `docs/spec/*.md`; read the files listed for the current milestone (§9) before starting it. Schema: `docs/data-model.md`.
> When a decision changes, update the file that owns it in the same commit. Each fact lives in one file; others refer to it by ID (`BR-x`, error code, section).

## 0. Agent Rules

- **Reuse before writing.** Before implementing, search the codebase for existing services, validators and utilities covering what you need. Never duplicate rule logic; implemented code is the source of truth for finished features.
- Respect the data model. Schema changes = new Flyway migration (`V{n}__desc.sql`) + update `docs/data-model.md`. Never edit an applied migration.
- Backend enforces all authorization and business rules. Frontend checks are UX only.
- Every new endpoint or rule ships with tests (§6). Each rule ID touched has at least one test.
- Don't add dependencies outside §2 without stating why in the commit message.
- Version-specific APIs (Spring Boot 4, Spring AMQP, Next.js 16, shadcn) must be checked against current docs (context7 MCP). If a class or option named in the spec doesn't exist, use the current equivalent and update the spec.
- Keep it simple: one backend instance, no microservices, no caching layer.

## 1. Scope

Web app for employees of Portuguese companies to manage vacations.

- Auth: register, login, logout; JWT access token + rotating refresh token.
- USER: picks an existing company and location at sign-up; manages **only their own** vacations via a calendar UI (create, edit, delete, cancel), Excel bulk import (async via RabbitMQ, live status via SSE) and AI suggestions based on Portuguese holidays.
- ADMIN: CRUD on all entities (companies, locations, municipalities, work schedules, holidays, vacation policies, entitlements, users, vacations) and vacation approval.

**Out of scope:** team/colleague calendars, email notifications, multi-instance scaling, i18n (English UI only), mobile apps, login rate limiting, deployment (CI and image publishing only).

## 2. Stack

| Area       | Tech                                                                                                                                                                                                                                                                                              |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Frontend   | Next.js 16 (App Router, Turbopack, `src/`), React 19, TypeScript (strict), TailwindCSS v4, shadcn/ui (Calendar = react-day-picker, Form = react-hook-form + @hookform/resolvers), Lucide React, Zod, Vitest + Testing Library + MSW, ESLint + Prettier, Playwright                                                     |
| Backend    | Java 25, Spring Boot 4 (Spring Framework 7, Spring Security 7, Jackson 3), Maven (wrapper `./mvnw`), Spring Web MVC, Spring Data JPA, Validation, Spring AMQP, OAuth2 Resource Server (Nimbus JWT), Lombok, MapStruct, Flyway, Apache POI (`poi-ooxml`), Anthropic Java SDK (`anthropic-java`, optional AI reason), Actuator, springdoc-openapi 3 (dev only) |
| Data/infra | PostgreSQL 18, RabbitMQ 4 (management image), Docker, Docker Compose                                                                                                                                                                                                                              |
| Tests (BE) | JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL, RabbitMQ) via `spring-boot-testcontainers` + `@ServiceConnection`                                                                                                                                                                          |
| CI/CD      | GitHub Actions; images pushed to GHCR                                                                                                                                                                                                                                                             |
| Runtime    | Node 24 LTS, Temurin 25                                                                                                                                                                                                                                                                           |

### Version gotchas (must follow)

- **Spring Boot 4 is modular.** Use `spring-boot-starter-webmvc` (not `-web`), `spring-boot-starter-flyway` (not bare `flyway-core`) plus `flyway-database-postgresql`, and per-technology test starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`, `-flyway-test`). `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure`. Use `@MockitoBean` (`@MockBean` was removed).
- Jackson 3 packages are `tools.jackson.*`; annotations stay `com.fasterxml.jackson.annotation.*`.
- **Next.js 16:** `src/proxy.ts` (exported `proxy` function, Node runtime) replaces `middleware.ts`. `cookies()`, `headers()`, `params` and `searchParams` are async: always `await` them. Server Components cannot set cookies; only `proxy.ts`, Route Handlers and Server Actions can.

## 3. Repository Layout (monorepo)

```
/SPEC.md  /CLAUDE.md  /docs/data-model.md  /.claude/{agents,rules,skills}/**/*.md  /.claude/settings.json  /docs/spec/*.md  /docker-compose.yml  /.env.example
/.github/workflows/ci.yml
/backend   pom.xml, Dockerfile, src/main/java/org/altechego/leavewise/..., src/main/resources/db/migration/
/frontend  package.json, Dockerfile, src/{app,components,lib,test}/..., e2e/
```

- **Backend packages (by feature):** `org.altechego.leavewise.{auth, user, company, location, municipality, schedule, holiday, policy, entitlement, vacation, suggestion, imports, file, ai, common}`. `common` holds `config/` (incl. `SecurityConfig`, `Clock` bean), `exception/`, `security/` (`AuthenticatedUser` principal, `Role`), `aop/`, `web/`, `messaging/`. Per-feature sub-packages and layering: `.claude/rules/spring-boot.md`.
- **Frontend:** route groups `(auth)` and `(app)`, admin pages under `(app)/admin/`, shared code in `src/lib/{api, auth, schemas}` and `src/components/{ui, calendar, forms, layout}` for common components (the forms only if we have a common form layout). The specific components for each route should be inside the routes themselves, using the `_components` folder (this should include, for instance, the specific forms). Full tree in `docs/spec/frontend.md`.

## 4. Architecture

```
Browser ──(same-origin, httpOnly cookies)──> Next.js (BFF: RSC, Server Actions, Route Handlers, proxy.ts)
                                                │ Authorization: Bearer <access>
                                                ▼
                                   Spring Boot REST /api    ──> PostgreSQL
                                     │  ▲ publish/consume
                                     ▼  │
                                   RabbitMQ (vacation.import)        Anthropic API (optional)
```

- The browser never calls the backend directly; the backend has no CORS config. Next.js is the only client.
- **Reads:** Server Components via `serverApi()` (`lib/api/server.ts`), which reads the `lw_at` cookie and calls `BACKEND_URL`.
- **JSON mutations:** Server Actions → Zod validation → backend → `revalidatePath`.
- **Route Handlers** only for auth cookie operations, multipart upload, SSE streaming, file download and client-side picker search (GET lookups for comboboxes).
- Business "today" = `LocalDate.now(clock)` with a `Clock` bean in `Europe/Lisbon`. Timestamps stored in UTC (`hibernate.jdbc.time_zone=UTC`).
- **Authorization model:** roles `USER` and `ADMIN` (JWT claim → `ROLE_<role>`). `@PreAuthorize` for roles, ownership checks in services. **A USER requesting another user's resource gets 404, not 403.** Details in `docs/spec/security.md`.

## 5. Conventions

### JPA

- `Long` ids with `@GeneratedValue(strategy = IDENTITY)`; `@ManyToOne(fetch = LAZY)`.
- Enums `@Enumerated(STRING)`, values matching the CHECK constraints.
- `LocalDate` for DATE, `LocalDateTime` (UTC) for TIMESTAMP; `created_at` set via `@PrePersist`, `updated_at` via `@PreUpdate` and `NULL` until the first update (no Spring Data auditing or Hibernate `@*Timestamp`; no `created_by`/`updated_by` columns).
- Lombok on entities: `@Getter @Setter @Builder @NoArgsConstructor(access = PROTECTED) @AllArgsConstructor`, `@Builder.Default` for fields with initial values, `@ToString.Exclude` on lazy associations; no `@Data`/`@EqualsAndHashCode`. `equals` on id (`id != null && id.equals(other.getId())`), `hashCode` = `getClass().hashCode()`.
- Mappings mirror the schema explicitly: `@Table(name)` on every entity, `@Column(name, nullable, length, unique)` on every field, multi-column unique keys as named `@Table(uniqueConstraints)`, `@JoinColumn(name, nullable)` with `optional = false` on required associations. Column lengths are constants in `<feature>/constants/XConstants`, shared by `@Column(length)` and the request DTOs' `@Size(max)`.
- Entity class names match the table, except `lw_user` → `LwUser` (avoids clashing with Spring Security's `User`).
- `spring.jpa.open-in-view=false`, `ddl-auto=validate`. Repositories extend `JpaRepository`.
- `day_of_week` is ISO: 1 = Monday … 7 = Sunday (`DayOfWeek.getValue()`).

### REST

- Base path `/api`, versioned by media type: `Accept`/`Content-Type: application/vnd.leavewise+json;version=1`. Responses that aren't JSON (SSE, file download) carry no version and get the default (`1`); multipart uploads send the versioned `Accept`. JSON camelCase, dates `yyyy-MM-dd`, timestamps ISO-8601 UTC. DTOs are records; requests use Bean Validation.
- Paths are nouns only; actions are sub-resources (`POST /vacations/{id}/approval`).
- Every collection is paged: `page` (0-based), `size` (default 20, max 100), `sort=field,asc` → `{content, page, size, totalElements, totalPages}`. `sort` accepts only the fields the endpoint lists in `docs/spec/api.md`; any other field → 400 `VALIDATION_ERROR`.
- Endpoint catalogue: `docs/spec/api.md`. How each side applies these: backend `.claude/rules/spring-boot.md`, frontend `.claude/rules/nextjs.md`.

### Errors

RFC 9457 `ProblemDetail` from a `@RestControllerAdvice`, with extra properties `code`, `timestamp` (ISO-8601 UTC) and, for validation, `errors:[{field,message}]`. The frontend maps `code` to English messages in `lib/api/errors.ts`.

| HTTP | Codes                                                                                                                                                                      |
| ---- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400  | `VALIDATION_ERROR`, `INVALID_FILE`, `UNSUPPORTED_API_VERSION`                                                                                                              |
| 401  | `UNAUTHENTICATED`, `INVALID_CREDENTIALS`, `INVALID_REFRESH_TOKEN`                                                                                                          |
| 403  | `FORBIDDEN`, `ACCOUNT_DISABLED`                                                                                                                                            |
| 404  | `NOT_FOUND`                                                                                                                                                                |
| 405  | `METHOD_NOT_ALLOWED`                                                                                                                                                       |
| 406  | `NOT_ACCEPTABLE`                                                                                                                                                           |
| 409  | `EMAIL_TAKEN`, `NIF_TAKEN`, `DUPLICATE`, `RESOURCE_IN_USE`, `LAST_ADMIN`                                                                                                   |
| 415  | `UNSUPPORTED_MEDIA_TYPE`                                                                                                                                                   |
| 422  | `MULTI_YEAR_RANGE`, `NO_WORKING_DAYS`, `VACATION_OVERLAP`, `NO_ENTITLEMENT`, `INSUFFICIENT_BALANCE`, `PAST_DATE`, `INVALID_STATUS_TRANSITION`, `LOCATION_COMPANY_MISMATCH` |
| 500  | `INTERNAL_ERROR`                                                                                                                                                           |

Backend: one `ErrorCode` enum holds these codes and their HTTP status. 405, 406, 415 and `UNSUPPORTED_API_VERSION` come from Spring MVC and signal a BFF bug, not a user error.

### Frontend

- Dates displayed `dd/MM/yyyy`; weeks start Monday. shadcn components and Lucide icons only; only dark mode.
- Loading states are skeletons (shadcn `Skeleton`) shaped like the content they replace; no page or section spinners. Buttons of a pending action are disabled and show an inline Lucide `Loader2`.
- One Zod schema per form, shared by the client form and the Server Action. No TypeScript `any`.

## 6. Testing (general)

- **Backend unit tests:** `*Test` (Surefire), JUnit 5 + Mockito + AssertJ, pure logic without Spring where possible.
- **Backend integration tests:** `*IT` (Failsafe), `@SpringBootTest` + Testcontainers PostgreSQL/RabbitMQ, MockMvc, real Flyway migrations, AI client mocked.
- Always cover: role and ownership (USER → 404 on another user's resource) and every 4xx code an endpoint can return.
- Coverage: JaCoCo ≥ 80% lines on `*ServiceImpl`, engines and calculators.
- **Frontend:** Vitest + Testing Library for schemas, utilities and key components, MSW for HTTP mocking (conventions: `.claude/rules/frontend-testing.md`); Playwright E2E against the compose stack (scenarios in `docs/spec/frontend.md`).
- Feature-specific test lists are in each module file.

## 7. Glossary

- **Entitlement:** a user's vacation days for one year (`vacation_entitlement`), created manually by ADMIN.
- **Policy:** a company's default days per year (`vacation_policy`); used only to prefill entitlements.
- **Applicable holiday:** see BR-6.
- **Balance / available days:** see BR-9.
- **Block:** a run of consecutive days off (suggestions).

## 8. Module Index (`docs/spec/`)

| File                | Owns                                                                                      |
| ------------------- | ----------------------------------------------------------------------------------------- |
| `security.md`       | tokens, refresh rotation, access policy, BFF cookies, `proxy.ts`, CSRF model, seeding (BR-2) |
| `business-rules.md` | BR-1..16, balance formula, status machine                                                 |
| `api.md`            | endpoint catalogue, `TokenPair`                                                           |
| `holidays.md`       | Portuguese holiday generator and triggers                                                 |
| `suggestions.md`    | suggestion engine, AI reason text                                                         |
| `import.md`         | Excel upload, RabbitMQ topology, consumer, SSE                                            |
| `frontend.md`       | page tree, UX per page, Playwright scenarios                                              |
| `devops.md`         | Docker, compose, env vars, profiles, CI                                                   |

## 9. Milestones (in order; check off when done)

|     | Milestone                                                                                         | Read                                                                                                     |
| --- | ------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| [x] | M0 Monorepo scaffold, compose, CI skeleton, Flyway V1, seed                                       | `devops.md`, `security.md` (seeding), `docs/data-model.md`                                               |
| [ ] | M1 Auth (register/login/refresh/logout), security config, BFF cookies + `proxy.ts`                | `security.md`, `api.md` (auth, public, me), `business-rules.md` (BR-1, BR-3), `frontend.md` (auth pages) |
| [ ] | M2 Admin CRUD: municipalities, companies, locations, work schedule, policies, users, entitlements | `api.md`, `business-rules.md` (BR-8, 13, 14, 15), `frontend.md` (admin)                                  |
| [ ] | M3 Holidays + generator; `PATCH /me` and `/me/*` endpoints                                        | `holidays.md`, `api.md` (holidays, me), `business-rules.md` (BR-6)                                       |
| [ ] | M4 Vacations: calculator, balance, status machine, preview, calendar UI, approvals                | `business-rules.md`, `api.md` (vacations), `frontend.md` (calendar, vacations, approvals)                |
| [ ] | M5 Import: storage, RabbitMQ, consumer, DLQ, SSE, UI                                              | `import.md`, `api.md` (imports), `frontend.md` (imports)                                                 |
| [ ] | M6 Suggestions: engine, AI reason, UI                                                             | `suggestions.md`, `api.md` (suggestions), `frontend.md` (suggestions)                                    |
| [ ] | M7 E2E suite, image publishing, README                                                            | `frontend.md` (Playwright), `devops.md` (CI)                                                             |

From M3 onwards, earlier milestones are implemented: rely on their code, not their spec files, unless the task changes them.

## 10. Definition of Done

- Builds, lint and tests pass locally and in CI. No TypeScript `any`; no Java compiler warnings in new code.
- Business rules enforced in the backend, with a test per rule ID touched.
- API changes reflected in `docs/spec/api.md`, the Zod schemas and the error mapper.
- Schema changes via a new migration, with `docs/data-model.md` updated.
- Milestone ticked in §9.
