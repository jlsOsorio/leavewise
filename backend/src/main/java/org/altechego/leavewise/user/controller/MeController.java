package org.altechego.leavewise.user.controller;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.user.dto.MeResponse;
import org.altechego.leavewise.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;


    @GetMapping(version = "1", produces = ApiMediaType.JSON)
    public ResponseEntity<MeResponse> findMe(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(userService.findMe(actor.id()));
    }
}
