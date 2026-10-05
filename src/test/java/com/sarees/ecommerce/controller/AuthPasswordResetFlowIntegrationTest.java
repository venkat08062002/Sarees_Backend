package com.sarees.ecommerce.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.constants.AuthMessages;
import com.sarees.ecommerce.domain.dto.request.ForgotPasswordRequest;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.request.ResetPasswordRequest;
import com.sarees.ecommerce.repository.PasswordResetTokenRepository;
import com.sarees.ecommerce.repository.RefreshTokenRepository;
import com.sarees.ecommerce.repository.UserRepository;
import com.sarees.ecommerce.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthPasswordResetFlowIntegrationTest {

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

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void cleanDatabase() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullPasswordResetFlow_oldRefreshTokenFailsAfterReset() throws Exception {
        registerUser();

        String oldRefreshToken = loginAndGetRefreshToken("SecurePass1");

        String resetToken = requestPasswordResetToken();

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(resetToken)
                .newPassword("NewSecure1")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.RESET_PASSWORD_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthMessages.RESET_PASSWORD_SUCCESS));

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequest.builder()
                                .email("subbarao@example.com")
                                .password("NewSecure1")
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequest.builder()
                                .email("subbarao@example.com")
                                .password("SecurePass1")
                                .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token revoked"));
    }

    private void registerUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Subba Rao")
                .email("subbarao@example.com")
                .phone("9876543210")
                .password("SecurePass1")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.REGISTER_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken(String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("subbarao@example.com")
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("refreshToken").asText();
    }

    private String requestPasswordResetToken() throws Exception {
        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .email("subbarao@example.com")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.FORGOT_PASSWORD_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("subbarao@example.com"), linkCaptor.capture());

        return extractTokenFromLink(linkCaptor.getValue());
    }

    private static String extractTokenFromLink(String resetLink) {
        String tokenParam = resetLink.substring(resetLink.indexOf("token=") + "token=".length());
        return URLDecoder.decode(tokenParam, StandardCharsets.UTF_8);
    }
}
