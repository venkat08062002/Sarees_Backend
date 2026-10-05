package com.sarees.ecommerce.service;

import com.sarees.ecommerce.config.JwtProperties;
import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.model.RefreshToken;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceLogoutTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void revokeRefreshTokenForUser_success() {
        String raw = "user-refresh-token";
        RefreshToken token = RefreshToken.builder()
                .userId(1L)
                .tokenHash(TokenHashUtil.hashToken(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(raw)))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revokeRefreshTokenForUser(raw, 1L);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeRefreshTokenForUser_alreadyRevoked_isIdempotent() {
        String raw = "user-refresh-token";
        RefreshToken token = RefreshToken.builder()
                .userId(1L)
                .tokenHash(TokenHashUtil.hashToken(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(raw)))
                .thenReturn(Optional.of(token));

        refreshTokenService.revokeRefreshTokenForUser(raw, 1L);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeRefreshTokenForUser_wrongUser_throwsInvalidRefreshToken() {
        String raw = "user-refresh-token";
        RefreshToken token = RefreshToken.builder()
                .userId(2L)
                .tokenHash(TokenHashUtil.hashToken(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(raw)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshTokenForUser(raw, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void revokeRefreshTokenForUser_unknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.revokeRefreshTokenForUser("missing", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
