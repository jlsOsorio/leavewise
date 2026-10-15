package org.altechego.leavewise.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, SecurityConfigIT.ProbeController.class})
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String PROBE_ADMIN = "/api/test-probe/admin";
    private static final String PROBE_ECHO = "/api/test-probe/echo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;


    @Test
    void health_unauthenticated_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // An empty body fails Bean Validation: a 400 proves security let the request reach the controller
    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/registration", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout"})
    void publicAuthPath_unauthenticated_shouldPassSecurity(String path) throws Exception {
        mockMvc.perform(post(path).contentType(VERSIONED_JSON).accept(VERSIONED_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void privatePath_unauthenticated_shouldReturn401Problem() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void privatePath_malformedToken_shouldReturn401Problem() throws Exception {
        mockMvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void privatePath_expiredToken_shouldReturn401Problem() throws Exception {
        var issuedAt = Instant.now().minus(1, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/anything")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER", issuedAt, issuedAt.plusSeconds(60))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownPrivatePath_authenticated_shouldReturn404Problem() throws Exception {
        mockMvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, validBearer("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void adminEndpoint_userRole_shouldReturn403Problem() throws Exception {
        mockMvc.perform(get(PROBE_ADMIN).accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, validBearer("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminEndpoint_adminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get(PROBE_ADMIN).accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, validBearer("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void versionedEndpoint_validBody_shouldRespondWithVendorMediaType() throws Exception {
        mockMvc.perform(echo(VERSIONED_JSON, "{\"value\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.value").value("hello"));
    }

    @Test
    void versionedEndpoint_invalidBody_shouldReturnFieldErrors() throws Exception {
        mockMvc.perform(echo(VERSIONED_JSON, "{\"value\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("value"));
    }

    @Test
    void versionedEndpoint_malformedBody_shouldReturnValidationError() throws Exception {
        mockMvc.perform(echo(VERSIONED_JSON, "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void versionedEndpoint_unsupportedVersion_shouldReturn400() throws Exception {
        mockMvc.perform(echo(ApiMediaType.JSON + ";version=9", "{\"value\":\"hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_API_VERSION"));
    }

    @Test
    void versionedEndpoint_wrongMethod_shouldReturn405() throws Exception {
        mockMvc.perform(get(PROBE_ECHO).accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, validBearer("USER")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void versionedEndpoint_unsupportedAccept_shouldReturn406() throws Exception {
        mockMvc.perform(post(PROBE_ECHO).contentType(VERSIONED_JSON).accept(MediaType.APPLICATION_XML)
                        .header(HttpHeaders.AUTHORIZATION, validBearer("USER"))
                        .content("{\"value\":\"hello\"}"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void versionedEndpoint_unsupportedContentType_shouldReturn415() throws Exception {
        mockMvc.perform(post(PROBE_ECHO).contentType(MediaType.TEXT_PLAIN).accept(VERSIONED_JSON)
                        .header(HttpHeaders.AUTHORIZATION, validBearer("USER"))
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/anything", PROBE_ADMIN})
    void problem_anyError_shouldCarryTimestamp(String path) throws Exception {
        mockMvc.perform(get(path).accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, validBearer("USER")))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void problem_unauthenticated_shouldCarryTimestamp() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void unlistedPath_unauthenticated_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    // Spring's default logout filter would redirect to /login?logout; it's disabled, so authorization denies it
    @Test
    void defaultLogoutPath_unauthenticated_shouldReturn401ProblemNotRedirect() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void basicCredentials_privatePath_shouldNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46cGFzc3dvcmQ="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }


    private RequestBuilder echo(String mediaType, String body) {
        return post(PROBE_ECHO).contentType(mediaType).accept(mediaType)
                .header(HttpHeaders.AUTHORIZATION, validBearer("USER"))
                .content(body);
    }

    private String validBearer(String role) {
        var now = Instant.now();
        return bearer(role, now, now.plus(15, ChronoUnit.MINUTES));
    }

    private String bearer(String role, Instant issuedAt, Instant expiresAt) {
        var claims = JwtClaimsSet.builder()
                .subject("1")
                .claim(JwtConfig.ROLE_CLAIM, role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        var header = JwsHeader.with(JwtConfig.SIGNING_ALGORITHM).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }


    // Nested in the test class, so component scanning skips it and only this context (via @Import) has it
    @RestController
    @RequestMapping("/api/test-probe")
    static class ProbeController {

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping(path = "/admin", version = "1", produces = ApiMediaType.JSON)
        EchoBody admin() {
            return new EchoBody("admin");
        }

        @PostMapping(path = "/echo", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
        EchoBody echo(@Valid @RequestBody EchoBody body) {
            return body;
        }
    }


    record EchoBody(@NotBlank String value) {}
}
