package org.altechego.leavewise.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.auth.constants.AuthConstants;
import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.auth.entity.RefreshToken;
import org.altechego.leavewise.auth.exception.AccountDisabledException;
import org.altechego.leavewise.auth.exception.InvalidRefreshTokenException;
import org.altechego.leavewise.auth.repository.RefreshTokenRepository;
import org.altechego.leavewise.auth.service.TokenService;
import org.altechego.leavewise.common.config.JwtConfig;
import org.altechego.leavewise.common.config.JwtProperties;
import org.altechego.leavewise.user.dto.UserResponse;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenServiceImpl implements TokenService {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final RefreshTokenRepository refreshTokenRepository;
    private final LwUserRepository lwUserRepository;
    private final UserMapper userMapper;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();


    @Override
    @Transactional
    public TokenPairResponse issue(UserResponse user) {
        // JWT timestamps have second precision; truncating keeps the response equal to the token's exp
        var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var accessExpiresAt = now.plus(jwtProperties.accessTtl());
        var refreshExpiresAt = now.plus(jwtProperties.refreshTtl());
        var rawRefreshToken = generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(lwUserRepository.getReferenceById(user.id()))
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(toUtc(refreshExpiresAt))
                .build());

        return new TokenPairResponse(
                encodeAccessToken(user, now, accessExpiresAt), accessExpiresAt,
                rawRefreshToken, refreshExpiresAt,
                user);
    }

    // Must commit despite the 401 it throws: a detected reuse revokes all the user's tokens
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPairResponse rotate(String rawRefreshToken) {
        var token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        var now = toUtc(clock.instant());

        if (token.isRevoked()) {
            revokeAllIfReused(token, now);
            throw new InvalidRefreshTokenException();
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (!token.getUser().isActive()) {
            throw new AccountDisabledException();
        }

        // Mapped before the bulk update, which clears the persistence context
        var user = userMapper.toResponse(token.getUser());
        if (refreshTokenRepository.revokeIfActive(token.getId(), now) == 0) {
            throw new InvalidRefreshTokenException(); // a concurrent request rotated it first
        }
        return issue(user);
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.revokeIfActiveByTokenHash(hash(rawRefreshToken), toUtc(clock.instant()));
    }

    @Override
    @Transactional
    public void revokeAll(Long userId) {
        var revoked = refreshTokenRepository.revokeAllActiveByUserId(userId, toUtc(clock.instant()));
        log.info("Revoked {} refresh tokens of user {}", revoked, userId);
    }


    private void revokeAllIfReused(RefreshToken token, LocalDateTime now) {
        if (token.getRevokedAt().plus(AuthConstants.REUSE_GRACE).isBefore(now)) {
            var userId = token.getUser().getId();
            log.warn("Reuse of a revoked refresh token for user {}", userId);
            revokeAll(userId);
        }
    }

    private String encodeAccessToken(UserResponse user, Instant issuedAt, Instant expiresAt) {
        var claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim(AuthConstants.EMAIL_CLAIM, user.email())
                .claim(JwtConfig.ROLE_CLAIM, user.role().name())
                .claim(AuthConstants.COMPANY_ID_CLAIM, user.companyId())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        var header = JwsHeader.with(JwtConfig.SIGNING_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateRefreshToken() {
        var bytes = new byte[AuthConstants.REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(HASH_ALGORITHM + " is required by every JVM", ex);
        }
    }

    private static LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
