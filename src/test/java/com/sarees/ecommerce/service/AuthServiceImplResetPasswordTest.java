package com.sarees.ecommerce.service;

import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.dto.request.ResetPasswordRequest;
import com.sarees.ecommerce.domain.enums.UserRole;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.PasswordResetToken;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.mappers.UserMapper;
import com.sarees.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplResetPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void resetPassword_revokesAllRefreshTokensForUser() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(1L)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        User user = User.builder()
                .id(1L)
                .email("subbarao@example.com")
                .passwordHash("old-hash")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("reset-token")
                .newPassword("NewSecure1")
                .build();

        when(passwordResetTokenService.validatePasswordResetToken("reset-token")).thenReturn(resetToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecure1")).thenReturn("new-hash");

        authService.resetPassword(request);

        verify(refreshTokenService).revokeAllRefreshTokensForUser(1L);
        verify(passwordResetTokenService).markTokenAsUsed("reset-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("new-hash");
    }
}
