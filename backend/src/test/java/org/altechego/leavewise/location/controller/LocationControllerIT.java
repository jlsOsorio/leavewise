package org.altechego.leavewise.location.controller;

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
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.location.service.LocationService;
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
class LocationControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String LOCATIONS = "/api/locations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MunicipalityService municipalityService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CompanyLocationRepository locationRepository;

    private String adminBearer;

    private String userBearer;

    private Long companyId;

    private MunicipalityResponse municipality;


    @BeforeEach
    void setUp() {
        adminBearer = TestAccessTokens.bearer(jwtEncoder, 1L, Role.ADMIN, null);
        userBearer = TestAccessTokens.bearer(jwtEncoder, 2L, Role.USER, null);
        companyId = companyService.create(new CompanyRequest("Company " + UUID.randomUUID())).id();
        municipality = existingMunicipality();
    }

    @Test
    void findByCompany_admin_shouldReturnLocationsWithMunicipality() throws Exception {
        var existing = existingLocation();

        perform(get(companyLocations(companyId)), adminBearer)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(existing.id()))
                .andExpect(jsonPath("$.content[0].companyId").value(companyId))
                .andExpect(jsonPath("$.content[0].municipality.name").value(municipality.name()));
    }

    @Test
    void findByCompany_user_shouldReturn403() throws Exception {
        perform(get(companyLocations(companyId)), userBearer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void findByCompany_unknownCompany_shouldReturn404() throws Exception {
        perform(get(companyLocations(Long.MAX_VALUE)), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findByCompany_sortByUnlistedField_shouldReturn400() throws Exception {
        perform(get(companyLocations(companyId)).param("sort", "address"), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_admin_shouldReturn201WithItemLocation() throws Exception {
        perform(post(companyLocations(companyId)).contentType(VERSIONED_JSON)
                        .content(body(" Sede ", " Rua A ", municipality.id())),
                adminBearer)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesRegex(".*" + LOCATIONS + "/\\d+")))
                .andExpect(jsonPath("$.name").value("Sede"))
                .andExpect(jsonPath("$.address").value("Rua A"))
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.municipality.id").value(municipality.id()));
    }

    @Test
    void create_user_shouldReturn403() throws Exception {
        perform(post(companyLocations(companyId)).contentType(VERSIONED_JSON)
                        .content(body("Sede", null, municipality.id())),
                userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void create_blankNameAndNoMunicipality_shouldReturn400WithFieldErrors() throws Exception {
        perform(post(companyLocations(companyId)).contentType(VERSIONED_JSON).content(body(" ", null, null)),
                adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void create_unknownCompany_shouldReturn404() throws Exception {
        perform(post(companyLocations(Long.MAX_VALUE)).contentType(VERSIONED_JSON)
                        .content(body("Sede", null, municipality.id())),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_unknownMunicipality_shouldReturn404() throws Exception {
        perform(post(companyLocations(companyId)).contentType(VERSIONED_JSON)
                        .content(body("Sede", null, Long.MAX_VALUE)),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_nameTakenInSameCompany_shouldReturn409() throws Exception {
        var existing = existingLocation();

        perform(post(companyLocations(companyId)).contentType(VERSIONED_JSON)
                        .content(body(existing.name(), null, municipality.id())),
                adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void create_nameTakenInAnotherCompany_shouldReturn201() throws Exception {
        var existing = existingLocation();
        var otherCompanyId = companyService.create(new CompanyRequest("Company " + UUID.randomUUID())).id();

        perform(post(companyLocations(otherCompanyId)).contentType(VERSIONED_JSON)
                        .content(body(existing.name(), null, municipality.id())),
                adminBearer)
                .andExpect(status().isCreated());
    }

    @Test
    void findById_admin_shouldReturnIt() throws Exception {
        var existing = existingLocation();

        perform(get(LOCATIONS + "/" + existing.id()), adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(existing.name()))
                .andExpect(jsonPath("$.municipality.district").value(municipality.district()));
    }

    @Test
    void findById_user_shouldReturn403() throws Exception {
        var existing = existingLocation();

        perform(get(LOCATIONS + "/" + existing.id()), userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_unknownId_shouldReturn404() throws Exception {
        perform(get(LOCATIONS + "/" + Long.MAX_VALUE), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_admin_shouldRenameAndMoveToAnotherMunicipality() throws Exception {
        var existing = existingLocation();
        var otherMunicipality = existingMunicipality();

        perform(put(LOCATIONS + "/" + existing.id()).contentType(VERSIONED_JSON)
                        .content(body("Filial", null, otherMunicipality.id())),
                adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Filial"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.municipality.id").value(otherMunicipality.id()));
    }

    @Test
    void update_user_shouldReturn403() throws Exception {
        var existing = existingLocation();

        perform(put(LOCATIONS + "/" + existing.id()).contentType(VERSIONED_JSON)
                        .content(body("Filial", null, municipality.id())),
                userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void update_unknownId_shouldReturn404() throws Exception {
        perform(put(LOCATIONS + "/" + Long.MAX_VALUE).contentType(VERSIONED_JSON)
                        .content(body("Filial", null, municipality.id())),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_unknownMunicipality_shouldReturn404() throws Exception {
        var existing = existingLocation();

        perform(put(LOCATIONS + "/" + existing.id()).contentType(VERSIONED_JSON)
                        .content(body("Filial", null, Long.MAX_VALUE)),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_toAnotherLocationsName_shouldReturn409() throws Exception {
        var first = existingLocation();
        var second = existingLocation();

        perform(put(LOCATIONS + "/" + second.id()).contentType(VERSIONED_JSON)
                        .content(body(first.name(), null, municipality.id())),
                adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void delete_locationWithoutUsers_shouldReturn204AndRemoveIt() throws Exception {
        var existing = existingLocation();

        perform(delete(LOCATIONS + "/" + existing.id()), adminBearer)
                .andExpect(status().isNoContent());
        perform(get(LOCATIONS + "/" + existing.id()), adminBearer)
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_locationWithUsers_shouldReturn409() throws Exception {
        perform(delete(LOCATIONS + "/" + seededOfficeId()), adminBearer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void delete_user_shouldReturn403() throws Exception {
        var existing = existingLocation();

        perform(delete(LOCATIONS + "/" + existing.id()), userBearer)
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_unknownId_shouldReturn404() throws Exception {
        perform(delete(LOCATIONS + "/" + Long.MAX_VALUE), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }


    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private LocationResponse existingLocation() {
        return locationService.create(
                companyId, new LocationRequest("Location " + UUID.randomUUID(), "Rua A", municipality.id()));
    }

    private MunicipalityResponse existingMunicipality() {
        return municipalityService.findOrCreate("Municipality " + UUID.randomUUID(), "Braga", "Portugal");
    }

    private Long seededOfficeId() {
        var seededCompanyId = companyRepository.findByName("Test Company").orElseThrow().getId();
        return locationRepository.findByCompanyIdAndName(seededCompanyId, "Test Office").orElseThrow().getId();
    }

    private static String companyLocations(Long companyId) {
        return "/api/companies/" + companyId + "/locations";
    }

    private static String body(String name, String address, Long municipalityId) {
        return """
                {"name":%s,"address":%s,"municipalityId":%s}"""
                .formatted(json(name), json(address), municipalityId);
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
