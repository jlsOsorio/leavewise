---
name: spring-boot-best-practices
description: LeaveWise Spring Boot 4 backend setup and how-to - JPA entities with Lombok, MapStruct mappers, media-type API versioning (WebConfig), SecurityConfig path beans, ProblemDetail GlobalExceptionHandler with ErrorCode, AOP aspects, N+1-safe queries and test setup. Use whenever creating, scaffolding, modifying or reviewing anything in backend/ (controllers, services, repositories, entities, DTOs, mappers, exceptions, security, config, aspects, endpoints or whole features), even if best practices are not mentioned. Takes precedence over modern-best-practice-java for Spring beans.
---

# Spring Boot Best Practices

Goal: every feature looks the same, so anyone can find, change and test a feature without touching others.

What to do is owned elsewhere and always applies: JPA/entity conventions, REST format and error codes in `SPEC.md` §5; per-file conventions (layout, layer flow, services, DTOs, REST, exceptions, persistence, security, test naming) in `.claude/rules/spring-boot.md` and `.claude/rules/java-style.md`. This skill covers setup and the *why*. For ready-to-adapt code read `references/examples.md` when scaffolding.

## JPA entities & Lombok (why)

- Why not `@Data`/`@EqualsAndHashCode` on entities: the generated `equals`/`hashCode`/`toString` load lazy associations, loop forever on bidirectional relations (`StackOverflowError`), and change the hash when the id is assigned on persist, so entities get lost in `HashSet`s. Hence id-based `equals` and a class-constant `hashCode` (`SPEC.md` §5)
- Why not business-key `equals` (email, name, ...): those keys are mutable here (an ADMIN renames a company, a user changes email), some are nullable (`nif`) or span lazy associations (`company_location` = company + name). Two unsaved entities are never "the same row"; to compare candidates before insert (dedupe, find-or-create), compare the key values explicitly or query with `existsBy...`/`findBy...`
- Why JPA callbacks instead of auditing annotations: Hibernate's `@UpdateTimestamp` and Spring Data's `@LastModifiedDate` both set `updated_at` on insert, but it must stay `NULL` until the first update. Spring Data auditing (`@EnableJpaAuditing` + `AuditingEntityListener`) only adds value with `@CreatedBy`/`@LastModifiedBy`, which the schema doesn't have
- Why explicit `@Table`/`@Column`/`@JoinColumn`: the entity reads as the schema (names, nullability, length, uniqueness) without opening the migration, and survives renames of Java fields. `ddl-auto=validate` checks only that tables and columns exist with compatible types, not nullability or length; Bean Validation on the request DTO (same `XConstants` lengths) and the DB constraints are what enforce them
- `@Data` is fine on plain mutable classes that aren't entities
- Proxied beans can't be `final` because CGLIB must subclass them
- Keep `lombok-mapstruct-binding` in the annotation processor path so MapStruct sees Lombok's generated accessors and builders

## MapStruct

- Typical methods: `toResponse(entity)`, `toEntity(request)`, and `void update(XRequest request, @MappingTarget X entity)` for updates
- Mappers hold no logic; anything computed belongs in the service
- Why DTOs only: entities leaking out of the service cause lazy-loading errors and accidental API changes when the schema changes

## API versioning (media type)

- Value and header rules: `SPEC.md` §5. `common/web/ApiMediaType` holds the constants
- `WebConfig implements WebMvcConfigurer` configures it in one place:
  - `configureApiVersioning`: `useMediaTypeParameter(ApiMediaType.VND, "version")`, `addSupportedVersions(...)` and `setDefaultVersion("1")`
  - The default version is what lets SSE and xlsx requests (no vnd media type) match mappings
  - Supported versions live in that list, not in controllers
- Jackson's converter already handles `application/*+json`, so the vnd type needs no extra converter
- Use `"1.1+"` (baseline) when an endpoint stays unchanged from a version onward; add a new major version only for breaking changes

## Pagination

- `common/web/PageResponse<T>(content, page, size, totalElements, totalPages)` with `static from(Page<T>)`, so the JSON shape is ours (`SPEC.md` §5), not Spring's `PageImpl` serialization
- Bound `size` via `spring.data.web.pageable.max-page-size=100` and `default-page-size=20`
- Why `@SortDefault` and not `@PageableDefault`: the latter's own `size = 10` silently overrides `default-page-size`
- Why restrict `sort` (`SortRestriction`): otherwise clients can order by fields the response hides (e.g. `passwordHash`, `nif`) and infer their values from the ordering
- Safety net for an endpoint that skips the restriction: an unknown property throws `PropertyReferenceException` (package `org.springframework.data.core` in Spring Data 4), which `GlobalExceptionHandler` turns into the same 400

## Security config

- Path groups are `@Bean`s in `SecurityConfig`: `publicPaths` (the **P** endpoints in `docs/spec/api.md` + `/actuator/health`) and `privatePaths` (`/api/**`). The `SecurityFilterChain` injects them, so access rules are declared once and are testable
- Deny by default: `publicPaths` → `permitAll`, `privatePaths` → `authenticated`, `anyRequest().denyAll()`
- ADMIN-only endpoints use `@PreAuthorize("hasRole('ADMIN')")` (there is no `/admin` path prefix); `@EnableMethodSecurity` on `SecurityConfig`
- Stateless session, CSRF disabled: only the Next.js BFF calls the backend, with a bearer token and no cookies (`docs/spec/security.md`)
- OAuth2 Resource Server with `common/security/AuthenticatedUserConverter`: it maps the `role` claim to `ROLE_<role>` and makes the principal an `AuthenticatedUser(id, email, role, companyId)` instead of the raw `Jwt`, so controllers get typed claims and no feature parses the token. It lives in `common`, not `auth`, because every feature injects the principal and `auth` already depends on `user`
- `dispatcherTypeMatchers(ASYNC, ERROR).permitAll()`: authorization runs on every dispatch, so without it SSE emitters and error dispatches fail with AccessDenied after the response is committed
- Passwords: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt)

## Exception handling

- One `GlobalExceptionHandler` (`@RestControllerAdvice extends ResponseEntityExceptionHandler`) returns `ProblemDetail` (RFC 9457) for every error, so clients get a single error shape
- Every problem carries the `code` property from `ErrorCode`; validation adds `errors: [{field, message}]` (`SPEC.md` §5)
- Handle the `ApiException` base type once; security errors (401/403) use the same shape via the `AuthenticationEntryPoint` and `AccessDeniedHandler`
- Log at `error` only for unexpected 5xx errors (`INTERNAL_ERROR`), and never expose stack traces or internal messages

## AOP

- Use an aspect only for behaviour that repeats across layers or features: method entry/exit and timing logs, slow-call warnings, correlation IDs
- Prefer annotation-based pointcuts (`@annotation(LogExecution)`) or narrow package pointcuts over broad `execution(* *(..))` patterns
- Aspects contain no business logic and never swallow exceptions. They only observe

## Persistence

- N+1 at query level: use `JOIN FETCH`, `@EntityGraph` or a DTO projection when a use case needs the association
- N+1 at logic level: batch-load (`findAllById(ids)`, `findByUserIdIn(ids)`), index with `Collectors.toMap(X::getId, identity())`, then look up from the map

## Testing

- `XServiceImplTest`: `@ExtendWith(MockitoExtension.class)`, mocking repositories, mappers and `Clock`
- `XControllerTest`: assert status, `ProblemDetail` `code` and the versioned media type
- `*IT`: Testcontainers (PostgreSQL, RabbitMQ) for repositories, security rules and full flows (`SPEC.md` §6)
