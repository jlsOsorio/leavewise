package org.altechego.leavewise.auth.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.altechego.leavewise.auth.entity.RefreshToken;
import org.altechego.leavewise.auth.repository.RefreshTokenRepository;
import org.altechego.leavewise.common.config.JwtConfig;
import org.altechego.leavewise.common.config.JwtProperties;
import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.user.dto.UserResponse;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3Rz";
    private static final Instant NOW = Instant.parse("2026-03-10T09:15:30.123Z");
    private static final Instant NOW_SECONDS = Instant.parse("2026-03-10T09:15:30Z");
    private static final LocalDateTime NOW_UTC = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final long USER_ID = 42L;
    private static final long TOKEN_ID = 7L;
    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final UserResponse USER =
            new UserResponse(USER_ID, "Ana", "ana@example.pt", Role.USER, 3L, 11L);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LwUserRepository lwUserRepository;

    private SecretKey signingKey;

    private TokenServiceImpl tokenService;


    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
        var jwtConfig = new JwtConfig();
        signingKey = jwtConfig.jwtSigningKey(properties);
        tokenService = new TokenServiceImpl(
                refreshTokenRepository,
                lwUserRepository,
                Mappers.getMapper(UserMapper.class),
                jwtConfig.jwtEncoder(signingKey),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issue_user_shouldSignAccessTokenWithUserClaims() throws Exception {
        stubUserReference();

        var pair = tokenService.issue(USER);

        var jwt = SignedJWT.parse(pair.accessToken());
        var claims = jwt.getJWTClaimsSet();
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(jwt.verify(new MACVerifier(signingKey))).isTrue();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getStringClaim("email")).isEqualTo("ana@example.pt");
        assertThat(claims.getStringClaim("role")).isEqualTo("USER");
        assertThat(claims.getLongClaim("companyId")).isEqualTo(3L);
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(NOW_SECONDS);
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(pair.accessTokenExpiresAt());
        assertThat(pair.accessTokenExpiresAt()).isEqualTo(NOW_SECONDS.plus(Duration.ofMinutes(15)));
        assertThat(pair.user()).isEqualTo(USER);
    }

    @Test
    void issue_user_shouldStoreOnlyTheRefreshTokenHash() {
        var userReference = stubUserReference();

        var pair = tokenService.issue(USER);

        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256Hex(pair.refreshToken()));
        assertThat(saved.getValue().getUser()).isSameAs(userReference);
        assertThat(saved.getValue().getExpiresAt())
                .isEqualTo(LocalDateTime.ofInstant(pair.refreshTokenExpiresAt(), ZoneOffset.UTC));
        assertThat(pair.refreshTokenExpiresAt()).isEqualTo(NOW_SECONDS.plus(Duration.ofDays(7)));
    }

    @Test
    void issue_calledTwice_shouldGenerateDistinctUrlSafeRefreshTokens() {
        stubUserReference();

        var first = tokenService.issue(USER).refreshToken();
        var second = tokenService.issue(USER).refreshToken();

        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void rotate_activeToken_shouldRevokeItAndIssueNewPair() {
        stubStoredToken(activeToken(activeUser()));
        when(refreshTokenRepository.revokeIfActive(TOKEN_ID, NOW_UTC)).thenReturn(1);
        stubUserReference();

        var pair = tokenService.rotate(RAW_TOKEN);

        assertThat(pair.refreshToken()).isNotEqualTo(RAW_TOKEN);
        assertThat(pair.user()).isEqualTo(USER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_unknownToken_shouldThrowInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(sha256Hex(RAW_TOKEN))).thenReturn(Optional.empty());

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rotate_tokenExpiringNow_shouldThrowInvalidRefreshToken() {
        var token = activeToken(activeUser());
        token.setExpiresAt(NOW_UTC);
        stubStoredToken(token);

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).revokeIfActive(anyLong(), any());
    }

    // Exactly at the 10 s edge still counts as a concurrent refresh, not as theft
    @Test
    void rotate_revokedWithinGraceWindow_shouldRejectWithoutRevokingOthers() {
        var token = activeToken(activeUser());
        token.setRevokedAt(NOW_UTC.minusSeconds(10));
        stubStoredToken(token);

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(anyLong(), any());
    }

    @Test
    void rotate_revokedAfterGraceWindow_shouldRevokeAllUserTokens() {
        var token = activeToken(activeUser());
        token.setRevokedAt(NOW_UTC.minusSeconds(11));
        stubStoredToken(token);

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository).revokeAllActiveByUserId(USER_ID, NOW_UTC);
    }

    @Test
    void rotate_inactiveUser_shouldThrowAccountDisabled() {
        var user = activeUser();
        user.setActive(false);
        stubStoredToken(activeToken(user));

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.ACCOUNT_DISABLED);
        verify(refreshTokenRepository, never()).revokeIfActive(anyLong(), any());
    }

    @Test
    void rotate_concurrentRotationWonFirst_shouldThrowWithoutIssuing() {
        stubStoredToken(activeToken(activeUser()));
        when(refreshTokenRepository.revokeIfActive(TOKEN_ID, NOW_UTC)).thenReturn(0);

        assertErrorCode(() -> tokenService.rotate(RAW_TOKEN), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revoke_rawToken_shouldRevokeItsHash() {
        tokenService.revoke(RAW_TOKEN);

        verify(refreshTokenRepository).revokeIfActiveByTokenHash(sha256Hex(RAW_TOKEN), NOW_UTC);
    }

    @Test
    void revokeAll_userId_shouldRevokeAllActiveTokensOfUser() {
        tokenService.revokeAll(USER_ID);

        verify(refreshTokenRepository).revokeAllActiveByUserId(USER_ID, NOW_UTC);
    }


    private LwUser stubUserReference() {
        var reference = LwUser.builder().id(USER_ID).build();
        when(lwUserRepository.getReferenceById(USER_ID)).thenReturn(reference);
        return reference;
    }

    private void stubStoredToken(RefreshToken token) {
        when(refreshTokenRepository.findByTokenHash(sha256Hex(RAW_TOKEN))).thenReturn(Optional.of(token));
    }

    private static LwUser activeUser() {
        return LwUser.builder()
                .id(USER_ID)
                .name("Ana")
                .email("ana@example.pt")
                .role(Role.USER)
                .company(Company.builder().id(3L).build())
                .companyLocation(CompanyLocation.builder().id(11L).build())
                .active(true)
                .build();
    }

    private static RefreshToken activeToken(LwUser user) {
        return RefreshToken.builder()
                .id(TOKEN_ID)
                .user(user)
                .tokenHash(sha256Hex(RAW_TOKEN))
                .expiresAt(NOW_UTC.plusDays(1))
                .build();
    }

    private static String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
