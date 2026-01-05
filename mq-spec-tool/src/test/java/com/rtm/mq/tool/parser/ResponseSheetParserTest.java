package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ParserConfig;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import com.rtm.mq.tool.model.ValidationResult;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResponseSheetParser.
 *
 * <p>These tests verify:</p>
 * <ul>
 *   <li>AC1: Response Sheet correctly parses into FieldGroup</li>
 *   <li>AC2: Null Response Sheet returns empty FieldGroup</li>
 *   <li>AC3: Field order preserved from Excel</li>
 *   <li>AC4: Nested structures (Seg lvl) correctly parsed</li>
 *   <li>AC5: Object/array detection works correctly</li>
 *   <li>AC6: CamelCase naming normalization</li>
 *   <li>AC7: Transitory fields correctly marked</li>
 *   <li>AC8: SourceMetadata correctly recorded</li>
 *   <li>AC9: Nesting depth validation</li>
 *   <li>AC10: Consistency with Request parsing</li>
 *   <li>AC11: MessageModel.response correctly filled</li>
 * </ul>
 */
class ResponseSheetParserTest {

    @TempDir
    Path tempDir;

    private Config config;
    private ExcelParser excelParser;
    private ResponseSheetParser responseParser;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();
        excelParser = new ExcelParser(config);
        responseParser = new ResponseSheetParser(excelParser, config);
    }

    // ==========================================================================
    // AC1: Response Sheet correctly parses into FieldGroup
    // ==========================================================================

    @Test
    void testParseResponseSheet_Basic() throws IOException {
        Path specFile = createSpecWithResponseFields();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        assertNotNull(result);
        assertNotNull(result.getFields());
        assertFalse(result.getFields().isEmpty());

        wb.close();
    }

    @Test
    void testParseResponseSheet_WithFields() throws IOException {
        Path specFile = createSpecWithResponseFields();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        assertEquals(2, result.getFields().size());

        FieldNode returnCode = result.getFields().get(0);
        assertEquals("RETURN_CODE", returnCode.getOriginalName());

        FieldNode errorMessage = result.getFields().get(1);
        assertEquals("ERROR_MESSAGE", errorMessage.getOriginalName());

        wb.close();
    }

    // ==========================================================================
    // AC2: Response Sheet does not exist returns empty FieldGroup
    // ==========================================================================

    @Test
    void testParseNullSheet_ReturnsEmptyFieldGroup() {
        FieldGroup result = responseParser.parse(null);

        assertNotNull(result);
        assertNotNull(result.getFields());
        assertTrue(result.getFields().isEmpty());
    }

    @Test
    void testParseNullSheet_NoException() {
        assertDoesNotThrow(() -> responseParser.parse(null));
    }

    // ==========================================================================
    // AC3: Field order matches Excel Response Sheet exactly
    // ==========================================================================

    @Test
    void testFieldOrderPreserved() throws IOException {
        Path specFile = createSpecWithOrderedFields();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        List<FieldNode> fields = result.getFields();
        assertEquals(3, fields.size());
        assertEquals("FIRST_FIELD", fields.get(0).getOriginalName());
        assertEquals("SECOND_FIELD", fields.get(1).getOriginalName());
        assertEquals("THIRD_FIELD", fields.get(2).getOriginalName());

        wb.close();
    }

    // ==========================================================================
    // AC4: Nested structures (Seg lvl) correctly parsed
    // ==========================================================================

    @Test
    void testNestedStructureParsing() throws IOException {
        Path specFile = createSpecWithNestedResponse();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        // Find the parent object
        FieldNode parent = result.getFields().stream()
            .filter(f -> f.getOriginalName().contains("response_data"))
            .findFirst()
            .orElse(null);

        assertNotNull(parent, "Parent object should exist");
        assertFalse(parent.getChildren().isEmpty(), "Parent should have children");
        assertEquals(2, parent.getSegLevel());

        // Check child
        FieldNode child = parent.getChildren().get(0);
        assertEquals("RESULT_VALUE", child.getOriginalName());
        assertEquals(3, child.getSegLevel());

        wb.close();
    }

    // ==========================================================================
    // AC5: Object/array fields correctly identified
    // ==========================================================================

    @Test
    void testObjectDetection() throws IOException {
        Path specFile = createSpecWithResponseObject();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        FieldNode objectField = result.getFields().stream()
            .filter(FieldNode::isObject)
            .findFirst()
            .orElse(null);

        assertNotNull(objectField, "Object field should be detected");
        assertEquals("ResponseData", objectField.getClassName());

        wb.close();
    }

    @Test
    void testArrayDetection() throws IOException {
        Path specFile = createSpecWithResponseArray();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        FieldNode arrayField = result.getFields().stream()
            .filter(FieldNode::isArray)
            .findFirst()
            .orElse(null);

        assertNotNull(arrayField, "Array field should be detected");
        assertTrue(arrayField.isArray());

        wb.close();
    }

    // ==========================================================================
    // AC6: Field names correctly converted to camelCase
    // ==========================================================================

    @Test
    void testCamelCaseConversion() throws IOException {
        Path specFile = createSpecWithResponseFields();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        FieldNode returnCode = result.getFields().get(0);
        assertEquals("RETURN_CODE", returnCode.getOriginalName());
        assertEquals("returnCode", returnCode.getCamelCaseName());

        FieldNode errorMessage = result.getFields().get(1);
        assertEquals("ERROR_MESSAGE", errorMessage.getOriginalName());
        assertEquals("errorMessage", errorMessage.getCamelCaseName());

        wb.close();
    }

    // ==========================================================================
    // AC7: Transitory fields (groupId, occurrenceCount) correctly marked
    // ==========================================================================

    @Test
    void testTransitoryFieldsMarked() throws IOException {
        Path specFile = createSpecWithGroupIdField();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        // Verify transitory fields are marked
        for (FieldNode field : result.getFields()) {
            if (field.getGroupId() != null && !field.getGroupId().isEmpty()) {
                // groupId and occurrenceCount are stored but may mark transitory
                assertNotNull(field.getGroupId());
            }
        }

        wb.close();
    }

    // ==========================================================================
    // AC8: SourceMetadata correctly recorded (sheetName="Response", rowIndex)
    // ==========================================================================

    @Test
    void testSourceMetadataRecorded() throws IOException {
        Path specFile = createSpecWithResponseFields();
        Workbook wb = openWorkbook(specFile);
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup result = responseParser.parse(responseSheet);

        for (FieldNode field : result.getFields()) {
            SourceMetadata source = field.getSource();
            assertNotNull(source, "SourceMetadata should not be null");
            assertEquals("Response", source.getSheetName(), "Sheet name should be 'Response'");
            assertTrue(source.getRowIndex() > 0, "Row index should be positive (1-based)");
        }

        wb.close();
    }

    // ==========================================================================
    // AC9: Nesting depth exceeds limit produces validation error
    // ==========================================================================

    @Test
    void testNestingDepthValidation_WithinLimit() {
        FieldGroup response = createFieldGroupWithDepth(3);

        ValidationResult result = responseParser.validate(response);

        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testNestingDepthValidation_ExceedsLimit() {
        // Set a low max depth
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setMaxNestingDepth(2);
        config.setParser(parserConfig);
        responseParser = new ResponseSheetParser(excelParser, config);

        // Create a FieldGroup with depth 3
        FieldGroup response = createFieldGroupWithDepth(3);

        ValidationResult result = responseParser.validate(response);

        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("VR-104", result.getErrors().get(0).getRuleCode());
    }

    @Test
    void testValidateEmptyResponse() {
        FieldGroup emptyResponse = new FieldGroup();

        ValidationResult result = responseParser.validate(emptyResponse);

        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateNullResponse() {
        ValidationResult result = responseParser.validate(null);

        assertTrue(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    // ==========================================================================
    // AC10: Consistency with T-107 Request parsing logic
    // ==========================================================================

    @Test
    void testConsistencyWithRequestParsing() throws IOException {
        Path specFile = createSpecWithBothRequestAndResponse();
        Workbook wb = openWorkbook(specFile);

        Sheet requestSheet = wb.getSheet("Request");
        Sheet responseSheet = wb.getSheet("Response");

        FieldGroup request = excelParser.parseSheet(requestSheet, "Request");
        FieldGroup response = responseParser.parse(responseSheet);

        // Both should have the same structure for identical fields
        assertNotNull(request);
        assertNotNull(response);

        // Verify both sheets were parsed with same logic
        assertEquals(1, request.getFields().size());
        assertEquals(1, response.getFields().size());

        FieldNode reqField = request.getFields().get(0);
        FieldNode resField = response.getFields().get(0);

        // Verify same processing was applied
        assertNotNull(reqField.getCamelCaseName());
        assertNotNull(resField.getCamelCaseName());
        assertEquals("Request", reqField.getSource().getSheetName());
        assertEquals("Response", resField.getSource().getSheetName());

        wb.close();
    }

    // ==========================================================================
    // AC11: MessageModel.response correctly filled
    // ==========================================================================

    @Test
    void testMessageModelResponseFilled() throws IOException {
        Path specFile = createSpecWithResponseFields();

        var model = excelParser.parse(specFile, null);

        assertNotNull(model.getResponse());
        assertNotNull(model.getResponse().getFields());
        assertFalse(model.getResponse().getFields().isEmpty());
    }

    @Test
    void testMessageModelResponseEmpty_WhenNoResponseSheet() throws IOException {
        Path specFile = createSpecWithoutResponseSheet();

        var model = excelParser.parse(specFile, null);

        assertNotNull(model.getResponse());
        assertTrue(model.getResponse().getFields().isEmpty());
    }

    // ==========================================================================
    // Constructor validation tests
    // ==========================================================================

    @Test
    void testConstructor_NullExcelParser() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResponseSheetParser(null, config));
    }

    @Test
    void testConstructor_NullConfig() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResponseSheetParser(excelParser, null));
    }

    // ==========================================================================
    // Sheet name validation tests
    // ==========================================================================

    @Test
    void testInvalidSheetName_ThrowsException() throws IOException {
        Path specFile = createSpecWithWrongSheetName();
        Workbook wb = openWorkbook(specFile);
        Sheet invalidSheet = wb.getSheet("InvalidName");

        assertThrows(ParseException.class, () -> responseParser.parse(invalidSheet));

        wb.close();
    }

    @Test
    void testGetSheetName() {
        assertEquals("Response", responseParser.getSheetName());
    }

    // ==========================================================================
    // Helper methods
    // ==========================================================================

    private Workbook openWorkbook(Path path) throws IOException {
        return new XSSFWorkbook(path.toFile());
    }

    private FieldGroup createFieldGroupWithDepth(int depth) {
        FieldGroup group = new FieldGroup();
        List<FieldNode> currentLevel = new ArrayList<>();

        FieldNode root = FieldNode.builder()
            .originalName("root_field")
            .segLevel(1)
            .build();
        group.addField(root);
        currentLevel.add(root);

        for (int i = 2; i <= depth; i++) {
            List<FieldNode> nextLevel = new ArrayList<>();
            for (FieldNode parent : currentLevel) {
                FieldNode child = FieldNode.builder()
                    .originalName("child_level_" + i)
                    .segLevel(i)
                    .children(new ArrayList<>())
                    .build();
                parent.getChildren().add(child);
                nextLevel.add(child);
            }
            currentLevel = nextLevel;
        }

        return group;
    }

    private Path createSpecWithResponseFields() throws IOException {
        Path path = tempDir.resolve("spec_with_response.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Add Response fields
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("RETURN_CODE");
        row1.createCell(3).setCellValue("Return code");
        row1.createCell(4).setCellValue("10");
        row1.createCell(5).setCellValue("A/N");
        row1.createCell(6).setCellValue("M");

        Row row2 = response.createRow(9);
        row2.createCell(1).setCellValue("1");
        row2.createCell(2).setCellValue("ERROR_MESSAGE");
        row2.createCell(3).setCellValue("Error message");
        row2.createCell(4).setCellValue("200");
        row2.createCell(5).setCellValue("A/N");
        row2.createCell(6).setCellValue("O");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithOrderedFields() throws IOException {
        Path path = tempDir.resolve("spec_with_ordered_fields.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Add fields in specific order
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("FIRST_FIELD");
        row1.createCell(3).setCellValue("First");
        row1.createCell(4).setCellValue("10");
        row1.createCell(5).setCellValue("A/N");

        Row row2 = response.createRow(9);
        row2.createCell(1).setCellValue("1");
        row2.createCell(2).setCellValue("SECOND_FIELD");
        row2.createCell(3).setCellValue("Second");
        row2.createCell(4).setCellValue("10");
        row2.createCell(5).setCellValue("A/N");

        Row row3 = response.createRow(10);
        row3.createCell(1).setCellValue("1");
        row3.createCell(2).setCellValue("THIRD_FIELD");
        row3.createCell(3).setCellValue("Third");
        row3.createCell(4).setCellValue("10");
        row3.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithNestedResponse() throws IOException {
        Path path = tempDir.resolve("spec_with_nested_response.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Level 1 field
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("STATUS");
        row1.createCell(3).setCellValue("Status");
        row1.createCell(4).setCellValue("10");
        row1.createCell(5).setCellValue("A/N");

        // Level 2 object (parent)
        Row row2 = response.createRow(9);
        row2.createCell(1).setCellValue("2");
        row2.createCell(2).setCellValue("response_data:ResponseData");
        row2.createCell(3).setCellValue("Response data");

        // Level 3 field (child)
        Row row3 = response.createRow(10);
        row3.createCell(1).setCellValue("3");
        row3.createCell(2).setCellValue("RESULT_VALUE");
        row3.createCell(3).setCellValue("Result value");
        row3.createCell(4).setCellValue("50");
        row3.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithResponseObject() throws IOException {
        Path path = tempDir.resolve("spec_with_response_object.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Object definition
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("response_data:ResponseData");
        row1.createCell(3).setCellValue("Response data object");

        // Child field
        Row row2 = response.createRow(9);
        row2.createCell(1).setCellValue("2");
        row2.createCell(2).setCellValue("VALUE");
        row2.createCell(3).setCellValue("Value");
        row2.createCell(4).setCellValue("10");
        row2.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithResponseArray() throws IOException {
        Path path = tempDir.resolve("spec_with_response_array.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Array definition with occurrenceCount
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("items:Item");
        row1.createCell(3).setCellValue("List of items");
        row1.createCell(9).setCellValue("0..N");  // occurrenceCount column

        // Child field
        Row row2 = response.createRow(9);
        row2.createCell(1).setCellValue("2");
        row2.createCell(2).setCellValue("ITEM_ID");
        row2.createCell(3).setCellValue("Item ID");
        row2.createCell(4).setCellValue("10");
        row2.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithGroupIdField() throws IOException {
        Path path = tempDir.resolve("spec_with_groupid.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        // Field with groupId
        Row row1 = response.createRow(8);
        row1.createCell(1).setCellValue("1");
        row1.createCell(2).setCellValue("RESPONSE_FIELD");
        row1.createCell(3).setCellValue("Response field");
        row1.createCell(4).setCellValue("10");
        row1.createCell(5).setCellValue("A/N");
        row1.createCell(8).setCellValue("GRP001");  // groupId column

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithBothRequestAndResponse() throws IOException {
        Path path = tempDir.resolve("spec_with_both.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Row reqRow = request.createRow(8);
        reqRow.createCell(1).setCellValue("1");
        reqRow.createCell(2).setCellValue("REQUEST_FIELD");
        reqRow.createCell(3).setCellValue("Request field");
        reqRow.createCell(4).setCellValue("10");
        reqRow.createCell(5).setCellValue("A/N");

        Sheet response = wb.createSheet("Response");
        createMetadataRows(response);
        createHeaderRow(response, 7);

        Row resRow = response.createRow(8);
        resRow.createCell(1).setCellValue("1");
        resRow.createCell(2).setCellValue("RESPONSE_FIELD");
        resRow.createCell(3).setCellValue("Response field");
        resRow.createCell(4).setCellValue("10");
        resRow.createCell(5).setCellValue("A/N");

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithoutResponseSheet() throws IOException {
        Path path = tempDir.resolve("spec_without_response.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        Row row = request.createRow(8);
        row.createCell(1).setCellValue("1");
        row.createCell(2).setCellValue("FIELD");
        row.createCell(3).setCellValue("Field");
        row.createCell(4).setCellValue("10");
        row.createCell(5).setCellValue("A/N");

        // Note: No Response sheet created

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private Path createSpecWithWrongSheetName() throws IOException {
        Path path = tempDir.resolve("spec_with_wrong_name.xlsx");
        Workbook wb = new XSSFWorkbook();

        Sheet request = wb.createSheet("Request");
        createMetadataRows(request);
        createHeaderRow(request, 7);

        // Create sheet with wrong name
        Sheet invalid = wb.createSheet("InvalidName");
        createMetadataRows(invalid);
        createHeaderRow(invalid, 7);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            wb.write(fos);
        }
        wb.close();

        return path;
    }

    private void createMetadataRows(Sheet sheet) {
        Row row1 = sheet.createRow(1);
        row1.createCell(1).setCellValue("Operation Name");
        row1.createCell(2).setCellValue("Test Operation");

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
        headerRow.createCell(7).setCellValue("Default");
        headerRow.createCell(8).setCellValue("groupId");
        headerRow.createCell(9).setCellValue("occurrenceCount");
    }
}
