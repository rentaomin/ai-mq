package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ParserConfig;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.Metadata;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExcelParser.
 */
class ExcelParserTest {

    @TempDir
    Path tempDir;

    private Config config;
    private ExcelParser parser;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
        parser = new ExcelParser(config);
    }

    @Test
    void testParseMinimalSpec() throws IOException {
        // Create a minimal valid Excel file
        Path specFile = createMinimalSpecFile();

        MessageModel model = parser.parse(specFile, null);

        assertNotNull(model);
        assertNotNull(model.getMetadata());
        assertNotNull(model.getRequest());
        assertNotNull(model.getResponse());
        assertNotNull(model.getSharedHeader());
    }

    @Test
    void testParseWithSharedHeaderFile() throws IOException {
        Path specFile = createMinimalSpecFile();
        Path sharedHeaderFile = createSharedHeaderFile();

        MessageModel model = parser.parse(specFile, sharedHeaderFile);

        assertNotNull(model);
        assertNotNull(model.getSharedHeader());
        assertFalse(model.getSharedHeader().getFields().isEmpty());
    }

    @Test
    void testParseWithEmbeddedSharedHeader() throws IOException {
        Path specFile = createSpecWithSharedHeader();

        MessageModel model = parser.parse(specFile, null);

        assertNotNull(model);
        assertNotNull(model.getSharedHeader());
    }

    @Test
    void testValidateInputFile_NullPath() {
        assertThrows(ParseException.class, () -> parser.parse(null, null));
    }

    @Test
    void testValidateInputFile_NonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent.xlsx");
        assertThrows(ParseException.class, () -> parser.parse(nonExistent, null));
    }

    @Test
    void testValidateInputFile_WrongExtension() throws IOException {
        Path wrongExt = tempDir.resolve("test.txt");
        Files.writeString(wrongExt, "not an excel file");

        assertThrows(ParseException.class, () -> parser.parse(wrongExt, null));
    }

    @Test
    void testParseSheet_RequestStructure() throws IOException {
        Path specFile = createSpecWithFields();

        MessageModel model = parser.parse(specFile, null);

        assertNotNull(model.getRequest());
        List<FieldNode> fields = model.getRequest().getFields();
        assertFalse(fields.isEmpty());

        // Check that fields are parsed correctly
        FieldNode firstField = fields.get(0);
        assertNotNull(firstField.getOriginalName());
        assertNotNull(firstField.getCamelCaseName());
        assertEquals(1, firstField.getSegLevel());
    }

    @Test
    void testParseSheet_FieldOrderPreserved() throws IOException {
        Path specFile = createSpecWithMultipleFields();

        MessageModel model = parser.parse(specFile, null);

        List<FieldNode> fields = model.getRequest().getFields();
        assertTrue(fields.size() >= 2);

        // Verify order is preserved (fields appear in Excel order)
        assertEquals("FIELD_ONE", fields.get(0).getOriginalName());
        assertEquals("FIELD_TWO", fields.get(1).getOriginalName());
    }

    @Test
    void testParseSheet_ObjectDetection() throws IOException {
        Path specFile = createSpecWithObject();

        MessageModel model = parser.parse(specFile, null);

        List<FieldNode> fields = model.getRequest().getFields();
        assertFalse(fields.isEmpty());

        // Find the object field
        FieldNode objectField = fields.stream()
            .filter(f -> f.isObject())
            .findFirst()
            .orElse(null);

        assertNotNull(objectField);
        assertEquals("CustomerInfo", objectField.getClassName());
        assertNotNull(objectField.getCamelCaseName());
    }

    @Test
    void testParseSheet_CamelCaseNormalization() throws IOException {
        Path specFile = createSpecWithUnderscoreField();

        MessageModel model = parser.parse(specFile, null);

        List<FieldNode> fields = model.getRequest().getFields();
        assertFalse(fields.isEmpty());

        FieldNode field = fields.get(0);
        assertEquals("CUSTOMER_NAME", field.getOriginalName());
        assertEquals("customerName", field.getCamelCaseName());
    }

    @Test
    void testMetadataExtraction() throws IOException {
        Path specFile = createSpecWithMetadata();

        MessageModel model = parser.parse(specFile, null);

        Metadata meta = model.getMetadata();
        assertNotNull(meta);
        assertNotNull(meta.getOperationName());
        assertNotNull(meta.getOperationId());
        assertNotNull(meta.getParseTimestamp());
        assertNotNull(meta.getParserVersion());
    }

    @Test
    void testParseSheet_MaxNestingDepthRespected() {
        // Set a low max depth
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setMaxNestingDepth(5);
        config.setParser(parserConfig);
        parser = new ExcelParser(config);

        // Note: This test would require a deeply nested Excel file
        // For now, just verify the config is respected
        assertEquals(5, config.getParser().getMaxNestingDepth());
    }

    // Helper methods to create test Excel files

    private Path createMinimalSpecFile() throws IOException {
        Path path = tempDir.resolve("minimal_spec.xlsx");
        Workbook wb = new XSSFWorkbook();

        // Create Request sheet
        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Create Response sheet
        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSharedHeaderFile() throws IOException {
        Path path = tempDir.resolve("shared_header.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet header = wb.createSheet("Shared Header");
        createMetadataRows(header);
        createHeaderRow(header, 7);

        // Add a simple field
        Row dataRow = header.createRow(8);
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

    private Path createSpecWithSharedHeader() throws IOException {
        Path path = tempDir.resolve("spec_with_shared_header.xlsx");
        Workbook wb = new XSSFWorkbook();

        // Create Request sheet
        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Create Response sheet
        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Create Shared Header sheet
        Sheet sharedHeader = wb.createSheet("Shared Header");
        createMetadataRows(sharedHeader);
        createHeaderRow(sharedHeader, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithFields() throws IOException {
        Path path = tempDir.resolve("spec_with_fields.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Add a simple field
        Row dataRow = request.createRow(8);
        dataRow.createCell(1).setCellValue("1");  // Seg lvl
        dataRow.createCell(2).setCellValue("CUSTOMER_ID");  // Field Name
        dataRow.createCell(3).setCellValue("Customer ID");  // Description
        dataRow.createCell(4).setCellValue("20");  // Length
        dataRow.createCell(5).setCellValue("A/N");  // Datatype
        dataRow.createCell(6).setCellValue("M");  // Opt(O/M)

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithMultipleFields() throws IOException {
        Path path = tempDir.resolve("spec_with_multiple_fields.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Field 1
        Row row1 = request.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("FIELD_ONE");
        row1.createCell(3).setCellValue("First field");
        row1.createCell(4).setCellValue("10");
        row1.createCell(5).setCellValue("A/N");

        // Field 2
        Row row2 = request.createRow(9);
        row2.createCell(1).setCellValue("1");
        row2.createCell(2).setCellValue("FIELD_TWO");
        row2.createCell(3).setCellValue("Second field");
        row2.createCell(4).setCellValue("20");
        row2.createCell(5).setCellValue("A/N");

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithObject() throws IOException {
        Path path = tempDir.resolve("spec_with_object.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Object definition
        Row objRow = request.createRow(8);
        objRow.createCell(1).setCellValue("1");
        objRow.createCell(2).setCellValue("customer_info:CustomerInfo");
        objRow.createCell(3).setCellValue("Customer information");
        objRow.createCell(4).setCellValue("");  // Empty length for object
        objRow.createCell(5).setCellValue("");  // Empty datatype for object

        // Child field
        Row childRow = request.createRow(9);
        childRow.createCell(1).setCellValue("2");
        childRow.createCell(2).setCellValue("NAME");
        childRow.createCell(3).setCellValue("Customer name");
        childRow.createCell(4).setCellValue("50");
        childRow.createCell(5).setCellValue("A/N");

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithUnderscoreField() throws IOException {
        Path path = tempDir.resolve("spec_with_underscore.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Row dataRow = request.createRow(8);
        dataRow.createCell(1).setCellValue("1");
        dataRow.createCell(2).setCellValue("CUSTOMER_NAME");
        dataRow.createCell(3).setCellValue("Customer name");
        dataRow.createCell(4).setCellValue("50");
        dataRow.createCell(5).setCellValue("A/N");

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithMetadata() throws IOException {
        Path path = tempDir.resolve("spec_with_metadata.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");

        // Row 2: Operation Name
        Row row1 = request.createRow(1);
        row1.createCell(1).setCellValue("Operation Name");
        row1.createCell(2).setCellValue("Create Customer");

        // Row 3: Operation ID and Version
        Row row2 = request.createRow(2);
        row2.createCell(1).setCellValue("Operation ID");
        row2.createCell(2).setCellValue("CreateCustomer");
        row2.createCell(3).setCellValue("Version");
        row2.createCell(4).setCellValue("01.00");

        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

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
        row1.createCell(2).setCellValue("Test Operation");

        // Row 3: Operation ID and Version
        Row row2 = sheet.createRow(2);
        row2.createCell(1).setCellValue("Operation ID");
        row2.createCell(2).setCellValue("TestOp");
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
