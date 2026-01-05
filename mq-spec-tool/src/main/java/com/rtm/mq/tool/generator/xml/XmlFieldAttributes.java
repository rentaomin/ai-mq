package com.rtm.mq.tool.generator.xml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container for XML field attributes used in Spring XML bean definitions.
 *
 * <p>This class uses a LinkedHashMap to preserve attribute order, which is
 * important for deterministic XML generation and readability. Attributes are
 * added in a specific order to ensure consistent output.</p>
 *
 * <p>The builder pattern is used to allow fluent construction of attribute sets.</p>
 */
public class XmlFieldAttributes {
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private XmlFieldType type;

    /**
     * Constructs an XmlFieldAttributes instance with the specified type.
     *
     * @param type the XML field type (DataField, CompositeField, or RepeatingField)
     */
    public XmlFieldAttributes(XmlFieldType type) {
        this.type = type;
        attributes.put("type", type.getValue());
    }

    /**
     * Sets the field name attribute.
     *
     * @param name the field name (camelCase)
     * @return this instance for method chaining
     */
    public XmlFieldAttributes name(String name) {
        if (name != null) attributes.put("name", name);
        return this;
    }

    /**
     * Sets the field length attribute.
     *
     * @param length the field length in characters
     * @return this instance for method chaining
     */
    public XmlFieldAttributes length(Integer length) {
        if (length != null) attributes.put("length", String.valueOf(length));
        return this;
    }

    /**
     * Sets the fixedLength attribute.
     *
     * @param value true to set fixedLength="true"
     * @return this instance for method chaining
     */
    public XmlFieldAttributes fixedLength(boolean value) {
        if (value) attributes.put("fixedLength", "true");
        return this;
    }

    /**
     * Sets the transitory attribute.
     *
     * @param value true to set transitory="true"
     * @return this instance for method chaining
     */
    public XmlFieldAttributes transitory(boolean value) {
        if (value) attributes.put("transitory", "true");
        return this;
    }

    /**
     * Sets the defaultValue attribute.
     *
     * @param value the default value
     * @return this instance for method chaining
     */
    public XmlFieldAttributes defaultValue(String value) {
        if (value != null) attributes.put("defaultValue", value);
        return this;
    }

    /**
     * Sets the pad attribute.
     *
     * @param value the padding character
     * @return this instance for method chaining
     */
    public XmlFieldAttributes pad(String value) {
        if (value != null) attributes.put("pad", value);
        return this;
    }

    /**
     * Sets the alignRight attribute.
     *
     * @param value true to set alignRight="true"
     * @return this instance for method chaining
     */
    public XmlFieldAttributes alignRight(boolean value) {
        if (value) attributes.put("alignRight", "true");
        return this;
    }

    /**
     * Sets the nullPad attribute.
     *
     * @param value the null padding character
     * @return this instance for method chaining
     */
    public XmlFieldAttributes nullPad(String value) {
        if (value != null) attributes.put("nullPad", value);
        return this;
    }

    /**
     * Sets the converter attribute.
     *
     * @param value the converter bean name
     * @return this instance for method chaining
     */
    public XmlFieldAttributes converter(String value) {
        if (value != null) attributes.put("converter", value);
        return this;
    }

    /**
     * Sets the forType attribute.
     *
     * @param value the fully qualified class name
     * @return this instance for method chaining
     */
    public XmlFieldAttributes forType(String value) {
        if (value != null) attributes.put("forType", value);
        return this;
    }

    /**
     * Sets the fixedCount attribute for repeating fields.
     *
     * @param count the fixed repeat count
     * @return this instance for method chaining
     */
    public XmlFieldAttributes fixedCount(int count) {
        attributes.put("fixedCount", String.valueOf(count));
        return this;
    }

    /**
     * Sets the floatingNumberLength attribute for numeric fields.
     *
     * @param length the floating point precision
     * @return this instance for method chaining
     */
    public XmlFieldAttributes floatingNumberLength(int length) {
        attributes.put("floatingNumberLength", String.valueOf(length));
        return this;
    }

    /**
     * Gets the field type.
     *
     * @return the XML field type
     */
    public XmlFieldType getType() {
        return type;
    }

    /**
     * Gets the attribute map.
     *
     * @return the map of attributes in insertion order
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }
}
