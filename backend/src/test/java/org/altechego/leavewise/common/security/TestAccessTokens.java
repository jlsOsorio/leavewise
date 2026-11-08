package org.altechego.leavewise.common.security;

import java.time.Duration;
import java.time.Instant;

import lombok.experimental.UtilityClass;
import org.altechego.leavewise.common.config.JwtConfig;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@UtilityClass
public class TestAccessTokens {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);


    public static String bearer(JwtEncoder encoder, long userId, Role role, Long companyId) {
        var now = Instant.now();
        return bearer(encoder, userId, role.name(), companyId, now, now.plus(ACCESS_TTL));
    }

    public static String bearer(
            JwtEncoder encoder,
            long userId,
            String role,
            Long companyId,
            Instant issuedAt,
            Instant expiresAt) {

        var claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .claim(JwtConfig.ROLE_CLAIM, role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);
        if (companyId != null) {
            claims.claim(JwtConfig.COMPANY_ID_CLAIM, companyId);
        }
        var header = JwsHeader.with(JwtConfig.SIGNING_ALGORITHM).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
