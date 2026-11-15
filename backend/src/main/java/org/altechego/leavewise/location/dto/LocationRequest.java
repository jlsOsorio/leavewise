package org.altechego.leavewise.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.location.constants.LocationConstants;

public record LocationRequest(
        @NotBlank @Size(max = LocationConstants.NAME_MAX_LENGTH) String name,
        @Size(max = LocationConstants.ADDRESS_MAX_LENGTH) String address,
        @NotNull Long municipalityId) {

    public LocationRequest {
        name = name == null ? null : name.trim();
        address = address == null || address.isBlank() ? null : address.trim();
    }
}
