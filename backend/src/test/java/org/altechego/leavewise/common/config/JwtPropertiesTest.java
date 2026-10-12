package org.altechego.leavewise.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

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
    void validate_secretOf32Bytes_shouldHaveNoViolations() {
        assertThat(validator.validate(propertiesWith(base64OfLength(32)))).isEmpty();
    }

    @Test
    void validate_secretShorterThan32Bytes_shouldBeRejected() {
        assertThat(violatedProperties(propertiesWith(base64OfLength(31)))).containsExactly("secretStrongEnough");
    }

    @Test
    void validate_secretNotBase64_shouldBeRejected() {
        assertThat(violatedProperties(propertiesWith("change-me-generate-with-openssl-rand-base64-32")))
                .containsExactly("secretStrongEnough");
    }

    @Test
    void validate_missingSecret_shouldBeRejectedAsBlankOnly() {
        assertThat(violatedProperties(propertiesWith(null))).containsExactly("secret");
    }


    private static JwtProperties propertiesWith(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7));
    }

    private static String base64OfLength(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    private static List<String> violatedProperties(JwtProperties properties) {
        return validator.validate(properties).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
