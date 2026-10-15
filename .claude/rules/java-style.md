---
paths:
  - "backend/**/*.java"
---

# Java Style (baseline)

Always applies to backend Java. For modern-Java features and design guidance use the `modern-best-practice-java` skill; for Spring beans and entities, `spring-boot-best-practices` wins.

## Naming

Names make the code self-explanatory; a reader never needs a comment to know what something is or does.

- **Classes**: singular nouns naming the concept (`VacationRequest`, `WorkingDayCalculator`), never `Manager`, `Helper`, `Utils`, `Data`
- **Methods**: verbs or verb phrases (`calculateBalance`, `findActiveEntitlement`, `rejectOverlappingRequest`)
- **Fields and variables**: describe the state and intent (`remainingDays`, `pendingRequests`), never `n`, `tmp`, `list`, `obj`
- **Booleans** read as predicates: `isPending`, `hasOverlap`, `canBeCancelled`

## Comments

- **DO NOT** restate what the code does - rename or extract a well-named method instead
- Comment only the _why_ of non-obvious logic (business rule, workaround, subtle edge case), or `TODO:` / `FIXME:` with the reason
- **AVOID** Javadoc that repeats the signature; reserve it for public APIs consumed outside the module

## Lombok

- Inject dependencies as `private final` fields + `@RequiredArgsConstructor`; no hand-written DI constructors
- Use `@Slf4j`, `@Getter`, `@Builder`, `@UtilityClass` to remove boilerplate
- **PREFER** `record` over `@Value`/`@Data` for immutable data carriers
- JPA entities and Spring beans: follow `spring-boot.md` (rule) and `spring-boot-best-practices` (skill)

## Methods

- Private methods that use no instance state (fields, injected beans), only their arguments and constants, are `static`; once a static helper is needed elsewhere, move it to a `@UtilityClass`

## Logging & Errors

- **DO NOT** use `System.out`, `System.err` or `printStackTrace()`; use SLF4J via `@Slf4j`
- Parameterised messages with the identifiers needed to troubleshoot: `log.info("Imported {} rows for user {}", rowCount, userId)`
- Levels: `error` needs action, `warn` unexpected but handled, `info` business event, `debug` diagnostic detail
- Throw specific exceptions with a contextual message; pass the cause when wrapping
- **DO NOT** swallow exceptions; log **or** rethrow, never both

## No Hardcoding

- Magic numbers/strings become `static final` constants or `enum`s named for their meaning; `Duration`/`Period` for time
- Environment-dependent values (URLs, credentials, limits, timeouts, flags) come from configuration bound to a record
- Inject a `Clock`; never call `LocalDate.now()` / `Instant.now()` directly. Only exception: the audit timestamps in `AuditableEntity` (entities can't take beans)

## Test Naming

- `<method_name>_<scenario>_<expected_behavior>`, e.g. `countWorkingDays_rangeIncludesHoliday_shouldExcludeHoliday`
- One reason to fail per test
- JUnit 5 runs the tests (`@Test`, `@ParameterizedTest`, `@BeforeEach`, `@Nested`, ...); assertions are AssertJ only (`assertThat`, `assertThatThrownBy`, `SoftAssertions`), never `org.junit.jupiter.api.Assertions`

## Formatting

Standard Java style plus these blank-line rules inside a type body:

- One blank line after the type declaration, before the first member
- **Two** blank lines between sections, ordered: static initialiser → fields → constructors → methods → nested types
- One blank line between members of the same section
- One blank line after a method/constructor signature that wraps over several lines
- Inside a block, one blank line between distinct steps; keep statements of one step together

```java
public final class BalanceCalculator {

    private static final int NO_DAYS = 0;


    public int calculateAvailableDays(
            VacationEntitlement entitlement,
            List<Vacation> countedVacations,
            LocalDate newVacationEndDate) {

        var usedDays = countedVacations.stream().mapToInt(Vacation::getDays).sum();

        if (entitlement.carriedOverAppliesTo(newVacationEndDate)) {
            return entitlement.getAnnualDays() + entitlement.getCarriedOverDays() - usedDays;
        }
        return entitlement.getAnnualDays() - (usedDays - carriedConsumed(entitlement, countedVacations));
    }
}
```
