package com.sarees.ecommerce.constants;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    EMAIL_ALREADY_REGISTERED("Email already registered", HttpStatus.CONFLICT),
    PHONE_ALREADY_REGISTERED("Phone number already registered", HttpStatus.CONFLICT),
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS("Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_INACTIVE("Account is inactive", HttpStatus.FORBIDDEN),
    ACCOUNT_BLOCKED("Account is blocked", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN("Invalid refresh token", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("Refresh token expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("Refresh token revoked", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD_RESET_TOKEN("Invalid password reset token", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_TOKEN_EXPIRED("Password reset token expired", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_TOKEN_USED("Password reset token already used", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
