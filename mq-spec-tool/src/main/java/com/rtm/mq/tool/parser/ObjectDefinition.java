package com.rtm.mq.tool.parser;

/**
 * Represents an object definition parsed from Excel field names.
 *
 * <p>Object definitions in the Excel spec use the format "fieldName:ClassName"
 * to indicate a nested structure. For example:</p>
 * <ul>
 *   <li>"customerInfo:CustomerInfo" - a field named customerInfo of type CustomerInfo</li>
 *   <li>"addresses:AddressItem" - a field named addresses of type AddressItem</li>
 * </ul>
 *
 * <p>This class is immutable and holds both components after parsing.</p>
 */
public class ObjectDefinition {

    private final String fieldName;
    private final String className;

    /**
     * Creates a new ObjectDefinition with the specified field name and class name.
     *
     * @param fieldName the field/property name
     * @param className the class/type name
     */
    public ObjectDefinition(String fieldName, String className) {
        this.fieldName = fieldName;
        this.className = className;
    }

    /**
     * Gets the field name (left side of the colon).
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the class name (right side of the colon).
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return fieldName + ":" + className;
    }
}
