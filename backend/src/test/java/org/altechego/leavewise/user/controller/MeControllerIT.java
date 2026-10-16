package org.altechego.leavewise.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.config.JwtConfig;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MeControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String ME = "/api/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    private long companyId;

    private long locationId;


    @BeforeEach
    void loadSeededCompany() {
        companyId = companyRepository.findByName("Test Company").orElseThrow().getId();
        locationId = locationRepository.findByCompanyIdAndName(companyId, "Test Office").orElseThrow().getId();
    }

    @Test
    void findMe_registeredUser_shouldReturnOwnProfileWithCompanyAndLocation() throws Exception {
        var email = uniqueEmail();
        var accessToken = registerAndGetAccessToken(email);

        getMe("Bearer " + accessToken)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.nif").doesNotExist())
                .andExpect(jsonPath("$.company.id").value(companyId))
                .andExpect(jsonPath("$.company.name").value("Test Company"))
                .andExpect(jsonPath("$.companyLocation.id").value(locationId))
                .andExpect(jsonPath("$.companyLocation.name").value("Test Office"));
    }

    // The id comes from the token, so each caller only ever sees their own profile
    @Test
    void findMe_twoUsers_shouldEachSeeOnlyTheirOwnProfile() throws Exception {
        var firstEmail = uniqueEmail();
        var secondEmail = uniqueEmail();
        var firstToken = registerAndGetAccessToken(firstEmail);
        var secondToken = registerAndGetAccessToken(secondEmail);

        getMe("Bearer " + firstToken).andExpect(jsonPath("$.email").value(firstEmail));
        getMe("Bearer " + secondToken).andExpect(jsonPath("$.email").value(secondEmail));
    }

    @Test
    void findMe_seededAdmin_shouldReportAdminRole() throws Exception {
        var pair = postJson("/api/auth/login", """
                {"email":"admin@leavewise.test","password":"test-password"}""")
                .andReturn().getResponse().getContentAsString();

        getMe("Bearer " + JsonPath.read(pair, "$.accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void findMe_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(ME).accept(VERSIONED_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // A validly signed token can outlive its user by up to the access TTL
    @Test
    void findMe_tokenForDeletedUser_shouldReturn404() throws Exception {
        getMe(bearerFor(Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }


    private String registerAndGetAccessToken(String email) throws Exception {
        var body = """
                {"name":"Ana Silva","email":"%s","password":"secret-password","companyId":%d,"companyLocationId":%d}"""
                .formatted(email, companyId, locationId);
        var pair = postJson("/api/auth/registration", body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(pair, "$.accessToken");
    }

    private ResultActions getMe(String authorization) throws Exception {
        return mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, authorization).accept(VERSIONED_JSON));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(VERSIONED_JSON).accept(VERSIONED_JSON).content(body));
    }

    private String bearerFor(long userId) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .claim(JwtConfig.ROLE_CLAIM, "USER")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .build();
        var header = JwsHeader.with(JwtConfig.SIGNING_ALGORITHM).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String uniqueEmail() {
        return "me-" + UUID.randomUUID() + "@example.pt";
    }
}
