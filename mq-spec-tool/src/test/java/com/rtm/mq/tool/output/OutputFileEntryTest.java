package com.rtm.mq.tool.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OutputFileEntry}.
 */
class OutputFileEntryTest {

    @Test
    void testConstructor() {
        OutputFileEntry entry = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertEquals("test.txt", entry.getRelativePath());
        assertEquals(1024L, entry.getSizeBytes());
        assertEquals("abc123", entry.getSha256Hash());
    }

    @Test
    void testConstructor_nullRelativePath_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new OutputFileEntry(null, 1024L, "abc123"));
    }

    @Test
    void testConstructor_nullHash_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new OutputFileEntry("test.txt", 1024L, null));
    }

    @Test
    void testEquals_sameObject() {
        OutputFileEntry entry = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertEquals(entry, entry);
    }

    @Test
    void testEquals_equalObjects() {
        OutputFileEntry entry1 = new OutputFileEntry("test.txt", 1024L, "abc123");
        OutputFileEntry entry2 = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertEquals(entry1, entry2);
    }

    @Test
    void testEquals_differentPath() {
        OutputFileEntry entry1 = new OutputFileEntry("test1.txt", 1024L, "abc123");
        OutputFileEntry entry2 = new OutputFileEntry("test2.txt", 1024L, "abc123");
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEquals_differentSize() {
        OutputFileEntry entry1 = new OutputFileEntry("test.txt", 1024L, "abc123");
        OutputFileEntry entry2 = new OutputFileEntry("test.txt", 2048L, "abc123");
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEquals_differentHash() {
        OutputFileEntry entry1 = new OutputFileEntry("test.txt", 1024L, "abc123");
        OutputFileEntry entry2 = new OutputFileEntry("test.txt", 1024L, "def456");
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEquals_null() {
        OutputFileEntry entry = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertNotEquals(null, entry);
    }

    @Test
    void testEquals_differentClass() {
        OutputFileEntry entry = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertNotEquals("not an entry", entry);
    }

    @Test
    void testHashCode_equalObjects() {
        OutputFileEntry entry1 = new OutputFileEntry("test.txt", 1024L, "abc123");
        OutputFileEntry entry2 = new OutputFileEntry("test.txt", 1024L, "abc123");
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void testToString() {
        OutputFileEntry entry = new OutputFileEntry("test.txt", 1024L, "abc123");
        String str = entry.toString();
        assertTrue(str.contains("test.txt"));
        assertTrue(str.contains("1024"));
        assertTrue(str.contains("abc123"));
    }
}
