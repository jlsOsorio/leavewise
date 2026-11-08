package org.altechego.leavewise.municipality.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.common.web.SortRestriction;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.altechego.leavewise.municipality.dto.MunicipalityRequest;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.service.MunicipalityService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(MunicipalityConstants.BASE_PATH)
@RequiredArgsConstructor
public class MunicipalityController {

    private final MunicipalityService municipalityService;


    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<MunicipalityResponse>> search(
            @RequestParam(required = false) @Size(max = MunicipalityConstants.NAME_MAX_LENGTH) String q,
            @SortDefault(MunicipalityConstants.DEFAULT_SORT) Pageable pageable) {

        SortRestriction.requireAllowed(pageable, MunicipalityConstants.SORTABLE_FIELDS);
        return ResponseEntity.ok(municipalityService.search(q, pageable));
    }

    @GetMapping(path = "/{id}", version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<MunicipalityResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(municipalityService.findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<MunicipalityResponse> create(@Valid @RequestBody MunicipalityRequest request) {
        var created = municipalityService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<MunicipalityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MunicipalityRequest request) {

        return ResponseEntity.ok(municipalityService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        municipalityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
