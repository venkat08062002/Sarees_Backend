package com.sarees.ecommerce.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.repository.UserRepository;
import com.sarees.ecommerce.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthRefreshTokenIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sarees_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void refreshToken_validToken_returnsNewAccessToken() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void refreshToken_newAccessTokenWorksWithMeEndpoint() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = body.get("data").get("accessToken").asText();

        mockMvc.perform(get(ApiConstants.AUTH_BASE_PATH + ApiConstants.ME_PATH)
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("subbarao@example.com"));
    }

    @Test
    void refreshToken_invalidToken_returnsUnauthorized() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("not-a-valid-refresh-token-value")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refreshToken_expiredToken_returnsUnauthorized() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();
        expireRefreshToken(refreshToken);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token expired"));
    }

    @Test
    void refreshToken_revokedToken_returnsUnauthorized() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();
        revokeRefreshToken(refreshToken);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token revoked"));
    }

    @Test
    void refreshToken_inactiveUser_returnsForbidden() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();
        User user = userRepository.findByEmail("subbarao@example.com").orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is inactive"));
    }

    @Test
    void refreshToken_blockedUser_returnsForbidden() throws Exception {
        String refreshToken = registerAndLoginRefreshToken();
        User user = userRepository.findByEmail("subbarao@example.com").orElseThrow();
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is blocked"));
    }

    private String registerAndLoginRefreshToken() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Subba Rao")
                .email("subbarao@example.com")
                .phone("9876543210")
                .password("SecurePass1")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REGISTER_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("SecurePass1")
                .build();

        MvcResult loginResult = mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginBody.get("data").get("refreshToken").asText();
    }

    private void expireRefreshToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setExpiresAt(Instant.now().minusSeconds(60));
                    refreshTokenRepository.save(token);
                });
    }

    private void revokeRefreshToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHashUtil.hashToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}
