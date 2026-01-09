package com.rtm.mq.tool.generator.xml;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XmlFieldAttributes.
 */
public class XmlFieldAttributesTest {

    @Test
    public void testDataFieldConstruction() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD);
        assertEquals(XmlFieldType.DATA_FIELD, attrs.getType());
        assertEquals("DataField", attrs.getAttributes().get("type"));
    }

    @Test
    public void testCompositeFieldConstruction() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.COMPOSITE_FIELD);
        assertEquals(XmlFieldType.COMPOSITE_FIELD, attrs.getType());
        assertEquals("CompositeField", attrs.getAttributes().get("type"));
    }

    @Test
    public void testRepeatingFieldConstruction() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.REPEATING_FIELD);
        assertEquals(XmlFieldType.REPEATING_FIELD, attrs.getType());
        assertEquals("RepeatingField", attrs.getAttributes().get("type"));
    }

    @Test
    public void testFluentMethodChaining() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name("testField")
            .length(10)
            .fixedLength(true)
            .converter("stringFieldConverter");

        assertEquals("testField", attrs.getAttributes().get("name"));
        assertEquals("10", attrs.getAttributes().get("length"));
        assertEquals("true", attrs.getAttributes().get("fixedLength"));
        assertEquals("stringFieldConverter", attrs.getAttributes().get("converter"));
    }

    @Test
    public void testNullValuesIgnored() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name(null)
            .defaultValue(null)
            .pad(null)
            .converter(null);

        // Only type should be set
        assertEquals(1, attrs.getAttributes().size());
        assertEquals("DataField", attrs.getAttributes().get("type"));
    }

    @Test
    public void testTransitoryAttribute() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .transitory(true);

        assertEquals("true", attrs.getAttributes().get("transitory"));

        // transitory(false) should not add attribute
        XmlFieldAttributes attrs2 = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .transitory(false);

        assertNull(attrs2.getAttributes().get("transitory"));
    }

    @Test
    public void testDefaultValueAttribute() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .defaultValue("ABC123");

        assertEquals("ABC123", attrs.getAttributes().get("defaultValue"));
    }

    @Test
    public void testPaddingAttributes() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .pad("0")
            .alignRight(true)
            .nullPad(" ");

        assertEquals("0", attrs.getAttributes().get("pad"));
        assertEquals("true", attrs.getAttributes().get("alignRight"));
        assertEquals(" ", attrs.getAttributes().get("nullPad"));
    }

    @Test
    public void testForTypeAttribute() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.COMPOSITE_FIELD)
            .forType("com.example.MyClass");

        assertEquals("com.example.MyClass", attrs.getAttributes().get("forType"));
    }

    @Test
    public void testFixedCountAttribute() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.REPEATING_FIELD)
            .fixedCount(9);

        assertEquals("9", attrs.getAttributes().get("fixedCount"));
    }

    @Test
    public void testFloatingNumberLengthAttribute() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .floatingNumberLength(2);

        assertEquals("2", attrs.getAttributes().get("floatingNumberLength"));
    }

    @Test
    public void testAttributeOrderPreserved() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name("field1")
            .length(10)
            .fixedLength(true)
            .converter("stringFieldConverter");

        Map<String, String> attribs = attrs.getAttributes();
        String[] keys = attribs.keySet().toArray(new String[0]);

        // Verify insertion order is preserved
        assertEquals("type", keys[0]);
        assertEquals("name", keys[1]);
        assertEquals("length", keys[2]);
        assertEquals("fixedLength", keys[3]);
        assertEquals("converter", keys[4]);
    }

    @Test
    public void testComplexDataFieldAttributes() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.DATA_FIELD)
            .name("accountNumber")
            .length(20)
            .fixedLength(true)
            .pad("0")
            .alignRight(true)
            .defaultValue("00000")
            .converter("stringFieldConverter");

        assertEquals("accountNumber", attrs.getAttributes().get("name"));
        assertEquals("20", attrs.getAttributes().get("length"));
        assertEquals("true", attrs.getAttributes().get("fixedLength"));
        assertEquals("0", attrs.getAttributes().get("pad"));
        assertEquals("true", attrs.getAttributes().get("alignRight"));
        assertEquals("00000", attrs.getAttributes().get("defaultValue"));
        assertEquals("stringFieldConverter", attrs.getAttributes().get("converter"));
    }

    @Test
    public void testCompositeFieldAttributes() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.COMPOSITE_FIELD)
            .name("customer")
            .forType("com.example.Customer");

        assertEquals("CompositeField", attrs.getAttributes().get("type"));
        assertEquals("customer", attrs.getAttributes().get("name"));
        assertEquals("com.example.Customer", attrs.getAttributes().get("forType"));
    }

    @Test
    public void testRepeatingFieldAttributes() {
        XmlFieldAttributes attrs = new XmlFieldAttributes(XmlFieldType.REPEATING_FIELD)
            .name("transactions")
            .fixedCount(99)
            .forType("com.example.Transaction");

        assertEquals("RepeatingField", attrs.getAttributes().get("type"));
        assertEquals("transactions", attrs.getAttributes().get("name"));
        assertEquals("99", attrs.getAttributes().get("fixedCount"));
        assertEquals("com.example.Transaction", attrs.getAttributes().get("forType"));
    }
}
