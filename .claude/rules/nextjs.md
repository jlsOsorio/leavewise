---
paths:
  - "frontend/**"
---

# Next.js Conventions (baseline)

Always applies to frontend code. Version gotchas (async `cookies()`/`params`, `proxy.ts`, who may set cookies) are in `SPEC.md` §2; UI rules in `SPEC.md` §5 Frontend; pages and UX in `docs/spec/frontend.md`. Tests: `frontend-testing.md`. For how/why use the `modern-best-practice-nextjs`, `modern-best-practice-react-components` and `clean-typescript` skills.

## Design reference

- The visual template lives on branch `style/fe-design` (never merged): the `/design` page plus `globals.css`, `components/{ui,layout,forms}` and `hooks/`. It is a reference, not a source to merge
- Chosen look: graphite dark base with the **emerald** accent (already the default tokens in the branch's `globals.css`)
- Fonts: **IBM Plex Sans / IBM Plex Mono first**, Geist Sans / Geist Mono as fallback, loaded via `next/font/google` in `src/app/layout.tsx` (`--font-sans` = Plex, then Geist). The branch's root layout still uses Geist only: fix that when bringing it over
- Bring files over only when a feature needs them, committed with that feature: `git checkout style/fe-design -- <path>`. Copy shadcn `ui/*` from the branch too (some are customised), not `shadcn add`
- Before writing UI: reuse what's on the current branch → else copy from `style/fe-design` (`git show style/fe-design:<path>`; the `/design` page shows how pieces compose) → only then write new code, in the same style
- Once copied, the file on main is the source of truth; adapt it freely

## Talking to the backend

- Only server code calls the backend: `serverApi()` (`src/lib/api/server.ts`) for reads and Server Actions, Route Handlers for auth cookies, upload, SSE and download. Never from client components
- Media type and version come from one module, `src/lib/api/media-type.ts` (`API_MEDIA_TYPE = 'application/vnd.leavewise+json;version=1'`; value owned by `SPEC.md` §5). **DO NOT** hardcode it elsewhere

| Request                        | `Accept`                       | `Content-Type`             |
| ------------------------------ | ------------------------------ | -------------------------- |
| JSON (read or mutation)        | `API_MEDIA_TYPE`               | `API_MEDIA_TYPE` with body |
| Multipart upload (`/imports`)  | `API_MEDIA_TYPE`               | unset (`fetch` sets the boundary) |
| SSE (`/imports/{id}/events`)   | `text/event-stream, application/problem+json` | —           |
| xlsx download                  | the xlsx media type, `application/problem+json` | —         |

- SSE and downloads carry no version; the backend applies the default. `application/problem+json` lets errors (401, 404) arrive as ProblemDetail instead of 406
- Lists are always paged (`SPEC.md` §5): pass `page`/`size`/`sort`, type responses as `Page<T>`
- Pickers: lists that can exceed one page use a shadcn Combobox (Popover + Command) that loads the first page and searches server-side via `q`, debounced ~300 ms, aborting the in-flight request (`AbortController`). The client calls a GET Route Handler that proxies to the backend (never a Server Action, which is POST and runs sequentially). Short bounded lists use a plain Select with `size=100`

## Errors

- Error bodies are `application/problem+json`; `serverApi()` parses them into `ApiError {status, code, detail, errors?: {field, message}[]}`
- `src/lib/api/errors.ts` maps every `code` from `SPEC.md` §5 to an English message; unknown codes fall back to `detail`
- 401 handling: §Auth

## Auth (design in `docs/spec/security.md`)

- **Never** pass a token to a client component or return it in a response body; pass only display fields (name, role). Who may set cookies: `SPEC.md` §2
- The JWT decoded on the frontend (without verification) is for UX only (redirects, menus); the backend decides access
- CSRF is handled centrally (`security.md` §Frontend): **DO NOT** add per-handler checks. GET Route Handlers never mutate
- Backend 401:
  - Server Actions and Route Handlers: clear both cookies, then redirect to `/login` (Server Action) or return 401 (Route Handler)
  - Server Components can't set cookies: `redirect('/login')` (the proxy clears them there)
  - Client callers of `/api/*` (upload, combobox, download) navigate to `/login?next=<current path>` on 401. SSE falls back to `router.refresh()`, which the proxy redirects

## Loading (skeletons, `SPEC.md` §5)

- Every `(app)` and `(auth)` route segment has a `loading.tsx` rendering its page skeleton (Next.js wraps `page` in Suspense)
- Independent slow sections (e.g. the calendar balance card) get their own `<Suspense fallback={<XSkeleton />}>` so the rest streams
- `loading.tsx` doesn't cover its own `layout.tsx`, and runtime data in a layout (`cookies()`, uncached fetch) blocks navigation: wrap the `(app)` header's user/company fetch in its own `<Suspense>` with a header skeleton
- Skeletons live next to what they mimic (`_components/XSkeleton.tsx`); shared ones (table, card) in `src/components/layout/`
- Client-side waits use skeletons too (combobox results, preview dialog); pending submits use `useActionState`/`useFormStatus` for the disabled button + `Loader2`

## Data flow

- Reads in Server Components via `serverApi()`
- Mutations in Server Actions: Zod parse → backend → `revalidatePath` → return `{ok: true} | {ok: false, error: {code, message, fieldErrors?}}`
- Forms: shadcn Form + react-hook-form + `zodResolver`; the same schema (`src/lib/schemas/`) runs in the Server Action
- Route-specific components live in the route's `_components/`; shared ones in `src/components/`
