package org.altechego.leavewise.user.service;

public interface UserService {

    boolean createAdminIfAbsent(String email, String rawPassword, String name, Long companyId, Long locationId);
}
