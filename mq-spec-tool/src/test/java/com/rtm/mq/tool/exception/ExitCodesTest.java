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
            ExitCodes.INTERNAL_ERROR
        };

        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i], codes[j],
                    "Exit codes at index " + i + " and " + j + " should be unique");
            }
        }
    }
}
