package com.rtm.mq.tool.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ExitCodes}.
 */
class ExitCodesTest {

    @Test
    void testExitCodeValues() {
        // Verify exit code values match specification
        assertEquals(0, ExitCodes.SUCCESS);
        assertEquals(1, ExitCodes.INPUT_VALIDATION_ERROR);
        assertEquals(2, ExitCodes.PARSE_ERROR);
        assertEquals(3, ExitCodes.GENERATION_ERROR);
        assertEquals(4, ExitCodes.VALIDATION_ERROR);
        assertEquals(5, ExitCodes.CONFIG_ERROR);
        assertEquals(99, ExitCodes.INTERNAL_ERROR);
    }

    @Test
    void testGetDescriptionSuccess() {
        assertEquals("Success", ExitCodes.getDescription(ExitCodes.SUCCESS));
    }

    @Test
    void testGetDescriptionInputValidationError() {
        assertEquals("Input validation error", ExitCodes.getDescription(ExitCodes.INPUT_VALIDATION_ERROR));
    }

    @Test
    void testGetDescriptionParseError() {
        assertEquals("Parse error", ExitCodes.getDescription(ExitCodes.PARSE_ERROR));
    }

    @Test
    void testGetDescriptionGenerationError() {
        assertEquals("Generation error", ExitCodes.getDescription(ExitCodes.GENERATION_ERROR));
    }

    @Test
    void testGetDescriptionValidationError() {
        assertEquals("Validation error", ExitCodes.getDescription(ExitCodes.VALIDATION_ERROR));
    }

    @Test
    void testGetDescriptionConfigError() {
        assertEquals("Configuration error", ExitCodes.getDescription(ExitCodes.CONFIG_ERROR));
    }

    @Test
    void testGetDescriptionInternalError() {
        assertEquals("Internal error", ExitCodes.getDescription(ExitCodes.INTERNAL_ERROR));
    }

    @Test
    void testGetDescriptionUnknownCode() {
        assertEquals("Unknown error", ExitCodes.getDescription(-1));
        assertEquals("Unknown error", ExitCodes.getDescription(100));
        assertEquals("Unknown error", ExitCodes.getDescription(Integer.MAX_VALUE));
    }

    @Test
    void testExitCodesAreUnique() {
        // Verify all exit codes are unique
        int[] codes = {
            ExitCodes.SUCCESS,
            ExitCodes.INPUT_VALIDATION_ERROR,
            ExitCodes.PARSE_ERROR,
            ExitCodes.GENERATION_ERROR,
            ExitCodes.VALIDATION_ERROR,
            ExitCodes.CONFIG_ERROR,
            ExitCodes.INTERNAL_ERROR,
            ExitCodes.E2E_COMPLETENESS_FAILED,
            ExitCodes.E2E_DETERMINISM_FAILED,
            ExitCodes.E2E_ATOMICITY_FAILED,
            ExitCodes.E2E_AUDIT_INTEGRITY_FAILED,
            ExitCodes.E2E_CLI_INTEGRATION_FAILED
        };

        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i], codes[j],
                    "Exit codes at index " + i + " and " + j + " should be unique");
            }
        }
    }

    // E2E Exit Codes Tests (T-310)

    @Test
    void testE2eExitCodeValues() {
        assertEquals(81, ExitCodes.E2E_COMPLETENESS_FAILED);
        assertEquals(82, ExitCodes.E2E_DETERMINISM_FAILED);
        assertEquals(83, ExitCodes.E2E_ATOMICITY_FAILED);
        assertEquals(84, ExitCodes.E2E_AUDIT_INTEGRITY_FAILED);
        assertEquals(85, ExitCodes.E2E_CLI_INTEGRATION_FAILED);
    }

    @Test
    void testGetDescriptionE2eCompletenessFailed() {
        assertEquals("E2E completeness verification failed",
                ExitCodes.getDescription(ExitCodes.E2E_COMPLETENESS_FAILED));
    }

    @Test
    void testGetDescriptionE2eDeterminismFailed() {
        assertEquals("E2E determinism verification failed",
                ExitCodes.getDescription(ExitCodes.E2E_DETERMINISM_FAILED));
    }

    @Test
    void testGetDescriptionE2eAtomicityFailed() {
        assertEquals("E2E atomicity verification failed",
                ExitCodes.getDescription(ExitCodes.E2E_ATOMICITY_FAILED));
    }

    @Test
    void testGetDescriptionE2eAuditIntegrityFailed() {
        assertEquals("E2E audit integrity verification failed",
                ExitCodes.getDescription(ExitCodes.E2E_AUDIT_INTEGRITY_FAILED));
    }

    @Test
    void testGetDescriptionE2eCliIntegrationFailed() {
        assertEquals("E2E CLI integration verification failed",
                ExitCodes.getDescription(ExitCodes.E2E_CLI_INTEGRATION_FAILED));
    }
}
