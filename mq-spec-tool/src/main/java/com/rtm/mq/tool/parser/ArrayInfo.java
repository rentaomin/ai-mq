package com.rtm.mq.tool.parser;

/**
 * Represents array/occurrence information parsed from occurrenceCount values.
 *
 * <p>The occurrenceCount in Excel specs uses the format "min..max" to indicate
 * how many times a structure can repeat. Common patterns:</p>
 * <ul>
 *   <li>"1..1" - exactly one occurrence (object, not array)</li>
 *   <li>"0..1" - zero or one occurrence (optional object)</li>
 *   <li>"0..9" - zero to nine occurrences (optional array)</li>
 *   <li>"1..5" - one to five occurrences (required array)</li>
 * </ul>
 *
 * <p>Key determinations:</p>
 * <ul>
 *   <li>isArray: true when max > 1 (can have multiple occurrences)</li>
 *   <li>isOptional: true when min == 0 (zero occurrences allowed)</li>
 * </ul>
 *
 * <p>This class is immutable.</p>
 */
public class ArrayInfo {

    private final int min;
    private final int max;
    private final boolean isArray;
    private final boolean isOptional;

    /**
     * Creates a new ArrayInfo with the specified parameters.
     *
     * @param min the minimum occurrence count
     * @param max the maximum occurrence count
     * @param isArray true if this represents an array (max > 1)
     * @param isOptional true if zero occurrences are allowed (min == 0)
     */
    public ArrayInfo(int min, int max, boolean isArray, boolean isOptional) {
        this.min = min;
        this.max = max;
        this.isArray = isArray;
        this.isOptional = isOptional;
    }

    /**
     * Gets the minimum occurrence count.
     *
     * @return the minimum count
     */
    public int getMin() {
        return min;
    }

    /**
     * Gets the maximum occurrence count.
     *
     * @return the maximum count
     */
    public int getMax() {
        return max;
    }

    /**
     * Checks if this represents an array type.
     *
     * <p>A field is considered an array when max > 1, meaning it can
     * contain multiple occurrences of the nested structure.</p>
     *
     * @return true if this is an array type
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Checks if this is optional.
     *
     * <p>A field is optional when min == 0, meaning zero occurrences
     * are valid.</p>
     *
     * @return true if this is optional
     */
    public boolean isOptional() {
        return isOptional;
    }

    /**
     * Gets the fixed count for XML RepeatingField generation.
     *
     * <p>This returns the maximum count, which is used when generating
     * XML bean definitions that need a fixed repeat count.</p>
     *
     * @return the fixed count (same as max)
     */
    public int getFixedCount() {
        return max;
    }

    @Override
    public String toString() {
        return min + ".." + max + (isArray ? " [array]" : " [object]") +
               (isOptional ? " [optional]" : " [required]");
    }
}
