package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SegLevelParser.
 */
class SegLevelParserTest {

    private Workbook workbook;
    private Sheet sheet;
    private Map<String, Integer> columnMap;
    private SegLevelParser parser;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Request");

        // Setup column mapping
        columnMap = new HashMap<>();
        columnMap.put(ColumnNames.SEG_LVL, 0);
        columnMap.put(ColumnNames.FIELD_NAME, 1);
        columnMap.put(ColumnNames.DESCRIPTION, 2);
        columnMap.put(ColumnNames.LENGTH, 3);
        columnMap.put(ColumnNames.MESSAGING_DATATYPE, 4);
        columnMap.put(ColumnNames.OPTIONALITY, 5);

        parser = new SegLevelParser(columnMap, "Request");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
    }

    /**
     * Helper method to create a data row.
     */
    private void createDataRow(int rowIndex, int segLevel, String fieldName, String description,
                               String length, String dataType, String optionality) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(segLevel);
        if (fieldName != null) row.createCell(1).setCellValue(fieldName);
        if (description != null) row.createCell(2).setCellValue(description);
        if (length != null) row.createCell(3).setCellValue(length);
        if (dataType != null) row.createCell(4).setCellValue(dataType);
        if (optionality != null) row.createCell(5).setCellValue(optionality);
    }

    @Test
    @DisplayName("Should parse flat structure with all Seg lvl = 1")
    void parseFields_flatStructure_returnsAllRootFields() {
        // Given - flat structure
        createDataRow(8, 1, "field1", "Description 1", "10", "A/N", "M");
        createDataRow(9, 1, "field2", "Description 2", "20", "N", "O");
        createDataRow(10, 1, "field3", "Description 3", "5", "A", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(3, result.size());
        assertEquals("field1", result.get(0).getOriginalName());
        assertEquals("field2", result.get(1).getOriginalName());
        assertEquals("field3", result.get(2).getOriginalName());
        assertEquals(1, result.get(0).getSegLevel());
        assertTrue(result.get(0).getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should parse simple nested structure")
    void parseFields_simpleNesting_createsHierarchy() {
        // Given - parent with children
        createDataRow(8, 1, "parent:", "Parent object", null, null, "M");
        createDataRow(9, 2, "child1", "Child 1", "10", "A/N", "M");
        createDataRow(10, 2, "child2", "Child 2", "20", "N", "O");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        FieldNode parent = result.get(0);
        assertEquals("parent:", parent.getOriginalName());
        assertEquals(1, parent.getSegLevel());
        assertEquals(2, parent.getChildren().size());
        assertEquals("child1", parent.getChildren().get(0).getOriginalName());
        assertEquals("child2", parent.getChildren().get(1).getOriginalName());
        assertEquals(2, parent.getChildren().get(0).getSegLevel());
    }

    @Test
    @DisplayName("Should parse multi-level nesting")
    void parseFields_multiLevelNesting_createsDeepHierarchy() {
        // Given - 3 levels deep
        createDataRow(8, 1, "root:", "Root", null, null, "M");
        createDataRow(9, 2, "level2:", "Level 2", null, null, "M");
        createDataRow(10, 3, "level3", "Level 3", "10", "A/N", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        FieldNode root = result.get(0);
        assertEquals("root:", root.getOriginalName());
        assertEquals(1, root.getChildren().size());

        FieldNode level2 = root.getChildren().get(0);
        assertEquals("level2:", level2.getOriginalName());
        assertEquals(2, level2.getSegLevel());
        assertEquals(1, level2.getChildren().size());

        FieldNode level3 = level2.getChildren().get(0);
        assertEquals("level3", level3.getOriginalName());
        assertEquals(3, level3.getSegLevel());
        assertTrue(level3.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should handle returning to lower level")
    void parseFields_levelDecrease_attachesToCorrectParent() {
        // Given - structure: 1, 2, 3, 2, 1
        createDataRow(8, 1, "root1:", "Root 1", null, null, "M");
        createDataRow(9, 2, "child1:", "Child 1", null, null, "M");
        createDataRow(10, 3, "grandchild", "Grandchild", "10", "A/N", "M");
        createDataRow(11, 2, "child2", "Child 2", "20", "N", "O");
        createDataRow(12, 1, "root2", "Root 2", "5", "A", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(2, result.size());

        // Verify root1 structure
        FieldNode root1 = result.get(0);
        assertEquals("root1:", root1.getOriginalName());
        assertEquals(2, root1.getChildren().size());
        assertEquals("child1:", root1.getChildren().get(0).getOriginalName());
        assertEquals("child2", root1.getChildren().get(1).getOriginalName());

        // Verify grandchild is under child1
        FieldNode child1 = root1.getChildren().get(0);
        assertEquals(1, child1.getChildren().size());
        assertEquals("grandchild", child1.getChildren().get(0).getOriginalName());

        // Verify root2 is separate
        FieldNode root2 = result.get(1);
        assertEquals("root2", root2.getOriginalName());
        assertTrue(root2.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should preserve field order")
    void parseFields_multipleFields_preservesOrder() {
        // Given
        createDataRow(8, 1, "field1", "First", "10", "A/N", "M");
        createDataRow(9, 1, "field2", "Second", "20", "N", "O");
        createDataRow(10, 1, "field3", "Third", "5", "A", "M");
        createDataRow(11, 1, "field4", "Fourth", "15", "A/N", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(4, result.size());
        assertEquals("field1", result.get(0).getOriginalName());
        assertEquals("field2", result.get(1).getOriginalName());
        assertEquals("field3", result.get(2).getOriginalName());
        assertEquals("field4", result.get(3).getOriginalName());
    }

    @Test
    @DisplayName("Should skip empty rows")
    void parseFields_emptyRows_skipsEmpty() {
        // Given - with empty rows
        createDataRow(8, 1, "field1", "First", "10", "A/N", "M");
        createDataRow(9, 0, null, null, null, null, null); // Empty row
        createDataRow(10, 1, "field2", "Second", "20", "N", "O");
        sheet.createRow(11); // Completely empty row

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(2, result.size());
        assertEquals("field1", result.get(0).getOriginalName());
        assertEquals("field2", result.get(1).getOriginalName());
    }

    @Test
    @DisplayName("Should throw ParseException when Seg lvl is empty")
    void parseFields_emptySegLevel_throwsParseException() {
        // Given
        Row row = sheet.createRow(8);
        row.createCell(0).setCellValue(""); // Empty Seg lvl
        row.createCell(1).setCellValue("testField");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> parser.parseFields(sheet));

        assertTrue(exception.getMessage().contains("Seg lvl is empty"));
        assertTrue(exception.getMessage().contains("Request"));
        assertTrue(exception.getMessage().contains("row 9"));
    }

    @Test
    @DisplayName("Should throw ParseException when Seg lvl is invalid")
    void parseFields_invalidSegLevel_throwsParseException() {
        // Given
        Row row = sheet.createRow(8);
        row.createCell(0).setCellValue("ABC"); // Invalid Seg lvl
        row.createCell(1).setCellValue("testField");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> parser.parseFields(sheet));

        assertTrue(exception.getMessage().contains("Invalid Seg lvl format"));
        assertTrue(exception.getMessage().contains("ABC"));
    }

    @Test
    @DisplayName("Should throw ParseException when Seg lvl is zero")
    void parseFields_zeroSegLevel_throwsParseException() {
        // Given
        createDataRow(8, 0, "testField", "Test", "10", "A/N", "M");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> parser.parseFields(sheet));

        assertTrue(exception.getMessage().contains("Invalid Seg lvl '0'"));
        assertTrue(exception.getMessage().contains("positive integer"));
    }

    @Test
    @DisplayName("Should throw ParseException when Seg lvl is negative")
    void parseFields_negativeSegLevel_throwsParseException() {
        // Given
        createDataRow(8, -1, "testField", "Test", "10", "A/N", "M");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> parser.parseFields(sheet));

        assertTrue(exception.getMessage().contains("Invalid Seg lvl '-1'"));
    }

    @Test
    @DisplayName("Should throw ParseException when Seg lvl jumps levels")
    void parseFields_segLevelJump_throwsParseException() {
        // Given - jump from 1 to 3
        createDataRow(8, 1, "field1", "First", "10", "A/N", "M");
        createDataRow(9, 3, "field2", "Second", "20", "N", "O");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> parser.parseFields(sheet));

        assertTrue(exception.getMessage().contains("Seg lvl jump from 1 to 3"));
        assertTrue(exception.getMessage().contains("Missing intermediate level"));
    }

    @Test
    @DisplayName("Should parse fields with null length")
    void parseFields_nullLength_parsesSuccessfully() {
        // Given - object with null length
        createDataRow(8, 1, "object:", "Object", null, null, "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        assertEquals("object:", result.get(0).getOriginalName());
        assertNull(result.get(0).getLength());
    }

    @Test
    @DisplayName("Should parse fields with numeric cell values")
    void parseFields_numericCells_parsesCorrectly() {
        // Given
        Row row = sheet.createRow(8);
        row.createCell(0).setCellValue(1); // Numeric Seg lvl
        row.createCell(1).setCellValue("field1");
        row.createCell(2).setCellValue("Description");
        row.createCell(3).setCellValue(10); // Numeric length
        row.createCell(4).setCellValue("A/N");
        row.createCell(5).setCellValue("M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        assertEquals("field1", result.get(0).getOriginalName());
        assertEquals(1, result.get(0).getSegLevel());
        assertEquals(10, result.get(0).getLength());
    }

    @Test
    @DisplayName("Should trim whitespace from field values")
    void parseFields_whitespace_trimsValues() {
        // Given
        createDataRow(8, 1, "  field1  ", "  Description  ", "  10  ", "  A/N  ", "  M  ");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        assertEquals("field1", result.get(0).getOriginalName());
        assertEquals("A/N", result.get(0).getDataType());
        assertEquals("M", result.get(0).getOptionality());
    }

    @Test
    @DisplayName("Should populate source metadata correctly")
    void parseFields_allFields_populatesSourceMetadata() {
        // Given
        createDataRow(8, 1, "field1", "Description", "10", "A/N", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        FieldNode node = result.get(0);
        assertNotNull(node.getSource());
        assertEquals("Request", node.getSource().getSheetName());
        assertEquals(9, node.getSource().getRowIndex()); // 1-based
    }

    @Test
    @DisplayName("Should handle complex nested structure")
    void parseFields_complexStructure_parsesCorrectly() {
        // Given - complex tree: root1{child1{gc1, gc2}, child2}, root2{child3}
        createDataRow(8, 1, "root1:", "Root 1", null, null, "M");
        createDataRow(9, 2, "child1:", "Child 1", null, null, "M");
        createDataRow(10, 3, "grandchild1", "GC 1", "10", "A/N", "M");
        createDataRow(11, 3, "grandchild2", "GC 2", "20", "N", "O");
        createDataRow(12, 2, "child2", "Child 2", "5", "A", "M");
        createDataRow(13, 1, "root2:", "Root 2", null, null, "O");
        createDataRow(14, 2, "child3", "Child 3", "15", "A/N", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(2, result.size());

        // Verify root1
        FieldNode root1 = result.get(0);
        assertEquals(2, root1.getChildren().size());
        assertEquals("child1:", root1.getChildren().get(0).getOriginalName());
        assertEquals("child2", root1.getChildren().get(1).getOriginalName());

        // Verify child1's grandchildren
        FieldNode child1 = root1.getChildren().get(0);
        assertEquals(2, child1.getChildren().size());
        assertEquals("grandchild1", child1.getChildren().get(0).getOriginalName());
        assertEquals("grandchild2", child1.getChildren().get(1).getOriginalName());

        // Verify root2
        FieldNode root2 = result.get(1);
        assertEquals(1, root2.getChildren().size());
        assertEquals("child3", root2.getChildren().get(0).getOriginalName());
    }

    @Test
    @DisplayName("Should use custom nesting depth validator")
    void constructor_customValidator_usesValidator() {
        // Given
        NestingDepthValidator customValidator = new NestingDepthValidator(2);
        SegLevelParser customParser = new SegLevelParser(columnMap, "Request", customValidator);

        // Create structure with depth 3 (exceeds limit of 2)
        createDataRow(8, 1, "root:", "Root", null, null, "M");
        createDataRow(9, 2, "child:", "Child", null, null, "M");
        createDataRow(10, 3, "grandchild", "GC", "10", "A/N", "M");

        // When - should not throw, just log warning
        List<FieldNode> result = customParser.parseFields(sheet);

        // Then - parsing should succeed despite depth warning
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(1, result.get(0).getChildren().get(0).getChildren().size());
    }

    @Test
    @DisplayName("Should return empty list for sheet with no data rows")
    void parseFields_noDataRows_returnsEmptyList() {
        // Given - sheet with no rows after row 8
        // (sheet is empty by default)

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle sheet with only header row")
    void parseFields_onlyHeaderRow_returnsEmptyList() {
        // Given - create header row but no data
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg lvl");
        headerRow.createCell(1).setCellValue("Field Name");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should identify container candidates with colon")
    void parseFields_containerWithColon_identifiedAsCandidate() {
        // Given
        createDataRow(8, 1, "object:", "Object definition", null, null, "M");
        createDataRow(9, 2, "field", "Field in object", "10", "A/N", "M");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then
        assertEquals(1, result.size());
        FieldNode container = result.get(0);
        assertEquals("object:", container.getOriginalName());
        assertEquals(1, container.getChildren().size());
    }

    @Test
    @DisplayName("Should handle non-container without colon")
    void parseFields_fieldWithoutColon_notPushedToStack() {
        // Given - field without colon should not act as container
        createDataRow(8, 1, "simpleField", "Simple field", "10", "A/N", "M");
        createDataRow(9, 1, "anotherField", "Another field", "20", "N", "O");

        // When
        List<FieldNode> result = parser.parseFields(sheet);

        // Then - both should be root level
        assertEquals(2, result.size());
        assertEquals("simpleField", result.get(0).getOriginalName());
        assertEquals("anotherField", result.get(1).getOriginalName());
        assertTrue(result.get(0).getChildren().isEmpty());
        assertTrue(result.get(1).getChildren().isEmpty());
    }
}
