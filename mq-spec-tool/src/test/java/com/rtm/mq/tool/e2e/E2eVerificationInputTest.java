package com.rtm.mq.tool.e2e;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link E2eVerificationInput}.
 */
class E2eVerificationInputTest {

    @Test
    void testDefaultConstructor() {
        E2eVerificationInput input = new E2eVerificationInput();

        assertFalse(input.isAuditLogPresent());
        assertFalse(input.isManifestPresent());
        assertFalse(input.isConsistencyResultPresent());
        assertFalse(input.isMessageValidationResultPresent());
        assertFalse(input.isCliInvocationPresent());
        assertNotNull(input.getManifestCategories());
        assertTrue(input.getManifestCategories().isEmpty());
        assertNotNull(input.getFixtureIdentifiers());
        assertTrue(input.getFixtureIdentifiers().isEmpty());
    }

    @Test
    void testAuditLogMetadata() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setAuditLogPresent(true);
        input.setAuditHasToolStart(true);
        input.setAuditHasToolCompletion(true);
        input.setAuditHasToolFailure(false);
        input.setAuditHasValidationResults(true);
        input.setAuditHasTransactionOutcome(true);
        input.setAuditExitCode(0);

        assertTrue(input.isAuditLogPresent());
        assertTrue(input.isAuditHasToolStart());
        assertTrue(input.isAuditHasToolCompletion());
        assertFalse(input.isAuditHasToolFailure());
        assertTrue(input.isAuditHasValidationResults());
        assertTrue(input.isAuditHasTransactionOutcome());
        assertEquals(0, input.getAuditExitCode());
    }

    @Test
    void testManifestMetadata() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setManifestPresent(true);
        input.setManifestTransactionId("tx-123");
        input.setManifestFileCount(5);
        input.setManifestCategories(Arrays.asList("xml", "java", "openapi"));

        assertTrue(input.isManifestPresent());
        assertEquals("tx-123", input.getManifestTransactionId());
        assertEquals(5, input.getManifestFileCount());
        assertEquals(3, input.getManifestCategories().size());
    }

    @Test
    void testManifestCategories_nullSetsEmptyList() {
        E2eVerificationInput input = new E2eVerificationInput();
        input.setManifestCategories(null);

        assertNotNull(input.getManifestCategories());
        assertTrue(input.getManifestCategories().isEmpty());
    }

    @Test
    void testConsistencyMetadata() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setConsistencyResultPresent(true);
        input.setConsistencyPassed(true);
        input.setConsistencyIssueCount(0);

        assertTrue(input.isConsistencyResultPresent());
        assertTrue(input.isConsistencyPassed());
        assertEquals(0, input.getConsistencyIssueCount());
    }

    @Test
    void testMessageValidationMetadata() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setMessageValidationResultPresent(true);
        input.setMessageValidationPassed(false);
        input.setMessageValidationIssueCount(3);

        assertTrue(input.isMessageValidationResultPresent());
        assertFalse(input.isMessageValidationPassed());
        assertEquals(3, input.getMessageValidationIssueCount());
    }

    @Test
    void testCliInvocationMetadata() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setCliInvocationPresent(true);
        input.setResolvedCommand("generate");
        input.setCommandsExecutedCount(1);
        input.setCliExitCode(0);

        assertTrue(input.isCliInvocationPresent());
        assertEquals("generate", input.getResolvedCommand());
        assertEquals(1, input.getCommandsExecutedCount());
        assertEquals(0, input.getCliExitCode());
    }

    @Test
    void testFixtureIdentifiers() {
        E2eVerificationInput input = new E2eVerificationInput();

        input.setFixtureIdentifiers(Arrays.asList("fixture1", "fixture2"));
        assertEquals(2, input.getFixtureIdentifiers().size());

        input.setFixtureIdentifiers(null);
        assertNotNull(input.getFixtureIdentifiers());
        assertTrue(input.getFixtureIdentifiers().isEmpty());
    }

    @Test
    void testToMap() {
        E2eVerificationInput input = new E2eVerificationInput();
        input.setAuditLogPresent(true);
        input.setManifestPresent(true);
        input.setManifestTransactionId("tx-123");

        Map<String, Object> map = input.toMap();

        assertNotNull(map.get("auditLog"));
        assertNotNull(map.get("cli"));
        assertNotNull(map.get("consistency"));
        assertNotNull(map.get("manifest"));
        assertNotNull(map.get("messageValidation"));
    }

    @Test
    void testEquals_sameObject() {
        E2eVerificationInput input = new E2eVerificationInput();
        assertEquals(input, input);
    }

    @Test
    void testEquals_equalObjects() {
        E2eVerificationInput input1 = new E2eVerificationInput();
        input1.setAuditLogPresent(true);
        input1.setManifestTransactionId("tx-123");

        E2eVerificationInput input2 = new E2eVerificationInput();
        input2.setAuditLogPresent(true);
        input2.setManifestTransactionId("tx-123");

        assertEquals(input1, input2);
    }

    @Test
    void testEquals_differentValues() {
        E2eVerificationInput input1 = new E2eVerificationInput();
        input1.setAuditLogPresent(true);

        E2eVerificationInput input2 = new E2eVerificationInput();
        input2.setAuditLogPresent(false);

        assertNotEquals(input1, input2);
    }

    @Test
    void testEquals_null() {
        E2eVerificationInput input = new E2eVerificationInput();
        assertNotEquals(null, input);
    }

    @Test
    void testHashCode_equalObjects() {
        E2eVerificationInput input1 = new E2eVerificationInput();
        input1.setManifestTransactionId("tx-123");

        E2eVerificationInput input2 = new E2eVerificationInput();
        input2.setManifestTransactionId("tx-123");

        assertEquals(input1.hashCode(), input2.hashCode());
    }
}
