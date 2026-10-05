package com.sarees.ecommerce.service;

import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.response.RefreshTokenResponse;
import com.sarees.ecommerce.domain.enums.UserRole;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.RefreshToken;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.mappers.UserMapper;
import com.sarees.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRefreshTokenTest {

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

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void refreshToken_success() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .userId(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        User user = activeUser();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(storedToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        RefreshTokenResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
    }

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("bad-token")
                .build();

        when(refreshTokenService.validateRefreshToken("bad-token"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refreshToken_inactiveUser_throwsForbidden() {
        RefreshToken storedToken = RefreshToken.builder()
                .userId(1L)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        User user = activeUser();
        user.setStatus(UserStatus.INACTIVE);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(storedToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    private static User activeUser() {
        return User.builder()
                .id(1L)
                .fullName("Subba Rao")
                .email("subbarao@example.com")
                .phone("9876543210")
                .passwordHash("$2a$10$hash")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
