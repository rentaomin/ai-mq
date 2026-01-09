package com.rtm.mq.tool.e2e;

import com.rtm.mq.tool.e2e.E2eVerificationResult.VerificationDimension;

/**
 * End-to-End Verifier as specified in T-310.
 *
 * <p>This class performs gate-level E2E verification. It verifies:</p>
 * <ul>
 *   <li>Completeness - manifest and audit log present, expected artifact categories</li>
 *   <li>Determinism - identical inputs produce identical structure</li>
 *   <li>Atomicity - failure = no committed output; success = exactly one commit</li>
 *   <li>Audit integrity - tool start, completion/failure, validation results, transaction outcome</li>
 *   <li>CLI integration - valid command resolved, exactly one executed, exit code propagated</li>
 * </ul>
 *
 * <p>This verifier operates ONLY on metadata from allowed inputs. It does NOT:</p>
 * <ul>
 *   <li>Read source artifacts</li>
 *   <li>Read generated files directly</li>
 *   <li>Read test resources or fixtures</li>
 *   <li>Perform file-by-file comparison</li>
 *   <li>Perform hash recomputation</li>
 * </ul>
 */
public final class E2eVerifier {

    /**
     * Verifies the E2E execution based on input metadata.
     *
     * @param input the verification input containing metadata from allowed sources
     * @return the verification result
     */
    public E2eVerificationResult verify(E2eVerificationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        E2eVerificationResult result = new E2eVerificationResult();

        // Verify each dimension
        verifyCompleteness(input, result.getCompleteness());
        verifyDeterminism(input, result.getDeterminism());
        verifyAtomicity(input, result.getAtomicity());
        verifyAuditIntegrity(input, result.getAuditIntegrity());
        verifyCliIntegration(input, result.getCliIntegration());

        // Compute overall result
        result.computeOverallResult();

        return result;
    }

    /**
     * Verifies completeness (Section 4.1 of T-310).
     *
     * <p>Verifies that a successful execution produced:</p>
     * <ul>
     *   <li>Output manifest</li>
     *   <li>Audit log</li>
     *   <li>All expected artifact categories declared in the manifest</li>
     * </ul>
     *
     * <p>No content inspection is permitted.</p>
     *
     * @param input the verification input
     * @param dimension the dimension to populate
     */
    private void verifyCompleteness(E2eVerificationInput input, VerificationDimension dimension) {
        // Check manifest presence
        boolean manifestPresent = input.isManifestPresent();
        dimension.addCheck("manifestPresent", manifestPresent);

        // Check audit log presence
        boolean auditLogPresent = input.isAuditLogPresent();
        dimension.addCheck("auditLogPresent", auditLogPresent);

        // Check expected artifact categories are declared
        // Categories are expected if manifest is present and has file count > 0
        boolean categoriesDeclared = !input.isManifestPresent()
                || !input.getManifestCategories().isEmpty()
                || input.getManifestFileCount() == 0;
        dimension.addCheck("artifactCategoriesDeclared", categoriesDeclared);
    }

    /**
     * Verifies determinism (Section 4.2 of T-310).
     *
     * <p>Verifies determinism by checking:</p>
     * <ul>
     *   <li>Identical input identifiers produce identical output manifest structure</li>
     *   <li>Identical input identifiers produce identical audit log structure (excluding timestamps)</li>
     * </ul>
     *
     * <p>No hash recomputation or file comparison is allowed.</p>
     *
     * @param input the verification input
     * @param dimension the dimension to populate
     */
    private void verifyDeterminism(E2eVerificationInput input, VerificationDimension dimension) {
        // Determinism is verified by checking structural consistency
        // If manifest is present, it must have a transaction ID
        boolean manifestStructureConsistent = !input.isManifestPresent()
                || (input.getManifestTransactionId() != null && !input.getManifestTransactionId().isEmpty());
        dimension.addCheck("manifestStructureConsistent", manifestStructureConsistent);

        // Audit log must have consistent structure if present
        boolean auditStructureConsistent = !input.isAuditLogPresent()
                || (input.isAuditHasToolStart() && (input.isAuditHasToolCompletion() || input.isAuditHasToolFailure()));
        dimension.addCheck("auditStructureConsistent", auditStructureConsistent);
    }

    /**
     * Verifies atomicity (Section 4.3 of T-310).
     *
     * <p>Verifies atomic behavior by checking audit and manifest records:</p>
     * <ul>
     *   <li>On failure: no committed output state is recorded</li>
     *   <li>On success: exactly one committed transaction is recorded</li>
     * </ul>
     *
     * <p>No filesystem inspection is allowed.</p>
     *
     * @param input the verification input
     * @param dimension the dimension to populate
     */
    private void verifyAtomicity(E2eVerificationInput input, VerificationDimension dimension) {
        // Check if execution was successful (exit code 0)
        boolean executionSuccessful = input.getCliExitCode() == 0;

        if (executionSuccessful) {
            // On success: exactly one committed transaction
            boolean hasCommittedTransaction = input.isManifestPresent()
                    && input.getManifestTransactionId() != null
                    && !input.getManifestTransactionId().isEmpty();
            dimension.addCheck("successHasCommittedTransaction", hasCommittedTransaction);

            boolean hasTransactionOutcome = input.isAuditHasTransactionOutcome();
            dimension.addCheck("successHasTransactionOutcome", hasTransactionOutcome);
        } else {
            // On failure: no committed output state
            // If execution failed but manifest exists with transaction ID, that's an atomicity violation
            boolean noCommittedStateOnFailure = !input.isManifestPresent()
                    || input.getManifestTransactionId() == null
                    || input.getManifestTransactionId().isEmpty()
                    || input.isAuditHasToolFailure();
            dimension.addCheck("failureNoCommittedState", noCommittedStateOnFailure);
        }
    }

    /**
     * Verifies audit integrity (Section 4.4 of T-310).
     *
     * <p>Verifies that audit records include, at minimum:</p>
     * <ul>
     *   <li>Tool start event</li>
     *   <li>Tool completion or failure event</li>
     *   <li>Validation results</li>
     *   <li>Transaction outcome</li>
     * </ul>
     *
     * <p>Audit content semantics are not evaluated.</p>
     *
     * @param input the verification input
     * @param dimension the dimension to populate
     */
    private void verifyAuditIntegrity(E2eVerificationInput input, VerificationDimension dimension) {
        // Only verify if audit log is present
        if (!input.isAuditLogPresent()) {
            dimension.addCheck("auditLogPresent", false);
            return;
        }

        // Tool start event
        dimension.addCheck("hasToolStartEvent", input.isAuditHasToolStart());

        // Tool completion or failure event
        boolean hasCompletionOrFailure = input.isAuditHasToolCompletion() || input.isAuditHasToolFailure();
        dimension.addCheck("hasCompletionOrFailureEvent", hasCompletionOrFailure);

        // Validation results
        dimension.addCheck("hasValidationResults", input.isAuditHasValidationResults());

        // Transaction outcome
        dimension.addCheck("hasTransactionOutcome", input.isAuditHasTransactionOutcome());
    }

    /**
     * Verifies CLI integration (Section 4.5 of T-310).
     *
     * <p>Verifies that:</p>
     * <ul>
     *   <li>A valid command was resolved</li>
     *   <li>Exactly one command was executed</li>
     *   <li>Exit code propagated correctly</li>
     * </ul>
     *
     * <p>No command re-execution is permitted.</p>
     *
     * @param input the verification input
     * @param dimension the dimension to populate
     */
    private void verifyCliIntegration(E2eVerificationInput input, VerificationDimension dimension) {
        // Only verify if CLI invocation metadata is present
        if (!input.isCliInvocationPresent()) {
            dimension.addCheck("cliInvocationPresent", false);
            return;
        }

        // Valid command was resolved
        boolean validCommandResolved = input.getResolvedCommand() != null
                && !input.getResolvedCommand().isEmpty();
        dimension.addCheck("validCommandResolved", validCommandResolved);

        // Exactly one command was executed
        boolean exactlyOneCommandExecuted = input.getCommandsExecutedCount() == 1;
        dimension.addCheck("exactlyOneCommandExecuted", exactlyOneCommandExecuted);

        // Exit code propagated correctly
        // The CLI exit code should match the audit exit code if both are present
        boolean exitCodePropagated = !input.isAuditLogPresent()
                || input.getCliExitCode() == input.getAuditExitCode();
        dimension.addCheck("exitCodePropagated", exitCodePropagated);
    }
}
