package com.rtm.mq.tool.exception;

/**
 * Exception thrown when output operations fail.
 *
 * <p>This exception is used for output-related errors such as:</p>
 * <ul>
 *   <li>File write failures</li>
 *   <li>Directory creation failures</li>
 *   <li>Output path conflicts</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new OutputException("Failed to write file: " + outputPath);
 *
 * // Or with cause:
 * throw new OutputException("Cannot create output directory", ioException);
 * }</pre>
 */
public class OutputException extends MqToolException {

    /**
     * Creates a new OutputException with the specified message.
     *
     * @param message the error message
     */
    public OutputException(String message) {
        super(message, ExitCodes.GENERATION_ERROR);
    }

    /**
     * Creates a new OutputException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public OutputException(String message, Throwable cause) {
        super(message, cause, ExitCodes.GENERATION_ERROR);
    }
}
