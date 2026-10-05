package com.sarees.ecommerce.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.LogoutRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.repository.UserRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthLogoutIntegrationTest {

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
    void logout_success() throws Exception {
        LoginTokens tokens = registerLoginUser("subbarao@example.com", "9876543210");

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken(tokens.refreshToken())
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void logout_withoutAccessToken_returnsUnauthorized() throws Exception {
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken("any-refresh-token")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void logout_invalidRefreshToken_returnsUnauthorized() throws Exception {
        LoginTokens tokens = registerLoginUser("invalid-refresh@example.com", "9876543211");

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken("not-a-valid-refresh-token")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void logout_refreshTokenBelongsToAnotherUser_returnsUnauthorized() throws Exception {
        LoginTokens userOne = registerLoginUser("user1@example.com", "9876543212");
        LoginTokens userTwo = registerLoginUser("user2@example.com", "9876543213");

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken(userOne.refreshToken())
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + userTwo.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void logout_alreadyRevokedToken_returnsSuccess() throws Exception {
        LoginTokens tokens = registerLoginUser("revoked@example.com", "9876543214");

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken(tokens.refreshToken())
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void logout_thenRefreshTokenFails() throws Exception {
        LoginTokens tokens = registerLoginUser("flow@example.com", "9876543215");

        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken(tokens.refreshToken())
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGOUT_PATH)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(tokens.refreshToken())
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token revoked"));
    }

    private LoginTokens registerLoginUser(String email, String phone) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Subba Rao")
                .email(email)
                .phone(phone)
                .password("SecurePass1")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REGISTER_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("SecurePass1")
                .build();

        MvcResult loginResult = mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("data").get("accessToken").asText();
        String refreshToken = loginBody.get("data").get("refreshToken").asText();
        return new LoginTokens(accessToken, refreshToken);
    }

    private record LoginTokens(String accessToken, String refreshToken) {
    }
}
