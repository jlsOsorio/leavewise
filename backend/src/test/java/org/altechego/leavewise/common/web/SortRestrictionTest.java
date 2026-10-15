package org.altechego.leavewise.common.web;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;

import org.altechego.leavewise.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class SortRestrictionTest {

    private static final Set<String> ALLOWED = Set.of("name");


    @Test
    void requireAllowed_unsorted_shouldPass() {
        assertThatCode(() -> SortRestriction.requireAllowed(PageRequest.of(0, 20), ALLOWED))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAllowed_allowedPropertyInEitherDirection_shouldPass() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("name")));

        assertThatCode(() -> SortRestriction.requireAllowed(pageable, ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void requireAllowed_oneDisallowedPropertyAmongAllowed_shouldThrowValidationError() {
        var pageable = PageRequest.of(0, 20, Sort.by("name", "createdAt"));

        assertErrorCode(() -> SortRestriction.requireAllowed(pageable, ALLOWED), ErrorCode.VALIDATION_ERROR);
    }
}
