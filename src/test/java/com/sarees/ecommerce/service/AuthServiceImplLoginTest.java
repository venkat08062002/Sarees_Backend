package com.sarees.ecommerce.service;

import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.response.LoginResponse;
import com.sarees.ecommerce.domain.dto.response.LoginUserResponse;
import com.sarees.ecommerce.domain.enums.UserRole;
import com.sarees.ecommerce.domain.enums.UserStatus;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLoginTest {

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
    void login_success() {
        User user = activeUser();
        LoginRequest request = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("SecurePass1")
                .build();

        when(userRepository.findByEmail("subbarao@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass1", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(userMapper.toLoginUserResponse(user)).thenReturn(
                LoginUserResponse.builder()
                        .id(1L)
                        .fullName("Subba Rao")
                        .email("subbarao@example.com")
                        .phone("9876543210")
                        .role(UserRole.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build());

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
        assertThat(response.getUser().getEmail()).isEqualTo("subbarao@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("missing@example.com")
                .password("SecurePass1")
                .build();

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = activeUser();
        LoginRequest request = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("WrongPass1")
                .build();

        when(userRepository.findByEmail("subbarao@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_inactiveUser_throwsForbidden() {
        User user = activeUser();
        user.setStatus(UserStatus.INACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("SecurePass1")
                .build();

        when(userRepository.findByEmail("subbarao@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_blockedUser_throwsForbidden() {
        User user = activeUser();
        user.setStatus(UserStatus.BLOCKED);

        LoginRequest request = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("SecurePass1")
                .build();

        when(userRepository.findByEmail("subbarao@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_BLOCKED);

        verify(passwordEncoder, never()).matches(any(), any());
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
