package com.rtm.mq.tool.validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtm.mq.tool.config.ConsistencyConfig;
import com.rtm.mq.tool.exception.ValidationException;
import com.rtm.mq.tool.model.ConsistencyIssue;
import com.rtm.mq.tool.model.ConsistencyReport;
import com.rtm.mq.tool.model.ValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cross-artifact consistency validator.
 *
 * <p>Validates consistency across XML, Java, and OpenAPI artifacts by comparing
 * normalized field representations from T-301/T-302/T-303 validator outputs.</p>
 *
 * <p>This validator performs minimal P0 consistency checks:</p>
 * <ul>
 *   <li>P0-1: Field presence alignment (MISSING_FIELD)</li>
 *   <li>P0-2: Type consistency via mapping rules (TYPE_MISMATCH)</li>
 *   <li>P0-3: Structure consistency - array/object/primitive (STRUCTURE_MISMATCH)</li>
 *   <li>P0-4: Required flag consistency (REQUIRED_MISMATCH)</li>
 * </ul>
 *
 * <p>Special rules:</p>
 * <ul>
 *   <li>groupId and occurrenceCount are XML-only fields and ignored for Java/OpenAPI</li>
 *   <li>Unknown types in strict-mode=true => ERROR; strict-mode=false => WARNING</li>
 *   <li>Fields in ignore-fields list are excluded from all checks</li>
 * </ul>
 *
 * @see Validator
 */
public class CrossArtifactConsistencyValidator implements Validator {

    private static final String TYPE = "cross-artifact";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Special fields allowed only in XML
    private static final Set<String> XML_ONLY_FIELDS = new HashSet<>(Arrays.asList("groupId", "occurrenceCount"));

    private final ConsistencyConfig config;
    private final Path outputDir;

    /**
     * Creates a new cross-artifact consistency validator.
     *
     * @param config the consistency configuration
     * @param outputDir the output directory for validation reports
     */
    public CrossArtifactConsistencyValidator(ConsistencyConfig config, Path outputDir) {
        if (config == null) {
            throw new IllegalArgumentException("ConsistencyConfig cannot be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
        this.config = config;
        this.outputDir = outputDir;
    }

    @Override
    public ValidationResult validate(Path targetPath) {
        ValidationResult result = new ValidationResult();

        try {
            // Step 1: Read validator JSON files
            Path xmlValidatorJson = targetPath.resolve("xml-validator-result.json");
            Path javaValidatorJson = targetPath.resolve("java-validator-result.json");
            Path openapiValidatorJson = targetPath.resolve("openapi-validator-result.json");

            Map<String, FieldInfo> xmlFields = readValidatorJson(xmlValidatorJson, "xml");
            Map<String, FieldInfo> javaFields = readValidatorJson(javaValidatorJson, "java");
            Map<String, FieldInfo> openapiFields = readValidatorJson(openapiValidatorJson, "openapi");

            // Step 2: Build consistency report
            ConsistencyReport report = performConsistencyChecks(xmlFields, javaFields, openapiFields);

            // Step 3: Write reports
            writeReports(report);

            // Step 4: Update validation result
            if (!report.isSuccess()) {
                // Add error to result to mark it as failed
                result.addError(new com.rtm.mq.tool.model.ValidationError(
                    "CONSISTENCY-001",
                    "Cross-artifact consistency validation failed",
                    "Found " + report.getErrorCount() + " error(s) and " + report.getWarningCount() + " warning(s)"
                ));
            }

        } catch (IOException e) {
            throw new ValidationException("Failed to read validator JSON files: " + e.getMessage());
        } catch (Exception e) {
            throw new ValidationException("Consistency validation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Reads and parses a validator JSON file into a field map.
     *
     * @param jsonPath the path to the JSON file
     * @param artifactType the artifact type (xml, java, openapi)
     * @return map of fieldPath to FieldInfo
     * @throws IOException if file cannot be read
     */
    private Map<String, FieldInfo> readValidatorJson(Path jsonPath, String artifactType) throws IOException {
        if (!Files.exists(jsonPath)) {
            throw new ValidationException("Validator JSON file not found: " + jsonPath.toAbsolutePath());
        }

        String json = Files.readString(jsonPath);
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        Map<String, FieldInfo> fields = new HashMap<>();

        // Extract fields array from JSON
        if (root.has("fields") && root.get("fields").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("fields")) {
                JsonObject fieldObj = element.getAsJsonObject();
                FieldInfo info = parseFieldInfo(fieldObj, artifactType);
                fields.put(info.fieldPath, info);
            }
        }

        return fields;
    }

    /**
     * Parses a field JSON object into FieldInfo.
     *
     * @param fieldObj the JSON object representing a field
     * @param artifactType the artifact type
     * @return the parsed FieldInfo
     */
    private FieldInfo parseFieldInfo(JsonObject fieldObj, String artifactType) {
        FieldInfo info = new FieldInfo();
        info.artifactType = artifactType;
        info.fieldPath = fieldObj.has("fieldPath") ? fieldObj.get("fieldPath").getAsString() : "";
        info.type = fieldObj.has("type") ? fieldObj.get("type").getAsString() : null;
        info.shape = fieldObj.has("shape") ? fieldObj.get("shape").getAsString() : null;
        info.required = fieldObj.has("required") && fieldObj.get("required").getAsBoolean();

        // Derive canonical type from type mapping rules
        if (info.type != null) {
            info.canonicalType = config.getTypeMappingRules().get(info.type);
        }

        return info;
    }

    /**
     * Performs consistency checks across all artifacts.
     *
     * @param xmlFields XML field map
     * @param javaFields Java field map
     * @param openapiFields OpenAPI field map
     * @return the consistency report
     */
    private ConsistencyReport performConsistencyChecks(
            Map<String, FieldInfo> xmlFields,
            Map<String, FieldInfo> javaFields,
            Map<String, FieldInfo> openapiFields) {

        ConsistencyReport report = new ConsistencyReport();
        report.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Build union set of all field paths
        Set<String> allFieldPaths = new TreeSet<>(); // TreeSet for deterministic ordering
        allFieldPaths.addAll(xmlFields.keySet());
        allFieldPaths.addAll(javaFields.keySet());
        allFieldPaths.addAll(openapiFields.keySet());

        // Check each field path
        for (String fieldPath : allFieldPaths) {
            // Skip ignored fields
            if (config.getIgnoreFields().contains(fieldPath)) {
                continue;
            }

            // Skip XML-only fields when checking Java/OpenAPI
            if (XML_ONLY_FIELDS.contains(fieldPath)) {
                continue;
            }

            FieldInfo xmlField = xmlFields.get(fieldPath);
            FieldInfo javaField = javaFields.get(fieldPath);
            FieldInfo openapiField = openapiFields.get(fieldPath);

            // P0-1: Field presence check
            checkFieldPresence(report, fieldPath, xmlField, javaField, openapiField);

            // Only perform type/structure checks if field exists in all artifacts
            if (xmlField != null && javaField != null && openapiField != null) {
                // P0-2: Type consistency check
                checkTypeConsistency(report, fieldPath, xmlField, javaField, openapiField);

                // P0-3: Structure consistency check
                checkStructureConsistency(report, fieldPath, xmlField, javaField, openapiField);

                // P0-4: Required flag consistency check
                checkRequiredConsistency(report, fieldPath, xmlField, javaField, openapiField);
            }
        }

        return report;
    }

    /**
     * P0-1: Checks field presence across artifacts.
     */
    private void checkFieldPresence(ConsistencyReport report, String fieldPath,
                                     FieldInfo xmlField, FieldInfo javaField, FieldInfo openapiField) {
        boolean xmlPresent = xmlField != null;
        boolean javaPresent = javaField != null;
        boolean openapiPresent = openapiField != null;

        // If not all present, report MISSING_FIELD
        if (!xmlPresent || !javaPresent || !openapiPresent) {
            ConsistencyIssue issue = new ConsistencyIssue(
                "MISSING_FIELD",
                "ERROR",
                fieldPath,
                "Field missing in one or more artifacts"
            );

            issue.addArtifactInfo("xml", createArtifactInfo(xmlField));
            issue.addArtifactInfo("java", createArtifactInfo(javaField));
            issue.addArtifactInfo("openapi", createArtifactInfo(openapiField));

            report.addIssue(issue);
        }
    }

    /**
     * P0-2: Checks type consistency via canonical type mapping.
     */
    private void checkTypeConsistency(ConsistencyReport report, String fieldPath,
                                      FieldInfo xmlField, FieldInfo javaField, FieldInfo openapiField) {
        String xmlCanonical = xmlField.canonicalType;
        String javaCanonical = javaField.canonicalType;
        String openapiCanonical = openapiField.canonicalType;

        // Check for unknown types
        boolean hasUnknownType = xmlCanonical == null || javaCanonical == null || openapiCanonical == null;

        if (hasUnknownType) {
            String severity = config.isStrictMode() ? "ERROR" : "WARNING";
            ConsistencyIssue issue = new ConsistencyIssue(
                "TYPE_UNKNOWN",
                severity,
                fieldPath,
                "Unknown type found (no canonical mapping)"
            );

            issue.addArtifactInfo("xml", createArtifactInfo(xmlField));
            issue.addArtifactInfo("java", createArtifactInfo(javaField));
            issue.addArtifactInfo("openapi", createArtifactInfo(openapiField));

            report.addIssue(issue);
            return;
        }

        // Check for type mismatch
        if (!xmlCanonical.equals(javaCanonical) || !xmlCanonical.equals(openapiCanonical)) {
            ConsistencyIssue issue = new ConsistencyIssue(
                "TYPE_MISMATCH",
                "ERROR",
                fieldPath,
                "Type mismatch across artifacts"
            );

            issue.addArtifactInfo("xml", createArtifactInfo(xmlField));
            issue.addArtifactInfo("java", createArtifactInfo(javaField));
            issue.addArtifactInfo("openapi", createArtifactInfo(openapiField));

            report.addIssue(issue);
        }
    }

    /**
     * P0-3: Checks structure consistency (array/object/primitive).
     */
    private void checkStructureConsistency(ConsistencyReport report, String fieldPath,
                                           FieldInfo xmlField, FieldInfo javaField, FieldInfo openapiField) {
        String xmlShape = xmlField.shape;
        String javaShape = javaField.shape;
        String openapiShape = openapiField.shape;

        // Skip if any shape is unknown
        if (xmlShape == null || javaShape == null || openapiShape == null) {
            return;
        }

        // Check for shape mismatch
        if (!xmlShape.equals(javaShape) || !xmlShape.equals(openapiShape)) {
            ConsistencyIssue issue = new ConsistencyIssue(
                "STRUCTURE_MISMATCH",
                "ERROR",
                fieldPath,
                "Structure mismatch (array/object/primitive) across artifacts"
            );

            issue.addArtifactInfo("xml", createArtifactInfo(xmlField));
            issue.addArtifactInfo("java", createArtifactInfo(javaField));
            issue.addArtifactInfo("openapi", createArtifactInfo(openapiField));

            report.addIssue(issue);
        }
    }

    /**
     * P0-4: Checks required flag consistency (minimal).
     */
    private void checkRequiredConsistency(ConsistencyReport report, String fieldPath,
                                          FieldInfo xmlField, FieldInfo javaField, FieldInfo openapiField) {
        boolean xmlRequired = xmlField.required;
        boolean javaRequired = javaField.required;
        boolean openapiRequired = openapiField.required;

        // Check for required mismatch
        if (xmlRequired != javaRequired || xmlRequired != openapiRequired) {
            String severity = config.isStrictMode() ? "ERROR" : "WARNING";
            ConsistencyIssue issue = new ConsistencyIssue(
                "REQUIRED_MISMATCH",
                severity,
                fieldPath,
                "Required flag mismatch across artifacts"
            );

            issue.addArtifactInfo("xml", createArtifactInfo(xmlField));
            issue.addArtifactInfo("java", createArtifactInfo(javaField));
            issue.addArtifactInfo("openapi", createArtifactInfo(openapiField));

            report.addIssue(issue);
        }
    }

    /**
     * Creates ArtifactFieldInfo from FieldInfo.
     */
    private ConsistencyIssue.ArtifactFieldInfo createArtifactInfo(FieldInfo field) {
        if (field == null) {
            return new ConsistencyIssue.ArtifactFieldInfo(false);
        }

        ConsistencyIssue.ArtifactFieldInfo info = new ConsistencyIssue.ArtifactFieldInfo(true);
        info.setType(field.type);
        info.setCanonicalType(field.canonicalType);
        info.setShape(field.shape);
        info.setRequired(field.required);
        return info;
    }

    /**
     * Writes consistency reports (JSON and Markdown).
     */
    private void writeReports(ConsistencyReport report) throws IOException {
        Path validationDir = outputDir.resolve("validation");
        Files.createDirectories(validationDir);

        // Write JSON report
        Path jsonPath = validationDir.resolve("consistency-report.json");
        String json = GSON.toJson(report);
        Files.writeString(jsonPath, json);

        // Write Markdown report
        Path mdPath = validationDir.resolve("consistency-report.md");
        String markdown = generateMarkdownReport(report);
        Files.writeString(mdPath, markdown);
    }

    /**
     * Generates Markdown report from ConsistencyReport.
     */
    private String generateMarkdownReport(ConsistencyReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Cross-Artifact Consistency Report\n\n");
        md.append("**Status:** ").append(report.isSuccess() ? "PASS" : "FAIL").append("\n");
        md.append("**Timestamp:** ").append(report.getTimestamp()).append("\n");
        md.append("**Errors:** ").append(report.getErrorCount()).append("\n");
        md.append("**Warnings:** ").append(report.getWarningCount()).append("\n\n");

        if (report.getIssues().isEmpty()) {
            md.append("No consistency issues found.\n");
        } else {
            md.append("## Issues\n\n");
            md.append("| Category | Severity | Field Path | XML | Java | OpenAPI |\n");
            md.append("|----------|----------|------------|-----|------|---------|\n");

            for (ConsistencyIssue issue : report.getIssues()) {
                md.append("| ").append(issue.getCategory()).append(" | ");
                md.append(issue.getSeverity()).append(" | ");
                md.append(issue.getFieldPath()).append(" | ");

                ConsistencyIssue.ArtifactFieldInfo xmlInfo = issue.getArtifacts().get("xml");
                ConsistencyIssue.ArtifactFieldInfo javaInfo = issue.getArtifacts().get("java");
                ConsistencyIssue.ArtifactFieldInfo openapiInfo = issue.getArtifacts().get("openapi");

                md.append(formatArtifactInfo(xmlInfo)).append(" | ");
                md.append(formatArtifactInfo(javaInfo)).append(" | ");
                md.append(formatArtifactInfo(openapiInfo)).append(" |\n");
            }
        }

        return md.toString();
    }

    /**
     * Formats artifact field info for markdown table.
     */
    private String formatArtifactInfo(ConsistencyIssue.ArtifactFieldInfo info) {
        if (info == null || !info.getPresent()) {
            return "MISSING";
        }
        StringBuilder sb = new StringBuilder();
        if (info.getType() != null) {
            sb.append(info.getType());
        }
        if (info.getShape() != null) {
            sb.append(" (").append(info.getShape()).append(")");
        }
        return sb.toString();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Internal field information holder.
     */
    private static class FieldInfo {
        String artifactType;
        String fieldPath;
        String type;
        String canonicalType;
        String shape;
        boolean required;
    }
}
