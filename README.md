# LeaveWise

Vacation management for employees of Portuguese companies.

- **Employees** manage their own vacations from a calendar, bulk-import them from Excel (processed asynchronously with live status) and get suggestions that make the most of Portuguese public holidays, optionally explained by AI.
- **Admins** manage companies, locations, municipalities, work schedules, holidays, vacation policies, entitlements and users, and approve vacation requests.

> Status: in development. See the milestones in [`SPEC.md`](SPEC.md#9-milestones-in-order-check-off-when-done).

## Architecture

```
Browser ──(same-origin, httpOnly cookies)──> Next.js BFF ──(Bearer JWT)──> Spring Boot REST /api ──> PostgreSQL
                                                                              │
                                                                              ├──> RabbitMQ (Excel import queue)
                                                                              └──> Anthropic API (optional)
```

The browser only talks to Next.js; Next.js is the backend's only client. The backend enforces all authorization and business rules.

## Tech stack

| Area     | Tech                                                                                                   |
| -------- | ------------------------------------------------------------------------------------------------------ |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS v4, shadcn/ui, Zod, Vitest + MSW, Playwright |
| Backend  | Java 25, Spring Boot 4 (Web MVC, Security, Data JPA, AMQP), Flyway, MapStruct, Apache POI, Anthropic Java SDK |
| Infra    | PostgreSQL 18, RabbitMQ 4, Docker Compose, GitHub Actions, GHCR                                        |

## Repository layout

```
backend/     Spring Boot API (Maven)
frontend/    Next.js BFF and UI
docs/        Data model and feature specs (docs/spec/*.md)
SPEC.md      Scope, conventions and milestones (source of truth)
CLAUDE.md    Guidance for Claude Code
```

## Getting started

### Prerequisites

- Docker (with Compose)
- Java 25 (Temurin) for backend development
- Node.js 24 LTS for frontend development

### Run the full stack

```bash
cp .env.example .env      # fill in secrets (JWT_SECRET, admin credentials, ...)
docker compose up -d --wait
```

Frontend at <http://localhost:3000>, backend at <http://localhost:8080>, RabbitMQ management UI at <http://localhost:15672>. Only the frontend port is published by `docker-compose.yml`; `docker-compose.override.yml` (loaded automatically on your machine) publishes the backend, PostgreSQL (5432) and RabbitMQ (5672, 15672). An admin account is seeded from the `ADMIN_*` variables in `.env`. All environment variables are listed in [`docs/spec/devops.md`](docs/spec/devops.md).

### Backend

```bash
docker compose up -d postgres rabbitmq                   # from the repo root
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # Swagger UI at /swagger-ui
./mvnw verify                                            # unit + integration tests (Docker required)
```

On Windows PowerShell use `mvnw.cmd` instead of `./mvnw`.

### Frontend

```bash
cd frontend
npm install
npm run dev
npm run lint && npm run typecheck && npm run format:check && npm test
npm run eslint:fix && npm run format                     # auto-fix lint issues and format
npx playwright test                                      # E2E, requires the compose stack
```

### CI

GitHub Actions (`.github/workflows/ci.yml`) runs on pull requests to `main` and on pushes to `main`. The `backend` job runs `./mvnw -B verify`; the `frontend` job runs the full frontend check plus `npm run build`. Each job only runs when its folder (or the workflow) changed. Details in [`docs/spec/devops.md`](docs/spec/devops.md).

Lint the workflow locally before pushing (Docker required, from the repo root):

```bash
docker run --rm -v "$(pwd):/repo" -w /repo rhysd/actionlint:latest
```

In Git Bash on Windows, prefix it with `MSYS_NO_PATHCONV=1` and use `$(pwd -W)` instead of `$(pwd)`.

## Documentation

| File                                                      | Contents                                    |
| --------------------------------------------------------- | ------------------------------------------- |
| [`SPEC.md`](SPEC.md)                                      | Scope, stack, conventions, error codes, milestones |
| [`docs/data-model.md`](docs/data-model.md)                | Database schema                             |
| [`docs/spec/api.md`](docs/spec/api.md)                    | REST endpoint catalogue                     |
| [`docs/spec/business-rules.md`](docs/spec/business-rules.md) | Business rules, balance, status machine  |
| [`docs/spec/security.md`](docs/spec/security.md)          | Auth, tokens, cookies, access policy        |
| [`docs/spec/holidays.md`](docs/spec/holidays.md)          | Portuguese holiday generator                |
| [`docs/spec/suggestions.md`](docs/spec/suggestions.md)    | Vacation suggestion engine                  |
| [`docs/spec/import.md`](docs/spec/import.md)              | Excel import, RabbitMQ, SSE                 |
| [`docs/spec/frontend.md`](docs/spec/frontend.md)          | Pages, UX, E2E scenarios                    |
| [`docs/spec/devops.md`](docs/spec/devops.md)              | Docker, environment, profiles, CI           |
