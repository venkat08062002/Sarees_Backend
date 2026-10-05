package com.sarees.ecommerce.service;

import com.sarees.ecommerce.config.PasswordResetProperties;
import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.model.PasswordResetToken;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.repository.PasswordResetTokenRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailService emailService;

    @Transactional
    public void issuePasswordResetToken(User user) {
        invalidateActiveTokensForUser(user.getId());

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        Instant expiresAt = Instant.now().plusSeconds(passwordResetProperties.getPasswordResetExpirationMinutes() * 60);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHashUtil.hashToken(rawToken))
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = buildResetLink(rawToken);
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validatePasswordResetToken(String rawToken) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(TokenHashUtil.hashToken(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        if (resetToken.getUsedAt() != null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_USED);
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        return resetToken;
    }

    @Transactional(readOnly = true)
    public boolean isTokenActive(String rawToken) {
        try {
            validatePasswordResetToken(rawToken);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    @Transactional
    public void markTokenAsUsed(String rawToken) {
        passwordResetTokenRepository.findByTokenHash(TokenHashUtil.hashToken(rawToken))
                .ifPresent(token -> {
                    token.setUsedAt(Instant.now());
                    passwordResetTokenRepository.save(token);
                });
    }

    @Transactional
    public void invalidateActiveTokensForUser(Long userId) {
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findByUserIdAndUsedAtIsNullAndExpiresAtAfter(userId, Instant.now());

        Instant now = Instant.now();
        for (PasswordResetToken token : activeTokens) {
            token.setUsedAt(now);
        }
        passwordResetTokenRepository.saveAll(activeTokens);
    }

    private String buildResetLink(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return passwordResetProperties.getPasswordResetUrl() + "?token=" + encodedToken;
    }
}
