package com.sarees.ecommerce.interceptors;

import com.sarees.ecommerce.constants.LoggingConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        String correlationId = request.getHeader(LoggingConstants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        MDC.put(LoggingConstants.CORRELATION_ID_MDC_KEY, correlationId);
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

        log.info("Incoming request {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
        long durationMs = startTime != null ? System.currentTimeMillis() - startTime : -1;

        if (ex != null) {
            log.warn(
                    "Request failed {} {} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        } else {
            log.info(
                    "Request completed {} {} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        }

        MDC.remove(LoggingConstants.CORRELATION_ID_MDC_KEY);
    }
}
