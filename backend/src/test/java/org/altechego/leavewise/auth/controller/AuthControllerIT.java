package org.altechego.leavewise.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private LwUserRepository lwUserRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long companyId;

    private long locationId;


    @BeforeEach
    void loadSeededCompany() {
        companyId = companyRepository.findByName("Test Company").orElseThrow().getId();
        locationId = locationRepository.findByCompanyIdAndName(companyId, "Test Office").orElseThrow().getId();
    }

    @Test
    void register_validRequest_shouldReturn201WithTokenPair() throws Exception {
        var email = uniqueEmail();

        register(email.toUpperCase(), null, companyId, locationId)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.companyId").value(companyId))
                .andExpect(jsonPath("$.user.companyLocationId").value(locationId));
    }

    // An unknown path behind authentication is 404; without a valid token it would be 401
    @Test
    void register_returnedAccessToken_shouldAuthenticatePrivatePaths() throws Exception {
        var pair = registerUser(uniqueEmail());

        mockMvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(pair)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_invalidFields_shouldReturn400WithFieldErrors() throws Exception {
        var body = """
                {"name":"Ana","email":"not-an-email","password":"short","nif":"123456780",
                 "companyId":%d,"companyLocationId":%d}""".formatted(companyId, locationId);

        postJson("/api/auth/registration", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "password", "nif")));
    }

    @Test
    void register_emailTakenInAnotherCase_shouldReturn409() throws Exception {
        var email = uniqueEmail();
        registerUser(email);

        register(email.toUpperCase(), null, companyId, locationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void register_nifTaken_shouldReturn409() throws Exception {
        register(uniqueEmail(), "123456789", companyId, locationId).andExpect(status().isCreated());

        register(uniqueEmail(), "123456789", companyId, locationId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NIF_TAKEN"));
    }

    @Test
    void register_unknownCompany_shouldReturn404() throws Exception {
        register(uniqueEmail(), null, Long.MAX_VALUE, locationId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void register_locationOutsideCompany_shouldReturn422() throws Exception {
        register(uniqueEmail(), null, companyId, Long.MAX_VALUE)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("LOCATION_COMPANY_MISMATCH"));
    }

    @Test
    void login_validCredentials_shouldReturn200WithTokenPair() throws Exception {
        var email = uniqueEmail();
        registerUser(email);

        login(" " + email.toUpperCase() + " ", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void login_wrongPassword_shouldReturn401() throws Exception {
        var email = uniqueEmail();
        registerUser(email);

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_unknownEmail_shouldReturn401() throws Exception {
        login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_inactiveUser_shouldReturn403() throws Exception {
        var email = uniqueEmail();
        registerUser(email);
        deactivate(email);

        login(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void refresh_activeToken_shouldRotateIt() throws Exception {
        var oldToken = refreshToken(registerUser(uniqueEmail()));

        var newPair = refresh(oldToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(refreshToken(newPair)).isNotEqualTo(oldToken);
    }

    @Test
    void refresh_rotatedTokenWithinGraceWindow_shouldReturn401AndKeepNewToken() throws Exception {
        var oldToken = refreshToken(registerUser(uniqueEmail()));
        var newToken = refreshToken(refresh(oldToken).andReturn().getResponse().getContentAsString());

        refresh(oldToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        refresh(newToken).andExpect(status().isOk());
    }

    // The revoke-all must commit even though the request ends in 401
    @Test
    void refresh_rotatedTokenAfterGraceWindow_shouldRevokeAllUserTokens() throws Exception {
        var email = uniqueEmail();
        var oldToken = refreshToken(registerUser(email));
        var newToken = refreshToken(refresh(oldToken).andReturn().getResponse().getContentAsString());
        moveRevocationsBeyondGraceWindow(email);

        refresh(oldToken).andExpect(status().isUnauthorized());

        refresh(newToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_unknownToken_shouldReturn401() throws Exception {
        refresh("unknown-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_inactiveUser_shouldReturn403() throws Exception {
        var email = uniqueEmail();
        var token = refreshToken(registerUser(email));
        deactivate(email);

        refresh(token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void logout_activeToken_shouldReturn204AndRevokeIt() throws Exception {
        var token = refreshToken(registerUser(uniqueEmail()));

        logout(token).andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_unknownOrRevokedToken_shouldStillReturn204() throws Exception {
        var token = refreshToken(registerUser(uniqueEmail()));
        logout(token);

        logout(token).andExpect(status().isNoContent());
        logout("unknown-token").andExpect(status().isNoContent());
    }


    private ResultActions register(String email, String nif, long company, long location) throws Exception {
        var body = """
                {"name":"Ana Silva","email":"%s","password":"%s",%s"companyId":%d,"companyLocationId":%d}"""
                .formatted(email, PASSWORD, nif == null ? "" : "\"nif\":\"" + nif + "\",", company, location);
        return postJson("/api/auth/registration", body);
    }

    private String registerUser(String email) throws Exception {
        return register(email, null, companyId, locationId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions login(String email, String password) throws Exception {
        return postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return postJson("/api/auth/refresh", refreshTokenBody(refreshToken));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return postJson("/api/auth/logout", refreshTokenBody(refreshToken));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(VERSIONED_JSON)
                .accept(VERSIONED_JSON)
                .content(body));
    }

    private void deactivate(String email) {
        var user = lwUserRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        lwUserRepository.save(user);
    }

    private void moveRevocationsBeyondGraceWindow(String email) {
        jdbcTemplate.update("""
                update refresh_token set revoked_at = revoked_at - interval '1 minute'
                where revoked_at is not null and user_id = (select id from lw_user where email = ?)""", email);
    }

    private static String refreshTokenBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}""".formatted(refreshToken);
    }

    private static String accessToken(String pairJson) {
        return JsonPath.read(pairJson, "$.accessToken");
    }

    private static String refreshToken(String pairJson) {
        return JsonPath.read(pairJson, "$.refreshToken");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.pt";
    }
}
