package org.altechego.leavewise.auth.service;

import org.altechego.leavewise.auth.dto.LoginRequest;
import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;

public interface AuthService {

    TokenPairResponse register(UserRegistrationRequest request);

    TokenPairResponse login(LoginRequest request);

    TokenPairResponse refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
