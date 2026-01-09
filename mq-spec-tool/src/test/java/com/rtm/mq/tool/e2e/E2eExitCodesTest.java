package com.rtm.mq.tool.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link E2eExitCodes}.
 */
class E2eExitCodesTest {

    @Test
    void testExitCodeValues() {
        assertEquals(0, E2eExitCodes.E2E_VERIFICATION_PASS);
        assertEquals(81, E2eExitCodes.E2E_COMPLETENESS_FAILED);
        assertEquals(82, E2eExitCodes.E2E_DETERMINISM_FAILED);
        assertEquals(83, E2eExitCodes.E2E_ATOMICITY_FAILED);
        assertEquals(84, E2eExitCodes.E2E_AUDIT_INTEGRITY_FAILED);
        assertEquals(85, E2eExitCodes.E2E_CLI_INTEGRATION_FAILED);
    }

    @Test
    void testGetDescription_pass() {
        assertEquals("E2E verification passed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_VERIFICATION_PASS));
    }

    @Test
    void testGetDescription_completenessFailed() {
        assertEquals("E2E completeness verification failed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_COMPLETENESS_FAILED));
    }

    @Test
    void testGetDescription_determinismFailed() {
        assertEquals("E2E determinism verification failed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_DETERMINISM_FAILED));
    }

    @Test
    void testGetDescription_atomicityFailed() {
        assertEquals("E2E atomicity verification failed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_ATOMICITY_FAILED));
    }

    @Test
    void testGetDescription_auditIntegrityFailed() {
        assertEquals("E2E audit integrity verification failed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_AUDIT_INTEGRITY_FAILED));
    }

    @Test
    void testGetDescription_cliIntegrationFailed() {
        assertEquals("E2E CLI integration verification failed",
                E2eExitCodes.getDescription(E2eExitCodes.E2E_CLI_INTEGRATION_FAILED));
    }

    @Test
    void testGetDescription_unknownCode() {
        assertEquals("Unknown E2E exit code", E2eExitCodes.getDescription(999));
    }

    @Test
    void testIsValidE2eExitCode_validCodes() {
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_VERIFICATION_PASS));
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_COMPLETENESS_FAILED));
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_DETERMINISM_FAILED));
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_ATOMICITY_FAILED));
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_AUDIT_INTEGRITY_FAILED));
        assertTrue(E2eExitCodes.isValidE2eExitCode(E2eExitCodes.E2E_CLI_INTEGRATION_FAILED));
    }

    @Test
    void testIsValidE2eExitCode_invalidCodes() {
        assertFalse(E2eExitCodes.isValidE2eExitCode(1));
        assertFalse(E2eExitCodes.isValidE2eExitCode(80));
        assertFalse(E2eExitCodes.isValidE2eExitCode(86));
        assertFalse(E2eExitCodes.isValidE2eExitCode(99));
        assertFalse(E2eExitCodes.isValidE2eExitCode(-1));
    }
}
