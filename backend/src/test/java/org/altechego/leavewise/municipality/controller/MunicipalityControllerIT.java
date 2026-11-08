package org.altechego.leavewise.municipality.controller;

import static org.hamcrest.Matchers.hasItem;
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
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
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
class MunicipalityControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String MUNICIPALITIES = "/api/municipalities";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private MunicipalityService municipalityService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    private String adminBearer;

    private String userBearer;


    @BeforeEach
    void createTokens() {
        adminBearer = TestAccessTokens.bearer(jwtEncoder, 1L, Role.ADMIN, null);
        userBearer = TestAccessTokens.bearer(jwtEncoder, 2L, Role.USER, null);
    }

    @Test
    void search_userToken_shouldReturnPage() throws Exception {
        var existing = existingMunicipality();

        perform(get(MUNICIPALITIES).param("q", existing.name()), userBearer)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].district").value(existing.district()))
                .andExpect(jsonPath("$.content[0].country").value("Portugal"));
    }

    @Test
    void search_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(MUNICIPALITIES).accept(VERSIONED_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void search_queryInAnotherCase_shouldMatchContainedName() throws Exception {
        var existing = existingMunicipality();

        perform(get(MUNICIPALITIES).param("q", existing.name().toUpperCase()), userBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem(existing.id().intValue())));
    }

    @Test
    void search_sortByUnlistedField_shouldReturn400() throws Exception {
        perform(get(MUNICIPALITIES).param("sort", "id"), userBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void search_queryTooLong_shouldReturn400() throws Exception {
        perform(get(MUNICIPALITIES).param("q", "a".repeat(151)), userBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    void findById_existingMunicipality_shouldReturnIt() throws Exception {
        var existing = existingMunicipality();

        perform(get(MUNICIPALITIES + "/" + existing.id()), userBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(existing.name()));
    }

    @Test
    void findById_unknownId_shouldReturn404() throws Exception {
        perform(get(MUNICIPALITIES + "/" + Long.MAX_VALUE), userBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_admin_shouldReturn201WithLocationAndDefaultCountry() throws Exception {
        var name = uniqueName();

        perform(post(MUNICIPALITIES).contentType(VERSIONED_JSON).content(body(" " + name + " ", "Braga", null)),
                adminBearer)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesRegex(".*" + MUNICIPALITIES + "/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.country").value("Portugal"));
    }

    @Test
    void create_user_shouldReturn403() throws Exception {
        perform(post(MUNICIPALITIES).contentType(VERSIONED_JSON).content(body(uniqueName(), "Braga", null)),
                userBearer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_blankName_shouldReturn400WithFieldError() throws Exception {
        perform(post(MUNICIPALITIES).contentType(VERSIONED_JSON).content(body("  ", "Braga", null)), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void create_sameNameDistrictCountryInAnotherCase_shouldReturn409() throws Exception {
        var existing = existingMunicipality();

        perform(post(MUNICIPALITIES).contentType(VERSIONED_JSON)
                        .content(body(existing.name().toLowerCase(), existing.district().toUpperCase(), "portugal")),
                adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void update_admin_shouldReturnUpdatedMunicipality() throws Exception {
        var existing = existingMunicipality();
        var newName = uniqueName();

        perform(put(MUNICIPALITIES + "/" + existing.id()).contentType(VERSIONED_JSON)
                        .content(body(newName, "Viana do Castelo", "Portugal")),
                adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.id()))
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.district").value("Viana do Castelo"));
    }

    @Test
    void update_user_shouldReturn403() throws Exception {
        var existing = existingMunicipality();

        perform(put(MUNICIPALITIES + "/" + existing.id()).contentType(VERSIONED_JSON)
                        .content(body(uniqueName(), "Braga", null)),
                userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void update_unknownId_shouldReturn404() throws Exception {
        perform(put(MUNICIPALITIES + "/" + Long.MAX_VALUE).contentType(VERSIONED_JSON)
                        .content(body(uniqueName(), "Braga", null)),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_toAnotherMunicipalitysKey_shouldReturn409() throws Exception {
        var first = existingMunicipality();
        var second = existingMunicipality();

        perform(put(MUNICIPALITIES + "/" + second.id()).contentType(VERSIONED_JSON)
                        .content(body(first.name(), first.district(), first.country())),
                adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void delete_unreferencedMunicipality_shouldReturn204AndRemoveIt() throws Exception {
        var existing = existingMunicipality();

        perform(delete(MUNICIPALITIES + "/" + existing.id()), adminBearer)
                .andExpect(status().isNoContent());
        perform(get(MUNICIPALITIES + "/" + existing.id()), adminBearer)
                .andExpect(status().isNotFound());
    }

    // BR-15: the seeded office is located in this municipality
    @Test
    void delete_referencedByLocation_shouldReturn409() throws Exception {
        perform(delete(MUNICIPALITIES + "/" + seededOfficeMunicipalityId()), adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void delete_user_shouldReturn403() throws Exception {
        var existing = existingMunicipality();

        perform(delete(MUNICIPALITIES + "/" + existing.id()), userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_unknownId_shouldReturn404() throws Exception {
        perform(delete(MUNICIPALITIES + "/" + Long.MAX_VALUE), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }


    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private MunicipalityResponse existingMunicipality() {
        return municipalityService.findOrCreate(uniqueName(), "Braga", "Portugal");
    }

    // Reading the id of a lazy proxy doesn't initialise it, so no session is needed
    private Long seededOfficeMunicipalityId() {
        var companyId = companyRepository.findByName("Test Company").orElseThrow().getId();
        return locationRepository.findByCompanyIdAndName(companyId, "Test Office").orElseThrow()
                .getMunicipality().getId();
    }

    private static String body(String name, String district, String country) {
        var countryJson = country == null ? "null" : "\"" + country + "\"";
        return """
                {"name":"%s","district":"%s","country":%s}""".formatted(name, district, countryJson);
    }

    private static String uniqueName() {
        return "Municipality " + UUID.randomUUID();
    }
}
