package org.altechego.leavewise.user.service;

import org.altechego.leavewise.user.dto.MeResponse;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.altechego.leavewise.user.dto.UserResponse;

public interface UserService {

    boolean createAdminIfAbsent(String email, String rawPassword, String name, Long companyId, Long locationId);

    UserResponse register(UserRegistrationRequest request);

    MeResponse findMe(Long userId);
}
