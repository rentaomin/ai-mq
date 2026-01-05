package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OutputException}.
 */
class OutputExceptionTest {

    @Test
    void testBasicConstructor() {
        OutputException exception = new OutputException("Failed to write file: output.xml");

        assertEquals("Failed to write file: output.xml", exception.getMessage());
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new java.io.IOException("Disk full");
        OutputException exception = new OutputException("Cannot create output directory", cause);

        assertEquals("Cannot create output directory", exception.getMessage());
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testInheritsMqToolException() {
        OutputException exception = new OutputException("Error");
        assertTrue(exception instanceof MqToolException);
    }

    @Test
    void testUsesGenerationErrorExitCode() {
        // OutputException uses GENERATION_ERROR as per spec
        OutputException exception = new OutputException("Error");
        assertEquals(ExitCodes.GENERATION_ERROR, exception.getExitCode());
    }

    @Test
    void testExceptionChaining() {
        Exception original = new SecurityException("Access denied");
        OutputException wrapped = new OutputException("Output failed", original);

        assertSame(original, wrapped.getCause());
        assertTrue(wrapped.getCause() instanceof SecurityException);
    }
}
