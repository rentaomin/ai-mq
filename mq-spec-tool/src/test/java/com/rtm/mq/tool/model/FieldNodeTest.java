package com.rtm.mq.tool.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldNode class.
 */
class FieldNodeTest {

    @Test
    void testBuilderWithAllFields() {
        SourceMetadata source = new SourceMetadata("RequestSheet", 5);
        List<FieldNode> children = Arrays.asList(
            FieldNode.builder().originalName("child1").build()
        );

        FieldNode node = FieldNode.builder()
            .originalName("CUST_ID")
            .camelCaseName("custId")
            .className("CustomerInfo")
            .segLevel(2)
            .length(20)
            .dataType("A/N")
            .optionality("M")
            .defaultValue("")
            .hardCodeValue("001")
            .groupId("G1")
            .occurrenceCount("1..1")
            .isArray(false)
            .isObject(true)
            .isTransitory(false)
            .children(children)
            .source(source)
            .build();

        assertEquals("CUST_ID", node.getOriginalName());
        assertEquals("custId", node.getCamelCaseName());
        assertEquals("CustomerInfo", node.getClassName());
        assertEquals(2, node.getSegLevel());
        assertEquals(Integer.valueOf(20), node.getLength());
        assertEquals("A/N", node.getDataType());
        assertEquals("M", node.getOptionality());
        assertEquals("", node.getDefaultValue());
        assertEquals("001", node.getHardCodeValue());
        assertEquals("G1", node.getGroupId());
        assertEquals("1..1", node.getOccurrenceCount());
        assertFalse(node.isArray());
        assertTrue(node.isObject());
        assertFalse(node.isTransitory());
        assertEquals(1, node.getChildren().size());
        assertEquals("RequestSheet", node.getSource().getSheetName());
        assertEquals(5, node.getSource().getRowIndex());
    }

    @Test
    void testBuilderWithMinimalFields() {
        FieldNode node = FieldNode.builder()
            .originalName("FIELD_NAME")
            .build();

        assertEquals("FIELD_NAME", node.getOriginalName());
        assertNull(node.getCamelCaseName());
        assertNull(node.getClassName());
        assertEquals(0, node.getSegLevel());
        assertNull(node.getLength());
        assertNull(node.getDataType());
        assertNull(node.getOptionality());
        assertNull(node.getDefaultValue());
        assertNull(node.getHardCodeValue());
        assertNull(node.getGroupId());
        assertNull(node.getOccurrenceCount());
        assertFalse(node.isArray());
        assertFalse(node.isObject());
        assertFalse(node.isTransitory());
        assertNotNull(node.getChildren());
        assertTrue(node.getChildren().isEmpty());
        assertNotNull(node.getSource());
    }

    @Test
    void testBuilderWithNullChildren() {
        FieldNode node = FieldNode.builder()
            .originalName("TEST")
            .children(null)
            .build();

        assertNotNull(node.getChildren());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void testBuilderWithNullSource() {
        FieldNode node = FieldNode.builder()
            .originalName("TEST")
            .source(null)
            .build();

        assertNotNull(node.getSource());
    }

    @Test
    void testArrayField() {
        FieldNode node = FieldNode.builder()
            .originalName("ITEMS")
            .occurrenceCount("0..N")
            .isArray(true)
            .build();

        assertTrue(node.isArray());
        assertEquals("0..N", node.getOccurrenceCount());
    }

    @Test
    void testFieldOrderPreserved() {
        FieldNode child1 = FieldNode.builder().originalName("first").segLevel(1).build();
        FieldNode child2 = FieldNode.builder().originalName("second").segLevel(2).build();
        FieldNode child3 = FieldNode.builder().originalName("third").segLevel(3).build();

        List<FieldNode> children = Arrays.asList(child1, child2, child3);

        FieldNode parent = FieldNode.builder()
            .originalName("parent")
            .children(children)
            .build();

        List<FieldNode> result = parent.getChildren();
        assertEquals(3, result.size());
        assertEquals("first", result.get(0).getOriginalName());
        assertEquals("second", result.get(1).getOriginalName());
        assertEquals("third", result.get(2).getOriginalName());
    }
}
