package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SharedHeaderLoader.
 */
class SharedHeaderLoaderTest {

    @TempDir
    Path tempDir;

    private Config config;
    private ExcelParser parser;
    private SharedHeaderLoader loader;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
        parser = new ExcelParser(config);
        loader = new SharedHeaderLoader(parser);
    }

    @Test
    void testLoadFromFile_WithSharedHeaderSheet() throws IOException {
        Path headerFile = createSharedHeaderFile("Shared Header");

        FieldGroup group = loader.loadFromFile(headerFile, config);

        assertNotNull(group);
        assertNotNull(group.getFields());
        assertFalse(group.getFields().isEmpty());
    }

    @Test
    void testLoadFromFile_FallbackToFirstSheet() throws IOException {
        // Create file without "Shared Header" sheet name
        Path headerFile = createSharedHeaderFile("Header");

        FieldGroup group = loader.loadFromFile(headerFile, config);

        assertNotNull(group);
        assertNotNull(group.getFields());
        assertFalse(group.getFields().isEmpty());
    }

    @Test
    void testLoadFromFile_EmptyWorkbook() throws IOException {
        Path headerFile = createEmptyWorkbook();

        FieldGroup group = loader.loadFromFile(headerFile, config);

        assertNotNull(group);
        assertTrue(group.getFields().isEmpty());
    }

    @Test
    void testLoadFromFile_NonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.xlsx");

        assertThrows(ParseException.class, () -> loader.loadFromFile(nonExistent, config));
    }

    @Test
    void testLoadFromFile_FieldsAreParsed() throws IOException {
        Path headerFile = createSharedHeaderWithFields();

        FieldGroup group = loader.loadFromFile(headerFile, config);

        assertNotNull(group);
        List<FieldNode> fields = group.getFields();
        assertFalse(fields.isEmpty());

        // Verify first field is parsed correctly
        FieldNode firstField = fields.get(0);
        assertNotNull(firstField.getOriginalName());
        assertNotNull(firstField.getCamelCaseName());
        assertEquals(1, firstField.getSegLevel());
    }

    @Test
    void testLoadFromFile_CamelCaseApplied() throws IOException {
        Path headerFile = createSharedHeaderWithUnderscoreField();

        FieldGroup group = loader.loadFromFile(headerFile, config);

        assertNotNull(group);
        assertFalse(group.getFields().isEmpty());

        FieldNode field = group.getFields().get(0);
        assertEquals("TRANSACTION_ID", field.getOriginalName());
        assertEquals("transactionId", field.getCamelCaseName());
    }

    // Helper methods to create test Excel files

    private Path createSharedHeaderFile(String sheetName) throws IOException {
        Path path = tempDir.resolve("shared_header_" + sheetName + ".xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet(sheetName);
        createMetadataRows(sheet);
        createHeaderRow(sheet, 7);

        // Add a field
        Row dataRow = sheet.createRow(8);
        dataRow.createCell(1).setCellValue("1");  // Seg lvl
        dataRow.createCell(2).setCellValue("HEADER_FIELD");  // Field Name
        dataRow.createCell(3).setCellValue("Header field");  // Description
        dataRow.createCell(4).setCellValue("10");  // Length
        dataRow.createCell(5).setCellValue("A/N");  // Datatype

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createEmptyWorkbook() throws IOException {
        Path path = tempDir.resolve("empty_workbook.xlsx");
        Workbook wb = new XSSFWorkbook();

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSharedHeaderWithFields() throws IOException {
        Path path = tempDir.resolve("shared_header_fields.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("Shared Header");
        createMetadataRows(sheet);
        createHeaderRow(sheet, 7);

        // Field 1
        Row row1 = sheet.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("MSG_ID");
        row1.createCell(3).setCellValue("Message ID");
        row1.createCell(4).setCellValue("20");
        row1.createCell(5).setCellValue("A/N");

        // Field 2
        Row row2 = sheet.createRow(9);
        row2.createCell(1).setCellValue("1");
        row2.createCell(2).setCellValue("MSG_TYPE");
        row2.createCell(3).setCellValue("Message Type");
        row2.createCell(4).setCellValue("10");
        row2.createCell(5).setCellValue("A");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSharedHeaderWithUnderscoreField() throws IOException {
        Path path = tempDir.resolve("shared_header_underscore.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = wb.createSheet("Shared Header");
        createMetadataRows(sheet);
        createHeaderRow(sheet, 7);

        Row dataRow = sheet.createRow(8);
        dataRow.createCell(1).setCellValue("1");
        dataRow.createCell(2).setCellValue("TRANSACTION_ID");
        dataRow.createCell(3).setCellValue("Transaction ID");
        dataRow.createCell(4).setCellValue("30");
        dataRow.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private void createMetadataRows(Sheet sheet) {
        // Row 2: Operation Name
        Row row1 = sheet.createRow(1);
        row1.createCell(1).setCellValue("Operation Name");
        row1.createCell(2).setCellValue("Test Shared Header");

        // Row 3: Operation ID and Version
        Row row2 = sheet.createRow(2);
        row2.createCell(1).setCellValue("Operation ID");
        row2.createCell(2).setCellValue("SharedHeader");
        row2.createCell(3).setCellValue("Version");
        row2.createCell(4).setCellValue("01.00");
    }

    private void createHeaderRow(Sheet sheet, int rowIndex) {
        Row headerRow = sheet.createRow(rowIndex);
        headerRow.createCell(1).setCellValue("Seg lvl");
        headerRow.createCell(2).setCellValue("Field Name");
        headerRow.createCell(3).setCellValue("Description");
        headerRow.createCell(4).setCellValue("Length");
        headerRow.createCell(5).setCellValue("Messaging Datatype");
        headerRow.createCell(6).setCellValue("Opt(O/M)");
    }
}
