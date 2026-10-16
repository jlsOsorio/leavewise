# Code templates

Adapt names to the feature. Imports omitted. Contents:

1. [Media type, WebConfig & PageResponse](#1-media-type-webconfig--pageresponse)
2. [SecurityConfig with path beans](#2-securityconfig-with-path-beans)
3. [ErrorCode, exceptions & GlobalExceptionHandler](#3-errorcode-exceptions--globalexceptionhandler)
4. [AuditableEntity](#4-auditableentity)
5. [Feature slice](#5-feature-slice-vacation)
6. [Logging aspect](#6-logging-aspect)

## 1. Media type, WebConfig & PageResponse

```java
@UtilityClass
public class ApiMediaType {

    public static final String JSON = "application/vnd.leavewise+json";
    public static final MediaType VND = MediaType.parseMediaType(JSON);
    public static final String VERSION_PARAM = "version";
    public static final String DEFAULT_VERSION = "1";
}

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] SUPPORTED_VERSIONS = {"1"};


    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useMediaTypeParameter(ApiMediaType.VND, ApiMediaType.VERSION_PARAM)
                .addSupportedVersions(SUPPORTED_VERSIONS)
                .setDefaultVersion(ApiMediaType.DEFAULT_VERSION);
    }
}

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
```

## 2. SecurityConfig with path beans

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public String[] publicPaths() {
        return new String[] {
                "/api/auth/registration", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                "/api/auth/companies", "/api/auth/companies/*/locations", "/actuator/health"};
    }

    @Bean
    public String[] privatePaths() {
        return new String[] {"/api/**"};
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("publicPaths") String[] publicPaths,
            @Qualifier("privatePaths") String[] privatePaths,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter, // common/security/AuthenticatedUserConverter
            ProblemAuthenticationEntryPoint entryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable) // bearer only, no cookies reach the backend
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(publicPaths).permitAll()
                        .requestMatchers(privatePaths).authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)         // 401 UNAUTHENTICATED
                        .accessDeniedHandler(accessDeniedHandler))    // 403 FORBIDDEN
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
// ProblemAuthenticationEntryPoint / ProblemAccessDeniedHandler write the same ProblemDetail
// (application/problem+json, `code`) as GlobalExceptionHandler.

// ADMIN-only endpoints: role on the controller method
@PreAuthorize("hasRole('ADMIN')")
@PostMapping(path = "/{id}/approval", version = "1", produces = ApiMediaType.JSON)
public VacationResponse approve(@PathVariable Long id) { ... }
```

## 3. ErrorCode, exceptions & GlobalExceptionHandler

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    VACATION_OVERLAP(HttpStatus.UNPROCESSABLE_CONTENT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);
    // ... one entry per code in SPEC.md §5

    private final HttpStatus status;
}

@Getter
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;


    protected ApiException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}

public class VacationNotFoundException extends ApiException {

    public VacationNotFoundException(Long id) {
        super(ErrorCode.NOT_FOUND, "Vacation %d not found".formatted(id));
    }
}

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String ERRORS = "errors";


    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return problem(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(ErrorCode.INTERNAL_ERROR, "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        var problem = problem(ErrorCode.VALIDATION_ERROR, "Validation failed");
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        problem.setProperty(ERRORS, errors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }


    private static ProblemDetail problem(ErrorCode code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        problem.setProperty(CODE, code.name());
        return problem;
    }


    public record FieldErrorResponse(String field, String message) {}
}
```

## 4. AuditableEntity

Audit timestamps per `SPEC.md` §5 (UTC `LocalDateTime`, JPA callbacks, no Spring Data auditing). Only for tables that have both columns; others declare `createdAt` themselves. `updatedAt` stays `NULL` until the first update.

```java
@Getter
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
```

## 5. Feature slice (vacation)

```java
// entity/Vacation.java
@Entity
@Table(name = "vacation")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Vacation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private LwUser user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days", nullable = false)
    private int days;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = VacationConstants.STATUS_MAX_LENGTH)
    @Builder.Default
    private VacationStatus status = VacationStatus.PENDING;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Vacation vacation && id != null && id.equals(vacation.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

// enums/VacationStatus.java
public enum VacationStatus { PENDING, ACCEPTED, REJECTED, CANCELLED }

// constants/VacationConstants.java
@UtilityClass
public class VacationConstants {

    public static final String BASE_PATH = "/api/vacations";
    public static final int STATUS_MAX_LENGTH = 30;
}

// dto/VacationRequest.java, dto/VacationResponse.java
// No @FutureOrPresent: BR-10 depends on the actor, so it runs in VacationRules
public record VacationRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, Long userId) {}
public record VacationResponse(Long id, Long userId, LocalDate startDate, LocalDate endDate, int days, VacationStatus status) {}

// repository/VacationRepository.java
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    Page<Vacation> findByUserId(Long userId, Pageable pageable);
}

// mapper/VacationMapper.java
@Mapper(componentModel = "spring")
public interface VacationMapper {

    @Mapping(target = "userId", source = "user.id")
    VacationResponse toResponse(Vacation vacation);
}

// service/VacationService.java
public interface VacationService {

    VacationResponse create(VacationRequest request, AuthenticatedUser actor);

    PageResponse<VacationResponse> findOwn(AuthenticatedUser actor, Pageable pageable);
}

// service/impl/VacationServiceImpl.java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacationServiceImpl implements VacationService {

    private final VacationRepository vacationRepository;
    private final UserRepository userRepository; // read-only cross-feature access
    private final VacationRules vacationRules;
    private final VacationMapper vacationMapper;


    @Override
    @Transactional
    public VacationResponse create(VacationRequest request, AuthenticatedUser actor) {
        var targetUserId = actor.isAdmin() && request.userId() != null ? request.userId() : actor.id();
        var user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        var check = vacationRules.validate(user, request.startDate(), request.endDate(), actor, null); // BR-4..10

        var vacation = vacationRepository.save(Vacation.builder()
                .user(user)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .days(check.days())
                .status(VacationStatus.initialFor(actor, user)) // BR-12
                .build());

        log.info("Created vacation {} for user {}", vacation.getId(), user.getId());
        return vacationMapper.toResponse(vacation);
    }

    @Override
    public PageResponse<VacationResponse> findOwn(AuthenticatedUser actor, Pageable pageable) {
        return PageResponse.from(vacationRepository.findByUserId(actor.id(), pageable).map(vacationMapper::toResponse));
    }
}

// controller/VacationController.java
@RestController
@RequestMapping(VacationConstants.BASE_PATH)
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;


    @PostMapping(version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<VacationResponse> create(
            @Valid @RequestBody VacationRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        var created = vacationService.create(request, actor);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<VacationResponse>> findOwn(
            @AuthenticationPrincipal AuthenticatedUser actor,
            Pageable pageable) {

        return ResponseEntity.ok(vacationService.findOwn(actor, pageable));
    }
}
```

N+1 at the logic level, batch then map:

```java
var usersById = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

return rows.stream()
        .map(row -> buildVacation(row, usersById.get(row.userId())))
        .toList();
```

## 6. Logging aspect

Needs `spring-boot-starter-aspectj` (Boot 4 name of the former `spring-boot-starter-aop`).

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {}

@Slf4j
@Aspect
@Component
public class ExecutionLoggingAspect {

    @Around("@annotation(org.altechego.leavewise.common.aop.LogExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        var method = joinPoint.getSignature().toShortString();
        var start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            log.debug("{} took {} ms", method, Duration.ofNanos(System.nanoTime() - start).toMillis());
        }
    }
}
```
