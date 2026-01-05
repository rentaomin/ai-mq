package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OccurrenceCountParser.
 */
class OccurrenceCountParserTest {

    private OccurrenceCountParser parser;

    @BeforeEach
    void setUp() {
        parser = new OccurrenceCountParser();
    }

    @Test
    void parse_zeroToNine_returnsOptionalArray() {
        ArrayInfo info = parser.parse("0..9");

        assertNotNull(info);
        assertEquals(0, info.getMin());
        assertEquals(9, info.getMax());
        assertTrue(info.isArray());
        assertTrue(info.isOptional());
    }

    @Test
    void parse_oneToOne_returnsRequiredObject() {
        ArrayInfo info = parser.parse("1..1");

        assertNotNull(info);
        assertEquals(1, info.getMin());
        assertEquals(1, info.getMax());
        assertFalse(info.isArray());
        assertFalse(info.isOptional());
    }

    @Test
    void parse_oneToTwo_returnsRequiredArray() {
        ArrayInfo info = parser.parse("1..2");

        assertNotNull(info);
        assertEquals(1, info.getMin());
        assertEquals(2, info.getMax());
        assertTrue(info.isArray());
        assertFalse(info.isOptional());
    }

    @Test
    void parse_zeroToOne_returnsOptionalObject() {
        ArrayInfo info = parser.parse("0..1");

        assertNotNull(info);
        assertEquals(0, info.getMin());
        assertEquals(1, info.getMax());
        assertFalse(info.isArray());
        assertTrue(info.isOptional());
    }

    @ParameterizedTest
    @CsvSource({
        "0..9, 0, 9, true, true",
        "1..1, 1, 1, false, false",
        "0..1, 0, 1, false, true",
        "1..5, 1, 5, true, false",
        "0..99, 0, 99, true, true",
        "2..10, 2, 10, true, false"
    })
    void parse_variousFormats_correctResults(String input, int expectedMin, int expectedMax,
                                              boolean expectedIsArray, boolean expectedIsOptional) {
        ArrayInfo info = parser.parse(input);

        assertNotNull(info);
        assertEquals(expectedMin, info.getMin());
        assertEquals(expectedMax, info.getMax());
        assertEquals(expectedIsArray, info.isArray());
        assertEquals(expectedIsOptional, info.isOptional());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void parse_nullOrEmpty_returnsNull(String input) {
        ArrayInfo info = parser.parse(input);

        assertNull(info);
    }

    @Test
    void parse_withWhitespace_trimmed() {
        ArrayInfo info = parser.parse("  1..5  ");

        assertNotNull(info);
        assertEquals(1, info.getMin());
        assertEquals(5, info.getMax());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid",
        "1-5",
        "1...5",
        "1.5",
        "a..b",
        "1..N",
        "0..N"
    })
    void parse_invalidFormat_throwsParseException(String input) {
        ParseException exception = assertThrows(ParseException.class, () -> parser.parse(input));

        assertTrue(exception.getMessage().contains("Invalid occurrenceCount format"));
        assertTrue(exception.getMessage().contains(input));
    }

    @Test
    void calculateFixedCount_validInput_returnsMax() {
        assertEquals(9, parser.calculateFixedCount("0..9"));
        assertEquals(1, parser.calculateFixedCount("1..1"));
        assertEquals(5, parser.calculateFixedCount("1..5"));
    }

    @Test
    void calculateFixedCount_nullOrEmpty_returnsOne() {
        assertEquals(1, parser.calculateFixedCount(null));
        assertEquals(1, parser.calculateFixedCount(""));
        assertEquals(1, parser.calculateFixedCount("   "));
    }
}
