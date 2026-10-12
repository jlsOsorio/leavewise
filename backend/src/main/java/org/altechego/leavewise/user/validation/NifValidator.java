package org.altechego.leavewise.user.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.altechego.leavewise.user.constants.UserConstants;

public class NifValidator implements ConstraintValidator<ValidNif, String> {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d{" + UserConstants.NIF_LENGTH + "}");
    private static final int MODULUS = 11;


    @Override
    public boolean isValid(String nif, ConstraintValidatorContext context) {
        if (nif == null || nif.isBlank()) {
            return true;
        }
        return DIGITS_ONLY.matcher(nif).matches() && hasValidCheckDigit(nif);
    }


    // Mod 11: the first 8 digits weighted 9 down to 2; a remainder of 0 or 1 gives check digit 0, else 11 - remainder
    private static boolean hasValidCheckDigit(String nif) {
        var lastIndex = UserConstants.NIF_LENGTH - 1;

        var weightedSum = 0;
        for (var index = 0; index < lastIndex; index++) {
            weightedSum += Character.digit(nif.charAt(index), 10) * (UserConstants.NIF_LENGTH - index);
        }

        var remainder = weightedSum % MODULUS;
        var expectedCheckDigit = remainder < 2 ? 0 : MODULUS - remainder;
        return Character.digit(nif.charAt(lastIndex), 10) == expectedCheckDigit;
    }
}
