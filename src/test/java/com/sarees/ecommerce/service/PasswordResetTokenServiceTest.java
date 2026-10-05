package com.sarees.ecommerce.service;

import com.sarees.ecommerce.config.PasswordResetProperties;
import com.sarees.ecommerce.domain.enums.UserRole;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.PasswordResetToken;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.repository.PasswordResetTokenRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetTokenService passwordResetTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Subba Rao")
                .email("subbarao@example.com")
                .phone("9876543210")
                .passwordHash("hash")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(passwordResetProperties.getPasswordResetExpirationMinutes()).thenReturn(15L);
        when(passwordResetProperties.getPasswordResetUrl()).thenReturn("http://localhost:3000/reset-password");
        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(eq(1L), any(Instant.class)))
                .thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issuePasswordResetToken_storesHashedTokenWithExpiration() {
        passwordResetTokenService.issuePasswordResetToken(user);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(Duration.ofMinutes(16)));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("subbarao@example.com"), linkCaptor.capture());

        String rawToken = extractTokenFromLink(linkCaptor.getValue());
        assertThat(saved.getTokenHash()).isEqualTo(TokenHashUtil.hashToken(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void issuePasswordResetToken_secondRequestInvalidatesPreviousToken() {
        PasswordResetToken existing = PasswordResetToken.builder()
                .id(5L)
                .userId(1L)
                .tokenHash("old-hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .build();

        when(passwordResetTokenRepository.findByUserIdAndUsedAtIsNullAndExpiresAtAfter(eq(1L), any(Instant.class)))
                .thenReturn(List.of(existing));

        passwordResetTokenService.issuePasswordResetToken(user);

        assertThat(existing.getUsedAt()).isNotNull();
        verify(passwordResetTokenRepository).saveAll(List.of(existing));
    }

    @Test
    void markTokenAsUsed_preventsReuse() {
        String rawToken = "sample-reset-token";
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(1L)
                .tokenHash(TokenHashUtil.hashToken(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(TokenHashUtil.hashToken(rawToken)))
                .thenReturn(Optional.of(token));

        assertThat(passwordResetTokenService.isTokenActive(rawToken)).isTrue();

        passwordResetTokenService.markTokenAsUsed(rawToken);

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(passwordResetTokenService.isTokenActive(rawToken)).isFalse();
    }

    private static String extractTokenFromLink(String resetLink) {
        String tokenParam = resetLink.substring(resetLink.indexOf("token=") + "token=".length());
        return URLDecoder.decode(tokenParam, StandardCharsets.UTF_8);
    }
}
