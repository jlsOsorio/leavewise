package org.altechego.leavewise.schedule.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.schedule.constants.WorkScheduleConstants;
import org.altechego.leavewise.schedule.validation.EachDayOfWeekOnce;

public record WorkScheduleRequest(
        @NotBlank @Size(max = WorkScheduleConstants.NAME_MAX_LENGTH) String name,
        @NotNull
        @Size(min = WorkScheduleConstants.DAYS_PER_WEEK, max = WorkScheduleConstants.DAYS_PER_WEEK)
        @EachDayOfWeekOnce
        List<@NotNull @Valid WorkScheduleDayRequest> days) {

    public WorkScheduleRequest {
        name = name == null ? null : name.trim();
    }
}
