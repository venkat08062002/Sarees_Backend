package com.sarees.ecommerce.constants;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    EMAIL_ALREADY_REGISTERED("Email already registered", HttpStatus.CONFLICT),
    PHONE_ALREADY_REGISTERED("Phone number already registered", HttpStatus.CONFLICT),
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
