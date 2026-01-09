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

    @Test
    void testCompleteness_allPresent() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getCompleteness().isPassed());
    }

    @Test
    void testDeterminism_structureConsistent() {
        E2eVerificationInput input = createSuccessfulInput();

        E2eVerificationResult result = verifier.verify(input);

        assertTrue(result.getDeterminism().isPassed());
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
}
