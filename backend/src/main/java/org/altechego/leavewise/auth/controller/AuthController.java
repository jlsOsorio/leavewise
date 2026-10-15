package org.altechego.leavewise.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.auth.dto.LoginRequest;
import org.altechego.leavewise.auth.dto.RefreshTokenRequest;
import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.auth.service.AuthService;
import org.altechego.leavewise.common.web.ApiMediaType;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping(path = "/registration", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<TokenPairResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping(path = "/login", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(path = "/refresh", version = "1", consumes = ApiMediaType.JSON, produces = ApiMediaType.JSON)
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping(path = "/logout", version = "1", consumes = ApiMediaType.JSON)
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
