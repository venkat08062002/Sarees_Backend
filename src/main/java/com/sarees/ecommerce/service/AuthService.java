package com.sarees.ecommerce.service;

import com.sarees.ecommerce.domain.dto.request.ForgotPasswordRequest;
import com.sarees.ecommerce.domain.dto.request.LoginRequest;
import com.sarees.ecommerce.domain.dto.request.LogoutRequest;
import com.sarees.ecommerce.domain.dto.request.RefreshTokenRequest;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.request.ResetPasswordRequest;
import com.sarees.ecommerce.domain.dto.response.LoginResponse;
import com.sarees.ecommerce.domain.dto.response.LoginUserResponse;
import com.sarees.ecommerce.domain.dto.response.RefreshTokenResponse;
import com.sarees.ecommerce.domain.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginUserResponse getCurrentUser(Long userId);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(Long userId, LogoutRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
