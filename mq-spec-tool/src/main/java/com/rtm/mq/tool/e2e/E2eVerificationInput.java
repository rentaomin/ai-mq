package com.rtm.mq.tool.e2e;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Input data for E2E verification as specified in T-310.
 *
 * <p>Allowed inputs (hard limit):</p>
 * <ul>
 *   <li>Audit log output (from T-308)</li>
 *   <li>Output manifest (from T-307)</li>
 *   <li>Consistency validation result (from T-304)</li>
 *   <li>Message validation result (from T-306)</li>
 *   <li>CLI invocation metadata (from T-309)</li>
 *   <li>Explicitly provided test fixture identifiers (names only)</li>
 * </ul>
 *
 * <p>This class encapsulates the metadata extracted from these inputs
 * without reading file contents directly.</p>
 */
public final class E2eVerificationInput {

    // Audit log metadata
    private boolean auditLogPresent;
    private boolean auditHasToolStart;
    private boolean auditHasToolCompletion;
    private boolean auditHasToolFailure;
    private boolean auditHasValidationResults;
    private boolean auditHasTransactionOutcome;
    private int auditExitCode;

    // Output manifest metadata
    private boolean manifestPresent;
    private String manifestTransactionId;
    private int manifestFileCount;
    private List<String> manifestCategories;

    // Consistency validation result metadata
    private boolean consistencyResultPresent;
    private boolean consistencyPassed;
    private int consistencyIssueCount;

    // Message validation result metadata
    private boolean messageValidationResultPresent;
    private boolean messageValidationPassed;
    private int messageValidationIssueCount;

    // CLI invocation metadata
    private boolean cliInvocationPresent;
    private String resolvedCommand;
    private int commandsExecutedCount;
    private int cliExitCode;

    // Test fixture identifiers (names only, no file parsing)
    private List<String> fixtureIdentifiers;

    /**
     * Creates an empty E2E verification input.
     */
    public E2eVerificationInput() {
        this.manifestCategories = Collections.emptyList();
        this.fixtureIdentifiers = Collections.emptyList();
    }

    // Audit log metadata accessors

    public boolean isAuditLogPresent() {
        return auditLogPresent;
    }

    public void setAuditLogPresent(boolean auditLogPresent) {
        this.auditLogPresent = auditLogPresent;
    }

    public boolean isAuditHasToolStart() {
        return auditHasToolStart;
    }

    public void setAuditHasToolStart(boolean auditHasToolStart) {
        this.auditHasToolStart = auditHasToolStart;
    }

    public boolean isAuditHasToolCompletion() {
        return auditHasToolCompletion;
    }

    public void setAuditHasToolCompletion(boolean auditHasToolCompletion) {
        this.auditHasToolCompletion = auditHasToolCompletion;
    }

    public boolean isAuditHasToolFailure() {
        return auditHasToolFailure;
    }

    public void setAuditHasToolFailure(boolean auditHasToolFailure) {
        this.auditHasToolFailure = auditHasToolFailure;
    }

    public boolean isAuditHasValidationResults() {
        return auditHasValidationResults;
    }

    public void setAuditHasValidationResults(boolean auditHasValidationResults) {
        this.auditHasValidationResults = auditHasValidationResults;
    }

    public boolean isAuditHasTransactionOutcome() {
        return auditHasTransactionOutcome;
    }

    public void setAuditHasTransactionOutcome(boolean auditHasTransactionOutcome) {
        this.auditHasTransactionOutcome = auditHasTransactionOutcome;
    }

    public int getAuditExitCode() {
        return auditExitCode;
    }

    public void setAuditExitCode(int auditExitCode) {
        this.auditExitCode = auditExitCode;
    }

    // Output manifest metadata accessors

    public boolean isManifestPresent() {
        return manifestPresent;
    }

    public void setManifestPresent(boolean manifestPresent) {
        this.manifestPresent = manifestPresent;
    }

    public String getManifestTransactionId() {
        return manifestTransactionId;
    }

    public void setManifestTransactionId(String manifestTransactionId) {
        this.manifestTransactionId = manifestTransactionId;
    }

    public int getManifestFileCount() {
        return manifestFileCount;
    }

    public void setManifestFileCount(int manifestFileCount) {
        this.manifestFileCount = manifestFileCount;
    }

    public List<String> getManifestCategories() {
        return manifestCategories;
    }

    public void setManifestCategories(List<String> manifestCategories) {
        this.manifestCategories = manifestCategories != null
                ? manifestCategories
                : Collections.emptyList();
    }

    // Consistency validation result accessors

    public boolean isConsistencyResultPresent() {
        return consistencyResultPresent;
    }

    public void setConsistencyResultPresent(boolean consistencyResultPresent) {
        this.consistencyResultPresent = consistencyResultPresent;
    }

    public boolean isConsistencyPassed() {
        return consistencyPassed;
    }

    public void setConsistencyPassed(boolean consistencyPassed) {
        this.consistencyPassed = consistencyPassed;
    }

    public int getConsistencyIssueCount() {
        return consistencyIssueCount;
    }

    public void setConsistencyIssueCount(int consistencyIssueCount) {
        this.consistencyIssueCount = consistencyIssueCount;
    }

    // Message validation result accessors

    public boolean isMessageValidationResultPresent() {
        return messageValidationResultPresent;
    }

    public void setMessageValidationResultPresent(boolean messageValidationResultPresent) {
        this.messageValidationResultPresent = messageValidationResultPresent;
    }

    public boolean isMessageValidationPassed() {
        return messageValidationPassed;
    }

    public void setMessageValidationPassed(boolean messageValidationPassed) {
        this.messageValidationPassed = messageValidationPassed;
    }

    public int getMessageValidationIssueCount() {
        return messageValidationIssueCount;
    }

    public void setMessageValidationIssueCount(int messageValidationIssueCount) {
        this.messageValidationIssueCount = messageValidationIssueCount;
    }

    // CLI invocation metadata accessors

    public boolean isCliInvocationPresent() {
        return cliInvocationPresent;
    }

    public void setCliInvocationPresent(boolean cliInvocationPresent) {
        this.cliInvocationPresent = cliInvocationPresent;
    }

    public String getResolvedCommand() {
        return resolvedCommand;
    }

    public void setResolvedCommand(String resolvedCommand) {
        this.resolvedCommand = resolvedCommand;
    }

    public int getCommandsExecutedCount() {
        return commandsExecutedCount;
    }

    public void setCommandsExecutedCount(int commandsExecutedCount) {
        this.commandsExecutedCount = commandsExecutedCount;
    }

    public int getCliExitCode() {
        return cliExitCode;
    }

    public void setCliExitCode(int cliExitCode) {
        this.cliExitCode = cliExitCode;
    }

    // Test fixture identifiers

    public List<String> getFixtureIdentifiers() {
        return fixtureIdentifiers;
    }

    public void setFixtureIdentifiers(List<String> fixtureIdentifiers) {
        this.fixtureIdentifiers = fixtureIdentifiers != null
                ? fixtureIdentifiers
                : Collections.emptyList();
    }

    /**
     * Converts to a map for deterministic serialization.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        // Audit log
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("exitCode", auditExitCode);
        audit.put("hasToolCompletion", auditHasToolCompletion);
        audit.put("hasToolFailure", auditHasToolFailure);
        audit.put("hasToolStart", auditHasToolStart);
        audit.put("hasTransactionOutcome", auditHasTransactionOutcome);
        audit.put("hasValidationResults", auditHasValidationResults);
        audit.put("present", auditLogPresent);
        map.put("auditLog", audit);

        // CLI
        Map<String, Object> cli = new LinkedHashMap<>();
        cli.put("commandsExecutedCount", commandsExecutedCount);
        cli.put("exitCode", cliExitCode);
        cli.put("present", cliInvocationPresent);
        cli.put("resolvedCommand", resolvedCommand);
        map.put("cli", cli);

        // Consistency
        Map<String, Object> consistency = new LinkedHashMap<>();
        consistency.put("issueCount", consistencyIssueCount);
        consistency.put("passed", consistencyPassed);
        consistency.put("present", consistencyResultPresent);
        map.put("consistency", consistency);

        // Manifest
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("categories", manifestCategories);
        manifest.put("fileCount", manifestFileCount);
        manifest.put("present", manifestPresent);
        manifest.put("transactionId", manifestTransactionId);
        map.put("manifest", manifest);

        // Message validation
        Map<String, Object> messageVal = new LinkedHashMap<>();
        messageVal.put("issueCount", messageValidationIssueCount);
        messageVal.put("passed", messageValidationPassed);
        messageVal.put("present", messageValidationResultPresent);
        map.put("messageValidation", messageVal);

        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        E2eVerificationInput that = (E2eVerificationInput) obj;
        return auditLogPresent == that.auditLogPresent
                && auditHasToolStart == that.auditHasToolStart
                && auditHasToolCompletion == that.auditHasToolCompletion
                && auditHasToolFailure == that.auditHasToolFailure
                && auditHasValidationResults == that.auditHasValidationResults
                && auditHasTransactionOutcome == that.auditHasTransactionOutcome
                && auditExitCode == that.auditExitCode
                && manifestPresent == that.manifestPresent
                && manifestFileCount == that.manifestFileCount
                && consistencyResultPresent == that.consistencyResultPresent
                && consistencyPassed == that.consistencyPassed
                && consistencyIssueCount == that.consistencyIssueCount
                && messageValidationResultPresent == that.messageValidationResultPresent
                && messageValidationPassed == that.messageValidationPassed
                && messageValidationIssueCount == that.messageValidationIssueCount
                && cliInvocationPresent == that.cliInvocationPresent
                && commandsExecutedCount == that.commandsExecutedCount
                && cliExitCode == that.cliExitCode
                && Objects.equals(manifestTransactionId, that.manifestTransactionId)
                && Objects.equals(manifestCategories, that.manifestCategories)
                && Objects.equals(resolvedCommand, that.resolvedCommand)
                && Objects.equals(fixtureIdentifiers, that.fixtureIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                auditLogPresent, auditHasToolStart, auditHasToolCompletion,
                auditHasToolFailure, auditHasValidationResults, auditHasTransactionOutcome,
                auditExitCode, manifestPresent, manifestTransactionId, manifestFileCount,
                manifestCategories, consistencyResultPresent, consistencyPassed,
                consistencyIssueCount, messageValidationResultPresent, messageValidationPassed,
                messageValidationIssueCount, cliInvocationPresent, resolvedCommand,
                commandsExecutedCount, cliExitCode, fixtureIdentifiers
        );
    }
}
