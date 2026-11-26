package org.altechego.leavewise.schedule.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorkScheduleRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;


    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validate_sevenDistinctDays_shouldHaveNoViolations() {
        assertThat(validator.validate(new WorkScheduleRequest("Standard week", week()))).isEmpty();
    }

    @Test
    void constructor_paddedName_shouldTrimIt() {
        assertThat(new WorkScheduleRequest("  Standard week ", week()).name()).isEqualTo("Standard week");
    }

    @Test
    void validate_blankName_shouldRejectName() {
        assertThat(violatedPaths(new WorkScheduleRequest("  ", week()))).containsExactly("name");
    }

    @Test
    void validate_sixDays_shouldRejectDays() {
        var days = new ArrayList<>(week());
        days.removeLast();

        assertThat(violatedPaths(new WorkScheduleRequest("Week", days))).containsExactly("days");
    }

    @Test
    void validate_eightDays_shouldRejectDays() {
        var days = new ArrayList<>(week());
        days.add(day(1));

        assertThat(violatedPaths(new WorkScheduleRequest("Week", days))).containsOnly("days");
    }

    @Test
    void validate_repeatedDayOfWeek_shouldRejectDays() {
        var days = new ArrayList<>(week());
        days.set(6, day(1));

        assertThat(violatedPaths(new WorkScheduleRequest("Week", days))).containsExactly("days");
    }

    @Test
    void validate_dayOfWeekOutOfRange_shouldRejectThatDay() {
        var days = new ArrayList<>(week());
        days.set(6, day(8));

        assertThat(violatedPaths(new WorkScheduleRequest("Week", days))).containsExactly("days[6].dayOfWeek");
    }

    @Test
    void validate_missingWorkingFlag_shouldRejectThatDay() {
        var days = new ArrayList<>(week());
        days.set(0, new WorkScheduleDayRequest((short) 1, null));

        assertThat(violatedPaths(new WorkScheduleRequest("Week", days))).containsExactly("days[0].isWorkingDay");
    }

    @Test
    void validate_nullDays_shouldRejectDays() {
        assertThat(violatedPaths(new WorkScheduleRequest("Week", null))).containsExactly("days");
    }


    private static List<String> violatedPaths(WorkScheduleRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }

    private static List<WorkScheduleDayRequest> week() {
        return IntStream.rangeClosed(1, 7).mapToObj(WorkScheduleRequestTest::day).toList();
    }

    private static WorkScheduleDayRequest day(int dayOfWeek) {
        return new WorkScheduleDayRequest((short) dayOfWeek, dayOfWeek <= 5);
    }
}
