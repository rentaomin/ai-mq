package com.rtm.mq.tool.e2e;

import com.rtm.mq.tool.e2e.E2eVerificationResult.VerificationDimension;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link E2eVerificationResult}.
 */
class E2eVerificationResultTest {

    @Test
    void testDefaultConstructor() {
        E2eVerificationResult result = new E2eVerificationResult();

        assertTrue(result.isPassed());
        assertEquals(E2eExitCodes.E2E_VERIFICATION_PASS, result.getExitCode());
        assertNotNull(result.getCompleteness());
        assertNotNull(result.getDeterminism());
        assertNotNull(result.getAtomicity());
        assertNotNull(result.getAuditIntegrity());
        assertNotNull(result.getCliIntegration());
    }

    @Test
    void testComputeOverallResult_allPassed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.computeOverallResult();

        assertTrue(result.isPassed());
        assertEquals(E2eExitCodes.E2E_VERIFICATION_PASS, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_completenessFailed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getCompleteness().setPassed(false);
        result.computeOverallResult();

        assertFalse(result.isPassed());
        assertEquals(E2eExitCodes.E2E_COMPLETENESS_FAILED, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_determinismFailed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getDeterminism().setPassed(false);
        result.computeOverallResult();

        assertFalse(result.isPassed());
        assertEquals(E2eExitCodes.E2E_DETERMINISM_FAILED, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_atomicityFailed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getAtomicity().setPassed(false);
        result.computeOverallResult();

        assertFalse(result.isPassed());
        assertEquals(E2eExitCodes.E2E_ATOMICITY_FAILED, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_auditIntegrityFailed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getAuditIntegrity().setPassed(false);
        result.computeOverallResult();

        assertFalse(result.isPassed());
        assertEquals(E2eExitCodes.E2E_AUDIT_INTEGRITY_FAILED, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_cliIntegrationFailed() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getCliIntegration().setPassed(false);
        result.computeOverallResult();

        assertFalse(result.isPassed());
        assertEquals(E2eExitCodes.E2E_CLI_INTEGRATION_FAILED, result.getExitCode());
    }

    @Test
    void testComputeOverallResult_firstFailureDeterminesExitCode() {
        E2eVerificationResult result = new E2eVerificationResult();
        result.getCompleteness().setPassed(false);
        result.getDeterminism().setPassed(false);
        result.computeOverallResult();

        // Completeness is checked first
        assertEquals(E2eExitCodes.E2E_COMPLETENESS_FAILED, result.getExitCode());
    }

    @Test
    void testToMap_deterministicOrdering() {
        E2eVerificationResult result = new E2eVerificationResult();
        Map<String, Object> map = result.toMap();

        // Keys should be in alphabetical order
        String[] expectedOrder = {"atomicity", "auditIntegrity", "cliIntegration",
                "completeness", "determinism", "exitCode", "passed"};
        String[] actualOrder = map.keySet().toArray(new String[0]);

        assertArrayEquals(expectedOrder, actualOrder);
    }

    @Test
    void testEquals_sameObject() {
        E2eVerificationResult result = new E2eVerificationResult();
        assertEquals(result, result);
    }

    @Test
    void testEquals_equalObjects() {
        E2eVerificationResult result1 = new E2eVerificationResult();
        E2eVerificationResult result2 = new E2eVerificationResult();
        assertEquals(result1, result2);
    }

    @Test
    void testEquals_differentPassed() {
        E2eVerificationResult result1 = new E2eVerificationResult();
        E2eVerificationResult result2 = new E2eVerificationResult();
        result2.setPassed(false);
        assertNotEquals(result1, result2);
    }

    @Test
    void testEquals_null() {
        E2eVerificationResult result = new E2eVerificationResult();
        assertNotEquals(null, result);
    }

    @Test
    void testHashCode_equalObjects() {
        E2eVerificationResult result1 = new E2eVerificationResult();
        E2eVerificationResult result2 = new E2eVerificationResult();
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    // VerificationDimension tests

    @Test
    void testVerificationDimension_constructor() {
        VerificationDimension dim = new VerificationDimension("test");
        assertEquals("test", dim.getName());
        assertTrue(dim.isPassed());
        assertTrue(dim.getChecks().isEmpty());
    }

    @Test
    void testVerificationDimension_constructor_nullName_throws() {
        assertThrows(NullPointerException.class,
                () -> new VerificationDimension(null));
    }

    @Test
    void testVerificationDimension_addCheck_passed() {
        VerificationDimension dim = new VerificationDimension("test");
        dim.addCheck("check1", true);

        assertEquals(1, dim.getChecks().size());
        assertEquals("check1:true", dim.getChecks().get(0));
        assertTrue(dim.isPassed());
    }

    @Test
    void testVerificationDimension_addCheck_failed() {
        VerificationDimension dim = new VerificationDimension("test");
        dim.addCheck("check1", false);

        assertEquals(1, dim.getChecks().size());
        assertEquals("check1:false", dim.getChecks().get(0));
        assertFalse(dim.isPassed());
    }

    @Test
    void testVerificationDimension_addCheck_multipleChecks() {
        VerificationDimension dim = new VerificationDimension("test");
        dim.addCheck("check1", true);
        dim.addCheck("check2", false);
        dim.addCheck("check3", true);

        assertEquals(3, dim.getChecks().size());
        assertFalse(dim.isPassed());
    }

    @Test
    void testVerificationDimension_toMap() {
        VerificationDimension dim = new VerificationDimension("test");
        dim.addCheck("check1", true);
        Map<String, Object> map = dim.toMap();

        assertEquals("test", map.get("name"));
        assertEquals(true, map.get("passed"));
        assertNotNull(map.get("checks"));
    }

    @Test
    void testVerificationDimension_equals() {
        VerificationDimension dim1 = new VerificationDimension("test");
        dim1.addCheck("check1", true);

        VerificationDimension dim2 = new VerificationDimension("test");
        dim2.addCheck("check1", true);

        assertEquals(dim1, dim2);
    }

    @Test
    void testVerificationDimension_equals_differentName() {
        VerificationDimension dim1 = new VerificationDimension("test1");
        VerificationDimension dim2 = new VerificationDimension("test2");

        assertNotEquals(dim1, dim2);
    }
}
