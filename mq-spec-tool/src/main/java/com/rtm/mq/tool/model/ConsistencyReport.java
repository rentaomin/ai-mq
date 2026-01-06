package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cross-artifact consistency validation report.
 *
 * <p>This report contains:</p>
 * <ul>
 *   <li>Overall validation status (pass/fail)</li>
 *   <li>List of all consistency issues found</li>
 *   <li>Summary statistics (error count, warning count)</li>
 * </ul>
 */
public class ConsistencyReport {
    private boolean success;
    private int errorCount;
    private int warningCount;
    private List<ConsistencyIssue> issues = new ArrayList<>();
    private String timestamp;

    public ConsistencyReport() {
        this.success = true;
        this.errorCount = 0;
        this.warningCount = 0;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public List<ConsistencyIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ConsistencyIssue> issues) {
        this.issues = issues;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Adds a consistency issue to the report.
     * Updates error/warning counts and success status accordingly.
     *
     * @param issue the issue to add
     */
    public void addIssue(ConsistencyIssue issue) {
        this.issues.add(issue);
        if ("ERROR".equals(issue.getSeverity())) {
            this.errorCount++;
            this.success = false;
        } else if ("WARNING".equals(issue.getSeverity())) {
            this.warningCount++;
        }
    }
}
