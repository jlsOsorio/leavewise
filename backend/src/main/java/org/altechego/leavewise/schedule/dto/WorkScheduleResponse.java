package org.altechego.leavewise.schedule.dto;

import java.util.List;

public record WorkScheduleResponse(
        Long id,
        Long companyId,
        String name,
        List<WorkScheduleDayResponse> days) {}
