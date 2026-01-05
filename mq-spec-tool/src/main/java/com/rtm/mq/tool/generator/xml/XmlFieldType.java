package com.rtm.mq.tool.generator.xml;

/**
 * Enumeration of XML field types used in Spring XML bean definitions.
 *
 * <p>Maps to the field type attributes in XML configuration:</p>
 * <ul>
 *   <li>DataField - Simple data fields with converters</li>
 *   <li>CompositeField - Nested object structures</li>
 *   <li>RepeatingField - Array/repeating structures</li>
 * </ul>
 */
public enum XmlFieldType {
    DATA_FIELD("DataField"),
    COMPOSITE_FIELD("CompositeField"),
    REPEATING_FIELD("RepeatingField");

    private final String value;

    XmlFieldType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
