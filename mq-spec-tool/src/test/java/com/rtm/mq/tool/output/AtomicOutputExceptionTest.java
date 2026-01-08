package com.rtm.mq.tool.output;

import com.rtm.mq.tool.exception.ExitCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AtomicOutputException}.
 */
class AtomicOutputExceptionTest {

    @Test
    void testConstructor_withMessageAndExitCode() {
        AtomicOutputException ex = new AtomicOutputException("test message", 61);
        assertEquals("test message", ex.getMessage());
        assertEquals(61, ex.getExitCode());
    }

    @Test
    void testConstructor_withCause() {
        Exception cause = new RuntimeException("root cause");
        AtomicOutputException ex = new AtomicOutputException("test message", cause, 62);
        assertEquals("test message", ex.getMessage());
        assertEquals(62, ex.getExitCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testOutputNotWritable() {
        AtomicOutputException ex = AtomicOutputException.outputNotWritable("/path/to/dir");
        assertEquals(ExitCodes.OUTPUT_NOT_WRITABLE, ex.getExitCode());
        assertTrue(ex.getMessage().contains("/path/to/dir"));
    }

    @Test
    void testInsufficientDiskSpace() {
        AtomicOutputException ex = AtomicOutputException.insufficientDiskSpace(1000L, 500L);
        assertEquals(ExitCodes.INSUFFICIENT_DISK_SPACE, ex.getExitCode());
        assertTrue(ex.getMessage().contains("1000"));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testConsistencyValidationFailed() {
        AtomicOutputException ex = AtomicOutputException.consistencyValidationFailed("field mismatch");
        assertEquals(ExitCodes.CONSISTENCY_VALIDATION_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("field mismatch"));
    }

    @Test
    void testMessageValidationFailed() {
        AtomicOutputException ex = AtomicOutputException.messageValidationFailed("invalid message");
        assertEquals(ExitCodes.MESSAGE_VALIDATION_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("invalid message"));
    }

    @Test
    void testAtomicCommitFailed() {
        AtomicOutputException ex = AtomicOutputException.atomicCommitFailed("move failed");
        assertEquals(ExitCodes.ATOMIC_COMMIT_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("move failed"));
    }

    @Test
    void testAtomicCommitFailed_withCause() {
        Exception cause = new RuntimeException("root cause");
        AtomicOutputException ex = AtomicOutputException.atomicCommitFailed("move failed", cause);
        assertEquals(ExitCodes.ATOMIC_COMMIT_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("move failed"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testRollbackFailed() {
        AtomicOutputException ex = AtomicOutputException.rollbackFailed("cleanup failed");
        assertEquals(ExitCodes.ROLLBACK_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("cleanup failed"));
    }

    @Test
    void testRollbackFailed_withCause() {
        Exception cause = new RuntimeException("root cause");
        AtomicOutputException ex = AtomicOutputException.rollbackFailed("cleanup failed", cause);
        assertEquals(ExitCodes.ROLLBACK_FAILED, ex.getExitCode());
        assertTrue(ex.getMessage().contains("cleanup failed"));
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testExitCodeValues() {
        // Verify exit codes match T-307 specification
        assertEquals(61, ExitCodes.OUTPUT_NOT_WRITABLE);
        assertEquals(62, ExitCodes.INSUFFICIENT_DISK_SPACE);
        assertEquals(63, ExitCodes.CONSISTENCY_VALIDATION_FAILED);
        assertEquals(64, ExitCodes.MESSAGE_VALIDATION_FAILED);
        assertEquals(65, ExitCodes.ATOMIC_COMMIT_FAILED);
        assertEquals(66, ExitCodes.ROLLBACK_FAILED);
    }
}
