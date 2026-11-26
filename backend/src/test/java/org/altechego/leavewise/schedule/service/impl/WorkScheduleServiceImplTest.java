package org.altechego.leavewise.schedule.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.schedule.dto.WorkScheduleDayRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleDayResponse;
import org.altechego.leavewise.schedule.dto.WorkScheduleRequest;
import org.altechego.leavewise.schedule.entity.WorkSchedule;
import org.altechego.leavewise.schedule.entity.WorkScheduleDay;
import org.altechego.leavewise.schedule.mapper.WorkScheduleMapper;
import org.altechego.leavewise.schedule.repository.WorkScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceImplTest {

    private static final long COMPANY_ID = 3L;
    private static final long OTHER_COMPANY_ID = 4L;
    private static final long SCHEDULE_ID = 9L;
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@x.pt", Role.ADMIN, null);
    private static final AuthenticatedUser USER = new AuthenticatedUser(2L, "user@x.pt", Role.USER, COMPANY_ID);

    @Mock
    private WorkScheduleRepository scheduleRepository;

    @Mock
    private CompanyRepository companyRepository;

    private WorkScheduleServiceImpl scheduleService;


    @BeforeEach
    void setUp() {
        scheduleService = new WorkScheduleServiceImpl(
                scheduleRepository, companyRepository, Mappers.getMapper(WorkScheduleMapper.class));
    }

    @Test
    void findByCompany_admin_shouldReturnDaysSortedByDayOfWeek() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(scheduleRepository.findWithDaysByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(schedule(7, 3, 1, 2, 4, 5, 6)));

        var response = scheduleService.findByCompany(ADMIN, COMPANY_ID);

        assertThat(response.companyId()).isEqualTo(COMPANY_ID);
        assertThat(response.days()).extracting(WorkScheduleDayResponse::dayOfWeek)
                .containsExactly((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7);
    }

    @Test
    void findByCompany_userOfThatCompany_shouldReturnIt() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(scheduleRepository.findWithDaysByCompanyId(COMPANY_ID)).thenReturn(Optional.of(schedule(1)));

        assertThat(scheduleService.findByCompany(USER, COMPANY_ID).id()).isEqualTo(SCHEDULE_ID);
    }

    @Test
    void findByCompany_userOfAnotherCompany_shouldThrowNotFoundWithoutQuerying() {
        assertErrorCode(() -> scheduleService.findByCompany(USER, OTHER_COMPANY_ID), ErrorCode.NOT_FOUND);
        verify(scheduleRepository, never()).findWithDaysByCompanyId(any());
    }

    @Test
    void findByCompany_unknownCompany_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> scheduleService.findByCompany(ADMIN, COMPANY_ID), ErrorCode.NOT_FOUND);
    }

    @Test
    void findByCompany_noSchedule_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(scheduleRepository.findWithDaysByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> scheduleService.findByCompany(ADMIN, COMPANY_ID), ErrorCode.NOT_FOUND);
    }

    @Test
    void upsert_noSchedule_shouldCreateItWithSevenDays() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(scheduleRepository.findWithDaysByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company());
        when(scheduleRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = scheduleService.upsert(COMPANY_ID, request("Standard week", 6));

        assertThat(response.name()).isEqualTo("Standard week");
        assertThat(response.companyId()).isEqualTo(COMPANY_ID);
        assertThat(response.days()).hasSize(7)
                .filteredOn(WorkScheduleDayResponse::isWorkingDay).hasSize(6);
    }

    @Test
    void upsert_existingSchedule_shouldUpdateDayRowsInPlace() {
        var existing = schedule(1, 2, 3, 4, 5, 6, 7);
        var mondayRow = existing.getDays().getFirst();
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(scheduleRepository.findWithDaysByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existing));
        when(scheduleRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = scheduleService.upsert(COMPANY_ID, request("Short week", 4));

        assertThat(existing.getDays()).hasSize(7).contains(mondayRow);
        assertThat(response.name()).isEqualTo("Short week");
        assertThat(response.days()).filteredOn(WorkScheduleDayResponse::isWorkingDay)
                .extracting(WorkScheduleDayResponse::dayOfWeek)
                .containsExactly((short) 1, (short) 2, (short) 3, (short) 4);
    }

    @Test
    void upsert_unknownCompany_shouldThrowNotFoundWithoutSaving() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> scheduleService.upsert(COMPANY_ID, request("Week", 5)), ErrorCode.NOT_FOUND);
        verify(scheduleRepository, never()).saveAndFlush(any());
    }


    private static Company company() {
        return Company.builder().id(COMPANY_ID).name("Acme").build();
    }

    private static WorkSchedule schedule(int... daysOfWeek) {
        var schedule = WorkSchedule.builder().id(SCHEDULE_ID).company(company()).name("Week").build();
        for (var dayOfWeek : daysOfWeek) {
            schedule.addDay(WorkScheduleDay.builder()
                    .id((long) dayOfWeek)
                    .dayOfWeek((short) dayOfWeek)
                    .workingDay(true)
                    .build());
        }
        return schedule;
    }

    private static WorkScheduleRequest request(String name, int workingDays) {
        List<WorkScheduleDayRequest> days = IntStream.rangeClosed(1, 7)
                .mapToObj(day -> new WorkScheduleDayRequest((short) day, day <= workingDays))
                .toList();
        return new WorkScheduleRequest(name, days);
    }
}
