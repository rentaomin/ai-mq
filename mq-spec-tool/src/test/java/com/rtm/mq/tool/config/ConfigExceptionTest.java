package com.rtm.mq.tool.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigException Tests")
class ConfigExceptionTest {

    @Test
    @DisplayName("should create exception with message only")
    void createWithMessageOnly() {
        // Given
        String message = "Test error message";

        // When
        ConfigException exception = new ConfigException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void createWithMessageAndCause() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        // When
        ConfigException exception = new ConfigException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("ConfigException should be a RuntimeException")
    void isRuntimeException() {
        // Given
        ConfigException exception = new ConfigException("test");

        // Then
        assertTrue(exception instanceof RuntimeException);
    }
}
