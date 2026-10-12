package org.altechego.leavewise.user.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NifValidatorTest {

    private final NifValidator validator = new NifValidator();


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isValid_missingNif_shouldBeValid(String nif) {
        assertThat(validator.isValid(nif, null)).isTrue();
    }

    // 123456789: remainder 2 → check digit 9; 100000010: remainder 0 → 0; 500000000: remainder 1 → 0; 999999990: remainder 0 → 0
    @ParameterizedTest
    @ValueSource(strings = {"123456789", "100000010", "500000000", "999999990"})
    void isValid_correctCheckDigit_shouldBeValid(String nif) {
        assertThat(validator.isValid(nif, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456780", "100000011", "999999991"})
    void isValid_wrongCheckDigit_shouldBeInvalid(String nif) {
        assertThat(validator.isValid(nif, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "1234567890", "12345678a", " 123456789"})
    void isValid_notNineAsciiDigits_shouldBeInvalid(String nif) {
        assertThat(validator.isValid(nif, null)).isFalse();
    }
}
