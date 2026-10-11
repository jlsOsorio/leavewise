package org.altechego.leavewise.user.service.impl;

import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.altechego.leavewise.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final LwUserRepository lwUserRepository;
    private final CompanyRepository companyRepository;
    private final CompanyLocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public boolean createAdminIfAbsent(String email, String rawPassword, String name, Long companyId, Long locationId) {
        var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        var existing = lwUserRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            logExistingUser(existing.get());
            return false;
        }

        var admin = lwUserRepository.save(LwUser.builder()
                .name(name)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .company(companyRepository.getReferenceById(companyId))
                .companyLocation(locationRepository.getReferenceById(locationId))
                .build());
        log.info("Created admin user {}", admin.getId());
        return true;
    }

    private void logExistingUser(LwUser user) {
        if (user.getRole() == Role.ADMIN) {
            log.info("Admin user {} already exists; left unchanged", user.getId());
        } else {
            log.warn("Seed admin email belongs to non-admin user {}; left unchanged", user.getId());
        }
    }
}
