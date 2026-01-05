package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field node in the message specification tree.
 *
 * <p>A field node can represent:</p>
 * <ul>
 *   <li>A simple field (leaf node with no children)</li>
 *   <li>An object (container node with children)</li>
 *   <li>An array (repeating structure with children)</li>
 * </ul>
 *
 * <p>This class uses the Builder pattern for construction to ensure
 * immutability-friendly design and readable code.</p>
 *
 * <p>Field nodes preserve source metadata for audit traceability,
 * allowing validation errors and generated code to reference back
 * to the original Excel specification.</p>
 *
 * @see SourceMetadata
 * @see FieldGroup
 * @see MessageModel
 */
public class FieldNode {

    /** The original field name as it appears in the Excel specification. */
    private String originalName;

    /** The camelCase version of the field name for Java bean properties. */
    private String camelCaseName;

    /** The class name for object definitions (used when isObject is true). */
    private String className;

    /** The segment level indicating the nesting depth (1-based). */
    private int segLevel;

    /** The field length constraint (may be null for unbounded fields). */
    private Integer length;

    /** The data type from the specification (e.g., "N", "A/N", "A"). */
    private String dataType;

    /** The optionality indicator: "M" for mandatory, "O" for optional. */
    private String optionality;

    /** The default value to use when field is not present. */
    private String defaultValue;

    /** The hardcoded value that must be used regardless of input. */
    private String hardCodeValue;

    /** The group identifier for related fields. */
    private String groupId;

    /** The occurrence count specification (e.g., "1..1", "0..N", "1..N"). */
    private String occurrenceCount;

    /** Flag indicating this field represents an array (repeating structure). */
    private boolean isArray;

    /** Flag indicating this field represents an object (nested structure). */
    private boolean isObject;

    /** Flag indicating this field is transitory and should not appear in final output. */
    private boolean isTransitory;

    /** Child field nodes for nested structures. Uses List to preserve field order. */
    private List<FieldNode> children = new ArrayList<>();

    /** Source metadata for audit traceability. */
    private SourceMetadata source = new SourceMetadata();

    /**
     * Private constructor for Builder pattern.
     */
    private FieldNode() {
    }

    /**
     * Gets the original field name as it appears in the Excel specification.
     *
     * @return the original name
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * Gets the camelCase version of the field name.
     *
     * @return the camelCase name
     */
    public String getCamelCaseName() {
        return camelCaseName;
    }

    /**
     * Gets the class name for object definitions.
     *
     * @return the class name, or null if not an object type
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the segment level (nesting depth).
     *
     * @return the segment level (1-based)
     */
    public int getSegLevel() {
        return segLevel;
    }

    /**
     * Gets the field length constraint.
     *
     * @return the length, or null if unbounded
     */
    public Integer getLength() {
        return length;
    }

    /**
     * Gets the data type from the specification.
     *
     * @return the data type (e.g., "N", "A/N", "A")
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Gets the optionality indicator.
     *
     * @return "M" for mandatory, "O" for optional
     */
    public String getOptionality() {
        return optionality;
    }

    /**
     * Gets the default value.
     *
     * @return the default value, or null if none
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets the hardcoded value.
     *
     * @return the hardcoded value, or null if none
     */
    public String getHardCodeValue() {
        return hardCodeValue;
    }

    /**
     * Gets the group identifier.
     *
     * @return the group ID, or null if not part of a group
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the occurrence count specification.
     *
     * @return the occurrence count (e.g., "1..1", "0..N", "1..N")
     */
    public String getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Checks if this field represents an array.
     *
     * @return true if this is an array type
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Checks if this field represents an object.
     *
     * @return true if this is an object type
     */
    public boolean isObject() {
        return isObject;
    }

    /**
     * Checks if this field is transitory.
     *
     * @return true if this field should not appear in final output
     */
    public boolean isTransitory() {
        return isTransitory;
    }

    /**
     * Gets the list of child field nodes.
     *
     * @return the children list (never null, may be empty)
     */
    public List<FieldNode> getChildren() {
        return children;
    }

    /**
     * Gets the source metadata for audit traceability.
     *
     * @return the source metadata (never null)
     */
    public SourceMetadata getSource() {
        return source;
    }

    /**
     * Creates a new Builder for constructing FieldNode instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing FieldNode instances.
     *
     * <p>This builder provides a fluent API for setting field properties
     * and ensures proper initialization of collection fields.</p>
     */
    public static class Builder {

        private final FieldNode node = new FieldNode();

        /**
         * Sets the original field name.
         *
         * @param name the original name from the specification
         * @return this builder for method chaining
         */
        public Builder originalName(String name) {
            node.originalName = name;
            return this;
        }

        /**
         * Sets the camelCase field name.
         *
         * @param name the camelCase name for Java properties
         * @return this builder for method chaining
         */
        public Builder camelCaseName(String name) {
            node.camelCaseName = name;
            return this;
        }

        /**
         * Sets the class name for object types.
         *
         * @param name the class name
         * @return this builder for method chaining
         */
        public Builder className(String name) {
            node.className = name;
            return this;
        }

        /**
         * Sets the segment level.
         *
         * @param level the segment level (1-based)
         * @return this builder for method chaining
         */
        public Builder segLevel(int level) {
            node.segLevel = level;
            return this;
        }

        /**
         * Sets the field length constraint.
         *
         * @param length the length constraint, or null for unbounded
         * @return this builder for method chaining
         */
        public Builder length(Integer length) {
            node.length = length;
            return this;
        }

        /**
         * Sets the data type.
         *
         * @param type the data type (e.g., "N", "A/N", "A")
         * @return this builder for method chaining
         */
        public Builder dataType(String type) {
            node.dataType = type;
            return this;
        }

        /**
         * Sets the optionality indicator.
         *
         * @param opt the optionality ("M" or "O")
         * @return this builder for method chaining
         */
        public Builder optionality(String opt) {
            node.optionality = opt;
            return this;
        }

        /**
         * Sets the default value.
         *
         * @param value the default value
         * @return this builder for method chaining
         */
        public Builder defaultValue(String value) {
            node.defaultValue = value;
            return this;
        }

        /**
         * Sets the hardcoded value.
         *
         * @param value the hardcoded value
         * @return this builder for method chaining
         */
        public Builder hardCodeValue(String value) {
            node.hardCodeValue = value;
            return this;
        }

        /**
         * Sets the group identifier.
         *
         * @param id the group ID
         * @return this builder for method chaining
         */
        public Builder groupId(String id) {
            node.groupId = id;
            return this;
        }

        /**
         * Sets the occurrence count specification.
         *
         * @param count the occurrence count (e.g., "1..1", "0..N")
         * @return this builder for method chaining
         */
        public Builder occurrenceCount(String count) {
            node.occurrenceCount = count;
            return this;
        }

        /**
         * Sets whether this field is an array type.
         *
         * @param array true if this is an array
         * @return this builder for method chaining
         */
        public Builder isArray(boolean array) {
            node.isArray = array;
            return this;
        }

        /**
         * Sets whether this field is an object type.
         *
         * @param object true if this is an object
         * @return this builder for method chaining
         */
        public Builder isObject(boolean object) {
            node.isObject = object;
            return this;
        }

        /**
         * Sets whether this field is transitory.
         *
         * @param transitory true if this field should not appear in final output
         * @return this builder for method chaining
         */
        public Builder isTransitory(boolean transitory) {
            node.isTransitory = transitory;
            return this;
        }

        /**
         * Sets the list of child field nodes.
         *
         * @param children the children list, or null for empty list
         * @return this builder for method chaining
         */
        public Builder children(List<FieldNode> children) {
            node.children = children != null ? children : new ArrayList<>();
            return this;
        }

        /**
         * Sets the source metadata.
         *
         * @param source the source metadata
         * @return this builder for method chaining
         */
        public Builder source(SourceMetadata source) {
            node.source = source != null ? source : new SourceMetadata();
            return this;
        }

        /**
         * Builds the FieldNode instance.
         *
         * @return the constructed FieldNode
         */
        public FieldNode build() {
            return node;
        }
    }
}
