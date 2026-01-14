package com.rtm.mq.tool.model;

/**
 * Represents a match between an MQ message field and a target field.
 *
 * <p>A match indicates that the same field exists in both the MQ message
 * and the target, with identical properties.</p>
 */
public class FieldMatch {

    /** The field from the MQ message */
    private FieldNode mqField;

    /** The corresponding field from the target (request/response) */
    private FieldNode targetField;

    /**
     * Constructs a FieldMatch.
     *
     * @param mqField the MQ message field
     * @param targetField the target field
     */
    public FieldMatch(FieldNode mqField, FieldNode targetField) {
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

    @Override
    public String toString() {
        return "FieldMatch{" +
                "field='" + (mqField != null ? mqField.getOriginalName() : "null") + '\'' +
                '}';
    }
}
