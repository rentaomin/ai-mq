package com.rtm.mq.tool.validator.java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaBeanValidationIssue.
 */
class JavaBeanValidationIssueTest {

    @Test
    void testConstructorAndGetters() {
        JavaBeanValidationIssue issue = new JavaBeanValidationIssue(
                "TestFile.java",
                "TestClass.fieldName",
                "JB-001",
                JavaBeanValidationIssue.Severity.ERROR,
                "Field naming violation"
        );

        assertEquals("TestFile.java", issue.getFilePath());
        assertEquals("TestClass.fieldName", issue.getPath());
        assertEquals("JB-001", issue.getRuleId());
        assertEquals(JavaBeanValidationIssue.Severity.ERROR, issue.getSeverity());
        assertEquals("Field naming violation", issue.getMessage());
    }

    @Test
    void testIsError() {
        JavaBeanValidationIssue errorIssue = new JavaBeanValidationIssue(
                "file.java", "path", "JB-001",
                JavaBeanValidationIssue.Severity.ERROR, "error message"
        );
        JavaBeanValidationIssue warningIssue = new JavaBeanValidationIssue(
                "file.java", "path", "JB-001",
                JavaBeanValidationIssue.Severity.WARNING, "warning message"
        );

        assertTrue(errorIssue.isError());
        assertFalse(errorIssue.isWarning());
        assertFalse(warningIssue.isError());
        assertTrue(warningIssue.isWarning());
    }

    @Test
    void testToString() {
        JavaBeanValidationIssue issue = new JavaBeanValidationIssue(
                "TestFile.java",
                "TestClass.field",
                "JB-002",
                JavaBeanValidationIssue.Severity.WARNING,
                "Test message"
        );

        String str = issue.toString();
        assertTrue(str.contains("WARNING"));
        assertTrue(str.contains("JB-002"));
        assertTrue(str.contains("TestFile.java"));
        assertTrue(str.contains("TestClass.field"));
        assertTrue(str.contains("Test message"));
    }

    @Test
    void testSeverityEnum() {
        assertEquals(2, JavaBeanValidationIssue.Severity.values().length);
        assertNotNull(JavaBeanValidationIssue.Severity.valueOf("ERROR"));
        assertNotNull(JavaBeanValidationIssue.Severity.valueOf("WARNING"));
    }
}
