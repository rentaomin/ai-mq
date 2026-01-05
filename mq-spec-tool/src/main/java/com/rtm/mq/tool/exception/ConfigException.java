package com.rtm.mq.tool.exception;

/**
 * Exception thrown when configuration is invalid or missing.
 *
 * <p>This exception is used for configuration-related errors such as:</p>
 * <ul>
 *   <li>Missing required configuration files</li>
 *   <li>Invalid configuration values</li>
 *   <li>Malformed configuration syntax</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new ConfigException("Required property 'output.dir' is not set");
 *
 * // Or with cause:
 * throw new ConfigException("Failed to load configuration file", ioException);
 * }</pre>
 */
public class ConfigException extends MqToolException {

    /**
     * Creates a new ConfigException with the specified message.
     *
     * @param message the error message
     */
    public ConfigException(String message) {
        super(message, ExitCodes.CONFIG_ERROR);
    }

    /**
     * Creates a new ConfigException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause, ExitCodes.CONFIG_ERROR);
    }
}
