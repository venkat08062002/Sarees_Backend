package com.sarees.ecommerce.controller;

import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.constants.AuthMessages;
import com.sarees.ecommerce.domain.dto.request.ForgotPasswordRequest;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.LogoutRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.request.ResetPasswordRequest;
import com.sarees.ecommerce.domain.dto.response.ApiResponse;
import com.sarees.ecommerce.domain.dto.response.LoginResponse;
import com.sarees.ecommerce.domain.dto.response.LoginUserResponse;
import com.sarees.ecommerce.domain.dto.response.RefreshTokenResponse;
import com.sarees.ecommerce.domain.dto.response.RegisterResponse;
import com.sarees.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiConstants.AUTH_BASE_PATH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApiConstants.REGISTER_PATH)
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Register API called for email={}", request.getEmail().trim().toLowerCase());
        RegisterResponse response = authService.register(request);
        log.info("Register API completed for userId={}", response.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping(ApiConstants.LOGIN_PATH)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login API called for email={}", request.getEmail().trim().toLowerCase());
        LoginResponse response = authService.login(request);
        log.info("Login API completed for userId={}", response.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping(ApiConstants.FORGOT_PASSWORD_PATH)
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password API called");
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(AuthMessages.FORGOT_PASSWORD_SUCCESS, null));
    }

    @PostMapping(ApiConstants.RESET_PASSWORD_PATH)
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password API called");
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(AuthMessages.RESET_PASSWORD_SUCCESS, null));
    }

    @PostMapping(ApiConstants.REFRESH_TOKEN_PATH)
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token API called");
        RefreshTokenResponse response = authService.refreshToken(request);
        log.info("Refresh token API completed");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping(ApiConstants.LOGOUT_PATH)
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LogoutRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        log.info("Logout API called for userId={}", userId);
        authService.logout(userId, request);
        log.info("Logout API completed for userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping(ApiConstants.ME_PATH)
    public ResponseEntity<ApiResponse<LoginUserResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        log.info("Get current user API called for userId={}", userId);
        LoginUserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", response));
    }
}
