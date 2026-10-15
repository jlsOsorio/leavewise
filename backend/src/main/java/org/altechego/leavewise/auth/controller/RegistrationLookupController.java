package org.altechego.leavewise.auth.controller;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.auth.constants.AuthConstants;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.common.web.SortRestriction;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.service.LocationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/companies")
@RequiredArgsConstructor
public class RegistrationLookupController {

    private final CompanyService companyService;
    private final LocationService locationService;


    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<CompanyResponse>> searchCompanies(
            @RequestParam(required = false) @Size(max = CompanyConstants.NAME_MAX_LENGTH) String q,
            @SortDefault(AuthConstants.LOOKUP_DEFAULT_SORT) Pageable pageable) {

        SortRestriction.requireAllowed(pageable, AuthConstants.LOOKUP_SORTABLE_FIELDS);
        return ResponseEntity.ok(companyService.search(q, pageable));
    }

    @GetMapping(path = "/{companyId}/locations", version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<LocationSummaryResponse>> findCompanyLocations(
            @PathVariable Long companyId,
            @SortDefault(AuthConstants.LOOKUP_DEFAULT_SORT) Pageable pageable) {

        SortRestriction.requireAllowed(pageable, AuthConstants.LOOKUP_SORTABLE_FIELDS);
        return ResponseEntity.ok(locationService.findByCompany(companyId, pageable));
    }
}
