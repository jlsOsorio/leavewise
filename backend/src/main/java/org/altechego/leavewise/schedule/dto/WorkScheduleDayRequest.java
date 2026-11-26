package org.altechego.leavewise.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.altechego.leavewise.schedule.constants.WorkScheduleConstants;

public record WorkScheduleDayRequest(
        @NotNull @Min(WorkScheduleConstants.FIRST_DAY_OF_WEEK) @Max(WorkScheduleConstants.DAYS_PER_WEEK)
        Short dayOfWeek,
        @NotNull Boolean isWorkingDay) {}
