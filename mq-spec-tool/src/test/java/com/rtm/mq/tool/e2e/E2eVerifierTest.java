package com.rtm.mq.tool.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link E2eVerifier}.
 */
class E2eVerifierTest {

    private E2eVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new E2eVerifier();
    }

    @Test
    void testVerify_nullInput_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> verifier.verify(null));
    }

    // Completeness verification tests

    @Test
    void testCompleteness_allPresent() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getCompleteness().isPassed());
    }

    @Test
    void testCompleteness_manifestMissing() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setManifestPresent(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCompleteness().isPassed());
        assertEquals(E2eExitCodes.E2E_COMPLETENESS_FAILED, result.getExitCode());
    }

    @Test
    void testCompleteness_auditLogMissing() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditLogPresent(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCompleteness().isPassed());
        assertEquals(E2eExitCodes.E2E_COMPLETENESS_FAILED, result.getExitCode());
    }

    // Determinism verification tests

    @Test
    void testDeterminism_structureConsistent() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getDeterminism().isPassed());
    }

    @Test
    void testDeterminism_manifestMissingTransactionId() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setManifestTransactionId(null);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getDeterminism().isPassed());
    }

    @Test
    void testDeterminism_manifestEmptyTransactionId() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setManifestTransactionId("");

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getDeterminism().isPassed());
    }

    @Test
    void testDeterminism_auditMissingStartAndCompletion() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasToolStart(false);
        input.setAuditHasToolCompletion(false);
        input.setAuditHasToolFailure(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getDeterminism().isPassed());
    }

    // Atomicity verification tests

    @Test
    void testAtomicity_successWithCommittedTransaction() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getAtomicity().isPassed());
    }

    @Test
    void testAtomicity_successWithoutCommittedTransaction() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setManifestTransactionId(null);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAtomicity().isPassed());
    }

    @Test
    void testAtomicity_failureWithNoCommittedState() {
        E2eVerificationInput input = createFailedInput();

        E2eVerificationResult result = verifier.verify(input);

        // Should pass atomicity - failure correctly has no committed state
        assertTrue(result.getAtomicity().isPassed());
    }

    // Audit integrity verification tests

    @Test
    void testAuditIntegrity_allEventsPresent() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_auditLogMissing() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditLogPresent(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_missingToolStart() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasToolStart(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_missingCompletionAndFailure() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasToolCompletion(false);
        input.setAuditHasToolFailure(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_hasFailureEventOnly() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasToolCompletion(false);
        input.setAuditHasToolFailure(true);

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_missingValidationResults() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasValidationResults(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAuditIntegrity().isPassed());
    }

    @Test
    void testAuditIntegrity_missingTransactionOutcome() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setAuditHasTransactionOutcome(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getAuditIntegrity().isPassed());
    }

    // CLI integration verification tests

    @Test
    void testCliIntegration_allCorrect() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_cliInvocationMissing() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setCliInvocationPresent(false);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_noCommandResolved() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setResolvedCommand(null);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_emptyCommandResolved() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setResolvedCommand("");

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_multipleCommandsExecuted() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setCommandsExecutedCount(2);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_zeroCommandsExecuted() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setCommandsExecutedCount(0);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    @Test
    void testCliIntegration_exitCodeMismatch() {
        E2eVerificationInput input = createSuccessfulInput();
        input.setCliExitCode(0);
        input.setAuditExitCode(1);

        E2eVerificationResult result = verifier.verify(input);

        assertFalse(result.getCliIntegration().isPassed());
    }

    // Overall verification tests

    @Test
    void testVerify_allPass() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.isPassed());
        assertEquals(E2eExitCodes.E2E_VERIFICATION_PASS, result.getExitCode());
    }

    @Test
    void testVerify_firstFailureDeterminesExitCode() {
        E2eVerificationInput input = createSuccessfulInput();
        // Both completeness and determinism will fail
        input.setAuditLogPresent(false);
        input.setManifestTransactionId(null);

        E2eVerificationResult result = verifier.verify(input);

        // Completeness is checked first
        assertEquals(E2eExitCodes.E2E_COMPLETENESS_FAILED, result.getExitCode());
    }

    // Helper methods

    private E2eVerificationInput createSuccessfulInput() {
        E2eVerificationInput input = new E2eVerificationInput();

        // Audit log
        input.setAuditLogPresent(true);
        input.setAuditHasToolStart(true);
        input.setAuditHasToolCompletion(true);
        input.setAuditHasToolFailure(false);
        input.setAuditHasValidationResults(true);
        input.setAuditHasTransactionOutcome(true);
        input.setAuditExitCode(0);

        // Manifest
        input.setManifestPresent(true);
        input.setManifestTransactionId("tx-123");
        input.setManifestFileCount(3);
        input.setManifestCategories(Arrays.asList("xml", "java"));

        // Consistency
        input.setConsistencyResultPresent(true);
        input.setConsistencyPassed(true);
        input.setConsistencyIssueCount(0);

        // Message validation
        input.setMessageValidationResultPresent(true);
        input.setMessageValidationPassed(true);
        input.setMessageValidationIssueCount(0);

        // CLI
        input.setCliInvocationPresent(true);
        input.setResolvedCommand("generate");
        input.setCommandsExecutedCount(1);
        input.setCliExitCode(0);

        return input;
    }

    private E2eVerificationInput createFailedInput() {
        E2eVerificationInput input = new E2eVerificationInput();

        // Audit log - indicates failure
        input.setAuditLogPresent(true);
        input.setAuditHasToolStart(true);
        input.setAuditHasToolCompletion(false);
        input.setAuditHasToolFailure(true);
        input.setAuditHasValidationResults(true);
        input.setAuditHasTransactionOutcome(true);
        input.setAuditExitCode(1);

        // Manifest - not present due to failure
        input.setManifestPresent(false);

        // CLI
        input.setCliInvocationPresent(true);
        input.setResolvedCommand("generate");
        input.setCommandsExecutedCount(1);
        input.setCliExitCode(1);

        return input;
    }
}
