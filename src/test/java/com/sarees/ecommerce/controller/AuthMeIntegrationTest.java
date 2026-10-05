package com.sarees.ecommerce.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthMeIntegrationTest {

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
    void cleanAndRegisterUser() throws Exception {
        registerUser();
    }

    @Test
    void getCurrentUser_withValidToken_returnsUserDetails() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get(ApiConstants.AUTH_BASE_PATH + ApiConstants.ME_PATH)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User details retrieved successfully"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.fullName").value("Subba Rao"))
                .andExpect(jsonPath("$.data.email").value("subbarao@example.com"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentUser_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(ApiConstants.AUTH_BASE_PATH + ApiConstants.ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void getCurrentUser_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(ApiConstants.AUTH_BASE_PATH + ApiConstants.ME_PATH)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void getCurrentUser_doesNotExposePasswordFields() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get(ApiConstants.AUTH_BASE_PATH + ApiConstants.ME_PATH)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password", not("SecurePass1")));
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

    private String loginAndGetAccessToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("subbarao@example.com")
                .password("SecurePass1")
                .build();

        MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("accessToken").asText();
    }
}
