package org.altechego.leavewise.schedule.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = EachDayOfWeekOnceValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface EachDayOfWeekOnce {

    String message() default "must not repeat a day of the week";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
