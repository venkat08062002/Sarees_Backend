package com.sarees.ecommerce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        log.info("Password reset email would be sent to {}", email);
    }
}
