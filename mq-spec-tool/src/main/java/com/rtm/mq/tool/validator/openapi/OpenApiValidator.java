package com.rtm.mq.tool.validator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.generator.openapi.OpenApiTypeMapper;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.ValidationResult;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.validator.Validator;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static structure validator for OpenAPI (OAS 3.x) artifacts.
 *
 * <p>This validator performs declarative, rule-based validation on OpenAPI YAML files.
 * Validation is limited to static analysis only - no runtime API behavior is evaluated.</p>
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>OA-001: YAML syntax validation</li>
 *   <li>OA-002: OpenAPI root structure validation (openapi, info, components, paths)</li>
 *   <li>OA-003: Schema definitions must declare explicit type</li>
 *   <li>OA-004: Object schemas must define properties</li>
 *   <li>OA-005: Invalid $ref reference (target not found)</li>
 *   <li>OA-006: Circular $ref reference detected</li>
 *   <li>OA-007: Forbidden field: groupId</li>
 *   <li>OA-008: Forbidden field: occurrenceCount</li>
 *   <li>OA-009: Type mapping mismatch with OpenApiTypeMapper</li>
 *   <li>OA-010: Structural alignment mismatch with JSON Tree</li>
 * </ul>
 *
 * @see OpenApiTypeMapper
 * @see OpenApiValidationResult
 */
public class OpenApiValidator implements Validator {

    /** Rule ID for YAML syntax error. */
    public static final String RULE_YAML_SYNTAX = "OA-001";

    /** Rule ID for missing required OpenAPI root field. */
    public static final String RULE_MISSING_ROOT_FIELD = "OA-002";

    /** Rule ID for schema missing explicit type. */
    public static final String RULE_MISSING_SCHEMA_TYPE = "OA-003";

    /** Rule ID for object schema missing properties. */
    public static final String RULE_MISSING_PROPERTIES = "OA-004";

    /** Rule ID for invalid $ref reference. */
    public static final String RULE_INVALID_REF = "OA-005";

    /** Rule ID for circular $ref reference. */
    public static final String RULE_CIRCULAR_REF = "OA-006";

    /** Rule ID for forbidden field: groupId. */
    public static final String RULE_FORBIDDEN_GROUP_ID = "OA-007";

    /** Rule ID for forbidden field: occurrenceCount. */
    public static final String RULE_FORBIDDEN_OCCURRENCE_COUNT = "OA-008";

    /** Rule ID for type mapping mismatch. */
    public static final String RULE_TYPE_MISMATCH = "OA-009";

    /** Rule ID for structural alignment mismatch. */
    public static final String RULE_STRUCTURAL_MISMATCH = "OA-010";

    /** Required root fields for OpenAPI document. */
    private static final List<String> REQUIRED_ROOT_FIELDS = List.of("openapi", "info", "components");

    /** Forbidden field name: groupId. */
    private static final String FORBIDDEN_GROUP_ID = "groupId";

    /** Forbidden field name: occurrenceCount. */
    private static final String FORBIDDEN_OCCURRENCE_COUNT = "occurrenceCount";

    private final Config config;
    private final OpenApiTypeMapper typeMapper;
    private final Yaml yaml;

    /**
     * Constructs an OpenApiValidator with the given configuration.
     *
     * @param config the configuration
     */
    public OpenApiValidator(Config config) {
        this.config = config;
        this.typeMapper = new OpenApiTypeMapper(config);
        this.yaml = new Yaml();
    }

    @Override
    public String getType() {
        return "openapi";
    }

    @Override
    public ValidationResult validate(Path targetPath) {
        ValidationResult result = new ValidationResult();
        OpenApiValidationResult oaResult = validateOpenApiFile(targetPath);

        // Convert OpenApiValidationResult to ValidationResult
        for (OpenApiValidationIssue issue : oaResult.getIssues()) {
            ValidationError error = new ValidationError(
                    issue.getRuleId(),
                    issue.getSchemaPath(),
                    issue.getMessage(),
                    issue.isError() ? ValidationError.Severity.ERROR : ValidationError.Severity.WARN
            );
            if (issue.isError()) {
                result.addError(error);
            } else {
                result.addWarning(error);
            }
        }

        return result;
    }

    /**
     * Validates an OpenAPI YAML file.
     *
     * @param filePath the path to the OpenAPI YAML file
     * @return the validation result
     */
    public OpenApiValidationResult validateOpenApiFile(Path filePath) {
        OpenApiValidationResult result = new OpenApiValidationResult();
        String filePathStr = filePath.toString();

        // Validate YAML syntax
        Map<String, Object> document = parseYamlFile(filePath, filePathStr, result);
        if (document == null) {
            return result; // Parsing failed, return syntax error
        }

        // Validate OpenAPI root structure
        validateRootStructure(document, filePathStr, result);

        // Validate schemas in components
        validateSchemas(document, filePathStr, result);

        return result;
    }

    /**
     * Validates an OpenAPI directory containing openapi.yaml and schemas/*.yaml.
     *
     * @param openApiDir the path to the OpenAPI output directory
     * @return the validation result
     */
    public OpenApiValidationResult validateOpenApiDirectory(Path openApiDir) {
        OpenApiValidationResult result = new OpenApiValidationResult();

        // Main OpenAPI file
        Path mainFile = openApiDir.resolve("openapi.yaml");
        if (Files.exists(mainFile)) {
            result.merge(validateOpenApiFile(mainFile));
        }

        // Schema files
        Path schemasDir = openApiDir.resolve("schemas");
        if (Files.exists(schemasDir) && Files.isDirectory(schemasDir)) {
            try {
                Files.list(schemasDir)
                        .filter(p -> p.toString().endsWith(".yaml"))
                        .sorted()
                        .forEach(schemaFile -> result.merge(validateSchemaFile(schemaFile)));
            } catch (IOException e) {
                result.addError(schemasDir.toString(), "", RULE_YAML_SYNTAX,
                        "Failed to read schemas directory: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Validates a single schema YAML file.
     *
     * @param schemaFile the path to the schema YAML file
     * @return the validation result
     */
    public OpenApiValidationResult validateSchemaFile(Path schemaFile) {
        OpenApiValidationResult result = new OpenApiValidationResult();
        String filePathStr = schemaFile.toString();

        Map<String, Object> schema = parseYamlFile(schemaFile, filePathStr, result);
        if (schema == null) {
            return result;
        }

        // Validate schema structure
        validateSchemaDefinition(schema, filePathStr, "#", result, new LinkedHashSet<>());

        return result;
    }

    /**
     * Validates OpenAPI schemas against the intermediate JSON tree structure.
     *
     * @param fieldGroup the field group from spec-tree.json
     * @param schemaName the expected schema name
     * @param schemaContent the parsed schema content
     * @param filePath the file path for reporting
     * @return the validation result
     */
    public OpenApiValidationResult validateStructuralAlignment(
            FieldGroup fieldGroup,
            String schemaName,
            Map<String, Object> schemaContent,
            String filePath) {
        OpenApiValidationResult result = new OpenApiValidationResult();

        if (fieldGroup == null || fieldGroup.getFields() == null) {
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schemaContent.get("properties");
        if (properties == null) {
            properties = new HashMap<>();
        }

        // Check each field from JSON tree exists in schema
        for (FieldNode field : fieldGroup.getFields()) {
            if (field.isTransitory()) {
                continue; // Skip transitory fields
            }

            String fieldName = field.getCamelCaseName();
            String schemaPath = "#/properties/" + fieldName;

            if (!properties.containsKey(fieldName)) {
                result.addError(filePath, schemaPath, RULE_STRUCTURAL_MISMATCH,
                        "Field '" + fieldName + "' from JSON tree not found in schema");
            } else {
                // Validate type mapping consistency
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldSchema = (Map<String, Object>) properties.get(fieldName);
                validateTypeMappingConsistency(field, fieldSchema, filePath, schemaPath, result);

                // Recursively validate nested structures
                if (field.hasChildren() && (field.isObject() || field.isArray())) {
                    validateNestedStructuralAlignment(field, fieldSchema, filePath, schemaPath, result);
                }
            }
        }

        return result;
    }

    /**
     * Parses a YAML file and returns the document as a Map.
     *
     * @param filePath the path to the YAML file
     * @param filePathStr the file path string for reporting
     * @param result the result to add errors to
     * @return the parsed document, or null if parsing failed
     */
    private Map<String, Object> parseYamlFile(Path filePath, String filePathStr,
                                               OpenApiValidationResult result) {
        try {
            String content = Files.readString(filePath);
            Object parsed = yaml.load(content);
            if (parsed == null) {
                result.addError(filePathStr, "", RULE_YAML_SYNTAX, "YAML file is empty");
                return null;
            }
            if (!(parsed instanceof Map)) {
                result.addError(filePathStr, "", RULE_YAML_SYNTAX,
                        "YAML root must be a mapping");
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) parsed;
            return document;
        } catch (YAMLException e) {
            result.addError(filePathStr, "", RULE_YAML_SYNTAX,
                    "YAML syntax error: " + e.getMessage());
            return null;
        } catch (IOException e) {
            result.addError(filePathStr, "", RULE_YAML_SYNTAX,
                    "Failed to read file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates the OpenAPI root structure.
     *
     * @param document the parsed OpenAPI document
     * @param filePath the file path for reporting
     * @param result the result to add issues to
     */
    private void validateRootStructure(Map<String, Object> document, String filePath,
                                        OpenApiValidationResult result) {
        // Check required root fields
        for (String field : REQUIRED_ROOT_FIELDS) {
            if (!document.containsKey(field)) {
                result.addError(filePath, "#/" + field, RULE_MISSING_ROOT_FIELD,
                        "Required field '" + field + "' is missing");
            }
        }

        // paths must exist (can be empty)
        if (!document.containsKey("paths")) {
            result.addError(filePath, "#/paths", RULE_MISSING_ROOT_FIELD,
                    "Required field 'paths' is missing");
        }
    }

    /**
     * Validates all schemas in the components section.
     *
     * @param document the parsed OpenAPI document
     * @param filePath the file path for reporting
     * @param result the result to add issues to
     */
    private void validateSchemas(Map<String, Object> document, String filePath,
                                  OpenApiValidationResult result) {
        Object componentsObj = document.get("components");
        if (componentsObj == null || !(componentsObj instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) componentsObj;
        Object schemasObj = components.get("schemas");
        if (schemasObj == null || !(schemasObj instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) schemasObj;

        // Store all schema names for ref validation
        Set<String> schemaNames = new HashSet<>(schemas.keySet());

        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            String schemaPath = "#/components/schemas/" + schemaName;

            if (!(entry.getValue() instanceof Map)) {
                result.addError(filePath, schemaPath, RULE_MISSING_SCHEMA_TYPE,
                        "Schema must be an object");
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) entry.getValue();

            // Validate schema definition
            validateSchemaDefinition(schema, filePath, schemaPath, result, new LinkedHashSet<>());

            // Validate $ref references
            validateReferences(schema, filePath, schemaPath, schemaNames, result, new LinkedHashSet<>());
        }
    }

    /**
     * Validates a schema definition.
     *
     * @param schema the schema to validate
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     * @param visited set of visited schema paths for circular detection
     */
    private void validateSchemaDefinition(Map<String, Object> schema, String filePath,
                                           String schemaPath, OpenApiValidationResult result,
                                           Set<String> visited) {
        // Check for $ref first
        if (schema.containsKey("$ref")) {
            // If $ref is present, other properties are ignored per OpenAPI spec
            return;
        }

        // Check for explicit type
        Object typeObj = schema.get("type");
        if (typeObj == null) {
            result.addError(filePath, schemaPath, RULE_MISSING_SCHEMA_TYPE,
                    "Schema must declare an explicit type");
            return;
        }

        String type = typeObj.toString();

        // For object type, validate properties
        if ("object".equals(type)) {
            validateObjectSchema(schema, filePath, schemaPath, result, visited);
        }

        // For array type, validate items
        if ("array".equals(type)) {
            validateArraySchema(schema, filePath, schemaPath, result, visited);
        }

        // Check for forbidden fields
        validateForbiddenFields(schema, filePath, schemaPath, result);
    }

    /**
     * Validates an object schema has properties defined.
     *
     * @param schema the object schema
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     * @param visited set of visited schema paths
     */
    private void validateObjectSchema(Map<String, Object> schema, String filePath,
                                       String schemaPath, OpenApiValidationResult result,
                                       Set<String> visited) {
        Object propertiesObj = schema.get("properties");
        if (propertiesObj == null) {
            result.addError(filePath, schemaPath, RULE_MISSING_PROPERTIES,
                    "Object schema must define properties");
            return;
        }

        if (!(propertiesObj instanceof Map)) {
            result.addError(filePath, schemaPath + "/properties", RULE_MISSING_PROPERTIES,
                    "Properties must be a mapping");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) propertiesObj;

        // Validate each property
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propName = entry.getKey();
            String propPath = schemaPath + "/properties/" + propName;

            // Check for forbidden field names
            if (FORBIDDEN_GROUP_ID.equals(propName)) {
                result.addError(filePath, propPath, RULE_FORBIDDEN_GROUP_ID,
                        "Forbidden field 'groupId' must not appear in schema");
            }
            if (FORBIDDEN_OCCURRENCE_COUNT.equals(propName)) {
                result.addError(filePath, propPath, RULE_FORBIDDEN_OCCURRENCE_COUNT,
                        "Forbidden field 'occurrenceCount' must not appear in schema");
            }

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> propSchema = (Map<String, Object>) entry.getValue();

                // Recursively validate nested schemas
                if (!propSchema.containsKey("$ref")) {
                    validateSchemaDefinition(propSchema, filePath, propPath, result, visited);
                }
            }
        }
    }

    /**
     * Validates an array schema has items defined.
     *
     * @param schema the array schema
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     * @param visited set of visited schema paths
     */
    private void validateArraySchema(Map<String, Object> schema, String filePath,
                                      String schemaPath, OpenApiValidationResult result,
                                      Set<String> visited) {
        Object itemsObj = schema.get("items");
        if (itemsObj == null) {
            result.addWarning(filePath, schemaPath, RULE_MISSING_SCHEMA_TYPE,
                    "Array schema should define items");
            return;
        }

        if (itemsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) itemsObj;
            if (!items.containsKey("$ref")) {
                validateSchemaDefinition(items, filePath, schemaPath + "/items", result, visited);
            }
        }
    }

    /**
     * Validates that forbidden fields are not present in schema.
     *
     * @param schema the schema to validate
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     */
    private void validateForbiddenFields(Map<String, Object> schema, String filePath,
                                          String schemaPath, OpenApiValidationResult result) {
        // Check if schema directly contains forbidden fields
        if (schema.containsKey(FORBIDDEN_GROUP_ID)) {
            result.addError(filePath, schemaPath + "/" + FORBIDDEN_GROUP_ID,
                    RULE_FORBIDDEN_GROUP_ID,
                    "Forbidden field 'groupId' must not appear in schema");
        }
        if (schema.containsKey(FORBIDDEN_OCCURRENCE_COUNT)) {
            result.addError(filePath, schemaPath + "/" + FORBIDDEN_OCCURRENCE_COUNT,
                    RULE_FORBIDDEN_OCCURRENCE_COUNT,
                    "Forbidden field 'occurrenceCount' must not appear in schema");
        }
    }

    /**
     * Validates $ref references in a schema.
     *
     * @param schema the schema containing references
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param validSchemas set of valid schema names
     * @param result the result to add issues to
     * @param visited set of visited refs for circular detection
     */
    private void validateReferences(Map<String, Object> schema, String filePath,
                                     String schemaPath, Set<String> validSchemas,
                                     OpenApiValidationResult result, Set<String> visited) {
        // Check for $ref at current level
        Object refObj = schema.get("$ref");
        if (refObj != null) {
            String ref = refObj.toString();
            validateSingleReference(ref, filePath, schemaPath, validSchemas, result, visited);
        }

        // Recursively check in properties
        Object propertiesObj = schema.get("properties");
        if (propertiesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propSchema = (Map<String, Object>) entry.getValue();
                    validateReferences(propSchema, filePath,
                            schemaPath + "/properties/" + entry.getKey(),
                            validSchemas, result, new LinkedHashSet<>(visited));
                }
            }
        }

        // Check in items for array type
        Object itemsObj = schema.get("items");
        if (itemsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> items = (Map<String, Object>) itemsObj;
            validateReferences(items, filePath, schemaPath + "/items",
                    validSchemas, result, new LinkedHashSet<>(visited));
        }
    }

    /**
     * Validates a single $ref reference.
     *
     * @param ref the reference string
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param validSchemas set of valid schema names
     * @param result the result to add issues to
     * @param visited set of visited refs for circular detection
     */
    private void validateSingleReference(String ref, String filePath, String schemaPath,
                                          Set<String> validSchemas, OpenApiValidationResult result,
                                          Set<String> visited) {
        // Check for circular reference
        if (visited.contains(ref)) {
            result.addError(filePath, schemaPath, RULE_CIRCULAR_REF,
                    "Circular $ref reference detected: " + ref);
            return;
        }
        visited.add(ref);

        // Validate local file references
        if (ref.startsWith("./") && ref.endsWith(".yaml")) {
            // Local file reference like "./SchemeName.yaml"
            String schemaName = ref.substring(2, ref.length() - 5);
            // This is valid for separate schema files
            return;
        }

        // Validate component references
        if (ref.startsWith("#/components/schemas/")) {
            String schemaName = ref.substring("#/components/schemas/".length());
            if (!validSchemas.contains(schemaName)) {
                result.addError(filePath, schemaPath, RULE_INVALID_REF,
                        "$ref target schema '" + schemaName + "' not found");
            }
        }
    }

    /**
     * Validates type mapping consistency between field and schema.
     *
     * @param field the field from JSON tree
     * @param fieldSchema the schema definition
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     */
    private void validateTypeMappingConsistency(FieldNode field, Map<String, Object> fieldSchema,
                                                 String filePath, String schemaPath,
                                                 OpenApiValidationResult result) {
        if (fieldSchema == null) {
            return;
        }

        // Skip ref validation (handled separately)
        if (fieldSchema.containsKey("$ref")) {
            return;
        }

        Object schemaType = fieldSchema.get("type");
        if (schemaType == null) {
            return; // Already reported by schema validation
        }

        // Use OpenApiTypeMapper to get expected schema
        Map<String, Object> expectedSchema = typeMapper.mapToSchema(field);
        Object expectedType = expectedSchema.get("type");

        if (expectedType != null && !expectedType.equals(schemaType)) {
            // Array vs non-array mismatch
            if (field.isArray() && !"array".equals(schemaType)) {
                result.addError(filePath, schemaPath, RULE_TYPE_MISMATCH,
                        "Array field should have type 'array', got: " + schemaType);
            } else if (!field.isArray() && !field.isObject() && !schemaType.equals(expectedType)) {
                result.addWarning(filePath, schemaPath, RULE_TYPE_MISMATCH,
                        "Type mismatch: expected '" + expectedType + "', got '" + schemaType + "'");
            }
        }

        // Validate format for special types
        if ("string".equals(schemaType)) {
            String dataType = field.getDataType();
            if (dataType != null) {
                String expectedFormat = getExpectedFormat(dataType);
                Object actualFormat = fieldSchema.get("format");
                if (expectedFormat != null && actualFormat == null) {
                    result.addWarning(filePath, schemaPath, RULE_TYPE_MISMATCH,
                            "Expected format '" + expectedFormat + "' for type '" + dataType + "'");
                }
            }
        }
    }

    /**
     * Gets the expected format for a data type.
     *
     * @param dataType the data type from spec
     * @return the expected format, or null if none
     */
    private String getExpectedFormat(String dataType) {
        if (dataType == null) {
            return null;
        }
        switch (dataType.toLowerCase()) {
            case "amount":
            case "currency":
                return "decimal";
            case "date":
                return "date";
            default:
                return null;
        }
    }

    /**
     * Validates nested structural alignment.
     *
     * @param field the field with children
     * @param fieldSchema the schema definition
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     */
    private void validateNestedStructuralAlignment(FieldNode field, Map<String, Object> fieldSchema,
                                                    String filePath, String schemaPath,
                                                    OpenApiValidationResult result) {
        if (!field.hasChildren()) {
            return;
        }

        // For array types, check items
        if (field.isArray() && "array".equals(fieldSchema.get("type"))) {
            Object itemsObj = fieldSchema.get("items");
            if (itemsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> items = (Map<String, Object>) itemsObj;
                // If items has $ref, we can't validate further without resolving
                if (!items.containsKey("$ref")) {
                    validateChildrenAlignment(field.getChildren(), items, filePath, schemaPath + "/items", result);
                }
            }
            return;
        }

        // For object types, check properties
        if (field.isObject() && "object".equals(fieldSchema.get("type"))) {
            validateChildrenAlignment(field.getChildren(), fieldSchema, filePath, schemaPath, result);
        }
    }

    /**
     * Validates alignment of child fields with schema properties.
     *
     * @param children the child field nodes
     * @param schema the schema containing properties
     * @param filePath the file path for reporting
     * @param schemaPath the JSON pointer path
     * @param result the result to add issues to
     */
    private void validateChildrenAlignment(List<FieldNode> children, Map<String, Object> schema,
                                            String filePath, String schemaPath,
                                            OpenApiValidationResult result) {
        Object propertiesObj = schema.get("properties");
        if (propertiesObj == null || !(propertiesObj instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) propertiesObj;

        for (FieldNode child : children) {
            if (child.isTransitory()) {
                continue;
            }

            String childName = child.getCamelCaseName();
            String childPath = schemaPath + "/properties/" + childName;

            if (!properties.containsKey(childName)) {
                result.addError(filePath, childPath, RULE_STRUCTURAL_MISMATCH,
                        "Child field '" + childName + "' from JSON tree not found in schema");
            }
        }
    }

    /**
     * Validates schemas with a FieldGroup representing the JSON tree.
     *
     * @param fieldGroup the field group from spec-tree.json
     * @param schemaName the schema name
     * @param schemaPath the path to the schema file
     * @return the validation result
     */
    public OpenApiValidationResult validateWithJsonTree(FieldGroup fieldGroup,
                                                         String schemaName,
                                                         Path schemaPath) {
        OpenApiValidationResult result = new OpenApiValidationResult();
        String filePathStr = schemaPath.toString();

        Map<String, Object> schema = parseYamlFile(schemaPath, filePathStr, result);
        if (schema == null) {
            return result;
        }

        // Validate schema structure first
        validateSchemaDefinition(schema, filePathStr, "#", result, new LinkedHashSet<>());

        // Validate structural alignment
        result.merge(validateStructuralAlignment(fieldGroup, schemaName, schema, filePathStr));

        return result;
    }
}
