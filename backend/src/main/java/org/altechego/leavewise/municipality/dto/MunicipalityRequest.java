package org.altechego.leavewise.municipality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;

public record MunicipalityRequest(
        @NotBlank @Size(max = MunicipalityConstants.NAME_MAX_LENGTH) String name,
        @NotBlank @Size(max = MunicipalityConstants.DISTRICT_MAX_LENGTH) String district,
        @Size(max = MunicipalityConstants.COUNTRY_MAX_LENGTH) String country) {

    public MunicipalityRequest {
        name = trim(name);
        district = trim(district);
        country = country == null || country.isBlank() ? MunicipalityConstants.DEFAULT_COUNTRY : country.trim();
    }


    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
