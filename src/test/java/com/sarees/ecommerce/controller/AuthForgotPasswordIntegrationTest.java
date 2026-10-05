package com.sarees.ecommerce.controller;

import tools.jackson.databind.ObjectMapper;
import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.constants.AuthMessages;
import com.sarees.ecommerce.domain.dto.request.ForgotPasswordRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
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
class AuthForgotPasswordIntegrationTest {

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

    @BeforeEach
    void registerUser() throws Exception {
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

    @Test
    void forgotPassword_existingEmail_returnsGenericSuccess() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("subbarao@example.com")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.FORGOT_PASSWORD_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(AuthMessages.FORGOT_PASSWORD_SUCCESS));
    }

    @Test
    void forgotPassword_unknownEmail_returnsSameSuccessResponse() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("unknown@example.com")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.FORGOT_PASSWORD_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(AuthMessages.FORGOT_PASSWORD_SUCCESS));
    }

    @Test
    void forgotPassword_invalidEmail_returnsBadRequest() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.FORGOT_PASSWORD_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
