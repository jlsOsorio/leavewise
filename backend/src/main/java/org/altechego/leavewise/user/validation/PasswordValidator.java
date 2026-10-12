package org.altechego.leavewise.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.altechego.leavewise.user.constants.PasswordPolicy;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        return password.length() >= PasswordPolicy.MIN_LENGTH && PasswordPolicy.fitsBcryptLimit(password);
    }
}
