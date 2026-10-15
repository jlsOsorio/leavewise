package org.altechego.leavewise.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.user.constants.UserConstants;

public record LoginRequest(
        @NotBlank @Size(max = UserConstants.EMAIL_MAX_LENGTH) String email,
        @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[]";
    }
}
