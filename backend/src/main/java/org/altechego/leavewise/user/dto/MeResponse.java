package org.altechego.leavewise.user.dto;

import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;

public record MeResponse(
        Long id,
        String name,
        String email,
        String nif,
        String contact,
        Role role,
        CompanyResponse company,
        LocationSummaryResponse companyLocation) {

    @Override
    public String toString() {
        return "MeResponse[id=%s, role=%s]".formatted(id, role);
    }
}
