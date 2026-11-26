package org.altechego.leavewise.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.common.security.TestAccessTokens;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.schedule.dto.WorkScheduleDayRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleRequest;
import org.altechego.leavewise.schedule.repository.WorkScheduleRepository;
import org.altechego.leavewise.schedule.service.WorkScheduleService;
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
class WorkScheduleControllerIT {

    private static final String VERSIONED_JSON = ApiMediaType.JSON + ";version=1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private WorkScheduleService scheduleService;

    @Autowired
    private WorkScheduleRepository scheduleRepository;

    private String adminBearer;

    private String userBearer;

    private Long companyId;


    @BeforeEach
    void setUp() {
        companyId = companyService.create(new CompanyRequest("Company " + UUID.randomUUID())).id();
        adminBearer = TestAccessTokens.bearer(jwtEncoder, 1L, Role.ADMIN, null);
        userBearer = TestAccessTokens.bearer(jwtEncoder, 2L, Role.USER, companyId);
    }

    @Test
    void findByCompany_admin_shouldReturnScheduleWithDaysInOrder() throws Exception {
        scheduleService.upsert(companyId, request("Standard week", 5));

        perform(get(workSchedule(companyId)), adminBearer)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ApiMediaType.VND))
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.name").value("Standard week"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.days[0].isWorkingDay").value(true))
                .andExpect(jsonPath("$.days[6].dayOfWeek").value(7))
                .andExpect(jsonPath("$.days[6].isWorkingDay").value(false));
    }

    @Test
    void findByCompany_userOfThatCompany_shouldReturnIt() throws Exception {
        scheduleService.upsert(companyId, request("Standard week", 5));

        perform(get(workSchedule(companyId)), userBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Standard week"));
    }

    @Test
    void findByCompany_userOfAnotherCompany_shouldReturn404() throws Exception {
        var otherCompanyId = companyService.create(new CompanyRequest("Company " + UUID.randomUUID())).id();
        scheduleService.upsert(otherCompanyId, request("Standard week", 5));

        perform(get(workSchedule(otherCompanyId)), userBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findByCompany_noSchedule_shouldReturn404() throws Exception {
        perform(get(workSchedule(companyId)), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findByCompany_unknownCompany_shouldReturn404() throws Exception {
        perform(get(workSchedule(Long.MAX_VALUE)), adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void findByCompany_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get(workSchedule(companyId)).accept(VERSIONED_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void upsert_noSchedule_shouldCreateItAndReturn200() throws Exception {
        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(body(" Standard week ", 5)),
                adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.name").value("Standard week"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[4].isWorkingDay").value(true))
                .andExpect(jsonPath("$.days[5].isWorkingDay").value(false));
    }

    @Test
    void upsert_existingSchedule_shouldReplaceItKeepingTheSameId() throws Exception {
        var existing = scheduleService.upsert(companyId, request("Standard week", 5));

        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(body("Six-day week", 6)),
                adminBearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.id()))
                .andExpect(jsonPath("$.name").value("Six-day week"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[5].isWorkingDay").value(true))
                .andExpect(jsonPath("$.days[6].isWorkingDay").value(false));
    }

    @Test
    void upsert_user_shouldReturn403() throws Exception {
        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(body("Week", 5)), userBearer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void upsert_unknownCompany_shouldReturn404() throws Exception {
        perform(put(workSchedule(Long.MAX_VALUE)).contentType(VERSIONED_JSON).content(body("Week", 5)),
                adminBearer)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void upsert_sixDays_shouldReturn400OnDays() throws Exception {
        var sixDays = """
                {"name":"Week","days":[%s]}""".formatted(dayEntries(1, 2, 3, 4, 5, 6));

        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(sixDays), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("days"));
    }

    @Test
    void upsert_repeatedDayOfWeek_shouldReturn400OnDays() throws Exception {
        var repeatedMonday = """
                {"name":"Week","days":[%s]}""".formatted(dayEntries(1, 2, 3, 4, 5, 6, 1));

        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(repeatedMonday), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("days"));
    }

    @Test
    void upsert_blankNameAndDayOutOfRange_shouldReturn400WithFieldErrors() throws Exception {
        var invalid = """
                {"name":" ","days":[%s]}""".formatted(dayEntries(1, 2, 3, 4, 5, 6, 8));

        perform(put(workSchedule(companyId)).contentType(VERSIONED_JSON).content(invalid), adminBearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void deleteCompany_withSchedule_shouldDeleteTheSchedule() {
        scheduleService.upsert(companyId, request("Standard week", 5));

        companyService.delete(companyId);

        assertThat(scheduleRepository.findWithDaysByCompanyId(companyId)).isEmpty();
    }


    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.accept(VERSIONED_JSON).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private static WorkScheduleRequest request(String name, int workingDays) {
        List<WorkScheduleDayRequest> days = IntStream.rangeClosed(1, 7)
                .mapToObj(day -> new WorkScheduleDayRequest((short) day, day <= workingDays))
                .toList();
        return new WorkScheduleRequest(name, days);
    }

    private static String body(String name, int workingDays) {
        var days = IntStream.rangeClosed(1, 7)
                .mapToObj(day -> dayEntry(day, day <= workingDays))
                .collect(Collectors.joining(","));
        return """
                {"name":"%s","days":[%s]}""".formatted(name, days);
    }

    private static String dayEntries(int... daysOfWeek) {
        return IntStream.of(daysOfWeek).mapToObj(day -> dayEntry(day, true)).collect(Collectors.joining(","));
    }

    private static String dayEntry(int dayOfWeek, boolean isWorkingDay) {
        return """
                {"dayOfWeek":%d,"isWorkingDay":%b}""".formatted(dayOfWeek, isWorkingDay);
    }

    private static String workSchedule(Long companyId) {
        return "/api/companies/" + companyId + "/work-schedule";
    }
}
