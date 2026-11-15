package org.altechego.leavewise.company.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.common.security.TestAccessTokens;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.location.service.LocationService;
import org.altechego.leavewise.municipality.service.MunicipalityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CompanyControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String COMPANIES = "/api/companies";
    private static final String SEEDED_COMPANY = "Test Company";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private MunicipalityService municipalityService;

    private Long seededCompanyId;

    private String adminBearer;

    private String userBearer;


    @BeforeEach
    void createTokens() {
        seededCompanyId = companyRepository.findByName(SEEDED_COMPANY).orElseThrow().getId();
        adminBearer = TestAccessTokens.bearer(jwtEncoder, 1L, Role.ADMIN, seededCompanyId);
        userBearer = TestAccessTokens.bearer(jwtEncoder, 2L, Role.USER, seededCompanyId);
    }

    @Test
    void search_admin_shouldFindAnyCompanyByName() throws Exception {
        var existing = existingCompany();

        perform(get(COMPANIES).param("q", existing.name().toUpperCase()), adminBearer)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(existing.id()));
    }

    @Test
    void search_user_shouldReturn403() throws Exception {
        perform(get(COMPANIES), userBearer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void search_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(COMPANIES).accept(VERSIONED_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void search_sortByUnlistedField_shouldReturn400() throws Exception {
        perform(get(COMPANIES).param("sort", "createdAt"), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void search_queryTooLong_shouldReturn400() throws Exception {
        perform(get(COMPANIES).param("q", "a".repeat(201)), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    void findById_userOwnCompany_shouldReturnIt() throws Exception {
        perform(get(COMPANIES + "/" + seededCompanyId), userBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(SEEDED_COMPANY));
    }

    @Test
    void findById_userAnotherCompany_shouldReturn404() throws Exception {
        var other = existingCompany();

        perform(get(COMPANIES + "/" + other.id()), userBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findById_adminAnyCompany_shouldReturnIt() throws Exception {
        var other = existingCompany();

        perform(get(COMPANIES + "/" + other.id()), adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(other.name()));
    }

    @Test
    void findById_unknownId_shouldReturn404() throws Exception {
        perform(get(COMPANIES + "/" + Long.MAX_VALUE), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_admin_shouldReturn201WithLocationAndTrimmedName() throws Exception {
        var name = uniqueName();

        perform(post(COMPANIES).contentType(VERSIONED_JSON).content(body(" " + name + " ")), adminBearer)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesRegex(".*" + COMPANIES + "/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void create_user_shouldReturn403() throws Exception {
        perform(post(COMPANIES).contentType(VERSIONED_JSON).content(body(uniqueName())), userBearer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_blankName_shouldReturn400WithFieldError() throws Exception {
        perform(post(COMPANIES).contentType(VERSIONED_JSON).content(body("  ")), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void create_existingName_shouldReturn409() throws Exception {
        perform(post(COMPANIES).contentType(VERSIONED_JSON).content(body(SEEDED_COMPANY)), adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void update_admin_shouldReturnRenamedCompany() throws Exception {
        var existing = existingCompany();
        var newName = uniqueName();

        perform(put(COMPANIES + "/" + existing.id()).contentType(VERSIONED_JSON).content(body(newName)), adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.id()))
                .andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    void update_user_shouldReturn403() throws Exception {
        perform(put(COMPANIES + "/" + seededCompanyId).contentType(VERSIONED_JSON).content(body(uniqueName())),
                userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void update_unknownId_shouldReturn404() throws Exception {
        perform(put(COMPANIES + "/" + Long.MAX_VALUE).contentType(VERSIONED_JSON).content(body(uniqueName())),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_toAnotherCompanysName_shouldReturn409() throws Exception {
        var existing = existingCompany();

        perform(put(COMPANIES + "/" + existing.id()).contentType(VERSIONED_JSON).content(body(SEEDED_COMPANY)),
                adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void delete_companyWithoutUsers_shouldReturn204AndCascadeToLocations() throws Exception {
        var existing = existingCompany();
        var municipality = municipalityService.findOrCreate("Municipality " + UUID.randomUUID(), "Braga", "Portugal");
        var location = locationService.findOrCreate(existing.id(), municipality.id(), "Sede");

        perform(delete(COMPANIES + "/" + existing.id()), adminBearer)
                .andExpect(status().isNoContent());
        perform(get(COMPANIES + "/" + existing.id()), adminBearer)
                .andExpect(status().isNotFound());
        assertThat(locationRepository.existsById(location.id())).isFalse();
    }

    @Test
    void delete_companyWithUsers_shouldReturn409() throws Exception {
        perform(delete(COMPANIES + "/" + seededCompanyId), adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void delete_user_shouldReturn403() throws Exception {
        var existing = existingCompany();

        perform(delete(COMPANIES + "/" + existing.id()), userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_unknownId_shouldReturn404() throws Exception {
        perform(delete(COMPANIES + "/" + Long.MAX_VALUE), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }


    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private CompanyResponse existingCompany() {
        return companyService.create(new CompanyRequest(uniqueName()));
    }

    private static String body(String name) {
        return """
                {"name":"%s"}""".formatted(name);
    }

    private static String uniqueName() {
        return "Company " + UUID.randomUUID();
    }
}
