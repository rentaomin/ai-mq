package com.rtm.mq.tool.exception;

import com.rtm.mq.tool.model.ValidationResult;

/**
 * Exception thrown when validation fails.
 *
 * <p>This exception can optionally carry a {@link ValidationResult} containing
 * detailed information about all validation errors that occurred.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new ValidationException("VR-106 - groupId found in Java Bean");
 *
 * // Or with full validation result:
 * ValidationResult result = validator.validate(model);
 * if (!result.isSuccess()) {
 *     throw new ValidationException("Validation failed", result);
 * }
 * }</pre>
 */
public class ValidationException extends MqToolException {

    private final ValidationResult result;

    /**
     * Creates a new ValidationException with the specified message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, ExitCodes.VALIDATION_ERROR);
        this.result = null;
    }

    /**
     * Creates a new ValidationException with a message and validation result.
     *
     * @param message the error message
     * @param result the validation result containing detailed errors
     */
    public ValidationException(String message, ValidationResult result) {
        super(message, ExitCodes.VALIDATION_ERROR);
        this.result = result;
    }

    /**
     * Gets the validation result associated with this exception.
     *
     * @return the validation result, or null if not set
     */
    public ValidationResult getResult() {
        return result;
    }
}
