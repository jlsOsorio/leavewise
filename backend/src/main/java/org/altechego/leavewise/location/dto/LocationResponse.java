package org.altechego.leavewise.location.dto;

import org.altechego.leavewise.municipality.dto.MunicipalityResponse;

public record LocationResponse(
        Long id,
        Long companyId,
        String name,
        String address,
        MunicipalityResponse municipality) {}
