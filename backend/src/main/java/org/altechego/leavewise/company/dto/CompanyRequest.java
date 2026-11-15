package org.altechego.leavewise.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.altechego.leavewise.company.constants.CompanyConstants;

public record CompanyRequest(@NotBlank @Size(max = CompanyConstants.NAME_MAX_LENGTH) String name) {

    public CompanyRequest {
        name = name == null ? null : name.trim();
    }
}
