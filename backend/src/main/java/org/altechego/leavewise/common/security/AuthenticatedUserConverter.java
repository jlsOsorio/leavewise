package org.altechego.leavewise.common.security;

import org.altechego.leavewise.common.config.JwtConfig;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthenticatedUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter = roleAuthoritiesConverter();


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var user = new AuthenticatedUser(
                Long.valueOf(Objects.requireNonNull(jwt.getSubject())),
                jwt.getClaimAsString(JwtConfig.EMAIL_CLAIM),
                Role.valueOf(jwt.getClaimAsString(JwtConfig.ROLE_CLAIM)),
                toLong(jwt.getClaim(JwtConfig.COMPANY_ID_CLAIM)));
        return new AuthenticatedUserToken(jwt, user, authoritiesConverter.convert(jwt));
    }


    private static JwtGrantedAuthoritiesConverter roleAuthoritiesConverter() {
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName(JwtConfig.ROLE_CLAIM);
        converter.setAuthorityPrefix(JwtConfig.ROLE_PREFIX);
        return converter;
    }

    private static Long toLong(Object claim) {
        return claim instanceof Number number ? number.longValue() : null;
    }
}
