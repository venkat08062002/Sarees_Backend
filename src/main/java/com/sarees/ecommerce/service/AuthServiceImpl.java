package com.sarees.ecommerce.service;

import com.sarees.ecommerce.constants.ErrorCode;
import com.sarees.ecommerce.domain.dto.request.RegisterRequest;
import com.sarees.ecommerce.domain.dto.response.RegisterResponse;
import com.sarees.ecommerce.domain.model.User;
import com.sarees.ecommerce.exception.BusinessException;
import com.sarees.ecommerce.mappers.UserMapper;
import com.sarees.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail().trim().toLowerCase());

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (userRepository.existsByEmail(email)) {
            log.warn("Duplicate email registration attempt: {}", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        if (userRepository.existsByPhone(phone)) {
            log.warn("Duplicate phone registration attempt: {}", phone);
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, passwordHash);
        User savedUser = userRepository.save(user);

        log.info("Registration successful for user id: {}", savedUser.getId());
        return userMapper.toRegisterResponse(savedUser);
    }
}
