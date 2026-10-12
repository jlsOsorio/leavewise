package org.altechego.leavewise.user.dto;

import org.altechego.leavewise.user.enums.Role;

public record UserResponse(Long id, String name, String email, Role role, Long companyId, Long companyLocationId) {}
