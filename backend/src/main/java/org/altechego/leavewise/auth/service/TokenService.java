package org.altechego.leavewise.auth.service;

import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.user.dto.UserResponse;

public interface TokenService {

    TokenPairResponse issue(UserResponse user);

    TokenPairResponse rotate(String rawRefreshToken);

    void revoke(String rawRefreshToken);

    void revokeAll(Long userId);
}
