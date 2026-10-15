package org.altechego.leavewise.auth.controller;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RegistrationLookupControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";
    private static final String COMPANIES = "/api/auth/companies";
    private static final String SEEDED_COMPANY = "Test Company";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyService companyService;

    private long seededCompanyId;


    @BeforeEach
    void loadSeededCompany() {
        seededCompanyId = companyRepository.findByName(SEEDED_COMPANY).orElseThrow().getId();
    }

    @Test
    void searchCompanies_unauthenticated_shouldReturnPage() throws Exception {
        getJson(COMPANIES)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.content[*].name").value(hasItem(SEEDED_COMPANY)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void searchCompanies_queryInAnotherCase_shouldMatchContainedName() throws Exception {
        searchCompanies(" company ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem(SEEDED_COMPANY)))
                .andExpect(jsonPath("$.content[*].name").value(everyItem(containsStringIgnoringCase("company"))));
    }

    // LIKE wildcards in the query are literal characters, not patterns
    @Test
    void searchCompanies_queryWithLikeWildcard_shouldNotMatchEverything() throws Exception {
        searchCompanies("%")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchCompanies_defaultSort_shouldOrderByName() throws Exception {
        companyService.findOrCreate("Zeta Lookup Lda");
        companyService.findOrCreate("Alfa Lookup Lda");

        searchCompanies("lookup lda")
                .andExpect(jsonPath("$.content[0].name").value("Alfa Lookup Lda"))
                .andExpect(jsonPath("$.content[1].name").value("Zeta Lookup Lda"));
    }

    @Test
    void searchCompanies_sizeAboveMaximum_shouldBeCappedAt100() throws Exception {
        getJson(COMPANIES + "?size=1000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void searchCompanies_queryTooLong_shouldReturn400() throws Exception {
        searchCompanies("a".repeat(201))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    void searchCompanies_sortByNameDesc_shouldReverseOrder() throws Exception {
        companyService.findOrCreate("Beta Reverse Lda");
        companyService.findOrCreate("Omega Reverse Lda");

        mockMvc.perform(get(COMPANIES).param("q", "reverse lda").param("sort", "name,desc").accept(VERSIONED_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Omega Reverse Lda"))
                .andExpect(jsonPath("$.content[1].name").value("Beta Reverse Lda"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"createdAt,asc", "unknownField"})
    void searchCompanies_sortByOtherThanName_shouldReturn400(String sort) throws Exception {
        getJson(COMPANIES + "?sort=" + sort)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void findCompanyLocations_sortByAddress_shouldReturn400() throws Exception {
        getJson(COMPANIES + "/" + seededCompanyId + "/locations?sort=address")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void findCompanyLocations_seededCompany_shouldReturnItsLocations() throws Exception {
        getJson(COMPANIES + "/" + seededCompanyId + "/locations")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem("Test Office")))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].companyId").doesNotExist());
    }

    @Test
    void findCompanyLocations_unknownCompany_shouldReturn404() throws Exception {
        getJson(COMPANIES + "/" + Long.MAX_VALUE + "/locations")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findCompanyLocations_nonNumericCompanyId_shouldReturn400() throws Exception {
        getJson(COMPANIES + "/abc/locations")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }


    private ResultActions searchCompanies(String query) throws Exception {
        return mockMvc.perform(get(COMPANIES).param("q", query).accept(VERSIONED_JSON));
    }

    private ResultActions getJson(String uri) throws Exception {
        return mockMvc.perform(get(uri).accept(VERSIONED_JSON));
    }
}
