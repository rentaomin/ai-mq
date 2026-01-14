package com.rtm.mq.tool.model;

/**
 * Represents a difference between an MQ message field and a target field.
 *
 * <p>A difference occurs when the same field exists in both the MQ message
 * and the target, but they have different properties (length, dataType, optional, etc.).</p>
 */
public class FieldDifference {

    /** The field from the MQ message */
    private FieldNode mqField;

    /** The corresponding field from the target (request/response) */
    private FieldNode targetField;

    /**
     * Constructs a FieldDifference.
     *
     * @param mqField the MQ message field
     * @param targetField the target field
     */
    public FieldDifference(FieldNode mqField, FieldNode targetField) {
        this.mqField = mqField;
        this.targetField = targetField;
    }

    /**
     * Gets the MQ message field.
     *
     * @return the MQ message field
     */
    public FieldNode getMqField() {
        return mqField;
    }

    /**
     * Sets the MQ message field.
     *
     * @param mqField the field to set
     */
    public void setMqField(FieldNode mqField) {
        this.mqField = mqField;
    }

    /**
     * Gets the target field.
     *
     * @return the target field
     */
    public FieldNode getTargetField() {
        return targetField;
    }

    /**
     * Sets the target field.
     *
     * @param targetField the field to set
     */
    public void setTargetField(FieldNode targetField) {
        this.targetField = targetField;
    }

    /**
     * Gets a summary of the differences between the two fields.
     *
     * @return a description of the differences
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (mqField == null || targetField == null) {
            return "One field is null";
        }

        String fieldName = mqField.getOriginalName() != null
                ? mqField.getOriginalName()
                : "(unnamed)";

        sb.append("Field '").append(fieldName).append("' differs: ");

        boolean first = true;

        if (!safeEquals(mqField.getLength(), targetField.getLength())) {
            if (!first) sb.append(", ");
            sb.append("length ").append(mqField.getLength()).append(" vs ").append(targetField.getLength());
            first = false;
        }

        if (!safeEquals(mqField.getDataType(), targetField.getDataType())) {
            if (!first) sb.append(", ");
            sb.append("type ").append(mqField.getDataType()).append(" vs ").append(targetField.getDataType());
            first = false;
        }

        if (!safeEquals(mqField.getOptionality(), targetField.getOptionality())) {
            if (!first) sb.append(", ");
            sb.append("optional ").append(mqField.getOptionality()).append(" vs ").append(targetField.getOptionality());
            first = false;
        }

        if (first) {
            sb.append("(no specific property differences detected)");
        }

        return sb.toString();
    }

    /**
     * Safe equals comparison for nullable objects.
     */
    private boolean safeEquals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        return "FieldDifference{" +
                "field='" + (mqField != null ? mqField.getOriginalName() : "null") + '\'' +
                '}';
    }
}
