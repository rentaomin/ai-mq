package com.rtm.mq.tool.validator.openapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiValidationIssue.
 */
class OpenApiValidationIssueTest {

    @Test
    void testConstructorAndGetters() {
        OpenApiValidationIssue issue = new OpenApiValidationIssue(
                "openapi.yaml",
                "#/components/schemas/Test",
                "OA-001",
                OpenApiValidationIssue.Severity.ERROR,
                "Test message"
        );

        assertEquals("openapi.yaml", issue.getFilePath());
        assertEquals("#/components/schemas/Test", issue.getSchemaPath());
        assertEquals("OA-001", issue.getRuleId());
        assertEquals(OpenApiValidationIssue.Severity.ERROR, issue.getSeverity());
        assertEquals("Test message", issue.getMessage());
    }

    @Test
    void testIsError() {
        OpenApiValidationIssue errorIssue = new OpenApiValidationIssue(
                "file.yaml", "#/path", "OA-001",
                OpenApiValidationIssue.Severity.ERROR, "Error");

        OpenApiValidationIssue warningIssue = new OpenApiValidationIssue(
                "file.yaml", "#/path", "OA-001",
                OpenApiValidationIssue.Severity.WARNING, "Warning");

        assertTrue(errorIssue.isError());
        assertFalse(errorIssue.isWarning());
        assertFalse(warningIssue.isError());
        assertTrue(warningIssue.isWarning());
    }

    @Test
    void testToString() {
        OpenApiValidationIssue issue = new OpenApiValidationIssue(
                "test.yaml",
                "#/test/path",
                "OA-007",
                OpenApiValidationIssue.Severity.ERROR,
                "Forbidden field"
        );

        String str = issue.toString();
        assertTrue(str.contains("ERROR"));
        assertTrue(str.contains("OA-007"));
        assertTrue(str.contains("test.yaml"));
        assertTrue(str.contains("#/test/path"));
        assertTrue(str.contains("Forbidden field"));
    }
}
