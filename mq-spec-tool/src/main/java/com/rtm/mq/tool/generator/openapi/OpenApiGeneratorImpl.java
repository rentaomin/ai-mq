package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI main file generator implementation.
 *
 * <p>Generates OpenAPI 3.x specification YAML file (api.yaml) from MessageModel.
 * The generated file includes:</p>
 * <ul>
 *   <li>OpenAPI version declaration (3.0.3)</li>
 *   <li>Info metadata (title, version, description)</li>
 *   <li>Servers section</li>
 *   <li>Paths with POST endpoint definitions</li>
 *   <li>Components/schemas with Request and Response definitions</li>
 * </ul>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>Field order is preserved exactly as in the JSON Tree/Excel spec</li>
 *   <li>Transitory fields (groupId, occurrenceCount) are excluded from schemas</li>
 *   <li>Required list is generated based on optionality="M"</li>
 *   <li>Uses LinkedHashMap for deterministic output ordering</li>
 * </ul>
 *
 * @see OpenApiTypeMapper
 */
public class OpenApiGeneratorImpl implements OpenApiGenerator {

    private static final String OUTPUT_FILENAME = "api.yaml";
    private static final String OPENAPI_VERSION = "3.0.3";
    private static final String GENERATOR_TYPE = "openapi";

    private final Config config;
    private final OpenApiTypeMapper typeMapper;
    private String generatedContent;
    private final List<String> generatedSchemaNames = new ArrayList<>();
    private MessageModel currentModel;

    /**
     * Creates a new OpenApiGeneratorImpl with the specified configuration.
     *
     * @param config the configuration containing OpenAPI settings
     */
    public OpenApiGeneratorImpl(Config config) {
        this.config = config;
        this.typeMapper = new OpenApiTypeMapper(config);
    }

    @Override
    public Map<String, String> generate(MessageModel model, Path outputDir) {
        this.currentModel = model;
        this.generatedSchemaNames.clear();

        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new GenerationException("Operation ID is required for OpenAPI generation")
                    .withGenerator(GENERATOR_TYPE);
        }

        // Build OpenAPI document structure
        Map<String, Object> openApiDoc = new LinkedHashMap<>();

        // 1. openapi version
        openApiDoc.put("openapi", OPENAPI_VERSION);

        // 2. info metadata
        openApiDoc.put("info", buildInfo(model, operationId));

        // 3. servers (optional)
        openApiDoc.put("servers", buildServers());

        // 4. paths
        openApiDoc.put("paths", buildPaths(model, operationId));

        // 5. components/schemas
        openApiDoc.put("components", buildComponents(model, operationId));

        // Convert to YAML
        this.generatedContent = toYaml(openApiDoc);

        // Return map of filename to content
        Map<String, String> result = new LinkedHashMap<>();
        result.put("openapi/" + OUTPUT_FILENAME, generatedContent);
        return result;
    }

    @Override
    public String getType() {
        return GENERATOR_TYPE;
    }

    @Override
    public String generateMainApi() {
        if (generatedContent == null && currentModel != null) {
            generate(currentModel, null);
        }
        return generatedContent;
    }

    @Override
    public String generateSchema(String schemaName) {
        if (schemaName == null || schemaName.isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        if (currentModel == null) {
            throw new GenerationException("No model available for schema generation")
                    .withGenerator(GENERATOR_TYPE)
                    .withArtifact(schemaName);
        }

        List<FieldNode> fields = findFieldsForSchema(schemaName);
        if (fields == null || fields.isEmpty()) {
            throw new GenerationException("Schema not found: " + schemaName)
                    .withGenerator(GENERATOR_TYPE)
                    .withArtifact(schemaName);
        }

        Map<String, Object> schemaDoc = typeMapper.generateObjectSchema(fields);
        return toYaml(schemaDoc);
    }

    /**
     * Finds the fields for a given schema name.
     */
    private List<FieldNode> findFieldsForSchema(String schemaName) {
        if (currentModel == null) {
            return null;
        }

        String operationId = currentModel.getMetadata().getOperationId();
        if (operationId == null) {
            return null;
        }

        // Check Request schema
        if (schemaName.equals(operationId + "Request")) {
            if (currentModel.getRequest() != null) {
                return currentModel.getRequest().getFields();
            }
        }

        // Check Response schema
        if (schemaName.equals(operationId + "Response")) {
            if (currentModel.getResponse() != null) {
                return currentModel.getResponse().getFields();
            }
        }

        // Search nested schemas
        List<FieldNode> result = searchNestedSchema(
                currentModel.getRequest() != null ? currentModel.getRequest().getFields() : null,
                schemaName);
        if (result != null) {
            return result;
        }

        return searchNestedSchema(
                currentModel.getResponse() != null ? currentModel.getResponse().getFields() : null,
                schemaName);
    }

    /**
     * Recursively searches for nested schema fields.
     */
    private List<FieldNode> searchNestedSchema(List<FieldNode> fields, String schemaName) {
        if (fields == null) {
            return null;
        }

        for (FieldNode field : fields) {
            if (field.isTransitory()) {
                continue;
            }

            if ((field.isObject() || field.isArray()) && schemaName.equals(field.getClassName())) {
                return field.getChildren();
            }

            List<FieldNode> nested = searchNestedSchema(field.getChildren(), schemaName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /**
     * Builds the info section of the OpenAPI document.
     */
    private Map<String, Object> buildInfo(MessageModel model, String operationId) {
        Map<String, Object> info = new LinkedHashMap<>();

        String title = config.getOpenapi() != null ? config.getOpenapi().getTitle() : null;
        if (title == null || title.isEmpty()) {
            title = operationId + " API";
        }
        info.put("title", title);

        String version = config.getOpenapi() != null ? config.getOpenapi().getApiVersion() : null;
        if (version == null || version.isEmpty()) {
            version = "1.0.0";
        }
        info.put("version", version);

        String description = config.getOpenapi() != null ? config.getOpenapi().getDescription() : null;
        if (description == null || description.isEmpty()) {
            description = "Generated from MQ message specification";
        }
        info.put("description", description);

        return info;
    }

    /**
     * Builds the servers section of the OpenAPI document.
     */
    private List<Map<String, Object>> buildServers() {
        List<Map<String, Object>> servers = new ArrayList<>();

        String serverUrl = config.getOpenapi() != null ? config.getOpenapi().getServerUrl() : null;
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = "http://localhost:8080";
        }

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", serverUrl);
        server.put("description", "Default server");
        servers.add(server);

        return servers;
    }

    /**
     * Builds the paths section of the OpenAPI document.
     */
    private Map<String, Object> buildPaths(MessageModel model, String operationId) {
        Map<String, Object> paths = new LinkedHashMap<>();

        String pathName = "/" + camelToKebab(operationId);
        Map<String, Object> pathItem = new LinkedHashMap<>();

        // POST operation
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("operationId", operationId);
        post.put("summary", operationId + " operation");
        post.put("description", "MQ message operation: " + operationId);
        post.put("tags", Collections.singletonList(operationId));

        // requestBody
        if (model.getRequest() != null && !model.getRequest().getFields().isEmpty()) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("required", true);
            requestBody.put("content", buildContent(operationId + "Request"));
            post.put("requestBody", requestBody);
        }

        // responses
        Map<String, Object> responses = new LinkedHashMap<>();

        // 200 response
        Map<String, Object> response200 = new LinkedHashMap<>();
        response200.put("description", "Successful response");
        if (model.getResponse() != null && !model.getResponse().getFields().isEmpty()) {
            response200.put("content", buildContent(operationId + "Response"));
        }
        responses.put("200", response200);

        // default error response
        Map<String, Object> responseDefault = new LinkedHashMap<>();
        responseDefault.put("description", "Error response");
        responses.put("default", responseDefault);

        post.put("responses", responses);
        pathItem.put("post", post);
        paths.put(pathName, pathItem);

        return paths;
    }

    /**
     * Builds the content section (application/json) with schema reference.
     */
    private Map<String, Object> buildContent(String schemaName) {
        Map<String, Object> content = new LinkedHashMap<>();
        Map<String, Object> jsonContent = new LinkedHashMap<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$ref", "#/components/schemas/" + schemaName);
        jsonContent.put("schema", schema);
        content.put("application/json", jsonContent);
        return content;
    }

    /**
     * Builds the components section of the OpenAPI document.
     */
    private Map<String, Object> buildComponents(MessageModel model, String operationId) {
        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();

        // Request Schema
        if (model.getRequest() != null && !model.getRequest().getFields().isEmpty()) {
            String requestSchemaName = operationId + "Request";
            schemas.put(requestSchemaName,
                    typeMapper.generateObjectSchema(model.getRequest().getFields()));
            generatedSchemaNames.add(requestSchemaName);

            // Collect nested schemas
            collectNestedSchemas(model.getRequest().getFields(), schemas);
        }

        // Response Schema
        if (model.getResponse() != null && !model.getResponse().getFields().isEmpty()) {
            String responseSchemaName = operationId + "Response";
            schemas.put(responseSchemaName,
                    typeMapper.generateObjectSchema(model.getResponse().getFields()));
            generatedSchemaNames.add(responseSchemaName);

            // Collect nested schemas
            collectNestedSchemas(model.getResponse().getFields(), schemas);
        }

        components.put("schemas", schemas);
        return components;
    }

    /**
     * Recursively collects nested schemas from field nodes.
     */
    private void collectNestedSchemas(List<FieldNode> fields, Map<String, Object> schemas) {
        for (FieldNode field : fields) {
            if (field.isTransitory()) {
                continue;
            }

            if (field.isObject() || field.isArray()) {
                String className = field.getClassName();
                if (className != null && !className.isEmpty() && !schemas.containsKey(className)) {
                    // Generate nested schema
                    if (!field.getChildren().isEmpty()) {
                        schemas.put(className,
                                typeMapper.generateObjectSchema(field.getChildren()));
                        generatedSchemaNames.add(className);

                        // Recursively collect child nested schemas
                        collectNestedSchemas(field.getChildren(), schemas);
                    }
                }
            }
        }
    }

    /**
     * Converts the OpenAPI document map to YAML string format.
     * Uses deterministic ordering and formatting.
     */
    private String toYaml(Map<String, Object> data) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Generated by MQ Tool\n");
        yaml.append("# DO NOT EDIT - This file is auto-generated\n\n");
        writeYamlMap(yaml, data, 0);
        return yaml.toString();
    }

    /**
     * Recursively writes a Map to YAML format.
     */
    private void writeYamlMap(StringBuilder yaml, Map<String, Object> map, int indent) {
        String indentStr = "  ".repeat(indent);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            yaml.append(indentStr).append(key).append(":");

            if (value instanceof Map) {
                yaml.append("\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                writeYamlMap(yaml, mapValue, indent + 1);
            } else if (value instanceof List) {
                yaml.append("\n");
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                writeYamlList(yaml, listValue, indent + 1);
            } else {
                yaml.append(" ").append(formatYamlValue(value)).append("\n");
            }
        }
    }

    /**
     * Recursively writes a List to YAML format.
     */
    private void writeYamlList(StringBuilder yaml, List<Object> list, int indent) {
        String indentStr = "  ".repeat(indent);

        for (Object item : list) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                // Write first key-value on same line as dash
                boolean first = true;
                for (Map.Entry<String, Object> entry : mapItem.entrySet()) {
                    if (first) {
                        yaml.append(indentStr).append("- ");
                        first = false;
                        Object entryValue = entry.getValue();
                        if (entryValue instanceof Map || entryValue instanceof List) {
                            yaml.append(entry.getKey()).append(":\n");
                            if (entryValue instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> subMap = (Map<String, Object>) entryValue;
                                writeYamlMap(yaml, subMap, indent + 2);
                            } else {
                                @SuppressWarnings("unchecked")
                                List<Object> subList = (List<Object>) entryValue;
                                writeYamlList(yaml, subList, indent + 2);
                            }
                        } else {
                            yaml.append(entry.getKey()).append(": ")
                                    .append(formatYamlValue(entryValue)).append("\n");
                        }
                    } else {
                        yaml.append(indentStr).append("  ");
                        Object entryValue = entry.getValue();
                        if (entryValue instanceof Map || entryValue instanceof List) {
                            yaml.append(entry.getKey()).append(":\n");
                            if (entryValue instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> subMap = (Map<String, Object>) entryValue;
                                writeYamlMap(yaml, subMap, indent + 2);
                            } else {
                                @SuppressWarnings("unchecked")
                                List<Object> subList = (List<Object>) entryValue;
                                writeYamlList(yaml, subList, indent + 2);
                            }
                        } else {
                            yaml.append(entry.getKey()).append(": ")
                                    .append(formatYamlValue(entryValue)).append("\n");
                        }
                    }
                }
            } else {
                yaml.append(indentStr).append("- ")
                        .append(formatYamlValue(item)).append("\n");
            }
        }
    }

    /**
     * Formats a value for YAML output.
     * Handles quoting for special characters.
     */
    private String formatYamlValue(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            // Cases that need quoting
            if (str.contains(":") || str.contains("#") || str.contains("'")
                    || str.contains("\"") || str.contains("\n")
                    || str.startsWith(" ") || str.endsWith(" ")
                    || str.isEmpty()) {
                return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
            return str;
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    /**
     * Converts camelCase to kebab-case.
     * Example: CreateApplication -> create-application
     */
    private String camelToKebab(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    /**
     * Gets the generated YAML content.
     *
     * @return the generated YAML content, or null if not yet generated
     */
    public String getGeneratedContent() {
        return generatedContent;
    }

    /**
     * Gets the list of generated schema names.
     *
     * @return a copy of the generated schema names list
     */
    public List<String> getGeneratedSchemaNames() {
        return new ArrayList<>(generatedSchemaNames);
    }

    /**
     * Gets the output file path for the generated api.yaml.
     *
     * @return the output file path
     */
    public Path getOutputPath() {
        String rootDir = config.getOutput() != null ? config.getOutput().getRootDir() : "./output";
        return Path.of(rootDir, "openapi", OUTPUT_FILENAME);
    }

    /**
     * Writes the generated content to the output file.
     *
     * @throws GenerationException if file writing fails
     */
    public void writeToFile() {
        if (generatedContent == null) {
            throw new GenerationException("No content generated to write")
                    .withGenerator(GENERATOR_TYPE);
        }

        Path outputPath = getOutputPath();
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, generatedContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenerationException("Failed to write OpenAPI file: " + e.getMessage(), e)
                    .withGenerator(GENERATOR_TYPE)
                    .withArtifact(OUTPUT_FILENAME);
        }
    }
}
