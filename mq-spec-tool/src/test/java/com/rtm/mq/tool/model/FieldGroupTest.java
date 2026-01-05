package com.rtm.mq.tool.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldGroup class.
 */
class FieldGroupTest {

    @Test
    void testDefaultConstructor() {
        FieldGroup group = new FieldGroup();

        assertNotNull(group.getFields());
        assertTrue(group.getFields().isEmpty());
    }

    @Test
    void testSetFields() {
        FieldGroup group = new FieldGroup();
        List<FieldNode> fields = Arrays.asList(
            FieldNode.builder().originalName("FIELD1").build(),
            FieldNode.builder().originalName("FIELD2").build()
        );

        group.setFields(fields);

        assertEquals(2, group.getFields().size());
        assertEquals("FIELD1", group.getFields().get(0).getOriginalName());
        assertEquals("FIELD2", group.getFields().get(1).getOriginalName());
    }

    @Test
    void testSetFieldsWithNull() {
        FieldGroup group = new FieldGroup();
        group.setFields(null);

        assertNotNull(group.getFields());
        assertTrue(group.getFields().isEmpty());
    }

    @Test
    void testAddField() {
        FieldGroup group = new FieldGroup();

        group.addField(FieldNode.builder().originalName("FIELD1").build());
        group.addField(FieldNode.builder().originalName("FIELD2").build());
        group.addField(FieldNode.builder().originalName("FIELD3").build());

        assertEquals(3, group.getFields().size());
    }

    @Test
    void testFieldOrderPreserved() {
        FieldGroup group = new FieldGroup();

        group.addField(FieldNode.builder().originalName("first").build());
        group.addField(FieldNode.builder().originalName("second").build());
        group.addField(FieldNode.builder().originalName("third").build());

        List<FieldNode> fields = group.getFields();
        assertEquals("first", fields.get(0).getOriginalName());
        assertEquals("second", fields.get(1).getOriginalName());
        assertEquals("third", fields.get(2).getOriginalName());
    }
}
