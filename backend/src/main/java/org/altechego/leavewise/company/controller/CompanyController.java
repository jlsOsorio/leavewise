package org.altechego.leavewise.company.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.common.web.SortRestriction;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.service.CompanyService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping(CompanyConstants.BASE_PATH)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<PageResponse<CompanyResponse>> search(
            @RequestParam(required = false) @Size(max = CompanyConstants.NAME_MAX_LENGTH) String q,
            @SortDefault(CompanyConstants.DEFAULT_SORT) Pageable pageable) {

        SortRestriction.requireAllowed(pageable, CompanyConstants.SORTABLE_FIELDS);
        return ResponseEntity.ok(companyService.search(q, pageable));
    }

    @GetMapping(path = "/{id}", version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<CompanyResponse> findById(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long id) {

        return ResponseEntity.ok(companyService.findById(actor, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        var created = companyService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<CompanyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest request) {

        return ResponseEntity.ok(companyService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
