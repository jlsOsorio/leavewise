# AI Suggestions

> Milestone: M6. Depends on (already implemented): `VacationService` (BR-4..12), `WorkingDayCalculator`, balance calculator (`vacation/`), applicable holidays (`holiday/`, BR-6), `vacation_suggestion` table (`docs/data-model.md`), endpoints in `api.md` §Suggestions.

## Generation

`POST /suggestions/generation?year=Y` (own user) → 200 `{created}`:
- `Y` must be the current or next year (`VALIDATION_ERROR`); an entitlement for `Y` is required (`NO_ENTITLEMENT`).
- Deletes the user's SUGGESTED rows for `Y` and regenerates them. DISMISSED and ACCEPTED rows are kept.
- `SuggestionServiceImpl` builds the engine input read-only, batch-loaded once per year: applicable holidays via `HolidayService`, the company work schedule, the user's PENDING/ACCEPTED vacations, the year's DISMISSED windows and the balance.
- Order: compute → `ReasonWriter` (outside any transaction) → persist in one transaction.
- SUGGESTED rows are not recomputed when holidays or vacations change (BR-16 covers vacations only); acceptance re-validates, and the UI offers regeneration.

## Algorithm (`SuggestionEngine`, pure, unit-tested)
1. A day is **off** when it is a non-working day (schedule), an applicable holiday, or covered by a PENDING or ACCEPTED vacation.
2. **Candidates:** windows of `k` consecutive working days (`k` = 1..5) within year `Y`, starting after today, where the day before and the day after the window are off. The **block** is the maximal run of off days around the window plus the window (it may extend into the neighbouring year). The block must contain at least one holiday. Windows equal to a DISMISSED row of `Y` are skipped.
3. **Score:** `efficiency = blockLength / k`. Ties → larger `blockLength`, then earlier date. Discard `efficiency < 2`.
4. **Select:** greedily pick non-overlapping, non-adjacent blocks, at most 5, while Σ`k` ≤ `available`. `available` is the conservative BR-9 value (as for a vacation ending after `carried_over_expiry_date`); acceptance re-checks.
5. **Persist:** `start_date`/`end_date` = the window, `block_start_date`/`block_end_date` = the block, `days = k`, `year = Y`, status SUGGESTED, `reason` from below. `blockLength` is derived in the DTO.

## Reason text (`ai` package, optional)
- `ReasonWriter` interface:
  - `TemplateReasonWriter` is the default and costs nothing; it is used when `ANTHROPIC_API_KEY` is empty.
  - `ClaudeReasonWriter` is active only when the key is set (`@ConditionalOnProperty`).
  - Settings in an `AnthropicProperties` record (`common/config`).
- `ClaudeReasonWriter`:
  - official Anthropic Java SDK (`com.anthropic:anthropic-java`);
  - model `ANTHROPIC_MODEL` (default `claude-haiku-4-5`);
  - one call per generation for all suggestions;
  - structured output mapped to `record Reasons(List<String> reasons)`;
  - `max_tokens` 1024, 10 s timeout, 1 SDK retry.
- The prompt contains only dates, holiday names and day counts; never personal data. Each reason ≤ 300 characters (trimmed), English, rendered as plain text.
- **Template** (also the per-suggestion fallback when the call fails, is refused or truncated, or returns a different number of reasons): `"Take {k} day(s) off around {holiday} ({dd/MM}) for {blockLength} consecutive days off."`
- Cost: API usage is billed per token through the Claude Console, separately from Claude subscriptions. Without a key nothing is called or charged.

## Acceptance and dismissal
- `POST /suggestions/{id}/acceptance` → 201 VacationDto. Calls `VacationService.create(...)` with the window (BR-4..10, initial status per BR-12; `days` recomputed by BR-5) and sets the suggestion ACCEPTED in the same transaction. If validation fails: 422 with the rule's code; the suggestion stays SUGGESTED.
- `POST /suggestions/{id}/dismissal` → 200 SuggestionDto, status DISMISSED.
- Only SUGGESTED rows can be accepted or dismissed (`INVALID_STATUS_TRANSITION`). Another user's suggestion → 404.

## Tests
- Unit:
  - engine with fixed calendars: holiday on Tuesday → take Monday; on Thursday → take Friday; no holiday → no suggestion;
  - balance limit, at most 5, existing vacations block candidates, dismissed windows skipped, window never crosses 31/12;
  - template text; Claude writer falls back when the reason count doesn't match.
- IT (AI client faked):
  - generation, acceptance and dismissal flows;
  - regeneration keeps DISMISSED/ACCEPTED rows;
  - `NO_ENTITLEMENT`, year out of range → 400, `INVALID_STATUS_TRANSITION`;
  - acceptance 422 leaves the suggestion SUGGESTED;
  - USER → 404 on another user's suggestion.
