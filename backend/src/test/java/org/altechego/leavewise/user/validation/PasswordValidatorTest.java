package org.altechego.leavewise.user.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();


    @Test
    void isValid_null_shouldBeLeftToNotBlankValidator() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    void isValid_asciiWithinLimits_shouldBeValid(int length) {
        assertThat(validator.isValid("a".repeat(length), null)).isTrue();
    }

    @Test
    void isValid_shorterThan8Characters_shouldBeInvalid() {
        assertThat(validator.isValid("1234567", null)).isFalse();
    }

    @Test
    void isValid_over72Bytes_shouldBeInvalid() {
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    void isValid_multibyteCharactersAt72Bytes_shouldBeValid() {
        assertThat(validator.isValid("ç".repeat(36), null)).isTrue();
    }

    @Test
    void isValid_multibyteCharactersOver72Bytes_shouldBeInvalid() {
        assertThat(validator.isValid("ç".repeat(37), null)).isFalse();
    }
}
