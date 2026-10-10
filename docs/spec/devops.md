# DevOps

> Milestones: M0 (compose, Dockerfiles, CI skeleton), M7 (E2E job, image publishing).

## Docker Compose (`docker-compose.yml`)

- Services: `postgres:18`, `rabbitmq:4-management`, `backend` (port 8080; published only by `docker-compose.override.yml`, which Compose auto-loads locally; CI and prod use the base file only), `frontend` (port 3000).
- Healthchecks on all services; `backend` `depends_on` postgres and rabbitmq with `condition: service_healthy`; `frontend` depends on a healthy backend (`/actuator/health`).
- Named volumes: `pgdata`, `uploads` (mounted at `STORAGE_PATH`).

## Dockerfiles (multi-stage, non-root user)

- Backend: build in `eclipse-temurin:25-jdk` with `./mvnw -DskipTests package`; run in `eclipse-temurin:25-jre`.
- Frontend: `node:24-alpine`, Next.js `output: 'standalone'`.

## Environment (`.env.example`)

| Service  | Variables                                                                                                                                                                                                                                                                                                                                       |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend  | `SPRING_PROFILES_ACTIVE` (compose default `prod`), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `STORAGE_PATH`, `ANTHROPIC_API_KEY` (optional), `ANTHROPIC_MODEL`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`, `SEED_COMPANY_NAME`, `SEED_LOCATION_NAME`, `SEED_MUNICIPALITY` |
| Frontend | `BACKEND_URL`, `COOKIE_SECURE`                                                                                                                                                                                                                                                                                                                  |

Never commit `.env`. Secrets in CI come from GitHub Actions secrets.

## Spring profiles

`dev` (springdoc at `/swagger-ui`, debug logs), `test`, `prod`.

## GitHub Actions (`.github/workflows/ci.yml`)

Runs on push and on PRs to `main`; path filters per job; `concurrency` cancels in-progress runs for the same ref.

1. `backend`: setup-java Temurin 25 with Maven cache → `./mvnw -B verify` (Testcontainers works on `ubuntu-latest`).
2. `frontend`: Node 24 with npm cache → `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.
3. `e2e` (PRs to `main` only): create `.env` from `.env.example` with secrets from GitHub Actions secrets → `docker compose up -d --wait` → Playwright; upload the report as an artifact (7-day retention).
4. `publish` (push to `main`, after 1 and 2): buildx → `ghcr.io/<owner>/leavewise-{backend,frontend}:{sha,latest}`, pushed with `GITHUB_TOKEN` (`permissions: packages: write`).

Cost: free on public repositories; on a private repository the GitHub Free plan's monthly minutes cover this pipeline if E2E stays PR-only.
