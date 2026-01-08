package com.rtm.mq.tool.validator.java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NamingConvention.
 */
class NamingConventionTest {

    private final NamingConvention convention = new NamingConvention();

    @Test
    void testValidFieldNames() {
        assertTrue(convention.isValidFieldName("fieldName"));
        assertTrue(convention.isValidFieldName("field"));
        assertTrue(convention.isValidFieldName("myFieldName"));
        assertTrue(convention.isValidFieldName("field123"));
        assertTrue(convention.isValidFieldName("a"));
        assertTrue(convention.isValidFieldName("camelCaseName"));
        assertTrue(convention.isValidFieldName("customer"));
        assertTrue(convention.isValidFieldName("accountNumber"));
    }

    @Test
    void testInvalidFieldNames() {
        assertFalse(convention.isValidFieldName("FieldName")); // Starts with uppercase
        assertFalse(convention.isValidFieldName("FIELD")); // All uppercase
        assertFalse(convention.isValidFieldName("field_name")); // Contains underscore
        assertFalse(convention.isValidFieldName("field-name")); // Contains hyphen
        assertFalse(convention.isValidFieldName("123field")); // Starts with digit
        assertFalse(convention.isValidFieldName("")); // Empty
        assertFalse(convention.isValidFieldName(null)); // Null
        assertFalse(convention.isValidFieldName("field.name")); // Contains dot
        assertFalse(convention.isValidFieldName("field name")); // Contains space
    }

    @Test
    void testValidClassNames() {
        assertTrue(convention.isValidClassName("ClassName"));
        assertTrue(convention.isValidClassName("MyClass"));
        assertTrue(convention.isValidClassName("A"));
        assertTrue(convention.isValidClassName("Class123"));
        assertTrue(convention.isValidClassName("CustomerAccount"));
        assertTrue(convention.isValidClassName("XMLParser")); // Though unconventional
    }

    @Test
    void testInvalidClassNames() {
        assertFalse(convention.isValidClassName("className")); // Starts with lowercase
        assertFalse(convention.isValidClassName("class_name")); // Contains underscore
        assertFalse(convention.isValidClassName("class-name")); // Contains hyphen
        assertFalse(convention.isValidClassName("123Class")); // Starts with digit
        assertFalse(convention.isValidClassName("")); // Empty
        assertFalse(convention.isValidClassName(null)); // Null
        assertFalse(convention.isValidClassName("Class.Name")); // Contains dot
    }

    @Test
    void testCustomPatterns() {
        // Custom pattern allowing underscores
        NamingConvention custom = new NamingConvention(
                "^[a-z][a-z_0-9]*$",
                "^[A-Z][A-Za-z_0-9]*$"
        );

        assertTrue(custom.isValidFieldName("field_name"));
        assertTrue(custom.isValidClassName("Class_Name"));
    }

    @Test
    void testGetPatternStrings() {
        assertEquals(NamingConvention.DEFAULT_FIELD_PATTERN,
                convention.getFieldPatternString());
        assertEquals(NamingConvention.DEFAULT_CLASS_PATTERN,
                convention.getClassPatternString());
    }

    @Test
    void testDefaultPatterns() {
        assertEquals("^[a-z][a-zA-Z0-9]*$", NamingConvention.DEFAULT_FIELD_PATTERN);
        assertEquals("^[A-Z][a-zA-Z0-9]*$", NamingConvention.DEFAULT_CLASS_PATTERN);
    }
}
