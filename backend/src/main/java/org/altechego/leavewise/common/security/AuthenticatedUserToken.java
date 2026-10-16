package org.altechego.leavewise.common.security;

import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthenticatedUserToken extends JwtAuthenticationToken {

    private final AuthenticatedUser user;


    public AuthenticatedUserToken(Jwt jwt, AuthenticatedUser user, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities, Objects.requireNonNull(jwt.getSubject()));
        this.user = user;
    }


    @Override
    public AuthenticatedUser getPrincipal() {
        return user;
    }
}
