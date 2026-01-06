package com.rtm.mq.tool.offset;

import com.rtm.mq.tool.exception.ValidationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.parser.ArrayInfo;
import com.rtm.mq.tool.parser.OccurrenceCountParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static offset calculator for message spec-tree fields.
 *
 * <p>This calculator walks a FieldNode tree and computes byte offsets for each
 * field based purely on structural analysis. It does NOT inspect or parse actual
 * message payloads.</p>
 *
 * <h2>Offset Calculation Rules</h2>
 * <ul>
 *   <li>Offset always starts at 0</li>
 *   <li>Field offset = previous field (offset + length)</li>
 *   <li>Parent/object nodes do NOT occupy bytes themselves</li>
 *   <li>For arrays: use occurrenceCount.max (or 1 if undefined)</li>
 *   <li>occurrenceCount = 0 means skip field entirely</li>
 *   <li>Transitory fields (groupId/occurrenceCount) participate in offset calculation</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>Missing length on leaf fields: ERROR</li>
 *   <li>Negative length: ERROR</li>
 *   <li>Negative occurrenceCount: ERROR</li>
 * </ul>
 */
public class OffsetCalculator {

    /**
     * Pattern for parsing occurrenceCount in "min..max" format.
     */
    private static final Pattern OCCURRENCE_PATTERN = Pattern.compile("(\\d+)\\.\\.(\\d+)");

    private final OccurrenceCountParser occurrenceCountParser;

    /**
     * Creates a new OffsetCalculator.
     */
    public OffsetCalculator() {
        this.occurrenceCountParser = new OccurrenceCountParser();
    }

    /**
     * Calculates offset table for a field group.
     *
     * @param messageType the message type identifier (e.g., "request", "response")
     * @param fieldGroup the field group containing the root fields
     * @return the calculated offset table
     * @throws ValidationException if the spec-tree contains invalid values
     */
    public OffsetTable calculate(String messageType, FieldGroup fieldGroup) {
        if (fieldGroup == null || fieldGroup.getFields() == null || fieldGroup.getFields().isEmpty()) {
            return new OffsetTable(messageType, 0, new ArrayList<>());
        }

        List<OffsetEntry> entries = new ArrayList<>();
        long currentOffset = 0;

        for (FieldNode node : fieldGroup.getFields()) {
            currentOffset = processNode(node, "", currentOffset, 0, entries);
        }

        return new OffsetTable(messageType, currentOffset, entries);
    }

    /**
     * Calculates offset table for a list of root field nodes.
     *
     * @param messageType the message type identifier
     * @param rootNodes the list of root field nodes
     * @return the calculated offset table
     * @throws ValidationException if the spec-tree contains invalid values
     */
    public OffsetTable calculate(String messageType, List<FieldNode> rootNodes) {
        if (rootNodes == null || rootNodes.isEmpty()) {
            return new OffsetTable(messageType, 0, new ArrayList<>());
        }

        List<OffsetEntry> entries = new ArrayList<>();
        long currentOffset = 0;

        for (FieldNode node : rootNodes) {
            currentOffset = processNode(node, "", currentOffset, 0, entries);
        }

        return new OffsetTable(messageType, currentOffset, entries);
    }

    /**
     * Processes a single node and its children, adding entries to the list.
     *
     * @param node the field node to process
     * @param parentPath the path prefix from parent nodes
     * @param currentOffset the current byte offset
     * @param nestingLevel the current nesting depth
     * @param entries the list to accumulate entries
     * @return the updated offset after processing this node and its children
     */
    private long processNode(FieldNode node, String parentPath, long currentOffset,
                             int nestingLevel, List<OffsetEntry> entries) {
        String fieldName = getFieldName(node);
        String fieldPath = buildFieldPath(parentPath, fieldName);

        // Determine occurrence count
        int occurrenceMax = parseOccurrenceMax(node.getOccurrenceCount(), fieldPath);

        // If occurrenceCount max is 0, skip this field entirely
        if (occurrenceMax == 0) {
            return currentOffset;
        }

        // Check if this is a container node (has children) or a leaf node
        boolean isContainer = node.hasChildren();

        if (isContainer) {
            // Container nodes (objects/arrays with children) do not occupy bytes themselves
            // Process children for each occurrence
            for (int i = 0; i < occurrenceMax; i++) {
                String indexedPath = occurrenceMax > 1 ? fieldPath + "[" + i + "]" : fieldPath;
                currentOffset = processChildren(node.getChildren(), indexedPath, currentOffset, nestingLevel + 1, entries);
            }
        } else {
            // Leaf node - validate and add entry
            int length = validateAndGetLength(node, fieldPath);

            // Create entries for each occurrence
            for (int i = 0; i < occurrenceMax; i++) {
                String indexedPath = occurrenceMax > 1 ? fieldPath + "[" + i + "]" : fieldPath;
                OffsetEntry entry = new OffsetEntry(indexedPath, currentOffset, length, nestingLevel);
                entries.add(entry);
                currentOffset += length;
            }
        }

        return currentOffset;
    }

    /**
     * Processes child nodes of a container.
     *
     * @param children the list of child nodes
     * @param parentPath the path prefix from the parent
     * @param currentOffset the current byte offset
     * @param nestingLevel the current nesting depth
     * @param entries the list to accumulate entries
     * @return the updated offset after processing all children
     */
    private long processChildren(List<FieldNode> children, String parentPath, long currentOffset,
                                 int nestingLevel, List<OffsetEntry> entries) {
        if (children == null || children.isEmpty()) {
            return currentOffset;
        }

        for (FieldNode child : children) {
            currentOffset = processNode(child, parentPath, currentOffset, nestingLevel, entries);
        }

        return currentOffset;
    }

    /**
     * Gets the field name from the node, preferring camelCaseName over originalName.
     *
     * @param node the field node
     * @return the field name
     */
    private String getFieldName(FieldNode node) {
        if (node.getCamelCaseName() != null && !node.getCamelCaseName().isEmpty()) {
            return node.getCamelCaseName();
        }
        return node.getOriginalName() != null ? node.getOriginalName() : "";
    }

    /**
     * Builds the complete field path by combining parent path and field name.
     *
     * @param parentPath the parent path prefix
     * @param fieldName the current field name
     * @return the complete field path
     */
    private String buildFieldPath(String parentPath, String fieldName) {
        if (parentPath == null || parentPath.isEmpty()) {
            return fieldName;
        }
        return parentPath + "." + fieldName;
    }

    /**
     * Parses the occurrence count and returns the max value.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Null or empty: returns 1</li>
     *   <li>Valid format "min..max": returns max</li>
     *   <li>max = 0: returns 0 (skip field)</li>
     *   <li>Negative values: ERROR</li>
     * </ul>
     *
     * @param occurrenceCount the occurrence count string
     * @param fieldPath the field path for error messages
     * @return the max occurrence count (0 or positive)
     * @throws ValidationException if the occurrence count is invalid
     */
    private int parseOccurrenceMax(String occurrenceCount, String fieldPath) {
        if (occurrenceCount == null || occurrenceCount.trim().isEmpty()) {
            return 1;
        }

        String trimmed = occurrenceCount.trim();
        Matcher matcher = OCCURRENCE_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            throw new ValidationException("Invalid occurrenceCount format '" + occurrenceCount +
                    "' at field '" + fieldPath + "'. Expected format: 'min..max'");
        }

        int min = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));

        if (min < 0 || max < 0) {
            throw new ValidationException("Negative occurrenceCount value at field '" + fieldPath +
                    "': " + occurrenceCount);
        }

        return max;
    }

    /**
     * Validates the field length and returns it.
     *
     * @param node the field node
     * @param fieldPath the field path for error messages
     * @return the validated length
     * @throws ValidationException if length is missing or negative
     */
    private int validateAndGetLength(FieldNode node, String fieldPath) {
        Integer length = node.getLength();

        if (length == null) {
            throw new ValidationException("Missing length for leaf field '" + fieldPath + "'");
        }

        if (length < 0) {
            throw new ValidationException("Negative length (" + length + ") for field '" + fieldPath + "'");
        }

        return length;
    }
}
