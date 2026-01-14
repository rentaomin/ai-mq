package com.rtm.mq.tool.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an MQ message field specification from a standalone file.
 *
 * <p>This model is distinct from embedded Shared Header sheets:</p>
 * <ul>
 *   <li>Source: Standalone MQ message file (e.g., ISM v2.0 FIX mapping.xlsx)</li>
 *   <li>Purpose: Field reference for comparison and validation</li>
 *   <li>Does NOT contain metadata (Operation Name, Operation ID, Version)</li>
 *   <li>Provides search capabilities by field name and byte offset</li>
 * </ul>
 *
 * @see MqMessageFormat
 * @see ComparisonResult
 */
public class MqMessageModel {

    /** Source file path */
    private String sourceFile;

    /** Format type (STANDARD, ISM_V2_FIX, UNKNOWN) */
    private MqMessageFormat format;

    /** Parsed field definitions */
    private FieldGroup fields;

    /** Indexed by field name (lowercase) for fast lookup */
    private Map<String, FieldNode> fieldIndex;

    /** Indexed by byte offset (for fixed-format messages like ISM) */
    private Map<Integer, FieldNode> offsetIndex;

    /**
     * Constructs an empty MqMessageModel.
     */
    public MqMessageModel() {
        this.format = MqMessageFormat.UNKNOWN;
        this.fields = new FieldGroup();
    }

    /**
     * Gets the source file path.
     *
     * @return the source file path
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the source file path.
     *
     * @param sourceFile the source file path to set
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * Gets the format type.
     *
     * @return the format
     */
    public MqMessageFormat getFormat() {
        return format;
    }

    /**
     * Sets the format type.
     *
     * @param format the format to set
     */
    public void setFormat(MqMessageFormat format) {
        this.format = format != null ? format : MqMessageFormat.UNKNOWN;
    }

    /**
     * Gets the field group.
     *
     * @return the field group
     */
    public FieldGroup getFields() {
        return fields;
    }

    /**
     * Sets the field group.
     *
     * @param fields the field group to set
     */
    public void setFields(FieldGroup fields) {
        this.fields = fields != null ? fields : new FieldGroup();
    }

    /**
     * Builds search indices from the fields.
     * Must be called after fields are populated.
     *
     * <p>This method indexes all fields by name (case-insensitive) and
     * by byte offset (if available, for fixed-format messages).</p>
     */
    public void buildIndices() {
        this.fieldIndex = new HashMap<>();
        this.offsetIndex = new HashMap<>();

        if (fields != null && fields.getFields() != null) {
            for (FieldNode field : fields.getFields()) {
                indexField(field);
            }
        }
    }

    /**
     * Recursively indexes a field and its children.
     */
    private void indexField(FieldNode field) {
        if (field == null) {
            return;
        }

        // Index by name (case-insensitive)
        if (field.getOriginalName() != null) {
            fieldIndex.put(field.getOriginalName().toLowerCase(), field);
        }

        // Index by byte offset (if available)
        if (field.getSource() != null && field.getSource().getByteOffset() != null) {
            offsetIndex.put(field.getSource().getByteOffset(), field);
        }

        // Recursively index children
        if (field.getChildren() != null && !field.getChildren().isEmpty()) {
            for (FieldNode child : field.getChildren()) {
                indexField(child);
            }
        }
    }

    /**
     * Finds field by name (case-insensitive).
     *
     * @param name the field name
     * @return the FieldNode if found, null otherwise
     */
    public FieldNode findFieldByName(String name) {
        if (name == null || fieldIndex == null) {
            return null;
        }
        return fieldIndex.get(name.toLowerCase());
    }

    /**
     * Finds field by byte offset (fixed-format only).
     *
     * <p>This is primarily useful for ISM v2.0 FIX format fields
     * which have specific byte offsets.</p>
     *
     * @param offset the byte offset
     * @return the FieldNode if found, null otherwise
     */
    public FieldNode findFieldByOffset(int offset) {
        if (offsetIndex == null) {
            return null;
        }
        return offsetIndex.get(offset);
    }

    /**
     * Returns all field names in definition order.
     *
     * @return list of field names (unmodifiable)
     */
    public List<String> getFieldNames() {
        if (fields == null || fields.getFields() == null) {
            return List.of();
        }
        return fields.getFields().stream()
                .map(FieldNode::getOriginalName)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Compares this MQ message with another field group.
     *
     * <p>Returns a ComparisonResult that contains:</p>
     * <ul>
     *   <li>Matching fields (present in both)</li>
     *   <li>Missing fields (in MQ message but not in target)</li>
     *   <li>Extra fields (in target but not in MQ message)</li>
     *   <li>Field differences (same name but different properties)</li>
     * </ul>
     *
     * @param other the field group to compare with
     * @return the comparison result
     */
    public ComparisonResult compareWith(FieldGroup other) {
        ComparisonResult result = new ComparisonResult();

        if (fields == null || fields.getFields() == null) {
            // No fields in MQ message - all target fields are "extra"
            if (other != null && other.getFields() != null) {
                result.getExtraInTarget().addAll(other.getFields());
            }
            return result;
        }

        if (other == null || other.getFields() == null) {
            // No fields in target - all MQ message fields are "missing"
            result.getMissingInTarget().addAll(fields.getFields());
            return result;
        }

        // Compare fields
        Map<String, FieldNode> targetMap = new HashMap<>();
        for (FieldNode target : other.getFields()) {
            if (target.getOriginalName() != null) {
                targetMap.put(target.getOriginalName().toLowerCase(), target);
            }
        }

        // Find matches and missing
        for (FieldNode mqField : fields.getFields()) {
            String lowerName = mqField.getOriginalName() != null
                    ? mqField.getOriginalName().toLowerCase()
                    : "";

            FieldNode targetField = targetMap.remove(lowerName);

            if (targetField != null) {
                // Found a match - check for differences
                FieldMatch match = new FieldMatch(mqField, targetField);
                if (hasDifferences(mqField, targetField)) {
                    FieldDifference diff = new FieldDifference(mqField, targetField);
                    result.getDifferences().add(diff);
                } else {
                    result.getMatches().add(match);
                }
            } else {
                // Field missing in target
                result.getMissingInTarget().add(mqField);
            }
        }

        // Remaining fields in target are "extra"
        result.getExtraInTarget().addAll(targetMap.values());

        return result;
    }

    /**
     * Checks if two fields have different properties.
     */
    private boolean hasDifferences(FieldNode mq, FieldNode target) {
        if (mq == null || target == null) {
            return true;
        }

        // Compare key properties
        if (!safeEquals(mq.getLength(), target.getLength())) {
            return true;
        }

        if (!safeEquals(mq.getDataType(), target.getDataType())) {
            return true;
        }

        if (!safeEquals(mq.getOptionality(), target.getOptionality())) {
            return true;
        }

        return false;
    }

    /**
     * Safe equals comparison for nullable objects.
     */
    private boolean safeEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        return "MqMessageModel{" +
                "sourceFile='" + sourceFile + '\'' +
                ", format=" + format +
                ", fields=" + (fields != null ? fields.getFields().size() + " fields" : "null") +
                '}';
    }
}
