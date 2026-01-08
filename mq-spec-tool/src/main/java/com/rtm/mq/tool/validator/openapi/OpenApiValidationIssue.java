package com.rtm.mq.tool.validator.openapi;

/**
 * Represents a single validation issue found during OpenAPI validation.
 *
 * <p>Each issue includes:</p>
 * <ul>
 *   <li>filePath - The file being validated</li>
 *   <li>schemaPath or fieldPath - The specific location within the file</li>
 *   <li>ruleId - A unique identifier for the validation rule</li>
 *   <li>severity - ERROR or WARNING</li>
 * </ul>
 *
 * <p>Rule IDs follow the pattern OA-XXX:</p>
 * <ul>
 *   <li>OA-001: YAML syntax error</li>
 *   <li>OA-002: Missing required OpenAPI root field</li>
 *   <li>OA-003: Schema missing explicit type</li>
 *   <li>OA-004: Object schema missing properties</li>
 *   <li>OA-005: Invalid $ref reference</li>
 *   <li>OA-006: Circular $ref reference detected</li>
 *   <li>OA-007: Forbidden field: groupId</li>
 *   <li>OA-008: Forbidden field: occurrenceCount</li>
 *   <li>OA-009: Type mapping mismatch</li>
 *   <li>OA-010: Structural alignment mismatch with JSON Tree</li>
 * </ul>
 */
public class OpenApiValidationIssue {

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

    /** The path within the schema (schema path or field path). */
    private final String schemaPath;

    /** The unique rule identifier. */
    private final String ruleId;

    /** The severity level. */
    private final Severity severity;

    /** Description of the issue. */
    private final String message;

    /**
     * Constructs an OpenApiValidationIssue.
     *
     * @param filePath the file path
     * @param schemaPath the schema or field path
     * @param ruleId the rule identifier
     * @param severity the severity level
     * @param message the issue description
     */
    public OpenApiValidationIssue(String filePath, String schemaPath, String ruleId,
                                   Severity severity, String message) {
        this.filePath = filePath;
        this.schemaPath = schemaPath;
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
     * Gets the schema or field path.
     *
     * @return the path within the schema
     */
    public String getSchemaPath() {
        return schemaPath;
    }

    /**
     * Gets the rule identifier.
     *
     * @return the rule ID (e.g., "OA-001")
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
                severity, ruleId, filePath, schemaPath, message);
    }
}
