package org.altechego.leavewise.auth.dto;

import java.time.Instant;

import org.altechego.leavewise.user.dto.UserResponse;

public record TokenPairResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserResponse user) {

    @Override
    public String toString() {
        return "TokenPairResponse[accessTokenExpiresAt=%s, refreshTokenExpiresAt=%s, userId=%s]"
                .formatted(accessTokenExpiresAt, refreshTokenExpiresAt, user == null ? null : user.id());
    }
}
