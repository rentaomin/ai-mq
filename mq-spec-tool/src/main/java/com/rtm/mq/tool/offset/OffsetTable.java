package com.rtm.mq.tool.offset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the complete offset table for a message type.
 *
 * <p>An offset table contains all field offset entries for a message structure,
 * maintaining stable ordering based on the spec-tree traversal. The total
 * length represents the sum of all field lengths in the structure.</p>
 *
 * <p>This class is immutable - entries are stored as an unmodifiable list.</p>
 */
public class OffsetTable {

    private final String messageType;
    private final long totalLength;
    private final List<OffsetEntry> entries;

    /**
     * Creates a new OffsetTable with the specified parameters.
     *
     * @param messageType the type identifier for this message (e.g., "request", "response")
     * @param totalLength the total byte length of all fields combined
     * @param entries the ordered list of offset entries
     */
    public OffsetTable(String messageType, long totalLength, List<OffsetEntry> entries) {
        this.messageType = messageType;
        this.totalLength = totalLength;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Gets the message type identifier.
     *
     * @return the message type (e.g., "request", "response")
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Gets the total byte length of the message structure.
     *
     * <p>This is the sum of all field lengths, including expanded array
     * occurrences. For nested objects, only leaf field lengths contribute
     * to this total.</p>
     *
     * @return the total length in bytes
     */
    public long getTotalLength() {
        return totalLength;
    }

    /**
     * Gets the ordered list of offset entries.
     *
     * <p>Entries are ordered according to spec-tree traversal order, which
     * preserves the original field order from the Excel specification.</p>
     *
     * @return an unmodifiable list of offset entries (never null, may be empty)
     */
    public List<OffsetEntry> getEntries() {
        return entries;
    }

    /**
     * Gets the number of entries in this table.
     *
     * @return the entry count
     */
    public int size() {
        return entries.size();
    }

    /**
     * Checks if this table has no entries.
     *
     * @return true if the table is empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public String toString() {
        return "OffsetTable{" +
                "messageType='" + messageType + '\'' +
                ", totalLength=" + totalLength +
                ", entryCount=" + entries.size() +
                '}';
    }
}
