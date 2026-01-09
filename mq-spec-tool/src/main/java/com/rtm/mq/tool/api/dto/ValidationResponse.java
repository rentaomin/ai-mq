package com.rtm.mq.tool.api.dto;

import com.rtm.mq.tool.model.ValidationError;

import java.util.List;

/**
 * Response DTO for validation API.
 */
public class ValidationResponse {

    /**
     * Validation type (excel, xml, java, openapi, cross-artifact).
     */
    private String validationType;

    /**
     * Whether validation passed.
     */
    private boolean passed;

    /**
     * List of validation errors (empty if passed).
     */
    private List<ValidationError> errors;

    /**
     * Number of issues found.
     */
    private int issueCount;

    /**
     * Validation timestamp.
     */
    private String timestamp;

    // Constructors

    public ValidationResponse() {
        this.timestamp = java.time.Instant.now().toString();
    }

    public ValidationResponse(String validationType, boolean passed, List<ValidationError> errors) {
        this();
        this.validationType = validationType;
        this.passed = passed;
        this.errors = errors;
        this.issueCount = errors != null ? errors.size() : 0;
    }

    // Getters and setters

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
        this.issueCount = errors != null ? errors.size() : 0;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(int issueCount) {
        this.issueCount = issueCount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
