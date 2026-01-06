package com.rtm.mq.tool.validator;

import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XmlBeanValidator.
 *
 * <p>Tests the minimal required scenarios:</p>
 * <ul>
 *   <li>valid XML - pass</li>
 *   <li>malformed XML - fail</li>
 *   <li>missing root attribute - fail</li>
 *   <li>unknown XML type - fail</li>
 * </ul>
 */
class XmlBeanValidatorTest {

    private XmlBeanValidator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = new XmlBeanValidator();
    }

    @Test
    void testGetType() {
        assertEquals("xml", validator.getType());
    }

    @Test
    void testValidXml_Pass() throws IOException {
        String validXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <DataField name=\"accountNumber\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +
            "  <DataField name=\"transactionId\" length=\"10\" converter=\"stringFieldConverter\"/>\n" +
            "  <CompositeField name=\"customer\" forType=\"com.example.Customer\">\n" +
            "    <DataField name=\"customerId\" length=\"15\" converter=\"stringFieldConverter\"/>\n" +
            "  </CompositeField>\n" +
            "  <RepeatingField name=\"transactions\" fixedCount=\"9\" forType=\"com.example.Transaction\">\n" +
            "    <DataField name=\"amount\" length=\"15\" converter=\"stringFieldConverter\"/>\n" +
            "  </RepeatingField>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("valid.xml");
        Files.writeString(xmlFile, validXml);

        ValidationResult result = validator.validate(xmlFile);

        assertTrue(result.isSuccess(), "Valid XML should pass validation");
        assertTrue(result.getErrors().isEmpty(), "No errors expected for valid XML");
    }

    @Test
    void testMalformedXml_Fail() throws IOException {
        String malformedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <DataField name=\"field1\" length=\"20\"\n" +  // Missing closing bracket
            "</beans>";

        Path xmlFile = tempDir.resolve("malformed.xml");
        Files.writeString(xmlFile, malformedXml);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "Malformed XML should fail validation");
        assertEquals(1, result.getErrors().size());
        assertEquals(XmlBeanValidator.ERR_PARSE_FAILED, result.getErrors().get(0).getRuleCode());
        assertTrue(result.getErrors().get(0).getDetails().contains("filePath="));
    }

    @Test
    void testMissingRootAttribute_MessageType_Fail() throws IOException {
        String xmlMissingMessageType = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans version=\"1.0\">\n" +  // Missing messageType
            "  <DataField name=\"field1\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("missing-messagetype.xml");
        Files.writeString(xmlFile, xmlMissingMessageType);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "XML missing messageType should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_ATTR_MISSING) &&
                          e.getDescription().contains("messageType")));
    }

    @Test
    void testMissingRootAttribute_Version_Fail() throws IOException {
        String xmlMissingVersion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\">\n" +  // Missing version
            "  <DataField name=\"field1\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("missing-version.xml");
        Files.writeString(xmlFile, xmlMissingVersion);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "XML missing version should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_ATTR_MISSING) &&
                          e.getDescription().contains("version")));
    }

    @Test
    void testUnknownXmlType_Fail() throws IOException {
        String xmlWithUnknownType = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <UnknownFieldType name=\"field1\" length=\"20\"/>\n" +  // Invalid type
            "</beans>";

        Path xmlFile = tempDir.resolve("unknown-type.xml");
        Files.writeString(xmlFile, xmlWithUnknownType);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "XML with unknown type should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_TYPE_UNKNOWN) &&
                          e.getDescription().contains("UnknownFieldType")));
    }

    @Test
    void testFileNotFound_Fail() {
        Path nonExistentFile = tempDir.resolve("nonexistent.xml");

        ValidationResult result = validator.validate(nonExistentFile);

        assertFalse(result.isSuccess(), "Non-existent file should fail validation");
        assertEquals(1, result.getErrors().size());
        assertEquals(XmlBeanValidator.ERR_FILE_NOT_FOUND, result.getErrors().get(0).getRuleCode());
    }

    @Test
    void testEmptyFile_Fail() throws IOException {
        Path emptyFile = tempDir.resolve("empty.xml");
        Files.writeString(emptyFile, "");

        ValidationResult result = validator.validate(emptyFile);

        assertFalse(result.isSuccess(), "Empty file should fail validation");
        assertEquals(1, result.getErrors().size());
        assertEquals(XmlBeanValidator.ERR_FILE_EMPTY, result.getErrors().get(0).getRuleCode());
    }

    @Test
    void testMissingFieldName_Fail() throws IOException {
        String xmlMissingName = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <DataField length=\"20\" converter=\"stringFieldConverter\"/>\n" +  // Missing name
            "</beans>";

        Path xmlFile = tempDir.resolve("missing-name.xml");
        Files.writeString(xmlFile, xmlMissingName);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "Field missing name should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_NAME_MISSING)));
    }

    @Test
    void testInvalidCamelCaseName_Fail() throws IOException {
        String xmlInvalidName = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <DataField name=\"AccountNumber\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +  // Starts with uppercase
            "</beans>";

        Path xmlFile = tempDir.resolve("invalid-camelcase.xml");
        Files.writeString(xmlFile, xmlInvalidName);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "Field with non-camelCase name should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_NAME_INVALID) &&
                          e.getDescription().contains("AccountNumber")));
    }

    @Test
    void testTransitoryFieldWithoutName_Pass() throws IOException {
        String xmlWithTransitory = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <DataField transitory=\"true\" length=\"10\" defaultValue=\"GRP001\" converter=\"stringFieldConverter\"/>\n" +
            "  <DataField name=\"normalField\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("transitory.xml");
        Files.writeString(xmlFile, xmlWithTransitory);

        ValidationResult result = validator.validate(xmlFile);

        assertTrue(result.isSuccess(), "Transitory field without name should pass");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testNestedFieldValidation() throws IOException {
        String xmlWithNestedInvalid = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <CompositeField name=\"customer\" forType=\"com.example.Customer\">\n" +
            "    <DataField name=\"InvalidName\" length=\"15\" converter=\"stringFieldConverter\"/>\n" +  // Invalid camelCase
            "  </CompositeField>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("nested-invalid.xml");
        Files.writeString(xmlFile, xmlWithNestedInvalid);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "Nested field with invalid name should fail");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getRuleCode().equals(XmlBeanValidator.ERR_NAME_INVALID) &&
                          e.getDescription().contains("InvalidName")));
    }

    @Test
    void testMultipleErrors_Collected() throws IOException {
        String xmlWithMultipleErrors = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans>\n" +  // Missing both messageType and version
            "  <DataField name=\"InvalidName\" length=\"20\" converter=\"stringFieldConverter\"/>\n" +  // Invalid camelCase
            "  <DataField length=\"10\" converter=\"stringFieldConverter\"/>\n" +  // Missing name
            "</beans>";

        Path xmlFile = tempDir.resolve("multiple-errors.xml");
        Files.writeString(xmlFile, xmlWithMultipleErrors);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess(), "XML with multiple errors should fail");
        // Should have at least 4 errors: missing messageType, missing version, invalid name, missing name
        assertTrue(result.getErrors().size() >= 4, "Should collect multiple errors");

        // Verify all errors have required context
        for (ValidationError error : result.getErrors()) {
            assertNotNull(error.getRuleCode());
            assertNotNull(error.getDescription());
            assertNotNull(error.getDetails());
            assertTrue(error.getDetails().contains("filePath="), "Error should include filePath");
        }
    }

    @Test
    void testErrorContextIncludesXpath() throws IOException {
        String xmlWithNestedError = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans messageType=\"Request\" version=\"1.0\">\n" +
            "  <CompositeField name=\"customer\" forType=\"com.example.Customer\">\n" +
            "    <RepeatingField name=\"orders\" fixedCount=\"5\" forType=\"com.example.Order\">\n" +
            "      <DataField name=\"InvalidOrderId\" length=\"10\" converter=\"stringFieldConverter\"/>\n" +
            "    </RepeatingField>\n" +
            "  </CompositeField>\n" +
            "</beans>";

        Path xmlFile = tempDir.resolve("nested-xpath.xml");
        Files.writeString(xmlFile, xmlWithNestedError);

        ValidationResult result = validator.validate(xmlFile);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getDetails().contains("xpath=")));
    }
}
