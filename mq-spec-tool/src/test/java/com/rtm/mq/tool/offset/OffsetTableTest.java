package com.rtm.mq.tool.offset;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OffsetTable.
 */
class OffsetTableTest {

    @Test
    void constructor_setsAllProperties() {
        List<OffsetEntry> entries = Arrays.asList(
                new OffsetEntry("field1", 0, 10, 0),
                new OffsetEntry("field2", 10, 20, 0)
        );

        OffsetTable table = new OffsetTable("request", 30, entries);

        assertEquals("request", table.getMessageType());
        assertEquals(30, table.getTotalLength());
        assertEquals(2, table.size());
        assertEquals(2, table.getEntries().size());
    }

    @Test
    void entries_unmodifiable() {
        List<OffsetEntry> entries = new ArrayList<>();
        entries.add(new OffsetEntry("field1", 0, 10, 0));

        OffsetTable table = new OffsetTable("request", 10, entries);

        assertThrows(UnsupportedOperationException.class,
                () -> table.getEntries().add(new OffsetEntry("field2", 10, 5, 0)));
    }

    @Test
    void entries_defensiveCopy() {
        List<OffsetEntry> entries = new ArrayList<>();
        entries.add(new OffsetEntry("field1", 0, 10, 0));

        OffsetTable table = new OffsetTable("request", 10, entries);

        // Modify original list
        entries.add(new OffsetEntry("field2", 10, 5, 0));

        // Table should not be affected
        assertEquals(1, table.size());
    }

    @Test
    void isEmpty_emptyList_returnsTrue() {
        OffsetTable table = new OffsetTable("request", 0, Collections.emptyList());

        assertTrue(table.isEmpty());
        assertEquals(0, table.size());
    }

    @Test
    void isEmpty_nonEmptyList_returnsFalse() {
        List<OffsetEntry> entries = Collections.singletonList(
                new OffsetEntry("field", 0, 10, 0)
        );

        OffsetTable table = new OffsetTable("request", 10, entries);

        assertFalse(table.isEmpty());
        assertEquals(1, table.size());
    }

    @Test
    void toString_containsKeyInfo() {
        List<OffsetEntry> entries = Arrays.asList(
                new OffsetEntry("field1", 0, 10, 0),
                new OffsetEntry("field2", 10, 20, 0)
        );

        OffsetTable table = new OffsetTable("response", 30, entries);

        String str = table.toString();

        assertTrue(str.contains("response"));
        assertTrue(str.contains("30"));
        assertTrue(str.contains("2"));
    }

    @Test
    void preservesEntryOrder() {
        List<OffsetEntry> entries = Arrays.asList(
                new OffsetEntry("first", 0, 10, 0),
                new OffsetEntry("second", 10, 10, 0),
                new OffsetEntry("third", 20, 10, 0)
        );

        OffsetTable table = new OffsetTable("request", 30, entries);

        assertEquals("first", table.getEntries().get(0).getFieldPath());
        assertEquals("second", table.getEntries().get(1).getFieldPath());
        assertEquals("third", table.getEntries().get(2).getFieldPath());
    }
}
