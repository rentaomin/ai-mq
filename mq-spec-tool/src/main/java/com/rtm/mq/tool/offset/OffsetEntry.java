package com.rtm.mq.tool.offset;

/**
 * Represents offset information for a single field in the message structure.
 *
 * <p>Each entry provides the byte position and size of a field in the
 * fixed-length message layout, based purely on static structural analysis
 * of the spec-tree.</p>
 *
 * <p>Key properties:</p>
 * <ul>
 *   <li>fieldPath - unique, deterministic path identifying the field</li>
 *   <li>offset - starting byte position (0-based)</li>
 *   <li>length - field length in bytes</li>
 *   <li>endOffset - offset + length (exclusive end position)</li>
 *   <li>nestingLevel - depth in the structure hierarchy</li>
 * </ul>
 *
 * <p>This class is immutable.</p>
 */
public class OffsetEntry {

    private final String fieldPath;
    private final long offset;
    private final int length;
    private final long endOffset;
    private final int nestingLevel;

    /**
     * Creates a new OffsetEntry with the specified parameters.
     *
     * @param fieldPath the unique path identifying this field (e.g., "header.msgType")
     * @param offset the starting byte offset (0-based)
     * @param length the field length in bytes
     * @param nestingLevel the depth in the structure hierarchy (0-based)
     */
    public OffsetEntry(String fieldPath, long offset, int length, int nestingLevel) {
        this.fieldPath = fieldPath;
        this.offset = offset;
        this.length = length;
        this.endOffset = offset + length;
        this.nestingLevel = nestingLevel;
    }

    /**
     * Gets the unique field path.
     *
     * <p>The path uses dot notation for nesting and bracket notation for
     * array indices (e.g., "items[0].name").</p>
     *
     * @return the field path (never null)
     */
    public String getFieldPath() {
        return fieldPath;
    }

    /**
     * Gets the starting byte offset.
     *
     * @return the offset (0-based, non-negative)
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Gets the field length in bytes.
     *
     * @return the length (positive integer)
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the end offset (exclusive).
     *
     * <p>This equals offset + length, representing the byte position
     * immediately after this field.</p>
     *
     * @return the end offset
     */
    public long getEndOffset() {
        return endOffset;
    }

    /**
     * Gets the nesting level in the structure hierarchy.
     *
     * <p>Root-level fields have nestingLevel 0, fields nested one level
     * deep have nestingLevel 1, etc.</p>
     *
     * @return the nesting level (0-based)
     */
    public int getNestingLevel() {
        return nestingLevel;
    }

    @Override
    public String toString() {
        return "OffsetEntry{" +
                "fieldPath='" + fieldPath + '\'' +
                ", offset=" + offset +
                ", length=" + length +
                ", endOffset=" + endOffset +
                ", nestingLevel=" + nestingLevel +
                '}';
    }
}
