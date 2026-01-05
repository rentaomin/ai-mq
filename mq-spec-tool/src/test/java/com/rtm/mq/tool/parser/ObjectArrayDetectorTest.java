package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ObjectArrayDetector.
 */
class ObjectArrayDetectorTest {

    private Map<String, Integer> columnMap;
    private ObjectArrayDetector detector;

    @BeforeEach
    void setUp() {
        columnMap = new HashMap<>();
        columnMap.put(ColumnNames.FIELD_NAME, 0);
        columnMap.put(ColumnNames.DESCRIPTION, 1);
        columnMap.put(ColumnNames.LENGTH, 2);
        columnMap.put(ColumnNames.MESSAGING_DATATYPE, 3);
        detector = new ObjectArrayDetector(columnMap);
    }

    @Nested
    class GroupIdFieldTests {

        @Test
        void isGroupIdField_groupid_returnsTrue() {
            assertTrue(detector.isGroupIdField("groupid"));
        }

        @Test
        void isGroupIdField_groupId_caseInsensitive() {
            assertTrue(detector.isGroupIdField("groupId"));
            assertTrue(detector.isGroupIdField("GROUPID"));
            assertTrue(detector.isGroupIdField("GroupId"));
            assertTrue(detector.isGroupIdField("GroupID"));
        }

        @Test
        void isGroupIdField_otherFields_returnsFalse() {
            assertFalse(detector.isGroupIdField("group"));
            assertFalse(detector.isGroupIdField("groupIdValue"));
            assertFalse(detector.isGroupIdField("myGroupId"));
            assertFalse(detector.isGroupIdField(null));
        }

        @Test
        void detect_groupIdField_setsGroupIdAndTransitory() {
            Row row = createMockRow("groupId", "GRP001", null, null);
            FieldNode node = createBaseNode("groupId", 2);

            FieldNode result = detector.detect(node, row);

            assertEquals("GRP001", result.getGroupId());
            assertTrue(result.isTransitory());
            assertEquals("groupid", result.getCamelCaseName());
        }
    }

    @Nested
    class OccurrenceCountFieldTests {

        @ParameterizedTest
        @ValueSource(strings = {"occurenceCount", "occurrenceCount"})
        void isOccurrenceCountField_bothSpellings_returnsTrue(String fieldName) {
            assertTrue(detector.isOccurrenceCountField(fieldName));
        }

        @Test
        void isOccurrenceCountField_caseInsensitive() {
            assertTrue(detector.isOccurrenceCountField("OCCURENCECOUNT"));
            assertTrue(detector.isOccurrenceCountField("OCCURRENCECOUNT"));
            assertTrue(detector.isOccurrenceCountField("OccurenceCount"));
            assertTrue(detector.isOccurrenceCountField("OccurrenceCount"));
        }

        @Test
        void isOccurrenceCountField_otherFields_returnsFalse() {
            assertFalse(detector.isOccurrenceCountField("occurrence"));
            assertFalse(detector.isOccurrenceCountField("count"));
            assertFalse(detector.isOccurrenceCountField(null));
        }

        @Test
        void detect_occurenceCountField_setsOccurrenceCountAndTransitory() {
            // Excel typo spelling
            Row row = createMockRow("occurenceCount", "0..9", null, null);
            FieldNode node = createBaseNode("occurenceCount", 2);

            FieldNode result = detector.detect(node, row);

            assertEquals("0..9", result.getOccurrenceCount());
            assertTrue(result.isTransitory());
        }

        @Test
        void detect_occurrenceCountField_correctSpelling() {
            Row row = createMockRow("occurrenceCount", "1..5", null, null);
            FieldNode node = createBaseNode("occurrenceCount", 2);

            FieldNode result = detector.detect(node, row);

            assertEquals("1..5", result.getOccurrenceCount());
            assertTrue(result.isTransitory());
        }
    }

    @Nested
    class ObjectDefinitionTests {

        @Test
        void isObjectDefinition_validFormat_returnsTrue() {
            assertTrue(detector.isObjectDefinition("customerInfo:CustomerInfo", null, null));
            assertTrue(detector.isObjectDefinition("addresses:AddressItem", "", ""));
            assertTrue(detector.isObjectDefinition("data:DataObject", "  ", "  "));
        }

        @Test
        void isObjectDefinition_noColon_returnsFalse() {
            assertFalse(detector.isObjectDefinition("customerInfo", null, null));
        }

        @Test
        void isObjectDefinition_withLength_returnsFalse() {
            assertFalse(detector.isObjectDefinition("field:value", "20", null));
        }

        @Test
        void isObjectDefinition_withDataType_returnsFalse() {
            assertFalse(detector.isObjectDefinition("field:value", null, "A/N"));
        }

        @Test
        void isObjectDefinition_nullFieldName_returnsFalse() {
            assertFalse(detector.isObjectDefinition(null, null, null));
        }

        @Test
        void parseObjectDefinition_valid_returnsObjectDefinition() {
            ObjectDefinition def = detector.parseObjectDefinition("customerInfo:CustomerInfo");

            assertEquals("customerInfo", def.getFieldName());
            assertEquals("CustomerInfo", def.getClassName());
        }

        @Test
        void parseObjectDefinition_withWhitespace_trimmed() {
            ObjectDefinition def = detector.parseObjectDefinition("  fieldName : ClassName  ");

            assertEquals("fieldName", def.getFieldName());
            assertEquals("ClassName", def.getClassName());
        }

        @Test
        void parseObjectDefinition_multipleColons_onlySplitsOnFirst() {
            ObjectDefinition def = detector.parseObjectDefinition("field:Class:Extra");

            assertEquals("field", def.getFieldName());
            assertEquals("Class:Extra", def.getClassName());
        }

        @Test
        void parseObjectDefinition_noColon_throwsParseException() {
            ParseException exception = assertThrows(ParseException.class,
                () -> detector.parseObjectDefinition("invalidFormat"));

            assertTrue(exception.getMessage().contains("Invalid object definition format"));
        }

        @Test
        void parseObjectDefinition_emptyParts_throwsParseException() {
            assertThrows(ParseException.class,
                () -> detector.parseObjectDefinition(":ClassName"));
            assertThrows(ParseException.class,
                () -> detector.parseObjectDefinition("fieldName:"));
            assertThrows(ParseException.class,
                () -> detector.parseObjectDefinition(":"));
        }

        @Test
        void parseObjectDefinition_null_throwsParseException() {
            assertThrows(ParseException.class,
                () -> detector.parseObjectDefinition(null));
        }

        @Test
        void detect_objectDefinition_setsObjectFlagsAndClassName() {
            Row row = createMockRow("addresses:AddressItem", "Address list", null, null);
            FieldNode node = createBaseNode("addresses:AddressItem", 2);

            FieldNode result = detector.detect(node, row);

            assertTrue(result.isObject());
            assertFalse(result.isArray());
            assertEquals("AddressItem", result.getClassName());
            assertNull(result.getCamelCaseName()); // T-106 will set this
        }
    }

    @Nested
    class UpdateContainerTypeTests {

        @Test
        void updateContainerType_withArrayOccurrenceCount_convertsToArray() {
            // Create container with occurrenceCount child indicating array
            List<FieldNode> children = new ArrayList<>();
            children.add(FieldNode.builder()
                .originalName("occurenceCount")
                .occurrenceCount("0..9")
                .isTransitory(true)
                .build());
            children.add(FieldNode.builder()
                .originalName("itemId")
                .build());

            FieldNode container = FieldNode.builder()
                .originalName("items:ItemObject")
                .className("ItemObject")
                .isObject(true)
                .isArray(false)
                .children(children)
                .build();

            FieldNode result = detector.updateContainerType(container);

            assertTrue(result.isArray());
            assertFalse(result.isObject());
            assertEquals("0..9", result.getOccurrenceCount());
            assertEquals("ItemObject", result.getClassName());
        }

        @Test
        void updateContainerType_withSingleOccurrence_remainsObject() {
            List<FieldNode> children = new ArrayList<>();
            children.add(FieldNode.builder()
                .originalName("occurrenceCount")
                .occurrenceCount("1..1")
                .isTransitory(true)
                .build());

            FieldNode container = FieldNode.builder()
                .originalName("header:HeaderObject")
                .className("HeaderObject")
                .isObject(true)
                .isArray(false)
                .children(children)
                .build();

            FieldNode result = detector.updateContainerType(container);

            // 1..1 means max=1, which is not > 1, so stays as object
            assertFalse(result.isArray());
            assertTrue(result.isObject());
        }

        @Test
        void updateContainerType_noOccurrenceCountChild_remainsUnchanged() {
            List<FieldNode> children = new ArrayList<>();
            children.add(FieldNode.builder()
                .originalName("normalField")
                .build());

            FieldNode container = FieldNode.builder()
                .originalName("data:DataObject")
                .className("DataObject")
                .isObject(true)
                .children(children)
                .build();

            FieldNode result = detector.updateContainerType(container);

            assertSame(container, result);
        }

        @Test
        void updateContainerType_nullChildren_returnsUnchanged() {
            FieldNode container = FieldNode.builder()
                .originalName("data:DataObject")
                .children(null)
                .build();

            FieldNode result = detector.updateContainerType(container);

            assertSame(container, result);
        }
    }

    @Nested
    class NormalFieldTests {

        @Test
        void detect_normalField_returnsUnchanged() {
            Row row = createMockRow("accountNumber", "Account number", "20", "A/N");
            FieldNode node = createBaseNode("accountNumber", 2);

            FieldNode result = detector.detect(node, row);

            assertSame(node, result);
            assertFalse(result.isTransitory());
            assertFalse(result.isObject());
            assertFalse(result.isArray());
        }
    }

    // Helper methods

    private FieldNode createBaseNode(String originalName, int segLevel) {
        return FieldNode.builder()
            .originalName(originalName)
            .segLevel(segLevel)
            .source(new SourceMetadata())
            .build();
    }

    private Row createMockRow(String fieldName, String description, String length, String dataType) {
        Row row = mock(Row.class);

        Cell fieldNameCell = createMockStringCell(fieldName);
        Cell descriptionCell = createMockStringCell(description);
        Cell lengthCell = createMockStringCell(length);
        Cell dataTypeCell = createMockStringCell(dataType);

        when(row.getCell(0)).thenReturn(fieldNameCell);
        when(row.getCell(1)).thenReturn(descriptionCell);
        when(row.getCell(2)).thenReturn(lengthCell);
        when(row.getCell(3)).thenReturn(dataTypeCell);

        return row;
    }

    private Cell createMockStringCell(String value) {
        if (value == null) {
            return null;
        }
        Cell cell = mock(Cell.class);
        when(cell.getCellType()).thenReturn(CellType.STRING);
        when(cell.getStringCellValue()).thenReturn(value);
        return cell;
    }
}
