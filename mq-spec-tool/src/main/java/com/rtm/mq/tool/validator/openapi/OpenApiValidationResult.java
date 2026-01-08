package com.rtm.mq.tool.validator.openapi;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of OpenAPI validation.
 *
 * <p>Contains a list of validation issues found during validation.
 * The result is considered successful if no ERROR-level issues exist.</p>
 */
public class OpenApiValidationResult {

    /** List of validation issues found. Uses List to preserve order. */
    private final List<OpenApiValidationIssue> issues = new ArrayList<>();

    /**
     * Adds a validation issue.
     *
     * @param issue the issue to add
     */
    public void addIssue(OpenApiValidationIssue issue) {
        issues.add(issue);
    }

    /**
     * Adds an error-level issue.
     *
     * @param filePath the file path
     * @param schemaPath the schema or field path
     * @param ruleId the rule identifier
     * @param message the issue message
     */
    public void addError(String filePath, String schemaPath, String ruleId, String message) {
        issues.add(new OpenApiValidationIssue(
                filePath, schemaPath, ruleId, OpenApiValidationIssue.Severity.ERROR, message));
    }

    /**
     * Adds a warning-level issue.
     *
     * @param filePath the file path
     * @param schemaPath the schema or field path
     * @param ruleId the rule identifier
     * @param message the issue message
     */
    public void addWarning(String filePath, String schemaPath, String ruleId, String message) {
        issues.add(new OpenApiValidationIssue(
                filePath, schemaPath, ruleId, OpenApiValidationIssue.Severity.WARNING, message));
    }

    /**
     * Gets all validation issues.
     *
     * @return the list of issues (never null)
     */
    public List<OpenApiValidationIssue> getIssues() {
        return issues;
    }

    /**
     * Gets only error-level issues.
     *
     * @return a list of error issues
     */
    public List<OpenApiValidationIssue> getErrors() {
        List<OpenApiValidationIssue> errors = new ArrayList<>();
        for (OpenApiValidationIssue issue : issues) {
            if (issue.isError()) {
                errors.add(issue);
            }
        }
        return errors;
    }

    /**
     * Gets only warning-level issues.
     *
     * @return a list of warning issues
     */
    public List<OpenApiValidationIssue> getWarnings() {
        List<OpenApiValidationIssue> warnings = new ArrayList<>();
        for (OpenApiValidationIssue issue : issues) {
            if (issue.isWarning()) {
                warnings.add(issue);
            }
        }
        return warnings;
    }

    /**
     * Checks if validation passed (no errors).
     *
     * @return true if no error-level issues exist
     */
    public boolean isSuccess() {
        for (OpenApiValidationIssue issue : issues) {
            if (issue.isError()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any issues were found.
     *
     * @return true if at least one issue exists
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Gets the total count of issues.
     *
     * @return the issue count
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * Gets the count of errors.
     *
     * @return the error count
     */
    public int getErrorCount() {
        int count = 0;
        for (OpenApiValidationIssue issue : issues) {
            if (issue.isError()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the count of warnings.
     *
     * @return the warning count
     */
    public int getWarningCount() {
        int count = 0;
        for (OpenApiValidationIssue issue : issues) {
            if (issue.isWarning()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Merges another result into this one.
     *
     * @param other the result to merge
     */
    public void merge(OpenApiValidationResult other) {
        if (other != null) {
            issues.addAll(other.getIssues());
        }
    }

    /**
     * Gets the exit code based on validation result.
     *
     * @return 0 for success (no errors), 1 for validation error (one or more errors)
     */
    public int getExitCode() {
        return isSuccess() ? 0 : 1;
    }
}
