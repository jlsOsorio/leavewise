package org.altechego.leavewise.common.security;

public record AuthenticatedUser(Long id, String email, Role role, Long companyId) {}
