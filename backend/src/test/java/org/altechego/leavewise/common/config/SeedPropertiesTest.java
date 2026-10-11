package org.altechego.leavewise.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SeedPropertiesTest {

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void country_blankOrMissing_shouldDefaultToPortugal(String country) {
        assertThat(propertiesWith("change-me", country).country()).isEqualTo(MunicipalityConstants.DEFAULT_COUNTRY);
    }

    @Test
    void country_given_shouldBeKept() {
        assertThat(propertiesWith("change-me", "Spain").country()).isEqualTo("Spain");
    }

    @Test
    void validate_validProperties_shouldHaveNoViolations() {
        assertThat(validator.validate(propertiesWith("change-me", null))).isEmpty();
    }

    @Test
    void validate_passwordShorterThan8Characters_shouldBeRejected() {
        var violations = validator.validate(propertiesWith("1234567", null));

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactly("adminPassword");
    }

    @Test
    void validate_passwordOver72Utf8Bytes_shouldBeRejected() {
        var violations = validator.validate(propertiesWith("ç".repeat(37), null));

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactly("adminPasswordWithinBcryptLimit");
    }

    @Test
    void validate_companyNameLongerThanColumn_shouldBeRejected() {
        var tooLongName = "x".repeat(CompanyConstants.NAME_MAX_LENGTH + 1);
        var properties = new SeedProperties(
                "admin@leavewise.local", "change-me", "Administrator", tooLongName, "Sede", "Lisboa", "Lisboa", null);

        assertThat(validator.validate(properties))
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactly("companyName");
    }

    private static SeedProperties propertiesWith(String adminPassword, String country) {
        return new SeedProperties(
                "admin@leavewise.local", adminPassword, "Administrator", "LeaveWise", "Sede", "Lisboa", "Lisboa", country);
    }
}
