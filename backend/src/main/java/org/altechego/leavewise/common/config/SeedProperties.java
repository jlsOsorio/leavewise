package org.altechego.leavewise.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.location.constants.LocationConstants;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.altechego.leavewise.user.constants.PasswordPolicy;
import org.altechego.leavewise.user.constants.UserConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("leavewise.seed")
public record SeedProperties(
        @NotBlank @Email @Size(max = UserConstants.EMAIL_MAX_LENGTH) String adminEmail,
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH) String adminPassword,
        @NotBlank @Size(max = UserConstants.NAME_MAX_LENGTH) String adminName,
        @NotBlank @Size(max = CompanyConstants.NAME_MAX_LENGTH) String companyName,
        @NotBlank @Size(max = LocationConstants.NAME_MAX_LENGTH) String locationName,
        @NotBlank @Size(max = MunicipalityConstants.NAME_MAX_LENGTH) String municipality,
        @NotBlank @Size(max = MunicipalityConstants.DISTRICT_MAX_LENGTH) String district,
        @Size(max = MunicipalityConstants.COUNTRY_MAX_LENGTH) String country) {

    public SeedProperties {
        if (country == null || country.isBlank()) {
            country = MunicipalityConstants.DEFAULT_COUNTRY;
        }
    }

    @AssertTrue(message = "must be at most " + PasswordPolicy.MAX_UTF8_BYTES + " UTF-8 bytes")
    public boolean isAdminPasswordWithinBcryptLimit() {
        return PasswordPolicy.fitsBcryptLimit(adminPassword);
    }
}
