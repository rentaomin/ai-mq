package com.rtm.mq.tool.offset;

import com.rtm.mq.tool.exception.ValidationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OffsetCalculator.
 *
 * <p>Test Strategy (per task spec):</p>
 * <ul>
 *   <li>Flat fields</li>
 *   <li>Single nested object</li>
 *   <li>Array of primitives</li>
 *   <li>Array of objects</li>
 *   <li>Empty message</li>
 * </ul>
 */
class OffsetCalculatorTest {

    private OffsetCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OffsetCalculator();
    }

    // ==================== Empty message tests ====================

    @Test
    void calculate_nullFieldGroup_returnsEmptyTable() {
        OffsetTable table = calculator.calculate("request", (FieldGroup) null);

        assertNotNull(table);
        assertEquals("request", table.getMessageType());
        assertEquals(0, table.getTotalLength());
        assertTrue(table.isEmpty());
    }

    @Test
    void calculate_emptyFieldGroup_returnsEmptyTable() {
        FieldGroup group = new FieldGroup();
        group.setFields(new ArrayList<>());

        OffsetTable table = calculator.calculate("response", group);

        assertNotNull(table);
        assertEquals("response", table.getMessageType());
        assertEquals(0, table.getTotalLength());
        assertTrue(table.isEmpty());
    }

    @Test
    void calculate_nullFields_returnsEmptyTable() {
        FieldGroup group = new FieldGroup();
        group.setFields(null);

        OffsetTable table = calculator.calculate("request", group);

        assertNotNull(table);
        assertEquals(0, table.getTotalLength());
        assertTrue(table.isEmpty());
    }

    // ==================== Flat fields tests ====================

    @Test
    void calculate_singleFlatField_correctOffset() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("msgType")
                .length(10)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(1, table.size());
        assertEquals(10, table.getTotalLength());

        OffsetEntry entry = table.getEntries().get(0);
        assertEquals("msgType", entry.getFieldPath());
        assertEquals(0, entry.getOffset());
        assertEquals(10, entry.getLength());
        assertEquals(10, entry.getEndOffset());
        assertEquals(0, entry.getNestingLevel());
    }

    @Test
    void calculate_multipleFlatFields_sequentialOffsets() {
        FieldNode field1 = FieldNode.builder()
                .camelCaseName("msgType")
                .length(10)
                .build();
        FieldNode field2 = FieldNode.builder()
                .camelCaseName("msgLength")
                .length(5)
                .build();
        FieldNode field3 = FieldNode.builder()
                .camelCaseName("msgId")
                .length(20)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Arrays.asList(field1, field2, field3));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(3, table.size());
        assertEquals(35, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();

        // First field
        assertEquals("msgType", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(10, entries.get(0).getLength());
        assertEquals(10, entries.get(0).getEndOffset());

        // Second field - starts where first ends
        assertEquals("msgLength", entries.get(1).getFieldPath());
        assertEquals(10, entries.get(1).getOffset());
        assertEquals(5, entries.get(1).getLength());
        assertEquals(15, entries.get(1).getEndOffset());

        // Third field - starts where second ends
        assertEquals("msgId", entries.get(2).getFieldPath());
        assertEquals(15, entries.get(2).getOffset());
        assertEquals(20, entries.get(2).getLength());
        assertEquals(35, entries.get(2).getEndOffset());
    }

    @Test
    void calculate_flatFieldWithOccurrenceCount_usesMaxValue() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("amount")
                .length(10)
                .occurrenceCount("1..1")
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(1, table.size());
        assertEquals(10, table.getTotalLength());
        assertEquals("amount", table.getEntries().get(0).getFieldPath());
    }

    // ==================== Single nested object tests ====================

    @Test
    void calculate_singleNestedObject_childrenHaveCorrectOffsets() {
        FieldNode child1 = FieldNode.builder()
                .camelCaseName("streetName")
                .length(30)
                .build();
        FieldNode child2 = FieldNode.builder()
                .camelCaseName("city")
                .length(20)
                .build();

        FieldNode parent = FieldNode.builder()
                .camelCaseName("address")
                .isObject(true)
                .occurrenceCount("1..1")
                .children(Arrays.asList(child1, child2))
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(parent));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(2, table.size());
        assertEquals(50, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();

        // First child
        assertEquals("address.streetName", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(30, entries.get(0).getLength());
        assertEquals(1, entries.get(0).getNestingLevel());

        // Second child
        assertEquals("address.city", entries.get(1).getFieldPath());
        assertEquals(30, entries.get(1).getOffset());
        assertEquals(20, entries.get(1).getLength());
        assertEquals(1, entries.get(1).getNestingLevel());
    }

    @Test
    void calculate_nestedObjectWithSiblingFields_correctOrder() {
        FieldNode header = FieldNode.builder()
                .camelCaseName("header")
                .length(10)
                .build();

        FieldNode addressChild1 = FieldNode.builder()
                .camelCaseName("street")
                .length(20)
                .build();
        FieldNode addressChild2 = FieldNode.builder()
                .camelCaseName("zip")
                .length(5)
                .build();

        FieldNode address = FieldNode.builder()
                .camelCaseName("address")
                .isObject(true)
                .children(Arrays.asList(addressChild1, addressChild2))
                .build();

        FieldNode footer = FieldNode.builder()
                .camelCaseName("footer")
                .length(15)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Arrays.asList(header, address, footer));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(4, table.size());
        assertEquals(50, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();

        assertEquals("header", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(0, entries.get(0).getNestingLevel());

        assertEquals("address.street", entries.get(1).getFieldPath());
        assertEquals(10, entries.get(1).getOffset());
        assertEquals(1, entries.get(1).getNestingLevel());

        assertEquals("address.zip", entries.get(2).getFieldPath());
        assertEquals(30, entries.get(2).getOffset());
        assertEquals(1, entries.get(2).getNestingLevel());

        assertEquals("footer", entries.get(3).getFieldPath());
        assertEquals(35, entries.get(3).getOffset());
        assertEquals(0, entries.get(3).getNestingLevel());
    }

    // ==================== Array of primitives tests ====================

    @Test
    void calculate_arrayOfPrimitives_expandsWithIndices() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("item")
                .length(10)
                .isArray(true)
                .occurrenceCount("0..3")
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(3, table.size());
        assertEquals(30, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();

        assertEquals("item[0]", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(10, entries.get(0).getLength());

        assertEquals("item[1]", entries.get(1).getFieldPath());
        assertEquals(10, entries.get(1).getOffset());
        assertEquals(10, entries.get(1).getLength());

        assertEquals("item[2]", entries.get(2).getFieldPath());
        assertEquals(20, entries.get(2).getOffset());
        assertEquals(10, entries.get(2).getLength());
    }

    @Test
    void calculate_arrayWithSingleOccurrence_noIndexBracket() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("item")
                .length(10)
                .occurrenceCount("1..1")
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(1, table.size());
        assertEquals("item", table.getEntries().get(0).getFieldPath());
    }

    // ==================== Array of objects tests ====================

    @Test
    void calculate_arrayOfObjects_expandsChildrenPerIndex() {
        FieldNode itemChild1 = FieldNode.builder()
                .camelCaseName("name")
                .length(20)
                .build();
        FieldNode itemChild2 = FieldNode.builder()
                .camelCaseName("price")
                .length(10)
                .build();

        FieldNode items = FieldNode.builder()
                .camelCaseName("items")
                .isArray(true)
                .isObject(true)
                .occurrenceCount("0..2")
                .children(Arrays.asList(itemChild1, itemChild2))
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(items));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(4, table.size());
        assertEquals(60, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();

        // First occurrence
        assertEquals("items[0].name", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(20, entries.get(0).getLength());
        assertEquals(1, entries.get(0).getNestingLevel());

        assertEquals("items[0].price", entries.get(1).getFieldPath());
        assertEquals(20, entries.get(1).getOffset());
        assertEquals(10, entries.get(1).getLength());
        assertEquals(1, entries.get(1).getNestingLevel());

        // Second occurrence
        assertEquals("items[1].name", entries.get(2).getFieldPath());
        assertEquals(30, entries.get(2).getOffset());
        assertEquals(20, entries.get(2).getLength());
        assertEquals(1, entries.get(2).getNestingLevel());

        assertEquals("items[1].price", entries.get(3).getFieldPath());
        assertEquals(50, entries.get(3).getOffset());
        assertEquals(10, entries.get(3).getLength());
        assertEquals(1, entries.get(3).getNestingLevel());
    }

    // ==================== OccurrenceCount = 0 (skip) tests ====================

    @Test
    void calculate_zeroOccurrence_skipsField() {
        FieldNode field1 = FieldNode.builder()
                .camelCaseName("before")
                .length(10)
                .build();
        FieldNode field2 = FieldNode.builder()
                .camelCaseName("skipped")
                .length(20)
                .occurrenceCount("0..0")
                .build();
        FieldNode field3 = FieldNode.builder()
                .camelCaseName("after")
                .length(15)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Arrays.asList(field1, field2, field3));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(2, table.size());
        assertEquals(25, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();
        assertEquals("before", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());

        // "after" should be immediately after "before", skipping "skipped"
        assertEquals("after", entries.get(1).getFieldPath());
        assertEquals(10, entries.get(1).getOffset());
    }

    // ==================== Error handling tests ====================

    @Test
    void calculate_missingLength_throwsValidationException() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("noLength")
                .length(null)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> calculator.calculate("request", group));

        assertTrue(ex.getMessage().contains("Missing length"));
        assertTrue(ex.getMessage().contains("noLength"));
    }

    @Test
    void calculate_negativeLength_throwsValidationException() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("negLength")
                .length(-5)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> calculator.calculate("request", group));

        assertTrue(ex.getMessage().contains("Negative length"));
        assertTrue(ex.getMessage().contains("negLength"));
    }

    @Test
    void calculate_invalidOccurrenceCountFormat_throwsValidationException() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("badOcc")
                .length(10)
                .occurrenceCount("invalid")
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> calculator.calculate("request", group));

        assertTrue(ex.getMessage().contains("Invalid occurrenceCount format"));
        assertTrue(ex.getMessage().contains("badOcc"));
    }

    // ==================== Field name fallback tests ====================

    @Test
    void calculate_usesOriginalNameWhenCamelCaseEmpty() {
        FieldNode field = FieldNode.builder()
                .originalName("ORIGINAL_NAME")
                .camelCaseName("")
                .length(10)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals("ORIGINAL_NAME", table.getEntries().get(0).getFieldPath());
    }

    @Test
    void calculate_usesOriginalNameWhenCamelCaseNull() {
        FieldNode field = FieldNode.builder()
                .originalName("ORIGINAL_FIELD")
                .camelCaseName(null)
                .length(10)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals("ORIGINAL_FIELD", table.getEntries().get(0).getFieldPath());
    }

    // ==================== List-based calculate method tests ====================

    @Test
    void calculate_listOfNodes_worksCorrectly() {
        FieldNode field1 = FieldNode.builder()
                .camelCaseName("field1")
                .length(10)
                .build();
        FieldNode field2 = FieldNode.builder()
                .camelCaseName("field2")
                .length(20)
                .build();

        List<FieldNode> nodes = Arrays.asList(field1, field2);

        OffsetTable table = calculator.calculate("response", nodes);

        assertEquals(2, table.size());
        assertEquals(30, table.getTotalLength());
        assertEquals("response", table.getMessageType());
    }

    @Test
    void calculate_nullNodeList_returnsEmptyTable() {
        OffsetTable table = calculator.calculate("request", (List<FieldNode>) null);

        assertNotNull(table);
        assertTrue(table.isEmpty());
        assertEquals(0, table.getTotalLength());
    }

    @Test
    void calculate_emptyNodeList_returnsEmptyTable() {
        OffsetTable table = calculator.calculate("request", new ArrayList<>());

        assertNotNull(table);
        assertTrue(table.isEmpty());
        assertEquals(0, table.getTotalLength());
    }

    // ==================== Transitory fields tests ====================

    @Test
    void calculate_transitoryFieldsIncluded() {
        FieldNode transitoryField = FieldNode.builder()
                .camelCaseName("groupId")
                .length(5)
                .isTransitory(true)
                .build();

        FieldNode regularField = FieldNode.builder()
                .camelCaseName("value")
                .length(10)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Arrays.asList(transitoryField, regularField));

        OffsetTable table = calculator.calculate("request", group);

        // Transitory fields participate in offset calculation
        assertEquals(2, table.size());
        assertEquals(15, table.getTotalLength());

        List<OffsetEntry> entries = table.getEntries();
        assertEquals("groupId", entries.get(0).getFieldPath());
        assertEquals(0, entries.get(0).getOffset());

        assertEquals("value", entries.get(1).getFieldPath());
        assertEquals(5, entries.get(1).getOffset());
    }

    // ==================== Deeply nested tests ====================

    @Test
    void calculate_deeplyNestedStructure_correctNestingLevels() {
        FieldNode deepChild = FieldNode.builder()
                .camelCaseName("deepValue")
                .length(5)
                .build();

        FieldNode level2 = FieldNode.builder()
                .camelCaseName("level2")
                .isObject(true)
                .children(Collections.singletonList(deepChild))
                .build();

        FieldNode level1 = FieldNode.builder()
                .camelCaseName("level1")
                .isObject(true)
                .children(Collections.singletonList(level2))
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(level1));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(1, table.size());
        assertEquals(5, table.getTotalLength());

        OffsetEntry entry = table.getEntries().get(0);
        assertEquals("level1.level2.deepValue", entry.getFieldPath());
        assertEquals(2, entry.getNestingLevel());
    }

    // ==================== Zero length field tests ====================

    @Test
    void calculate_zeroLengthField_allowedAndIncluded() {
        FieldNode field = FieldNode.builder()
                .camelCaseName("marker")
                .length(0)
                .build();

        FieldGroup group = new FieldGroup();
        group.setFields(Collections.singletonList(field));

        OffsetTable table = calculator.calculate("request", group);

        assertEquals(1, table.size());
        assertEquals(0, table.getTotalLength());

        OffsetEntry entry = table.getEntries().get(0);
        assertEquals(0, entry.getOffset());
        assertEquals(0, entry.getLength());
        assertEquals(0, entry.getEndOffset());
    }
}
