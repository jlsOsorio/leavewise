package org.altechego.leavewise.schedule.validation;

import java.util.List;
import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.altechego.leavewise.schedule.dto.WorkScheduleDayRequest;

public class EachDayOfWeekOnceValidator
        implements ConstraintValidator<EachDayOfWeekOnce, List<WorkScheduleDayRequest>> {

    @Override
    public boolean isValid(List<WorkScheduleDayRequest> days, ConstraintValidatorContext context) {
        if (days == null) {
            return true;
        }

        var dayValues = days.stream()
                .filter(Objects::nonNull)
                .map(WorkScheduleDayRequest::dayOfWeek)
                .filter(Objects::nonNull)
                .toList();

        return dayValues.stream().distinct().count() == dayValues.size();
    }
}
