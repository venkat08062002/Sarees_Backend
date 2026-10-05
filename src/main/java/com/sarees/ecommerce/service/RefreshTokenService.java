package com.sarees.ecommerce.service;

import com.sarees.ecommerce.config.JwtProperties;
import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.model.RefreshToken;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String createRefreshToken(Long userId) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpirationSeconds());
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenHashUtil.hashToken(rawRefreshToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawRefreshToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawRefreshToken) {
        String tokenHash = TokenHashUtil.hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (refreshToken.isRevoked()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshTokenForUser(String rawRefreshToken, Long userId) {
        String tokenHash = TokenHashUtil.hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!refreshToken.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (refreshToken.isRevoked()) {
            return;
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllRefreshTokensForUser(Long userId) {
        var tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }
}
