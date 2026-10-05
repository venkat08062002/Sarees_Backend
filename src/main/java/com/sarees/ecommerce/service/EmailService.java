package com.sarees.ecommerce.service;

public interface EmailService {

    void sendPasswordResetEmail(String email, String resetLink);
}
