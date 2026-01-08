package com.rtm.mq.tool.cli;

import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.exception.MqToolException;

/**
 * Exception thrown when CLI argument parsing fails.
 */
public class CliParseException extends MqToolException {

    /**
     * Creates a new CliParseException with a message.
     *
     * @param message the error message
     */
    public CliParseException(String message) {
        super(message, ExitCodes.CLI_ARGUMENT_ERROR);
    }

    /**
     * Creates a new CliParseException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CliParseException(String message, Throwable cause) {
        super(message, cause, ExitCodes.CLI_ARGUMENT_ERROR);
    }
}
