---
paths:
  - "frontend/**/*.test.ts"
  - "frontend/**/*.test.tsx"
  - "frontend/src/test/**"
---

# Frontend Testing Conventions (Vitest)

Always applies to frontend unit and component tests. What to test is owned by `SPEC.md` §6 and the per-feature lists in `docs/spec/*.md`.

## What to test

- Test **behaviour, not layout**: what the user can do and what the code decides. Examples: validation and error messages, interactions and the resulting state, data → displayed result, enabled/disabled actions, error and 401 handling, schema and utility logic
- **DO NOT** test markup structure, CSS classes, styling, static text or static pages, and don't write snapshot tests
- **DO NOT** test shadcn primitives or Next.js itself; test our components that use them
- Visual and layout checks belong to Playwright E2E, not unit tests

## Structure & naming

- Test files sit next to the code they test: `nif.ts` → `nif.test.ts`
- Use `test(...)`, never `it(...)`
- Test names state the expected behaviour in the present tense, read after the word "test": `test('rejects a NIF with a wrong check digit')`, `test('shows the app name as the main heading')`. Never start with "should" or "it"
- `describe` only groups **two or more** tests of the same unit; a file with a single test has none. Its name is the unit under test (`describe('nifSchema')`, `describe('CompanyCombobox')`), so `describe` + `test` reads as one sentence
- Arrange / act / assert separated by a blank line; one reason to fail per test

## Rendering

- Query by role, label or text (`getByRole`, `getByLabelText`), as a user would; `getByTestId` only as a last resort
- Interactions with `@testing-library/user-event`, not `fireEvent`
- Components needing a React context (providers): render through `src/test/utils.tsx`, created when the first provider is needed:
  - A `renderWithContext(ui, options)` that wraps `ui` in the required providers
  - Re-exports everything from `@testing-library/react` and exports `renderWithContext` **as `render`**
  - Tests then import `render`, `screen`, ... from `@/test/utils` instead of `@testing-library/react`

## Network mocking (MSW)

- Never mock `fetch` with `vi.fn`/`vi.spyOn`; intercept HTTP with MSW (`msw/node`)
- `src/test/msw/handlers.ts`: default happy-path handlers, built from `process.env.BACKEND_URL` (set in `vitest.config.mts`) for the backend and relative `/api/*` paths for our Route Handlers. Response bodies follow `docs/spec/api.md`; errors are `application/problem+json` with `code`
- `src/test/msw/server.ts`: exports `server = setupServer(...handlers)`
- `src/test/setup.ts` starts the server with `onUnhandledRequest: 'error'`, resets handlers after each test and closes it at the end. **DO NOT** repeat this lifecycle in test files
- Error or edge cases override a handler inside the test with `server.use(http.get(...))`; it is reset after the test
- Assert on what the code does with the response (returned value, rendered UI), not on the request having been made
- Use for: `serverApi()` and its error parsing, Server Actions, Route Handlers, client components calling `/api/*` (combobox, upload, download)
