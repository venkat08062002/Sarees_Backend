package com.sarees.ecommerce.config;

import com.sarees.ecommerce.constants.ApiConstants;
import com.sarees.ecommerce.exception.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                ApiConstants.AUTH_BASE_PATH + ApiConstants.REGISTER_PATH,
                                ApiConstants.AUTH_BASE_PATH + ApiConstants.LOGIN_PATH,
                                ApiConstants.AUTH_BASE_PATH + ApiConstants.REFRESH_TOKEN_PATH,
                                ApiConstants.AUTH_BASE_PATH + ApiConstants.FORGOT_PASSWORD_PATH,
                                ApiConstants.AUTH_BASE_PATH + ApiConstants.RESET_PASSWORD_PATH)
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(restAuthenticationEntryPoint));

        return http.build();
    }
}
