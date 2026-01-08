package com.rtm.mq.tool.validator.java;

/**
 * Represents a single validation issue found during Java Bean validation.
 *
 * <p>Each issue includes:</p>
 * <ul>
 *   <li>filePath - The file being validated</li>
 *   <li>fieldPath or classPath - The specific location within the file</li>
 *   <li>ruleId - A unique identifier for the validation rule</li>
 *   <li>severity - ERROR or WARNING</li>
 * </ul>
 *
 * <p>Rule IDs follow the pattern JB-XXX:</p>
 * <ul>
 *   <li>JB-001: Field naming convention violation</li>
 *   <li>JB-002: Class naming convention violation</li>
 *   <li>JB-003: Forbidden field detected (groupId)</li>
 *   <li>JB-004: Forbidden field detected (occurrenceCount)</li>
 *   <li>JB-005: Type mapping mismatch</li>
 *   <li>JB-006: Nested class structure mismatch</li>
 *   <li>JB-007: Missing enum helper method</li>
 *   <li>JB-008: Missing nested class</li>
 * </ul>
 */
public class JavaBeanValidationIssue {

    /**
     * Severity levels for validation issues.
     */
    public enum Severity {
        /** An error that must be fixed. */
        ERROR,
        /** A warning that should be reviewed. */
        WARNING
    }

    /** The path to the file being validated. */
    private final String filePath;

    /** The path within the class (field name or class path). */
    private final String path;

    /** The unique rule identifier. */
    private final String ruleId;

    /** The severity level. */
    private final Severity severity;

    /** Description of the issue. */
    private final String message;

    /**
     * Constructs a JavaBeanValidationIssue.
     *
     * @param filePath the file path
     * @param path the field or class path
     * @param ruleId the rule identifier
     * @param severity the severity level
     * @param message the issue description
     */
    public JavaBeanValidationIssue(String filePath, String path, String ruleId,
                                    Severity severity, String message) {
        this.filePath = filePath;
        this.path = path;
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
    }

    /**
     * Gets the file path.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the field or class path.
     *
     * @return the path within the class
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the rule identifier.
     *
     * @return the rule ID (e.g., "JB-001")
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Gets the severity level.
     *
     * @return the severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Gets the issue message.
     *
     * @return the message describing the issue
     */
    public String getMessage() {
        return message;
    }

    /**
     * Checks if this issue is an error.
     *
     * @return true if severity is ERROR
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /**
     * Checks if this issue is a warning.
     *
     * @return true if severity is WARNING
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s (%s)",
                severity, ruleId, filePath, path, message);
    }
}
