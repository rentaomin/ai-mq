package com.rtm.mq.tool.offset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OffsetEntry.
 */
class OffsetEntryTest {

    @Test
    void constructor_setsAllProperties() {
        OffsetEntry entry = new OffsetEntry("header.msgType", 100, 20, 1);

        assertEquals("header.msgType", entry.getFieldPath());
        assertEquals(100, entry.getOffset());
        assertEquals(20, entry.getLength());
        assertEquals(120, entry.getEndOffset());
        assertEquals(1, entry.getNestingLevel());
    }

    @Test
    void endOffset_calculatedCorrectly() {
        OffsetEntry entry = new OffsetEntry("field", 50, 30, 0);

        assertEquals(80, entry.getEndOffset());
    }

    @Test
    void zeroOffset_valid() {
        OffsetEntry entry = new OffsetEntry("first", 0, 10, 0);

        assertEquals(0, entry.getOffset());
        assertEquals(10, entry.getEndOffset());
    }

    @Test
    void zeroLength_valid() {
        OffsetEntry entry = new OffsetEntry("marker", 50, 0, 0);

        assertEquals(50, entry.getOffset());
        assertEquals(0, entry.getLength());
        assertEquals(50, entry.getEndOffset());
    }

    @Test
    void toString_containsAllFields() {
        OffsetEntry entry = new OffsetEntry("items[0].name", 100, 20, 2);

        String str = entry.toString();

        assertTrue(str.contains("items[0].name"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("20"));
        assertTrue(str.contains("120"));
        assertTrue(str.contains("2"));
    }
}
