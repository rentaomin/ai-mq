package com.rtm.mq.tool.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ObjectDefinition.
 */
class ObjectDefinitionTest {

    @Test
    void constructor_setsFieldNameAndClassName() {
        ObjectDefinition def = new ObjectDefinition("customerInfo", "CustomerInfo");

        assertEquals("customerInfo", def.getFieldName());
        assertEquals("CustomerInfo", def.getClassName());
    }

    @Test
    void constructor_allowsNullValues() {
        ObjectDefinition def = new ObjectDefinition(null, null);

        assertNull(def.getFieldName());
        assertNull(def.getClassName());
    }

    @Test
    void toString_returnsColonSeparatedFormat() {
        ObjectDefinition def = new ObjectDefinition("addresses", "AddressItem");

        assertEquals("addresses:AddressItem", def.toString());
    }

    @Test
    void constructor_preservesWhitespace() {
        // ObjectDefinition does not trim - that's the parser's job
        ObjectDefinition def = new ObjectDefinition(" fieldName ", " ClassName ");

        assertEquals(" fieldName ", def.getFieldName());
        assertEquals(" ClassName ", def.getClassName());
    }
}
