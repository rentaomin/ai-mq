package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for occurrenceCount values from Excel specifications.
 *
 * <p>Parses occurrenceCount strings in the format "min..max" where both
 * min and max are non-negative integers. Examples:</p>
 * <ul>
 *   <li>"0..9" - optional, up to 9 occurrences (array)</li>
 *   <li>"1..1" - required, exactly one (object)</li>
 *   <li>"1..2" - required, up to 2 (array)</li>
 *   <li>"0..1" - optional, at most one (optional object)</li>
 * </ul>
 *
 * <p>This parser determines:</p>
 * <ul>
 *   <li>Whether the field is an array (max > 1)</li>
 *   <li>Whether the field is optional (min == 0)</li>
 *   <li>The fixed count for XML generation (max value)</li>
 * </ul>
 */
public class OccurrenceCountParser {

    /**
     * Pattern for parsing "min..max" format.
     * Captures two groups: the minimum and maximum values.
     */
    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.\\.(\\d+)");

    /**
     * Parses an occurrenceCount value into ArrayInfo.
     *
     * @param occurrenceCount the occurrence count string (e.g., "0..9", "1..1")
     * @return the parsed ArrayInfo, or null if input is null or empty
     * @throws ParseException if the format is invalid
     */
    public ArrayInfo parse(String occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(occurrenceCount.trim());
        if (!matcher.matches()) {
            throw new ParseException("Invalid occurrenceCount format: " + occurrenceCount +
                ". Expected format: 'min..max' (e.g., '0..9', '1..1')");
        }

        int min = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));

        // max > 1 indicates an array (can have multiple occurrences)
        boolean isArray = (max > 1);
        // min == 0 indicates optional (zero occurrences allowed)
        boolean isOptional = (min == 0);

        return new ArrayInfo(min, max, isArray, isOptional);
    }

    /**
     * Calculates the fixed count for XML RepeatingField generation.
     *
     * <p>Returns the maximum occurrence count, which is used when generating
     * XML bean definitions that need a repeat count. Returns 1 if the
     * occurrence count is null or empty.</p>
     *
     * @param occurrenceCount the occurrence count string
     * @return the fixed count (max value, or 1 if not specified)
     */
    public int calculateFixedCount(String occurrenceCount) {
        ArrayInfo info = parse(occurrenceCount);
        if (info == null) {
            return 1;
        }
        return info.getMax();
    }
}
