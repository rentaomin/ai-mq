package com.rtm.mq.tool.exception;

import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationException}.
 */
class ValidationExceptionTest {

    @Test
    void testBasicConstructor() {
        ValidationException exception = new ValidationException("VR-106 - groupId found in Java Bean");

        assertEquals("VR-106 - groupId found in Java Bean", exception.getMessage());
        assertEquals(ExitCodes.VALIDATION_ERROR, exception.getExitCode());
        assertNull(exception.getResult());
    }

    @Test
    void testConstructorWithValidationResult() {
        ValidationResult result = new ValidationResult();
        result.addError(new ValidationError("VR-106", "groupId found in Java Bean", "Field 'groupId' should not appear in Java Bean output"));

        ValidationException exception = new ValidationException("Validation failed", result);

        assertEquals("Validation failed", exception.getMessage());
        assertEquals(ExitCodes.VALIDATION_ERROR, exception.getExitCode());
        assertSame(result, exception.getResult());
        assertFalse(exception.getResult().isSuccess());
        assertEquals(1, exception.getResult().getErrors().size());
    }

    @Test
    void testInheritsMqToolException() {
        ValidationException exception = new ValidationException("Error");
        assertTrue(exception instanceof MqToolException);
    }

    @Test
    void testResultIsNullWhenNotProvided() {
        ValidationException exception = new ValidationException("Simple error");
        assertNull(exception.getResult());
    }

    @Test
    void testWithEmptyValidationResult() {
        ValidationResult result = new ValidationResult();
        ValidationException exception = new ValidationException("No errors but wrapped", result);

        assertNotNull(exception.getResult());
        assertTrue(exception.getResult().isSuccess());
        assertTrue(exception.getResult().getErrors().isEmpty());
    }

    @Test
    void testWithMultipleErrors() {
        ValidationResult result = new ValidationResult();
        result.addError(new ValidationError("VR-101", "Error 1", "Details 1"));
        result.addError(new ValidationError("VR-102", "Error 2", "Details 2"));
        result.addError(new ValidationError("VR-103", "Error 3", "Details 3"));

        ValidationException exception = new ValidationException("Multiple validation failures", result);

        assertEquals(3, exception.getResult().getErrors().size());
    }
}
