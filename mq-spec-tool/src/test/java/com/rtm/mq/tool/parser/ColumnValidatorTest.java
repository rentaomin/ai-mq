package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ColumnValidator.
 */
class ColumnValidatorTest {

    private ColumnValidator validator;
    private Workbook workbook;
    private Sheet sheet;

    @BeforeEach
    void setUp() {
        validator = new ColumnValidator();
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Request");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
    }

    @Test
    @DisplayName("Should validate and map all columns correctly")
    void validateAndMapColumns_allColumnsPresent_returnsMapping() {
        // Given
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg lvl");
        headerRow.createCell(1).setCellValue("Field Name");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Length");
        headerRow.createCell(4).setCellValue("Messaging Datatype");
        headerRow.createCell(5).setCellValue("Opt(O/M)");
        headerRow.createCell(6).setCellValue("Remarks");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertEquals(7, result.size());
        assertEquals(0, result.get("Seg lvl"));
        assertEquals(1, result.get("Field Name"));
        assertEquals(2, result.get("Description"));
        assertEquals(3, result.get("Length"));
        assertEquals(4, result.get("Messaging Datatype"));
        assertEquals(5, result.get("Opt(O/M)"));
        assertEquals(6, result.get("Remarks"));
    }

    @Test
    @DisplayName("Should throw ParseException when required columns are missing")
    void validateAndMapColumns_requiredColumnsMissing_throwsParseException() {
        // Given - missing "Seg lvl" and "Length"
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Field Name");
        headerRow.createCell(1).setCellValue("Description");
        headerRow.createCell(2).setCellValue("Messaging Datatype");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> validator.validateAndMapColumns(headerRow, "Request"));

        String message = exception.getMessage();
        assertTrue(message.contains("Required column(s) not found"));
        assertTrue(message.contains("Seg lvl"));
        assertTrue(message.contains("Length"));
        assertTrue(message.contains("Request"));
        assertTrue(message.contains("row 8"));
    }

    @Test
    @DisplayName("Should throw ParseException when header row is null")
    void validateAndMapColumns_nullHeaderRow_throwsParseException() {
        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> validator.validateAndMapColumns(null, "Request"));

        assertTrue(exception.getMessage().contains("Header row is null"));
        assertTrue(exception.getMessage().contains("Request"));
    }

    @Test
    @DisplayName("Should normalize column names with newlines")
    void validateAndMapColumns_columnNameWithNewline_normalizesCorrectly() {
        // Given - column name with newline (common in Excel)
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg\nlvl");
        headerRow.createCell(1).setCellValue("Field\nName");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Length");
        headerRow.createCell(4).setCellValue("Messaging\nDatatype");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("Seg lvl"));
        assertEquals(1, result.get("Field Name"));
        assertEquals(4, result.get("Messaging Datatype"));
    }

    @Test
    @DisplayName("Should normalize column names with multiple spaces")
    void validateAndMapColumns_columnNameWithMultipleSpaces_normalizesCorrectly() {
        // Given - column name with extra spaces
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg   lvl");
        headerRow.createCell(1).setCellValue("Field  Name");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Length");
        headerRow.createCell(4).setCellValue("Messaging   Datatype");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("Seg lvl"));
        assertEquals(1, result.get("Field Name"));
        assertEquals(4, result.get("Messaging Datatype"));
    }

    @Test
    @DisplayName("Should preserve column order in LinkedHashMap")
    void validateAndMapColumns_multipleColumns_preservesOrder() {
        // Given
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg lvl");
        headerRow.createCell(1).setCellValue("Field Name");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Length");
        headerRow.createCell(4).setCellValue("Messaging Datatype");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then - verify order is preserved
        List<String> keys = new ArrayList<>(result.keySet());
        assertEquals("Seg lvl", keys.get(0));
        assertEquals("Field Name", keys.get(1));
        assertEquals("Description", keys.get(2));
        assertEquals("Length", keys.get(3));
        assertEquals("Messaging Datatype", keys.get(4));
    }

    @Test
    @DisplayName("Should skip empty cells")
    void validateAndMapColumns_emptyCells_skipsEmpty() {
        // Given - with empty cells in between
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg lvl");
        headerRow.createCell(1).setCellValue(""); // empty
        headerRow.createCell(2).setCellValue("Field Name");
        headerRow.createCell(3).setCellValue("Description");
        headerRow.createCell(4).setCellValue("Length");
        headerRow.createCell(5).setCellValue("Messaging Datatype");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertEquals(5, result.size()); // Only 5 non-empty columns
        assertFalse(result.containsKey(""));
    }

    @Test
    @DisplayName("Should handle numeric cell values")
    void validateAndMapColumns_numericCellValue_convertsToString() {
        // Given
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("Seg lvl");
        headerRow.createCell(1).setCellValue("Field Name");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Length");
        headerRow.createCell(4).setCellValue("Messaging Datatype");
        headerRow.createCell(5).setCellValue(123); // numeric

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("123"));
        assertEquals(5, result.get("123"));
    }

    @Test
    @DisplayName("Should return immutable list for required columns")
    void getRequiredColumns_returnsUnmodifiableList() {
        // When
        List<String> required = ColumnValidator.getRequiredColumns();

        // Then
        assertThrows(UnsupportedOperationException.class,
            () -> required.add("New Column"));
    }

    @Test
    @DisplayName("Should return immutable list for optional columns")
    void getOptionalColumns_returnsUnmodifiableList() {
        // When
        List<String> optional = ColumnValidator.getOptionalColumns();

        // Then
        assertThrows(UnsupportedOperationException.class,
            () -> optional.add("New Column"));
    }

    @Test
    @DisplayName("Should handle column names with leading/trailing spaces")
    void validateAndMapColumns_columnNameWithSpaces_trimsCorrectly() {
        // Given
        Row headerRow = sheet.createRow(7);
        headerRow.createCell(0).setCellValue("  Seg lvl  ");
        headerRow.createCell(1).setCellValue("  Field Name  ");
        headerRow.createCell(2).setCellValue("Description  ");
        headerRow.createCell(3).setCellValue("  Length");
        headerRow.createCell(4).setCellValue("Messaging Datatype");

        // When
        Map<String, Integer> result = validator.validateAndMapColumns(headerRow, "Request");

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("Seg lvl"));
        assertEquals(1, result.get("Field Name"));
        assertEquals(2, result.get("Description"));
        assertEquals(3, result.get("Length"));
    }
}
