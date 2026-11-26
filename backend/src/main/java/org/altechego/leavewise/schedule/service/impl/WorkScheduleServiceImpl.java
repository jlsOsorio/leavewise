package org.altechego.leavewise.schedule.service.impl;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.exception.CompanyNotFoundException;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.schedule.dto.WorkScheduleDayRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleResponse;
import org.altechego.leavewise.schedule.entity.WorkSchedule;
import org.altechego.leavewise.schedule.entity.WorkScheduleDay;
import org.altechego.leavewise.schedule.exception.WorkScheduleNotFoundException;
import org.altechego.leavewise.schedule.mapper.WorkScheduleMapper;
import org.altechego.leavewise.schedule.repository.WorkScheduleRepository;
import org.altechego.leavewise.schedule.service.WorkScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final WorkScheduleRepository scheduleRepository;
    private final CompanyRepository companyRepository;
    private final WorkScheduleMapper scheduleMapper;


    @Override
    public WorkScheduleResponse findByCompany(AuthenticatedUser actor, Long companyId) {
        if (actor.role() != Role.ADMIN && !companyId.equals(actor.companyId())) {
            throw new CompanyNotFoundException(companyId);
        }
        requireCompany(companyId);
        var schedule = scheduleRepository.findWithDaysByCompanyId(companyId)
                .orElseThrow(() -> new WorkScheduleNotFoundException(companyId));
        return toSortedResponse(schedule);
    }

    @Override
    @Transactional
    public WorkScheduleResponse upsert(Long companyId, WorkScheduleRequest request) {
        requireCompany(companyId);
        var schedule = scheduleRepository.findWithDaysByCompanyId(companyId)
                .orElseGet(() -> newSchedule(companyId));
        schedule.setName(request.name());
        applyDays(schedule, request);

        var saved = scheduleRepository.saveAndFlush(schedule);
        log.info("Saved work schedule {} for company {}", saved.getId(), companyId);
        return toSortedResponse(saved);
    }

    private void requireCompany(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
    }

    private WorkSchedule newSchedule(Long companyId) {
        return WorkSchedule.builder().company(companyRepository.getReferenceById(companyId)).build();
    }

    private static void applyDays(WorkSchedule schedule, WorkScheduleRequest request) {
        var existingByDay = schedule.getDays().stream()
                .collect(Collectors.toMap(WorkScheduleDay::getDayOfWeek, Function.identity()));

        for (WorkScheduleDayRequest dayRequest : request.days()) {
            var existing = existingByDay.get(dayRequest.dayOfWeek());
            if (existing != null) {
                existing.setWorkingDay(dayRequest.isWorkingDay());
            } else {
                schedule.addDay(WorkScheduleDay.builder()
                        .dayOfWeek(dayRequest.dayOfWeek())
                        .workingDay(dayRequest.isWorkingDay())
                        .build());
            }
        }
    }

    private WorkScheduleResponse toSortedResponse(WorkSchedule schedule) {
        schedule.getDays().sort(Comparator.comparing(WorkScheduleDay::getDayOfWeek));
        return scheduleMapper.toResponse(schedule);
    }
}
