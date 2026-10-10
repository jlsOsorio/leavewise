# Excel Import (async)

> Milestone: M5. Depends on (already implemented): auth (M1), `VacationRules` and balance (`vacation/`, BR-4..12), `file` and `vacation_import*` tables (`docs/data-model.md`), endpoints in `api.md` §Imports.

## Upload
`POST /imports` (multipart `file`):
- `.xlsx` only, ≤ 5 MB (`spring.servlet.multipart.max-file-size` and `max-request-size`), ZIP magic bytes `PK`, content type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`; otherwise 400 `INVALID_FILE`. `GlobalExceptionHandler` maps `MaxUploadSizeExceededException` to 400 `INVALID_FILE` (not Spring's default 413).
- Stored through the `file` feature: `imports` calls `FileService`, which owns the `StorageService` interface (`LocalStorageService`, root `STORAGE_PATH`, key `{userId}/{uuid}.xlsx`).
- Creates `file` + `vacation_import` (PENDING), publishes the message **after commit** (`@TransactionalEventListener(AFTER_COMMIT)`), returns 202 ImportDto.

## Sheet format
- First sheet; headers in row 1 (case-insensitive): `start_date`, `end_date`, optional `email`. Max 500 data rows. Empty rows are skipped.
- Dates: Excel date cells, `yyyy-MM-dd` or `dd/MM/yyyy`.
- `email` (matched case-insensitively, BR-3) is for ADMIN only: the target user, who must exist and be active; otherwise a row error. An empty `email` means the uploader. For a USER, an `email` different from their own is a row error. Initial status per BR-12.
- `GET /imports/template` serves a sample file generated with POI.

## RabbitMQ topology (beans)
- Direct exchange `vacation.exchange`, routing key `vacation.import`.
- Queue `vacation.import` (durable; args `x-dead-letter-exchange=vacation.dlx`, `x-dead-letter-routing-key=vacation.import.dlq`).
- Exchange `vacation.dlx` → queue `vacation.import.dlq`.
- Message: JSON `{ "importId": <long> }` with `JacksonJsonMessageConverter` (Spring AMQP 4, Jackson 3; not the deprecated `Jackson2JsonMessageConverter`).

## Retries and DLQ
- Listener retry: 1 attempt + 3 retries (max attempts 4), backoff 1 s × 2, stateless.
- When exhausted: reject without requeue → DLQ. A DLQ listener marks the import FAILED, adds an error row (`row_number=1`, "Processing failed after retries") and pushes an SSE event.

## Consumer
1. Idempotency: if status is COMPLETED or FAILED, ack and skip.
2. Set PROCESSING in its own transaction; push an SSE event.
3. Parse and validate every row with `VacationRules`. Valid rows accumulate per target user, so later rows see them: overlap (BR-7) and days used (BR-9) across rows of the same file.
4. In **one transaction**: insert valid vacations, insert `vacation_import_error` rows (Excel row number + the `ApiException` detail as message), set `total_rows`, `imported_rows`, `processed_at`, status COMPLETED. Push an SSE event.
5. File-level problems (unreadable file, missing headers, no data rows, > 500 rows) → FAILED with one error row. Unexpected exceptions are rethrown so the retry applies.

SSE events are pushed **after commit** (as the publish), so a client that refreshes sees the committed state.

## SSE
- `GET /imports/{id}/events` → `SseEmitter` (5 min timeout). Ownership is checked (404) before the emitter is created, so errors are still a ProblemDetail.
- `ImportEventRegistry` (in memory, single instance assumption) holds several emitters per import and removes them in `onCompletion`/`onTimeout`/`onError`.
- Event name `import-status`, data `{id, status, totalRows, importedRows, errorCount}`. Current state sent on subscribe; the stream completes on COMPLETED or FAILED (immediately if already terminal).
- The emitter's async dispatch must be permitted by Spring Security (`spring-boot-best-practices` skill §Security config).
- `spring.threads.virtual.enabled=true` (app-wide; keeps long-lived SSE connections cheap).
- Frontend: `EventSource` to `/api/imports/[id]/events`; that Route Handler calls the backend with the bearer token and `request.signal` (so the backend stream closes when the browser leaves) and returns `response.body` as the stream. Headers per `.claude/rules/nextjs.md`. On error, fall back to `router.refresh()` every 3 s until a terminal status.

## Errors list
`GET /imports/{id}/errors` is paged, sorted by `rowNumber`; the import detail carries only `errorCount`.

## Tests
- Unit: row parser (date formats, headers, email matching and rules), file validation.
- IT (Testcontainers RabbitMQ): valid file → COMPLETED; partial file → COMPLETED with errors; bad headers and empty sheet → FAILED; listener forced to throw → DLQ → FAILED after 4 attempts; redelivered COMPLETED message is skipped; SSE event sequence PENDING → PROCESSING → COMPLETED; balance exhausted across rows of the same file; ADMIN `email` row starts ACCEPTED (BR-12); `INVALID_FILE` for wrong type, wrong magic bytes and > 5 MB; USER → 404 on another user's import, errors and events; errors list is paged.
