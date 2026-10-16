---
paths:
  - "backend/**"
---

# Spring Boot Conventions (baseline)

Always applies to backend code, on top of `java-style.md`; for Spring beans this rule wins. JPA/entity conventions, REST format, pagination bounds and error codes are owned by `SPEC.md` §5. For setup (configs, entities, exception handler, AOP, versioning, security, tests) use the `spring-boot-best-practices` skill.

## Package layout (feature-based)

```
org.altechego.leavewise/
  common/
    config/       WebConfig, SecurityConfig, ClockConfig, *Properties records
    exception/    ErrorCode, ApiException, GlobalExceptionHandler
    security/     AuthenticatedUser (principal), Role, AuthenticatedUserConverter
    entity/       AuditableEntity (audit timestamps: created_at / updated_at)
    aop/          cross-cutting aspects
    web/          ApiMediaType, PageResponse
    messaging/    AMQP config and shared messaging types
  <feature>/      e.g. vacation/
    controller/  service/ (VacationService) + service/impl/ (VacationServiceImpl)
    repository/  entity/  dto/  mapper/  constants/  enums/  exception/  event/
```

- Features: `SPEC.md` §3. Create only the sub-packages a feature needs; move code to `common/` only once it is genuinely shared

## Layer flow

- **controller → service → repository**. Controllers handle HTTP only (binding, `@Valid`, status, headers); never touch a repository or entity
- Cross-feature calls:
  - Writes or operations that need a result: inject the other feature's **service interface** (e.g. import and suggestion call `VacationService` so `VacationRules` runs once). Never write through another feature's repository
  - Reads: the other feature's service or repository, read-only
  - Fire-and-forget side effects: publish an event, the other feature listens
  - No cyclic dependencies between features
- Business rules live in the service impl or plain domain classes it calls (calculators, `VacationRules`)

## Services & beans

- Each feature has `XService` + `service/impl/XServiceImpl`
- `@Transactional(readOnly = true)` on the impl class, `@Transactional` on write methods; never in controllers
- Return DTOs, never entities; throw a feature exception instead of returning `null`
- No field or setter `@Autowired`
- **DO NOT** make proxied beans `final` (`@Transactional`, `@Async`, `@Cacheable`, aspects)

## DTOs & mapping

- Request/response DTOs are `record`s in `dto/`: `XRequest`, `XResponse`, `XSummaryResponse`
- Bean Validation on request records + `@Valid` in the controller; cross-field and business rules in the service
- One MapStruct `@Mapper(componentModel = "spring")` per feature, field mapping only
- Entities never leave the service layer

## Constants, enums & configuration

- Feature constants: `<feature>/constants/XConstants` (`@UtilityClass`); cross-feature ones in `common/`
- Enums: `<feature>/enums/`
- Environment values: `@ConfigurationProperties` records in `common/config/*Properties`, not `@Value`

## RESTful API

- Paths: plural nouns, kebab-case, nested at most one level (`/api/vacations`, `/api/companies/{id}/locations`). **No verbs**: actions are noun sub-resources (`POST /api/vacations/{id}/approval`, `POST /api/holidays/generation`)
- JSON mappings declare the version and media type: `@GetMapping(version = "1", produces = ApiMediaType.JSON)` (plus `consumes` with a body). Multipart endpoints use `consumes = MULTIPART_FORM_DATA_VALUE` with the JSON `produces`; SSE and file-download endpoints declare their own `produces` and rely on the default version
- Handler methods return `ResponseEntity<T>` (`ok`, `created(location)`, `status(...)`, `noContent()`), never a bare body or `@ResponseStatus`, so status and headers are visible in one place
- Every collection endpoint takes `Pageable` (bounds in `SPEC.md` §5) and returns `PageResponse<T>`; never a `List` or Spring's `Page` directly
- Sortable fields: a `Set<String>` constant per endpoint, checked with `SortRestriction.requireAllowed(pageable, fields)` in the controller; default order via `@SortDefault`

| Operation                                        | Response                              |
| ------------------------------------------------ | ------------------------------------- |
| `GET`                                            | 200                                   |
| `POST` creating a resource                       | 201 + `Location`                      |
| `POST` action sub-resource (`/approval`, ...)    | 200 with the updated resource/result  |
| `POST` accepted for async processing             | 202                                   |
| `PUT`/`PATCH`                                    | 200, or 204 with no body              |
| `DELETE`                                         | 204                                   |
| Bean Validation / malformed input                | 400                                   |
| Unauthenticated / forbidden                      | 401 / 403                             |
| Not found (incl. another user's resource)        | 404                                   |
| Conflict                                         | 409                                   |
| Business rule violation                          | 422                                   |

Error codes per status: `SPEC.md` §5.

## Exceptions

- Codes live in `common/exception/ErrorCode` (enum: code + `HttpStatus`, one entry per `SPEC.md` §5 code)
- Feature exceptions extend `ApiException(ErrorCode, detail)` (`VacationNotFoundException` → `NOT_FOUND`, `VacationOverlapException` → `VACATION_OVERLAP`)
- **DO NOT** catch exceptions in controllers to build responses; `GlobalExceptionHandler` does it

## Persistence

- Flyway owns the schema: every entity change needs a migration (`SPEC.md` §0)
- **Never call a repository inside a loop or stream**: batch-load (`findAllById(ids)`), index into a map, look up from it
- Derived queries or `@Query` JPQL; never string-concatenated SQL

## Security

- **Never log** secrets, tokens or personal data (e.g. NIF)
- Paths in `SecurityConfig` beans (`publicPaths`, `privatePaths`); roles via `@PreAuthorize`; ownership ("own data only", 404 otherwise) in the service via the authenticated principal (`@AuthenticationPrincipal AuthenticatedUser actor` in the controller, passed to the service)

## Tests

- `XServiceImplTest`: Mockito unit test · `XControllerTest`: `@WebMvcTest` slice · `*IT`: `@SpringBootTest` + Testcontainers
