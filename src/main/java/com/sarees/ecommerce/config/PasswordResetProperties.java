package com.sarees.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class PasswordResetProperties {

    private String passwordResetUrl = "http://localhost:3000/reset-password";
    private long passwordResetExpirationMinutes = 15;
}
