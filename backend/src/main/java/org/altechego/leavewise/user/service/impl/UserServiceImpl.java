package org.altechego.leavewise.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.company.exception.CompanyNotFoundException;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.user.constants.UserConstants;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.altechego.leavewise.user.dto.UserResponse;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.exception.EmailTakenException;
import org.altechego.leavewise.user.exception.LocationCompanyMismatchException;
import org.altechego.leavewise.user.exception.NifTakenException;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.altechego.leavewise.user.service.UserService;
import org.altechego.leavewise.user.util.EmailNormalizer;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UserMapper userMapper;


    @Override
    @Transactional
    public boolean createAdminIfAbsent(String email, String rawPassword, String name, Long companyId, Long locationId) {
        var normalizedEmail = EmailNormalizer.normalize(email);

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

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        var email = EmailNormalizer.normalize(request.email());
        var nif = blankToNull(request.nif());

        if (!companyRepository.existsById(request.companyId())) {
            throw new CompanyNotFoundException(request.companyId());
        }
        if (!locationRepository.existsByIdAndCompanyId(request.companyLocationId(), request.companyId())) {
            throw new LocationCompanyMismatchException();
        }
        if (lwUserRepository.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        if (nif != null && lwUserRepository.existsByNif(nif)) {
            throw new NifTakenException();
        }

        var user = saveNewUser(LwUser.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nif(nif)
                .contact(blankToNull(request.contact()))
                .role(Role.USER)
                .active(true)
                .company(companyRepository.getReferenceById(request.companyId()))
                .companyLocation(locationRepository.getReferenceById(request.companyLocationId()))
                .build());
        log.info("Registered user {}", user.getId());
        return userMapper.toResponse(user);
    }

    // A concurrent registration can pass the exists checks; the unique constraints still reject it
    private LwUser saveNewUser(LwUser user) {
        try {
            return lwUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateUserException(ex);
        }
    }

    private static RuntimeException duplicateUserException(DataIntegrityViolationException ex) {
        var constraint = ex.getCause() instanceof ConstraintViolationException violation
                ? violation.getConstraintName()
                : null;
        return switch (constraint) {
            case UserConstants.EMAIL_UNIQUE_CONSTRAINT -> new EmailTakenException();
            case UserConstants.NIF_UNIQUE_CONSTRAINT -> new NifTakenException();
            case null, default -> ex;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void logExistingUser(LwUser user) {
        if (user.getRole() == Role.ADMIN) {
            log.info("Admin user {} already exists; left unchanged", user.getId());
        } else {
            log.warn("Seed admin email belongs to non-admin user {}; left unchanged", user.getId());
        }
    }
}
