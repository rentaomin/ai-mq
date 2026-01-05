package com.rtm.mq.tool.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NestingDepthValidator.
 */
class NestingDepthValidatorTest {

    private NestingDepthValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NestingDepthValidator();
    }

    @Test
    @DisplayName("Should return true when depth is within default limit")
    void validateDepth_withinLimit_returnsTrue() {
        // When
        boolean result = validator.validateDepth(25, 100, "testField");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when depth equals maximum")
    void validateDepth_equalsMaximum_returnsTrue() {
        // When
        boolean result = validator.validateDepth(50, 100, "testField");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when depth exceeds default limit")
    void validateDepth_exceedsLimit_returnsFalse() {
        // When
        boolean result = validator.validateDepth(51, 100, "testField");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when depth greatly exceeds limit")
    void validateDepth_greatlyExceedsLimit_returnsFalse() {
        // When
        boolean result = validator.validateDepth(100, 200, "testField");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when depth is at level 1")
    void validateDepth_levelOne_returnsTrue() {
        // When
        boolean result = validator.validateDepth(1, 10, "rootField");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should use custom maximum depth when provided")
    void constructor_customMaxDepth_usesCustomValue() {
        // Given
        NestingDepthValidator customValidator = new NestingDepthValidator(10);

        // When
        boolean withinLimit = customValidator.validateDepth(10, 50, "testField");
        boolean exceedsLimit = customValidator.validateDepth(11, 51, "testField");

        // Then
        assertTrue(withinLimit);
        assertFalse(exceedsLimit);
        assertEquals(10, customValidator.getMaxDepth());
    }

    @Test
    @DisplayName("Should use default depth when custom value is invalid")
    void constructor_invalidMaxDepth_usesDefault() {
        // Given
        NestingDepthValidator invalidValidator = new NestingDepthValidator(-5);

        // When & Then
        assertEquals(50, invalidValidator.getMaxDepth()); // Should use default
    }

    @Test
    @DisplayName("Should use default depth when custom value is zero")
    void constructor_zeroMaxDepth_usesDefault() {
        // Given
        NestingDepthValidator zeroValidator = new NestingDepthValidator(0);

        // When & Then
        assertEquals(50, zeroValidator.getMaxDepth()); // Should use default
    }

    @Test
    @DisplayName("Should accept very small custom maximum depth")
    void constructor_smallCustomMaxDepth_acceptsValue() {
        // Given
        NestingDepthValidator smallValidator = new NestingDepthValidator(3);

        // When
        boolean valid = smallValidator.validateDepth(3, 10, "field");
        boolean invalid = smallValidator.validateDepth(4, 11, "field");

        // Then
        assertTrue(valid);
        assertFalse(invalid);
        assertEquals(3, smallValidator.getMaxDepth());
    }

    @Test
    @DisplayName("Should accept very large custom maximum depth")
    void constructor_largeCustomMaxDepth_acceptsValue() {
        // Given
        NestingDepthValidator largeValidator = new NestingDepthValidator(1000);

        // When
        boolean valid = largeValidator.validateDepth(999, 10, "field");

        // Then
        assertTrue(valid);
        assertEquals(1000, largeValidator.getMaxDepth());
    }

    @Test
    @DisplayName("Should get default maximum depth")
    void getMaxDepth_defaultConstructor_returnsDefaultValue() {
        // When
        int maxDepth = validator.getMaxDepth();

        // Then
        assertEquals(50, maxDepth);
    }

    @Test
    @DisplayName("Should handle multiple validations consistently")
    void validateDepth_multipleValidations_consistentResults() {
        // Given
        NestingDepthValidator testValidator = new NestingDepthValidator(5);

        // When & Then
        assertTrue(testValidator.validateDepth(1, 10, "field1"));
        assertTrue(testValidator.validateDepth(3, 20, "field2"));
        assertTrue(testValidator.validateDepth(5, 30, "field3"));
        assertFalse(testValidator.validateDepth(6, 40, "field4"));
        assertFalse(testValidator.validateDepth(10, 50, "field5"));
    }

    @Test
    @DisplayName("Should not throw exception when depth exceeds limit")
    void validateDepth_depthExceeds_doesNotThrowException() {
        // When & Then - should not throw, just return false
        assertDoesNotThrow(() -> validator.validateDepth(100, 200, "deepField"));
    }
}
