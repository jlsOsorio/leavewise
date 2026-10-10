---
name: modern-best-practice-java
description: Write clean, modern Java 25 code - records, sealed types, pattern matching, virtual threads, ScopedValue, SOLID, KISS/DRY, small focused classes, safe refactoring and testable design. Use whenever writing, refactoring or reviewing Java code (services, DTOs, domain logic, concurrency, tests), even if best practices are not explicitly mentioned.
---

# Modern Best Practice Java

Code is read far more often than it is written. Optimise for **maintainability, easy troubleshooting and fast onboarding**: each piece of code solves one specific problem, in the simplest design that works, and is easy to test automatically.

Baseline style (naming, comments, Lombok, logging & errors, no hardcoding, test naming, formatting) lives in `.claude/rules/java-style.md` and always applies. This skill adds the modern-Java and design guidance.

## Modern Java (25)

- **PREFER** `record` for immutable data carriers (DTOs, value objects, events, config, query results). Validate and normalise in the compact constructor
- **DO NOT** use records for classes that must be mutable or proxied by a framework (e.g. JPA entities)
- **PREFER** `sealed` interfaces + records for closed hierarchies, handled with `switch` pattern matching and record patterns, instead of `instanceof` chains, visitors or type-code fields. Exhaustive switches mean the compiler flags every missing case
- Use unnamed variables `_` for values you ignore (`catch (NumberFormatException _)`, unused lambda params)
- Use `var` only when the type is obvious from the right-hand side; text blocks, `List.of`/`Map.of` and sequenced-collection methods (`getFirst()`, `reversed()`) where they fit
- Validate arguments before `super(...)`/`this(...)` (flexible constructor bodies) instead of static helper tricks
- **PREFER** `ScopedValue` over `ThreadLocal` for passing request context (immutable, bounded lifetime, cheap with virtual threads)
- `Optional` is for return values only - never fields, parameters or collection elements
- **AVOID** preview features (structured concurrency, primitive patterns) unless the build already enables `--enable-preview`

## Concurrency

- **PREFER** virtual threads for blocking I/O: `try (var executor = Executors.newVirtualThreadPerTaskExecutor()) { ... }`
- **DO NOT** pool virtual threads or size thread pools for I/O work; limit concurrency on a scarce resource with a `Semaphore`
- Keep platform-thread pools (or parallel streams) for CPU-bound work only
- **PREFER** immutable data shared between threads over locks; when state must be shared, use `java.util.concurrent` types instead of hand-rolled synchronisation

## Design

- **Single responsibility**: one reason to change per class; small methods that each do one thing at one level of abstraction
- **KISS first, then DRY**: duplicate small snippets until a real shared business rule emerges. Extract an abstraction only when several features genuinely depend on the same logic - premature generic layers cost more readability than they save
- Introduce an interface only when there are multiple implementations or a boundary that needs swapping (external systems, tests). **Exception:** the project's `XService` + `XServiceImpl` convention (see `spring-boot.md` rule); framework-required interfaces (Spring Data repositories, MapStruct mappers) don't count either
- **PREFER** composition over inheritance (inject collaborators, see the Lombok rule). Keep plain classes `final` unless designed for extension (proxied Spring beans: see `spring-boot.md` rule)
- Keep methods to about **three parameters**. When more values travel together, group them in a record (parameter object) or use a builder, or split the method if it is doing more than one thing
- Return early instead of nesting conditionals; keep the happy path unindented
- Splitting into several small classes is fine as long as each one has a single, obvious job a newcomer can guess from its name. Avoid splits that only add indirection (pass-through classes, one-line wrappers)

## Refactoring

- Preserve existing behaviour (return values, error signalling, sync vs async, rounding) unless the user asks to change it - a refactor changes structure, not outcomes
- If you spot a behaviour worth changing (a bug, a swallowed error, a leaked executor), keep the original behaviour and list the suggestion separately for the user to decide

## Testability

- Keep business logic in plain classes free of I/O, so it can be unit tested without frameworks
- Make inputs explicit (constructor-injected collaborators, `Clock`, parameters) - no hidden static state

## Worked example

Applies the rule (naming, Lombok, logging, constants, `Clock`, formatting) and this skill (final plain class, early return, `Optional` as return only):

```java
@Slf4j
@RequiredArgsConstructor
public final class EntitlementCalculator {

    private static final int NO_DAYS = 0;
    private final EntitlementRepository entitlementRepository;
    private final Clock clock;


    public Entitlement grantEntitlement(
            UserId userId,
            Year year,
            int annualDays) {

        log.info("Granting {} days to user {} for {}", annualDays, userId, year);
        return entitlementRepository.save(new Entitlement(userId, year, annualDays));
    }

    public int calculateRemainingDays(UserId userId) {
        if (userId.isAnonymous()) {
            return NO_DAYS;
        }

        var currentYear = Year.now(clock);
        return entitlementRepository.findByUserAndYear(userId, currentYear)
                .map(Entitlement::remainingDays)
                .orElse(NO_DAYS);
    }
}
```
