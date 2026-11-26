package org.altechego.leavewise.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.schedule.constants.WorkScheduleConstants;
import org.altechego.leavewise.schedule.dto.WorkScheduleRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleResponse;
import org.altechego.leavewise.schedule.service.WorkScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(WorkScheduleConstants.PATH)
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService scheduleService;


    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<WorkScheduleResponse> findByCompany(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long companyId) {

        return ResponseEntity.ok(scheduleService.findByCompany(actor, companyId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<WorkScheduleResponse> upsert(
            @PathVariable Long companyId,
            @Valid @RequestBody WorkScheduleRequest request) {

        return ResponseEntity.ok(scheduleService.upsert(companyId, request));
    }
}
