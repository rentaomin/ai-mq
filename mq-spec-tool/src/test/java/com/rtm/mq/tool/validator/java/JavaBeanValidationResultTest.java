package com.rtm.mq.tool.validator.java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaBeanValidationResult.
 */
class JavaBeanValidationResultTest {

    @Test
    void testEmptyResultIsSuccess() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        assertTrue(result.isSuccess());
        assertFalse(result.hasIssues());
        assertEquals(0, result.getIssueCount());
        assertEquals(0, result.getErrorCount());
        assertEquals(0, result.getWarningCount());
    }

    @Test
    void testAddError() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addError("file.java", "field.path", "JB-001", "Error message");

        assertFalse(result.isSuccess());
        assertTrue(result.hasIssues());
        assertEquals(1, result.getIssueCount());
        assertEquals(1, result.getErrorCount());
        assertEquals(0, result.getWarningCount());
    }

    @Test
    void testAddWarning() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addWarning("file.java", "field.path", "JB-002", "Warning message");

        assertTrue(result.isSuccess()); // Warnings don't fail validation
        assertTrue(result.hasIssues());
        assertEquals(1, result.getIssueCount());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getWarningCount());
    }

    @Test
    void testMixedErrorsAndWarnings() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addError("file.java", "field1", "JB-001", "Error 1");
        result.addWarning("file.java", "field2", "JB-002", "Warning 1");
        result.addError("file.java", "field3", "JB-003", "Error 2");
        result.addWarning("file.java", "field4", "JB-004", "Warning 2");

        assertFalse(result.isSuccess());
        assertEquals(4, result.getIssueCount());
        assertEquals(2, result.getErrorCount());
        assertEquals(2, result.getWarningCount());
        assertEquals(2, result.getErrors().size());
        assertEquals(2, result.getWarnings().size());
    }

    @Test
    void testAddIssue() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        JavaBeanValidationIssue issue = new JavaBeanValidationIssue(
                "file.java", "path", "JB-001",
                JavaBeanValidationIssue.Severity.ERROR, "message"
        );
        result.addIssue(issue);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getIssueCount());
        assertSame(issue, result.getIssues().get(0));
    }

    @Test
    void testMerge() {
        JavaBeanValidationResult result1 = new JavaBeanValidationResult();
        result1.addError("file1.java", "field1", "JB-001", "Error 1");

        JavaBeanValidationResult result2 = new JavaBeanValidationResult();
        result2.addError("file2.java", "field2", "JB-002", "Error 2");
        result2.addWarning("file2.java", "field3", "JB-003", "Warning 1");

        result1.merge(result2);

        assertEquals(3, result1.getIssueCount());
        assertEquals(2, result1.getErrorCount());
        assertEquals(1, result1.getWarningCount());
    }

    @Test
    void testMergeNull() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addError("file.java", "field", "JB-001", "Error");

        result.merge(null); // Should not throw

        assertEquals(1, result.getIssueCount());
    }

    @Test
    void testGetErrorsReturnsOnlyErrors() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addError("file.java", "field1", "JB-001", "Error");
        result.addWarning("file.java", "field2", "JB-002", "Warning");

        var errors = result.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).isError());
    }

    @Test
    void testGetWarningsReturnsOnlyWarnings() {
        JavaBeanValidationResult result = new JavaBeanValidationResult();
        result.addError("file.java", "field1", "JB-001", "Error");
        result.addWarning("file.java", "field2", "JB-002", "Warning");

        var warnings = result.getWarnings();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).isWarning());
    }
}
