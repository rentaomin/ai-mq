package com.rtm.mq.tool.exception;

/**
 * Base exception class for all MQ tool exceptions.
 *
 * <p>All custom exceptions in the MQ tool extend this class, which provides:</p>
 * <ul>
 *   <li>An exit code for CLI integration</li>
 *   <li>Standard exception chaining support</li>
 * </ul>
 *
 * <p>Subclasses should use the appropriate exit code from {@link ExitCodes}.</p>
 *
 * @see ExitCodes
 */
public class MqToolException extends RuntimeException {

    private final int exitCode;

    /**
     * Creates a new MqToolException with a message and exit code.
     *
     * @param message the error message
     * @param exitCode the exit code for CLI integration
     */
    public MqToolException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Creates a new MqToolException with a message, cause, and exit code.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param exitCode the exit code for CLI integration
     */
    public MqToolException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    /**
     * Gets the exit code associated with this exception.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return exitCode;
    }
}
