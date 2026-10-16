package org.altechego.leavewise.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthenticatedUserConverterTest {

    private final AuthenticatedUserConverter converter = new AuthenticatedUserConverter();


    @Test
    void convert_accessToken_shouldExposeClaimsAsAuthenticatedUserPrincipal() {
        var authentication = converter.convert(jwt().claim("companyId", 3L).build());

        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(42L, "ana@example.pt", Role.ADMIN, 3L));
        assertThat(authentication.getName()).isEqualTo("42");
    }

    @Test
    void convert_roleClaim_shouldBecomePrefixedAuthority() {
        var authentication = converter.convert(jwt().build());

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_missingCompanyIdClaim_shouldLeaveItNull() {
        var authentication = converter.convert(jwt().build());

        assertThat(((AuthenticatedUser) authentication.getPrincipal()).companyId()).isNull();
    }


    private static Jwt.Builder jwt() {
        var now = Instant.parse("2026-03-10T09:15:30Z");
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("42")
                .claim("email", "ana@example.pt")
                .claim("role", "ADMIN")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900));
    }
}
