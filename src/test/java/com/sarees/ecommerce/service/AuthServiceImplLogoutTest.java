package com.sarees.ecommerce.service;

import com.sarees.ecommerce.domain.dto.request.LogoutRequest;
import com.sarees.ecommerce.mappers.UserMapper;
import com.sarees.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplLogoutTest {

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
    void logout_delegatesToRefreshTokenService() {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("refresh-token-value")
                .build();

        authService.logout(1L, request);

        verify(refreshTokenService).revokeRefreshTokenForUser("refresh-token-value", 1L);
    }
}
