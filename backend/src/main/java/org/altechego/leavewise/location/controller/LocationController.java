package org.altechego.leavewise.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.common.web.SortRestriction;
import org.altechego.leavewise.location.constants.LocationConstants;
import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.service.LocationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LocationController {

    private final LocationService locationService;


    @GetMapping(path = LocationConstants.COMPANY_LOCATIONS_PATH, version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<LocationResponse>> findByCompany(
            @PathVariable Long companyId,
            @SortDefault(LocationConstants.DEFAULT_SORT) Pageable pageable) {

        SortRestriction.requireAllowed(pageable, LocationConstants.SORTABLE_FIELDS);
        return ResponseEntity.ok(locationService.findDetailsByCompany(companyId, pageable));
    }

    @PostMapping(
            path = LocationConstants.COMPANY_LOCATIONS_PATH,
            version = "1",
            consumes = ApiMediaType.JSON,
            produces = ApiMediaType.JSON)
    public ResponseEntity<LocationResponse> create(
            @PathVariable Long companyId,
            @Valid @RequestBody LocationRequest request) {

        var created = locationService.create(companyId, request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(LocationConstants.BASE_PATH + "/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping(path = LocationConstants.BASE_PATH + "/{id}", version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<LocationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PutMapping(
            path = LocationConstants.BASE_PATH + "/{id}",
            version = "1",
            consumes = ApiMediaType.JSON,
            produces = ApiMediaType.JSON)
    public ResponseEntity<LocationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {

        return ResponseEntity.ok(locationService.update(id, request));
    }

    @DeleteMapping(path = LocationConstants.BASE_PATH + "/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
