package com.sarees.ecommerce.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingEmailServiceTest {

    private ListAppender<ILoggingEvent> logAppender;
    private LoggingEmailService loggingEmailService;

    @BeforeEach
    void setUp() {
        loggingEmailService = new LoggingEmailService();
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingEmailService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingEmailService.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void sendPasswordResetEmail_doesNotLogResetToken() {
        String secretToken = "super-secret-reset-token-value";
        String resetLink = "http://localhost:3000/reset-password?token=" + secretToken;

        loggingEmailService.sendPasswordResetEmail("subbarao@example.com", resetLink);

        assertThat(logAppender.list).hasSize(1);
        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("subbarao@example.com");
        assertThat(logMessage).doesNotContain(secretToken);
        assertThat(logMessage).doesNotContain("token=");
    }
}
