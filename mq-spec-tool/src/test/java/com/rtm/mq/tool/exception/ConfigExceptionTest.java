package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigException}.
 */
class ConfigExceptionTest {

    @Test
    void testBasicConstructor() {
        ConfigException exception = new ConfigException("Required property 'output.dir' is not set");

        assertEquals("Required property 'output.dir' is not set", exception.getMessage());
        assertEquals(ExitCodes.CONFIG_ERROR, exception.getExitCode());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new java.io.FileNotFoundException("config.yaml not found");
        ConfigException exception = new ConfigException("Failed to load configuration file", cause);

        assertEquals("Failed to load configuration file", exception.getMessage());
        assertEquals(ExitCodes.CONFIG_ERROR, exception.getExitCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testInheritsMqToolException() {
        ConfigException exception = new ConfigException("Error");
        assertTrue(exception instanceof MqToolException);
    }

    @Test
    void testExceptionChaining() {
        Exception original = new IllegalStateException("Invalid state");
        ConfigException wrapped = new ConfigException("Configuration error", original);

        assertSame(original, wrapped.getCause());
        assertTrue(wrapped.getCause() instanceof IllegalStateException);
    }
}
