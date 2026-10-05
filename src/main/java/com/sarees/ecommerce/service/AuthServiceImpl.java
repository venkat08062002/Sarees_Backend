package com.sarees.ecommerce.service;

import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.constants.JwtConstants;
import com.sarees.ecommerce.domain.dto.request.ForgotPasswordRequest;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.LogoutRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.request.ResetPasswordRequest;
import com.sarees.ecommerce.domain.model.PasswordResetToken;
import com.sarees.ecommerce.domain.dto.response.LoginResponse;
import com.sarees.ecommerce.domain.dto.response.LoginUserResponse;
import com.sarees.ecommerce.domain.dto.response.RefreshTokenResponse;
import com.sarees.ecommerce.domain.dto.response.RegisterResponse;
import com.sarees.ecommerce.domain.model.RefreshToken;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.mappers.UserMapper;
import com.sarees.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail().trim().toLowerCase());

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (userRepository.existsByEmail(email)) {
            log.warn("Duplicate email registration attempt: {}", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        if (userRepository.existsByPhone(phone)) {
            log.warn("Duplicate phone registration attempt: {}", phone);
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, passwordHash);
        User savedUser = userRepository.save(user);

        log.info("Registration successful for user id: {}", savedUser.getId());
        return userMapper.toRegisterResponse(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Login request received for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed for email: {}", email);
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login denied for inactive account: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Login denied for blocked account: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed for email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("Login successful for user id: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(JwtConstants.TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenService.getAccessTokenExpirationSeconds())
                .user(userMapper.toLoginUserResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginUserResponse getCurrentUser(Long userId) {
        log.info("Fetching current user details for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Current user not found for user id: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        return userMapper.toLoginUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> {
                    log.warn("User not found for refresh token, user id: {}", refreshToken.getUserId());
                    return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                });

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Refresh token denied for inactive account, user id: {}", user.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Refresh token denied for blocked account, user id: {}", user.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
        }

        String accessToken = jwtTokenService.generateAccessToken(user);

        log.info("Refresh token successful for user id: {}", user.getId());

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType(JwtConstants.TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        log.info("Logout request received for user id: {}", userId);
        refreshTokenService.revokeRefreshTokenForUser(request.getRefreshToken(), userId);
        log.info("Logout successful for user id: {}", userId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request received");

        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .ifPresent(passwordResetTokenService::issuePasswordResetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request received");

        PasswordResetToken resetToken = passwordResetTokenService.validatePasswordResetToken(request.getToken());

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Reset password denied for inactive account, user id: {}", user.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Reset password denied for blocked account, user id: {}", user.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenService.markTokenAsUsed(request.getToken());
        refreshTokenService.revokeAllRefreshTokensForUser(user.getId());

        log.info("Reset password successful for user id: {}", user.getId());
    }
}
