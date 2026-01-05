package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of related fields.
 *
 * <p>A field group is used to organize fields within a message section
 * (e.g., request, response, shared header). It maintains field order
 * using a List to ensure deterministic serialization.</p>
 *
 * @see FieldNode
 * @see MessageModel
 */
public class FieldGroup {

    /** The list of fields in this group. Uses List to preserve field order. */
    private List<FieldNode> fields = new ArrayList<>();

    /**
     * Gets the list of fields in this group.
     *
     * @return the fields list (never null, may be empty)
     */
    public List<FieldNode> getFields() {
        return fields;
    }

    /**
     * Sets the list of fields in this group.
     *
     * @param fields the fields to set, or null for empty list
     */
    public void setFields(List<FieldNode> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    /**
     * Adds a field to this group.
     *
     * @param field the field to add
     */
    public void addField(FieldNode field) {
        this.fields.add(field);
    }
}
