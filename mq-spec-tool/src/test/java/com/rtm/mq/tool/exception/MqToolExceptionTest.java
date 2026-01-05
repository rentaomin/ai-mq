package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MqToolException}.
 */
class MqToolExceptionTest {

    @Test
    void testConstructorWithMessageAndExitCode() {
        MqToolException exception = new MqToolException("Test error", ExitCodes.PARSE_ERROR);

        assertEquals("Test error", exception.getMessage());
        assertEquals(ExitCodes.PARSE_ERROR, exception.getExitCode());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageCauseAndExitCode() {
        Throwable cause = new RuntimeException("Root cause");
        MqToolException exception = new MqToolException("Test error", cause, ExitCodes.GENERATION_ERROR);

        assertEquals("Test error", exception.getMessage());
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testExitCodePreserved() {
        MqToolException exception = new MqToolException("Error", ExitCodes.CONFIG_ERROR);
        assertEquals(ExitCodes.CONFIG_ERROR, exception.getExitCode());
    }

    @Test
    void testExceptionChaining() {
        Exception original = new IllegalArgumentException("Original error");
        MqToolException wrapped = new MqToolException("Wrapped error", original, ExitCodes.INTERNAL_ERROR);

        assertSame(original, wrapped.getCause());
        assertEquals("Wrapped error", wrapped.getMessage());
    }

    @Test
    void testIsRuntimeException() {
        MqToolException exception = new MqToolException("Test", ExitCodes.SUCCESS);
        assertTrue(exception instanceof RuntimeException);
    }
}
