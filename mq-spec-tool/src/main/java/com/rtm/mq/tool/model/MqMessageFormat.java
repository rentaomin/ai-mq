package com.rtm.mq.tool.model;

/**
 * Enumeration of supported MQ message file formats.
 *
 * <p>MQ message files can be in different formats, each with distinct
 * structure and encoding:</p>
 * <ul>
 *   <li>{@link #STANDARD} - Hierarchical format with Seg lvl (nesting levels)</li>
 *   <li>{@link #ISM_V2_FIX} - ISM v2.0 FIX fixed-format specification</li>
 *   <li>{@link #UNKNOWN} - Format cannot be determined</li>
 * </ul>
 *
 * @see MqMessageModel
 */
public enum MqMessageFormat {

    /**
     * Standard hierarchical format with Seg lvl (nesting levels).
     *
     * <p>Structure:</p>
     * <ul>
     *   <li>Contains metadata rows (Operation Name, Operation ID, Version)</li>
     *   <li>Contains Seg lvl column for hierarchical nesting</li>
     *   <li>Fields can be nested at different levels</li>
     * </ul>
     */
    STANDARD,

    /**
     * ISM v2.0 FIX fixed-format specification.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>No metadata rows (no Operation Name, Operation ID, Version)</li>
     *   <li>No Seg lvl column - all fields at same level</li>
     *   <li>Fields identified by byte position (Start Position, Length)</li>
     *   <li>Used for MQ message header specification</li>
     * </ul>
     */
    ISM_V2_FIX,

    /**
     * Unknown or unrecognized format.
     *
     * <p>Used when the format cannot be determined or is not recognized.</p>
     */
    UNKNOWN
}
