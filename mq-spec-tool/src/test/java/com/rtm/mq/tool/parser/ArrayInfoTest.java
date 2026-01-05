package com.rtm.mq.tool.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ArrayInfo.
 */
class ArrayInfoTest {

    @Test
    void constructor_setsAllProperties() {
        ArrayInfo info = new ArrayInfo(0, 9, true, true);

        assertEquals(0, info.getMin());
        assertEquals(9, info.getMax());
        assertTrue(info.isArray());
        assertTrue(info.isOptional());
    }

    @Test
    void getFixedCount_returnsMax() {
        ArrayInfo info = new ArrayInfo(1, 5, true, false);

        assertEquals(5, info.getFixedCount());
    }

    @Test
    void singleOccurrence_isNotArray() {
        ArrayInfo info = new ArrayInfo(1, 1, false, false);

        assertFalse(info.isArray());
        assertFalse(info.isOptional());
        assertEquals(1, info.getFixedCount());
    }

    @Test
    void optionalSingle_isOptionalNotArray() {
        ArrayInfo info = new ArrayInfo(0, 1, false, true);

        assertFalse(info.isArray());
        assertTrue(info.isOptional());
    }

    @Test
    void toString_containsAllInfo() {
        ArrayInfo arrayInfo = new ArrayInfo(0, 9, true, true);
        String str = arrayInfo.toString();

        assertTrue(str.contains("0..9"));
        assertTrue(str.contains("[array]"));
        assertTrue(str.contains("[optional]"));

        ArrayInfo objectInfo = new ArrayInfo(1, 1, false, false);
        String str2 = objectInfo.toString();

        assertTrue(str2.contains("1..1"));
        assertTrue(str2.contains("[object]"));
        assertTrue(str2.contains("[required]"));
    }
}
