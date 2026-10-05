package com.sarees.ecommerce.service;

import com.sarees.ecommerce.config.JwtProperties;
import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.model.RefreshToken;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_persistsHashNotRawToken() {
        when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(1L);

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(TokenHashUtil.hashToken(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void validateRefreshToken_expired_throwsExpired() {
        String raw = "sample-refresh-token";
        RefreshToken token = RefreshToken.builder()
                .userId(1L)
                .tokenHash(TokenHashUtil.hashToken(raw))
                .expiresAt(Instant.now().minusSeconds(10))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(raw)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(raw))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void validateRefreshToken_revoked_throwsRevoked() {
        String raw = "sample-refresh-token";
        RefreshToken token = RefreshToken.builder()
                .userId(1L)
                .tokenHash(TokenHashUtil.hashToken(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(raw)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(raw))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED);
    }
}
