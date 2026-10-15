package org.altechego.leavewise.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.auth.dto.LoginRequest;
import org.altechego.leavewise.auth.dto.TokenPairResponse;
import org.altechego.leavewise.auth.exception.AccountDisabledException;
import org.altechego.leavewise.auth.exception.InvalidCredentialsException;
import org.altechego.leavewise.auth.exception.InvalidRefreshTokenException;
import org.altechego.leavewise.auth.service.AuthService;
import org.altechego.leavewise.auth.service.TokenService;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.altechego.leavewise.user.service.UserService;
import org.altechego.leavewise.user.util.EmailNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final LwUserRepository lwUserRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public TokenPairResponse register(UserRegistrationRequest request) {
        return tokenService.issue(userService.register(request));
    }

    @Override
    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        var user = lwUserRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }
        return tokenService.issue(userMapper.toResponse(user));
    }

    // Same noRollbackFor as TokenService.rotate, or this outer transaction would undo its revoke-all
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPairResponse refresh(String rawRefreshToken) {
        return tokenService.rotate(rawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }
}
