package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.Metadata;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetadataExtractor.
 */
class MetadataExtractorTest {

    private MetadataExtractor extractor;
    private Workbook workbook;

    @BeforeEach
    void setUp() {
        extractor = new MetadataExtractor();
        workbook = new XSSFWorkbook();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }

    @Test
    void extract_withValidMetadata_extractsAllFields() {
        Sheet sheet = createSheetWithMetadata("Create application from SMP", "CreateAppSMP", "01.00");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("Create application from SMP", meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
        assertEquals("01.00", meta.getVersion());
        assertNotNull(meta.getParseTimestamp());
        assertNotNull(meta.getParserVersion());
        assertTrue(meta.getSourceFile().endsWith("test.xlsx"));
        assertNull(meta.getSharedHeaderFile());
    }

    @Test
    void extract_withSharedHeaderFile_setsSharedHeaderPath() {
        Sheet sheet = createSheetWithMetadata("Test Op", "TestOpId", "1.0");
        Path sourceFile = Paths.get("test.xlsx");
        Path sharedHeaderFile = Paths.get("shared-header.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, sharedHeaderFile);

        assertTrue(meta.getSharedHeaderFile().endsWith("shared-header.xlsx"));
    }

    @Test
    void extract_withMissingOperationName_returnsNullForName() {
        Sheet sheet = createSheetWithMetadata(null, "CreateAppSMP", "01.00");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertNull(meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
        assertEquals("01.00", meta.getVersion());
    }

    @Test
    void extract_withMissingOperationId_returnsNullForId() {
        Sheet sheet = createSheetWithMetadata("Create application from SMP", null, "01.00");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("Create application from SMP", meta.getOperationName());
        assertNull(meta.getOperationId());
        assertEquals("01.00", meta.getVersion());
    }

    @Test
    void extract_withMissingVersion_returnsNullForVersion() {
        Sheet sheet = createSheetWithMetadata("Create application from SMP", "CreateAppSMP", null);
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("Create application from SMP", meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
        assertNull(meta.getVersion());
    }

    @Test
    void extract_withNumericVersion_convertsToString() {
        Sheet sheet = workbook.createSheet("Request");
        Row row2 = sheet.createRow(1);
        row2.createCell(1).setCellValue("Operation Name");
        row2.createCell(2).setCellValue("Test Operation");

        Row row3 = sheet.createRow(2);
        row3.createCell(1).setCellValue("Operation ID");
        row3.createCell(2).setCellValue("TestOp");
        row3.createCell(3).setCellValue("Version");
        row3.createCell(4).setCellValue(1.5); // Numeric version

        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("1.5", meta.getVersion());
    }

    @Test
    void extract_withIntegerNumericVersion_convertsWithoutDecimal() {
        Sheet sheet = workbook.createSheet("Request");
        Row row2 = sheet.createRow(1);
        row2.createCell(1).setCellValue("Operation Name");
        row2.createCell(2).setCellValue("Test Operation");

        Row row3 = sheet.createRow(2);
        row3.createCell(1).setCellValue("Operation ID");
        row3.createCell(2).setCellValue("TestOp");
        row3.createCell(3).setCellValue("Version");
        row3.createCell(4).setCellValue(2.0); // Integer stored as double

        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("2", meta.getVersion());
    }

    @Test
    void extract_withWhitespaceValues_trimsWhitespace() {
        Sheet sheet = createSheetWithMetadata("  Create app  ", "  CreateAppSMP  ", "  01.00  ");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertEquals("Create app", meta.getOperationName());
        assertEquals("CreateAppSMP", meta.getOperationId());
        assertEquals("01.00", meta.getVersion());
    }

    @Test
    void extract_withEmptyStringValues_returnsNull() {
        Sheet sheet = createSheetWithMetadata("", "", "");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertNull(meta.getOperationName());
        assertNull(meta.getOperationId());
        assertNull(meta.getVersion());
    }

    @Test
    void extract_withWhitespaceOnlyValues_returnsNull() {
        Sheet sheet = createSheetWithMetadata("   ", "   ", "   ");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertNull(meta.getOperationName());
        assertNull(meta.getOperationId());
        assertNull(meta.getVersion());
    }

    @Test
    void extract_withEmptySheet_returnsNullForAllExceptSystemFields() {
        Sheet sheet = workbook.createSheet("Request");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        assertNull(meta.getOperationName());
        assertNull(meta.getOperationId());
        assertNull(meta.getVersion());
        assertNotNull(meta.getParseTimestamp());
        assertNotNull(meta.getParserVersion());
    }

    @Test
    void extract_parseTimestamp_isIso8601Format() {
        Sheet sheet = createSheetWithMetadata("Test", "TestOp", "1.0");
        Path sourceFile = Paths.get("test.xlsx");

        Metadata meta = extractor.extract(sheet, sourceFile, null);

        // ISO 8601 format should contain 'T' separator
        assertTrue(meta.getParseTimestamp().contains("T"),
                "Timestamp should be in ISO 8601 format: " + meta.getParseTimestamp());
    }

    @Test
    void validate_withOperationId_returnsTrue() {
        Metadata meta = new Metadata();
        meta.setOperationId("CreateAppSMP");

        assertTrue(extractor.validate(meta));
    }

    @Test
    void validate_withoutOperationId_returnsFalse() {
        Metadata meta = new Metadata();

        assertFalse(extractor.validate(meta));
    }

    @Test
    void validate_withEmptyOperationId_returnsFalse() {
        Metadata meta = new Metadata();
        meta.setOperationId("");

        assertFalse(extractor.validate(meta));
    }

    @Test
    void validate_withOnlyOperationName_returnsFalse() {
        Metadata meta = new Metadata();
        meta.setOperationName("Create application from SMP");

        assertFalse(extractor.validate(meta));
    }

    @Test
    void getMissingFields_withMissingOperationId_reportsIt() {
        Metadata meta = new Metadata();

        String missing = extractor.getMissingFields(meta);

        assertEquals("Operation ID", missing);
    }

    @Test
    void getMissingFields_withAllRequired_returnsEmpty() {
        Metadata meta = new Metadata();
        meta.setOperationId("CreateAppSMP");

        String missing = extractor.getMissingFields(meta);

        assertEquals("", missing);
    }

    @Test
    void getMissingFields_withEmptyOperationId_reportsIt() {
        Metadata meta = new Metadata();
        meta.setOperationId("");

        String missing = extractor.getMissingFields(meta);

        assertEquals("Operation ID", missing);
    }

    /**
     * Helper method to create a sheet with the standard metadata structure.
     */
    private Sheet createSheetWithMetadata(String operationName, String operationId, String version) {
        Sheet sheet = workbook.createSheet("Request-" + System.nanoTime());

        // Row 2 (index 1): Operation Name
        Row row2 = sheet.createRow(1);
        row2.createCell(1).setCellValue("Operation Name");
        if (operationName != null) {
            row2.createCell(2).setCellValue(operationName);
        }

        // Row 3 (index 2): Operation ID and Version
        Row row3 = sheet.createRow(2);
        row3.createCell(1).setCellValue("Operation ID");
        if (operationId != null) {
            row3.createCell(2).setCellValue(operationId);
        }
        row3.createCell(3).setCellValue("Version");
        if (version != null) {
            row3.createCell(4).setCellValue(version);
        }

        return sheet;
    }
}
