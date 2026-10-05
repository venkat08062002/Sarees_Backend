package com.sarees.ecommerce.mappers;

import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.response.LoginUserResponse;
import com.sarees.ecommerce.domain.dto.response.RegisterResponse;
import com.sarees.ecommerce.domain.enums.UserRole;
import com.sarees.ecommerce.domain.enums.UserStatus;
import com.sarees.ecommerce.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String passwordHash) {
        return User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .passwordHash(passwordHash)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public RegisterResponse toRegisterResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public LoginUserResponse toLoginUserResponse(User user) {
        return LoginUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
