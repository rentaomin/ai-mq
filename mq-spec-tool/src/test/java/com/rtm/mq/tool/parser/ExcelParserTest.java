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
