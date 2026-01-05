package com.rtm.mq.tool.model;

/**
 * Represents a validation error or warning.
 *
 * <p>Validation errors are used to report issues found during
 * message specification validation. Each error includes:</p>
 * <ul>
 *   <li>A rule code (e.g., "VR-101") for categorization</li>
 *   <li>A human-readable description</li>
 *   <li>Details about the specific issue</li>
 *   <li>Severity level (ERROR or WARN)</li>
 * </ul>
 *
 * @see ValidationResult
 */
public class ValidationError {

    /** The rule code (e.g., "VR-101") identifying the validation rule. */
    private String ruleCode;

    /** A human-readable description of the error. */
    private String description;

    /** Detailed information about the specific issue. */
    private String details;

    /** The severity level of this error. */
    private Severity severity;

    /**
     * Severity levels for validation errors.
     */
    public enum Severity {
        /** An error that must be fixed. */
        ERROR,
        /** A warning that should be reviewed. */
        WARN
    }

    /**
     * Creates a validation error with ERROR severity.
     *
     * @param ruleCode the rule code (e.g., "VR-101")
     * @param description the human-readable description
     * @param details the detailed error information
     */
    public ValidationError(String ruleCode, String description, String details) {
        this(ruleCode, description, details, Severity.ERROR);
    }

    /**
     * Creates a validation error with the specified severity.
     *
     * @param ruleCode the rule code (e.g., "VR-101")
     * @param description the human-readable description
     * @param details the detailed error information
     * @param severity the severity level
     */
    public ValidationError(String ruleCode, String description, String details, Severity severity) {
        this.ruleCode = ruleCode;
        this.description = description;
        this.details = details;
        this.severity = severity;
    }

    /**
     * Gets the rule code.
     *
     * @return the rule code (e.g., "VR-101")
     */
    public String getRuleCode() {
        return ruleCode;
    }

    /**
     * Gets the human-readable description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the detailed error information.
     *
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Gets the severity level.
     *
     * @return the severity
     */
    public Severity getSeverity() {
        return severity;
    }
}
