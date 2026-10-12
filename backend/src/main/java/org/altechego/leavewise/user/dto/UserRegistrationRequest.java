package org.altechego.leavewise.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.user.constants.UserConstants;
import org.altechego.leavewise.user.validation.ValidNif;
import org.altechego.leavewise.user.validation.ValidPassword;

public record UserRegistrationRequest(
        @NotBlank @Size(max = UserConstants.NAME_MAX_LENGTH) String name,
        @NotBlank @Email @Size(max = UserConstants.EMAIL_MAX_LENGTH) String email,
        @NotBlank @ValidPassword String password,
        @ValidNif String nif,
        @Size(max = UserConstants.CONTACT_MAX_LENGTH) String contact,
        @NotNull Long companyId,
        @NotNull Long companyLocationId) {

    @Override
    public String toString() {
        return "UserRegistrationRequest[companyId=%s, companyLocationId=%s]".formatted(companyId, companyLocationId);
    }
}
