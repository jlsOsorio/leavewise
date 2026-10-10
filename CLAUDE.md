# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

LeaveWise: vacation management for employees of Portuguese companies (Spring Boot 4 backend + Next.js 16 BFF frontend, monorepo).

Keep your replies extremely concise and focus on conveying the key information. No unnecessary fluff, no long code snippets.

@SPEC.md

`SPEC.md` is the source of truth for scope, stack, layout, architecture, conventions and milestones. Don't restate its rules elsewhere; update the file that owns a decision.

## Working on a milestone

- Before starting a milestone, read the files listed for it in `SPEC.md` §9. Don't load other `docs/spec/*.md` files unless the task changes that feature.
- Read `docs/data-model.md` before touching entities or migrations.
- For any version-specific API (Spring Boot 4, Spring AMQP, Jackson 3, Next.js 16, shadcn, Tailwind v4), check current docs first: delegate to the `docs-explorer` agent (`.claude/agents/`) or use context7 directly.
- After finishing a milestone, tick it in `SPEC.md` §9.

## Commands

Shell is Git Bash on Windows (`./mvnw` works there; use `mvnw.cmd` from PowerShell).

Backend (`cd backend`; Docker must be running for Testcontainers):

```bash
./mvnw verify                                   # compile, unit (*Test) + integration (*IT) tests, JaCoCo
./mvnw test -Dtest=VacationServiceImplTest          # single unit test class (append #method for one method)
./mvnw verify -Dit.test=VacationControllerIT -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false   # single IT only
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # needs postgres + rabbitmq (docker compose up -d postgres rabbitmq)
```

Frontend (`cd frontend`):

```bash
npm run lint && npm run typecheck && npm test   # full check (CI also runs npm run build)
npx vitest run src/lib/schemas/nif.test.ts      # single test file (-t "name" to filter)
npx playwright test e2e/<file>.spec.ts          # E2E; requires the compose stack up
npm run dev
```

Full stack: `docker compose up -d --wait` (frontend :3000, backend :8080, seeded admin from `.env`). Copy `.env.example` to `.env` first.
