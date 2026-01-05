package com.rtm.mq.tool.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationResult class.
 */
class ValidationResultTest {

    @Test
    void testDefaultConstructor() {
        ValidationResult result = new ValidationResult();

        assertTrue(result.isSuccess());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getWarnings());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testConstructorWithErrors() {
        ValidationError error = new ValidationError("VR-101", "Missing field", "Field X is required");

        ValidationResult result = new ValidationResult(Arrays.asList(error));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals("VR-101", result.getErrors().get(0).getRuleCode());
    }

    @Test
    void testConstructorWithEmptyErrors() {
        ValidationResult result = new ValidationResult(Collections.emptyList());

        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testConstructorWithNullErrors() {
        ValidationResult result = new ValidationResult(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testAddError() {
        ValidationResult result = new ValidationResult();
        assertTrue(result.isSuccess());

        ValidationError error = new ValidationError("VR-102", "Invalid type", "Expected numeric");
        result.addError(error);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals("VR-102", result.getErrors().get(0).getRuleCode());
    }

    @Test
    void testAddMultipleErrors() {
        ValidationResult result = new ValidationResult();

        result.addError(new ValidationError("VR-101", "Error 1", "Details 1"));
        result.addError(new ValidationError("VR-102", "Error 2", "Details 2"));
        result.addError(new ValidationError("VR-103", "Error 3", "Details 3"));

        assertFalse(result.isSuccess());
        assertEquals(3, result.getErrors().size());

        // Verify order is preserved
        assertEquals("VR-101", result.getErrors().get(0).getRuleCode());
        assertEquals("VR-102", result.getErrors().get(1).getRuleCode());
        assertEquals("VR-103", result.getErrors().get(2).getRuleCode());
    }

    @Test
    void testAddWarning() {
        ValidationResult result = new ValidationResult();

        ValidationError warning = new ValidationError(
            "VW-001", "Deprecated field", "Consider using X instead",
            ValidationError.Severity.WARN
        );
        result.addWarning(warning);

        // Warnings don't affect success status
        assertTrue(result.isSuccess());
        assertEquals(1, result.getWarnings().size());
        assertEquals("VW-001", result.getWarnings().get(0).getRuleCode());
        assertEquals(ValidationError.Severity.WARN, result.getWarnings().get(0).getSeverity());
    }

    @Test
    void testMixedErrorsAndWarnings() {
        ValidationResult result = new ValidationResult();

        result.addWarning(new ValidationError("VW-001", "Warning 1", "Details", ValidationError.Severity.WARN));
        result.addError(new ValidationError("VR-101", "Error 1", "Details"));
        result.addWarning(new ValidationError("VW-002", "Warning 2", "Details", ValidationError.Severity.WARN));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals(2, result.getWarnings().size());
    }
}
