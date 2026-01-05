package com.rtm.mq.tool.config;

/**
 * Exception thrown when configuration loading or validation fails.
 *
 * <p>This exception indicates issues such as:
 * <ul>
 *   <li>YAML parsing errors</li>
 *   <li>Missing required configuration fields</li>
 *   <li>Invalid configuration values</li>
 *   <li>Configuration file not found or unreadable</li>
 * </ul>
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
