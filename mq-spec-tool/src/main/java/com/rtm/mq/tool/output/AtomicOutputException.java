package com.rtm.mq.tool.output;

import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.exception.MqToolException;

/**
 * Exception thrown when atomic output operations fail.
 *
 * <p>This exception uses specific exit codes defined in T-307:</p>
 * <ul>
 *   <li>61 - OUTPUT_NOT_WRITABLE</li>
 *   <li>62 - INSUFFICIENT_DISK_SPACE</li>
 *   <li>63 - CONSISTENCY_VALIDATION_FAILED</li>
 *   <li>64 - MESSAGE_VALIDATION_FAILED</li>
 *   <li>65 - ATOMIC_COMMIT_FAILED</li>
 *   <li>66 - ROLLBACK_FAILED</li>
 * </ul>
 */
public class AtomicOutputException extends MqToolException {

    /**
     * Creates a new AtomicOutputException with the specified message and exit code.
     *
     * @param message the error message
     * @param exitCode the exit code (should be one of the T-307 codes: 61-66)
     */
    public AtomicOutputException(String message, int exitCode) {
        super(message, exitCode);
    }

    /**
     * Creates a new AtomicOutputException with a message, cause, and exit code.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param exitCode the exit code (should be one of the T-307 codes: 61-66)
     */
    public AtomicOutputException(String message, Throwable cause, int exitCode) {
        super(message, cause, exitCode);
    }

    /**
     * Creates an exception for output directory not writable.
     *
     * @param path the path that is not writable
     * @return a new AtomicOutputException with exit code 61
     */
    public static AtomicOutputException outputNotWritable(String path) {
        return new AtomicOutputException(
                "Output directory is not writable: " + path,
                ExitCodes.OUTPUT_NOT_WRITABLE);
    }

    /**
     * Creates an exception for insufficient disk space.
     *
     * @param requiredBytes the required space in bytes
     * @param availableBytes the available space in bytes
     * @return a new AtomicOutputException with exit code 62
     */
    public static AtomicOutputException insufficientDiskSpace(long requiredBytes, long availableBytes) {
        return new AtomicOutputException(
                String.format("Insufficient disk space: required %d bytes, available %d bytes",
                        requiredBytes, availableBytes),
                ExitCodes.INSUFFICIENT_DISK_SPACE);
    }

    /**
     * Creates an exception for consistency validation failure.
     *
     * @param reason the reason for failure
     * @return a new AtomicOutputException with exit code 63
     */
    public static AtomicOutputException consistencyValidationFailed(String reason) {
        return new AtomicOutputException(
                "Consistency validation failed: " + reason,
                ExitCodes.CONSISTENCY_VALIDATION_FAILED);
    }

    /**
     * Creates an exception for message validation failure.
     *
     * @param reason the reason for failure
     * @return a new AtomicOutputException with exit code 64
     */
    public static AtomicOutputException messageValidationFailed(String reason) {
        return new AtomicOutputException(
                "Message validation failed: " + reason,
                ExitCodes.MESSAGE_VALIDATION_FAILED);
    }

    /**
     * Creates an exception for atomic commit failure.
     *
     * @param reason the reason for failure
     * @return a new AtomicOutputException with exit code 65
     */
    public static AtomicOutputException atomicCommitFailed(String reason) {
        return new AtomicOutputException(
                "Atomic commit failed: " + reason,
                ExitCodes.ATOMIC_COMMIT_FAILED);
    }

    /**
     * Creates an exception for atomic commit failure with cause.
     *
     * @param reason the reason for failure
     * @param cause the underlying cause
     * @return a new AtomicOutputException with exit code 65
     */
    public static AtomicOutputException atomicCommitFailed(String reason, Throwable cause) {
        return new AtomicOutputException(
                "Atomic commit failed: " + reason,
                cause,
                ExitCodes.ATOMIC_COMMIT_FAILED);
    }

    /**
     * Creates an exception for rollback failure.
     *
     * @param reason the reason for failure
     * @return a new AtomicOutputException with exit code 66
     */
    public static AtomicOutputException rollbackFailed(String reason) {
        return new AtomicOutputException(
                "Rollback failed: " + reason,
                ExitCodes.ROLLBACK_FAILED);
    }

    /**
     * Creates an exception for rollback failure with cause.
     *
     * @param reason the reason for failure
     * @param cause the underlying cause
     * @return a new AtomicOutputException with exit code 66
     */
    public static AtomicOutputException rollbackFailed(String reason, Throwable cause) {
        return new AtomicOutputException(
                "Rollback failed: " + reason,
                cause,
                ExitCodes.ROLLBACK_FAILED);
    }
}
