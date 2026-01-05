package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a validation operation.
 *
 * <p>A validation result contains:</p>
 * <ul>
 *   <li>Overall pass/fail status</li>
 *   <li>List of validation errors (if any)</li>
 *   <li>List of validation warnings (if any)</li>
 *   <li>Source location information for each issue</li>
 * </ul>
 *
 * @see ValidationError
 */
public class ValidationResult {

    /** Indicates whether validation passed (no errors). */
    private boolean success;

    /** List of validation errors. Uses List to preserve order. */
    private List<ValidationError> errors = new ArrayList<>();

    /** List of validation warnings. Uses List to preserve order. */
    private List<ValidationError> warnings = new ArrayList<>();

    /**
     * Creates a successful validation result with no errors.
     */
    public ValidationResult() {
        this.success = true;
    }

    /**
     * Creates a validation result with the specified errors.
     *
     * @param errors the list of validation errors
     */
    public ValidationResult(List<ValidationError> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
        this.success = this.errors.isEmpty();
    }

    /**
     * Checks if validation was successful.
     *
     * @return true if no errors occurred
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the list of validation errors.
     *
     * @return the errors list (never null)
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Gets the list of validation warnings.
     *
     * @return the warnings list (never null)
     */
    public List<ValidationError> getWarnings() {
        return warnings;
    }

    /**
     * Adds a validation error and sets success to false.
     *
     * @param error the error to add
     */
    public void addError(ValidationError error) {
        this.errors.add(error);
        this.success = false;
    }

    /**
     * Adds a validation warning (does not affect success status).
     *
     * @param warning the warning to add
     */
    public void addWarning(ValidationError warning) {
        this.warnings.add(warning);
    }
}
