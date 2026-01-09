package com.rtm.mq.tool.generator.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for XmlFieldType enumeration.
 */
public class XmlFieldTypeTest {

    @Test
    public void testDataFieldValue() {
        assertEquals("DataField", XmlFieldType.DATA_FIELD.getValue());
    }

    @Test
    public void testCompositeFieldValue() {
        assertEquals("CompositeField", XmlFieldType.COMPOSITE_FIELD.getValue());
    }

    @Test
    public void testRepeatingFieldValue() {
        assertEquals("RepeatingField", XmlFieldType.REPEATING_FIELD.getValue());
    }

    @Test
    public void testEnumValues() {
        XmlFieldType[] values = XmlFieldType.values();
        assertEquals(3, values.length);
        assertEquals(XmlFieldType.DATA_FIELD, values[0]);
        assertEquals(XmlFieldType.COMPOSITE_FIELD, values[1]);
        assertEquals(XmlFieldType.REPEATING_FIELD, values[2]);
    }
}
