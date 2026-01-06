package com.rtm.mq.tool.validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rtm.mq.tool.config.ConsistencyConfig;
import com.rtm.mq.tool.model.ConsistencyReport;
import com.rtm.mq.tool.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CrossArtifactConsistencyValidator.
 */
class CrossArtifactConsistencyValidatorTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    private ConsistencyConfig config;
    private Path outputDir;

    @BeforeEach
    void setUp() {
        config = new ConsistencyConfig();
        config.setDefaults();
        outputDir = tempDir.resolve("output");
    }

    /**
     * Test 1: Missing field detection
     * - xml has fieldA, java has fieldA, openapi missing fieldA => one ERROR category MISSING_FIELD
     */
    @Test
    void testMissingFieldDetection() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // XML has fieldA
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldA", "xs:string", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldA
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldA", "String", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI missing fieldA
        Map<String, Object> openapiData = createValidatorResult();
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert
        assertFalse(result.isSuccess());

        // Read consistency report
        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertEquals(1, report.getErrorCount());
        assertEquals(1, report.getIssues().size());
        assertEquals("MISSING_FIELD", report.getIssues().get(0).getCategory());
        assertEquals("ERROR", report.getIssues().get(0).getSeverity());
        assertEquals("fieldA", report.getIssues().get(0).getFieldPath());
    }

    /**
     * Test 2: Type mismatch detection (mapping-based)
     * - xml xs:string, java String, openapi integer => TYPE_MISMATCH ERROR
     */
    @Test
    void testTypeMismatchDetection() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // XML has fieldA as string
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldA", "xs:string", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldA as String
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldA", "String", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI has fieldA as integer
        Map<String, Object> openapiData = createValidatorResult(
            createField("fieldA", "integer", "primitive", true)
        );
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert
        assertFalse(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertTrue(report.getErrorCount() >= 1);
        assertTrue(report.getIssues().stream()
            .anyMatch(i -> "TYPE_MISMATCH".equals(i.getCategory()) && "fieldA".equals(i.getFieldPath())));
    }

    /**
     * Test 3: Structure mismatch detection
     * - java List<T> but openapi scalar => STRUCTURE_MISMATCH ERROR
     */
    @Test
    void testStructureMismatchDetection() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // XML has fieldB as array
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldB", "xs:string", "array", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldB as array (List<String>)
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldB", "String", "array", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI has fieldB as primitive (scalar)
        Map<String, Object> openapiData = createValidatorResult(
            createField("fieldB", "string", "primitive", true)
        );
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert
        assertFalse(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertTrue(report.getErrorCount() >= 1);
        assertTrue(report.getIssues().stream()
            .anyMatch(i -> "STRUCTURE_MISMATCH".equals(i.getCategory()) && "fieldB".equals(i.getFieldPath())));
    }

    /**
     * Test 4: Ignore-fields works
     * - fieldPath in ignore list => not reported even if mismatched
     */
    @Test
    void testIgnoreFieldsWorks() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // Configure ignore fields
        config.setIgnoreFields(Collections.singletonList("fieldIgnored"));

        // XML has fieldIgnored
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldIgnored", "xs:string", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldIgnored with different type
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldIgnored", "Integer", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI missing fieldIgnored
        Map<String, Object> openapiData = createValidatorResult();
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert - should pass because field is ignored
        assertTrue(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertTrue(report.isSuccess());
        assertEquals(0, report.getErrorCount());
        assertEquals(0, report.getIssues().size());
    }

    /**
     * Test 5a: Strict-mode behavior - unknown type with strict-mode=true => ERROR
     */
    @Test
    void testStrictModeUnknownTypeError() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // Enable strict mode
        config.setStrictMode(true);

        // XML has fieldC with unknown type
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldC", "xs:unknownType", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldC with known type
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldC", "String", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI has fieldC with known type
        Map<String, Object> openapiData = createValidatorResult(
            createField("fieldC", "string", "primitive", true)
        );
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert
        assertFalse(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertTrue(report.getErrorCount() >= 1);
        assertTrue(report.getIssues().stream()
            .anyMatch(i -> "TYPE_UNKNOWN".equals(i.getCategory()) && "ERROR".equals(i.getSeverity())));
    }

    /**
     * Test 5b: Strict-mode behavior - unknown type with strict-mode=false => WARNING
     */
    @Test
    void testStrictModeUnknownTypeWarning() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // Disable strict mode
        config.setStrictMode(false);

        // XML has fieldD with unknown type
        Map<String, Object> xmlData = createValidatorResult(
            createField("fieldD", "xs:unknownType", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java has fieldD with known type
        Map<String, Object> javaData = createValidatorResult(
            createField("fieldD", "String", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        // OpenAPI has fieldD with known type
        Map<String, Object> openapiData = createValidatorResult(
            createField("fieldD", "string", "primitive", true)
        );
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert - should pass (warnings don't fail validation)
        assertTrue(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertTrue(report.isSuccess());
        assertEquals(0, report.getErrorCount());
        assertTrue(report.getWarningCount() >= 1);
        assertTrue(report.getIssues().stream()
            .anyMatch(i -> "TYPE_UNKNOWN".equals(i.getCategory()) && "WARNING".equals(i.getSeverity())));
    }

    /**
     * Test: groupId and occurrenceCount should be ignored for Java/OpenAPI
     */
    @Test
    void testXmlOnlyFieldsIgnored() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // XML has groupId and occurrenceCount (XML-only fields)
        Map<String, Object> xmlData = createValidatorResult(
            createField("groupId", "xs:string", "primitive", true),
            createField("occurrenceCount", "xs:int", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), xmlData);

        // Java and OpenAPI don't have these fields
        Map<String, Object> javaData = createValidatorResult();
        writeJson(validatorDir.resolve("java-validator-result.json"), javaData);

        Map<String, Object> openapiData = createValidatorResult();
        writeJson(validatorDir.resolve("openapi-validator-result.json"), openapiData);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        ValidationResult result = validator.validate(validatorDir);

        // Assert - should pass because these are XML-only fields
        assertTrue(result.isSuccess());

        ConsistencyReport report = readConsistencyReport();
        assertNotNull(report);
        assertTrue(report.isSuccess());
        assertEquals(0, report.getErrorCount());
    }

    /**
     * Test: Reports are created in correct location
     */
    @Test
    void testReportsCreated() throws IOException {
        // Arrange
        Path validatorDir = tempDir.resolve("validators");
        Files.createDirectories(validatorDir);

        // Create simple valid data
        Map<String, Object> data = createValidatorResult(
            createField("fieldX", "xs:string", "primitive", true)
        );
        writeJson(validatorDir.resolve("xml-validator-result.json"), data);

        data = createValidatorResult(
            createField("fieldX", "String", "primitive", true)
        );
        writeJson(validatorDir.resolve("java-validator-result.json"), data);

        data = createValidatorResult(
            createField("fieldX", "string", "primitive", true)
        );
        writeJson(validatorDir.resolve("openapi-validator-result.json"), data);

        // Act
        CrossArtifactConsistencyValidator validator = new CrossArtifactConsistencyValidator(config, outputDir);
        validator.validate(validatorDir);

        // Assert - both reports should exist
        Path jsonReport = outputDir.resolve("validation/consistency-report.json");
        Path mdReport = outputDir.resolve("validation/consistency-report.md");

        assertTrue(Files.exists(jsonReport), "JSON report should exist");
        assertTrue(Files.exists(mdReport), "Markdown report should exist");

        // Verify JSON report is valid JSON
        String jsonContent = Files.readString(jsonReport);
        ConsistencyReport report = GSON.fromJson(jsonContent, ConsistencyReport.class);
        assertNotNull(report);

        // Verify MD report has expected structure
        String mdContent = Files.readString(mdReport);
        assertTrue(mdContent.contains("# Cross-Artifact Consistency Report"));
        assertTrue(mdContent.contains("**Status:**"));
    }

    // Helper methods

    private Map<String, Object> createValidatorResult(Map<String, Object>... fields) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            fieldList.add(field);
        }
        result.put("fields", fieldList);
        return result;
    }

    private Map<String, Object> createField(String fieldPath, String type, String shape, boolean required) {
        Map<String, Object> field = new HashMap<>();
        field.put("fieldPath", fieldPath);
        field.put("type", type);
        field.put("shape", shape);
        field.put("required", required);
        return field;
    }

    private void writeJson(Path path, Object data) throws IOException {
        String json = GSON.toJson(data);
        Files.writeString(path, json);
    }

    private ConsistencyReport readConsistencyReport() throws IOException {
        Path reportPath = outputDir.resolve("validation/consistency-report.json");
        if (!Files.exists(reportPath)) {
            return null;
        }
        String json = Files.readString(reportPath);
        return GSON.fromJson(json, ConsistencyReport.class);
    }
}
