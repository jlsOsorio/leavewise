package org.altechego.leavewise.user.dto;

import org.altechego.leavewise.common.security.Role;

public record UserResponse(Long id, String name, String email, Role role, Long companyId, Long companyLocationId) {}
