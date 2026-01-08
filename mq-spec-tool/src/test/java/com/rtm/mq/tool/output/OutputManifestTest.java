package com.rtm.mq.tool.output;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OutputManifest}.
 */
class OutputManifestTest {

    @Test
    void testConstructor_withTimestamp() {
        List<OutputFileEntry> files = Arrays.asList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest = new OutputManifest("tx-123", "2024-01-01T12:00:00Z", files);

        assertEquals("tx-123", manifest.getTransactionId());
        assertEquals("2024-01-01T12:00:00Z", manifest.getTimestamp());
        assertEquals(1, manifest.getFileCount());
    }

    @Test
    void testConstructor_withoutTimestamp() {
        List<OutputFileEntry> files = new ArrayList<>();
        OutputManifest manifest = new OutputManifest("tx-123", files);

        assertEquals("tx-123", manifest.getTransactionId());
        assertNotNull(manifest.getTimestamp());
        assertEquals(0, manifest.getFileCount());
    }

    @Test
    void testConstructor_nullTransactionId_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new OutputManifest(null, "timestamp", Collections.emptyList()));
    }

    @Test
    void testConstructor_nullTimestamp_throwsException() {
        assertThrows(NullPointerException.class,
                () -> new OutputManifest("tx-123", null, Collections.emptyList()));
    }

    @Test
    void testConstructor_nullFiles_emptyList() {
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", null);
        assertEquals(0, manifest.getFileCount());
        assertNotNull(manifest.getFiles());
    }

    @Test
    void testGetFiles_returnsUnmodifiableList() {
        List<OutputFileEntry> files = new ArrayList<>();
        files.add(new OutputFileEntry("test.txt", 100L, "hash1"));
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", files);

        assertThrows(UnsupportedOperationException.class,
                () -> manifest.getFiles().add(new OutputFileEntry("new.txt", 50L, "hash2")));
    }

    @Test
    void testGetFileCount() {
        List<OutputFileEntry> files = Arrays.asList(
                new OutputFileEntry("a.txt", 100L, "hash1"),
                new OutputFileEntry("b.txt", 200L, "hash2"),
                new OutputFileEntry("c.txt", 300L, "hash3")
        );
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", files);

        assertEquals(3, manifest.getFileCount());
    }

    @Test
    void testGetTotalSizeBytes() {
        List<OutputFileEntry> files = Arrays.asList(
                new OutputFileEntry("a.txt", 100L, "hash1"),
                new OutputFileEntry("b.txt", 200L, "hash2"),
                new OutputFileEntry("c.txt", 300L, "hash3")
        );
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", files);

        assertEquals(600L, manifest.getTotalSizeBytes());
    }

    @Test
    void testGetTotalSizeBytes_emptyFiles() {
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", Collections.emptyList());
        assertEquals(0L, manifest.getTotalSizeBytes());
    }

    @Test
    void testEquals_sameObject() {
        List<OutputFileEntry> files = Collections.singletonList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", files);
        assertEquals(manifest, manifest);
    }

    @Test
    void testEquals_equalObjects() {
        List<OutputFileEntry> files = Collections.singletonList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest1 = new OutputManifest("tx-123", "timestamp", files);
        OutputManifest manifest2 = new OutputManifest("tx-123", "timestamp", files);
        assertEquals(manifest1, manifest2);
    }

    @Test
    void testEquals_differentTransactionId() {
        List<OutputFileEntry> files = Collections.singletonList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest1 = new OutputManifest("tx-123", "timestamp", files);
        OutputManifest manifest2 = new OutputManifest("tx-456", "timestamp", files);
        assertNotEquals(manifest1, manifest2);
    }

    @Test
    void testEquals_null() {
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", Collections.emptyList());
        assertNotEquals(null, manifest);
    }

    @Test
    void testHashCode_equalObjects() {
        List<OutputFileEntry> files = Collections.singletonList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest1 = new OutputManifest("tx-123", "timestamp", files);
        OutputManifest manifest2 = new OutputManifest("tx-123", "timestamp", files);
        assertEquals(manifest1.hashCode(), manifest2.hashCode());
    }

    @Test
    void testToString() {
        List<OutputFileEntry> files = Collections.singletonList(
                new OutputFileEntry("test.txt", 100L, "hash1")
        );
        OutputManifest manifest = new OutputManifest("tx-123", "timestamp", files);
        String str = manifest.toString();

        assertTrue(str.contains("tx-123"));
        assertTrue(str.contains("timestamp"));
        assertTrue(str.contains("fileCount=1"));
        assertTrue(str.contains("totalSizeBytes=100"));
    }
}
