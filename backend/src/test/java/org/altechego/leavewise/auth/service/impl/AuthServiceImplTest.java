package org.altechego.leavewise.auth.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.altechego.leavewise.auth.dto.LoginRequest;
import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.auth.service.TokenService;
import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.altechego.leavewise.user.dto.UserResponse;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.altechego.leavewise.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "ana@example.pt";
    private static final String PASSWORD = "secret-password";
    private static final String PASSWORD_HASH = "{bcrypt}hash";
    private static final UserResponse USER = new UserResponse(42L, "Ana", EMAIL, Role.USER, 3L, 11L);
    private static final TokenPairResponse PAIR =
            new TokenPairResponse("access", Instant.EPOCH, "refresh", Instant.EPOCH, USER);

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private LwUserRepository lwUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private AuthServiceImpl authService;


    @Test
    void register_validRequest_shouldIssueTokensForCreatedUser() {
        var request = new UserRegistrationRequest("Ana", EMAIL, PASSWORD, null, null, 3L, 11L);
        when(userService.register(request)).thenReturn(USER);
        when(tokenService.issue(USER)).thenReturn(PAIR);

        assertThat(authService.register(request)).isSameAs(PAIR);
    }

    @Test
    void login_validCredentials_shouldIssueTokens() {
        when(lwUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedUser(true)));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenService.issue(USER)).thenReturn(PAIR);

        assertThat(authService.login(new LoginRequest(EMAIL, PASSWORD))).isSameAs(PAIR);
    }

    // Only the lookup matters here; the unstubbed repository finds nobody, so the login itself fails
    @Test
    void login_emailWithSpacesAndUppercase_shouldLookUpNormalizedEmail() {
        catchThrowable(() -> authService.login(new LoginRequest(" Ana@Example.PT ", PASSWORD)));

        verify(lwUserRepository).findByEmail(EMAIL);
    }

    @Test
    void login_unknownEmail_shouldThrowInvalidCredentials() {
        when(lwUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertErrorCode(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)), ErrorCode.INVALID_CREDENTIALS);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_wrongPassword_shouldThrowInvalidCredentials() {
        when(lwUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedUser(true)));
        when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

        assertErrorCode(
                () -> authService.login(new LoginRequest(EMAIL, "wrong-password")), ErrorCode.INVALID_CREDENTIALS);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_inactiveUserWithCorrectPassword_shouldThrowAccountDisabled() {
        when(lwUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedUser(false)));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertErrorCode(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)), ErrorCode.ACCOUNT_DISABLED);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void refresh_rawToken_shouldRotateIt() {
        when(tokenService.rotate("refresh")).thenReturn(PAIR);

        assertThat(authService.refresh("refresh")).isSameAs(PAIR);
    }

    @Test
    void logout_rawToken_shouldRevokeIt() {
        authService.logout("refresh");

        verify(tokenService).revoke("refresh");
    }


    private static LwUser storedUser(boolean active) {
        return LwUser.builder()
                .id(42L)
                .name("Ana")
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .role(Role.USER)
                .company(Company.builder().id(3L).build())
                .companyLocation(CompanyLocation.builder().id(11L).build())
                .active(active)
                .build();
    }
}
