package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI Schema type mapper.
 *
 * <p>Maps {@link FieldNode} instances to OpenAPI 3.x Schema definitions.
 * This class is responsible for:</p>
 * <ul>
 *   <li>Mapping Excel dataType to OpenAPI type and format</li>
 *   <li>Generating array schemas with items.$ref</li>
 *   <li>Generating object references with $ref</li>
 *   <li>Collecting required fields based on optionality</li>
 *   <li>Generating maxLength constraints</li>
 *   <li>Generating default values</li>
 *   <li>Filtering transitory fields (groupId, occurrenceCount)</li>
 * </ul>
 *
 * <p>Uses {@link LinkedHashMap} to preserve field order consistent with
 * the intermediate JSON Tree.</p>
 *
 * @see FieldNode
 */
public class OpenApiTypeMapper {

    private final Config config;

    /**
     * Constructs a new OpenApiTypeMapper with the specified configuration.
     *
     * @param config the configuration; may be null for default behavior
     */
    public OpenApiTypeMapper(Config config) {
        this.config = config;
    }

    /**
     * Maps a FieldNode to an OpenAPI Schema definition.
     *
     * <p>The returned Map can be directly serialized to YAML. The structure
     * varies based on the field type:</p>
     * <ul>
     *   <li>Array: type: array, items: {$ref: ...}, optionally maxItems</li>
     *   <li>Object: $ref: ...</li>
     *   <li>Primitive: type: string, optionally format, maxLength, default</li>
     * </ul>
     *
     * @param field the field node to map
     * @return the Schema definition as a Map; never null
     */
    public Map<String, Object> mapToSchema(FieldNode field) {
        Map<String, Object> schema = new LinkedHashMap<>();

        // Array type
        if (field.isArray()) {
            schema.put("type", "array");
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("$ref", "./" + field.getClassName() + ".yaml");
            schema.put("items", items);

            // maxItems from occurrenceCount (e.g., "0..9")
            Integer maxItems = parseMaxItems(field.getOccurrenceCount());
            if (maxItems != null) {
                schema.put("maxItems", maxItems);
            }
            return schema;
        }

        // Object type (use $ref)
        if (field.isObject()) {
            schema.put("$ref", "./" + field.getClassName() + ".yaml");
            return schema;
        }

        // Primitive type mapping
        String dataType = field.getDataType();
        mapPrimitiveType(dataType, schema);

        // maxLength
        if (field.getLength() != null && field.getLength() > 0) {
            schema.put("maxLength", field.getLength());
        }

        // default
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            schema.put("default", field.getDefaultValue());
        }

        return schema;
    }

    /**
     * Maps a primitive data type to OpenAPI type and format.
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li>String, AN, A, N, Number, Unsigned Integer: type: string</li>
     *   <li>Amount, Currency: type: string, format: decimal</li>
     *   <li>Date: type: string, format: date</li>
     *   <li>Unknown/null: type: string (fallback)</li>
     * </ul>
     *
     * @param dataType the data type from the specification
     * @param schema the schema map to populate
     */
    private void mapPrimitiveType(String dataType, Map<String, Object> schema) {
        if (dataType == null || dataType.isEmpty()) {
            schema.put("type", "string");
            return;
        }

        switch (dataType.toLowerCase()) {
            case "string":
            case "an":
            case "a":
            case "a/n":
            case "number":
            case "n":
            case "unsigned integer":
                schema.put("type", "string");
                break;

            case "amount":
            case "currency":
                schema.put("type", "string");
                schema.put("format", "decimal");
                break;

            case "date":
                schema.put("type", "string");
                schema.put("format", "date");
                break;

            default:
                schema.put("type", "string");
                break;
        }
    }

    /**
     * Filters transitory fields from the list.
     *
     * <p>Transitory fields (groupId, occurrenceCount) should not appear
     * in the OpenAPI Schema output.</p>
     *
     * @param fields the list of fields to filter
     * @return a new list containing only non-transitory fields
     */
    public List<FieldNode> filterTransitoryFields(List<FieldNode> fields) {
        List<FieldNode> filtered = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory()) {
                filtered.add(field);
            }
        }
        return filtered;
    }

    /**
     * Collects required field names from the list.
     *
     * <p>A field is required if its optionality is "M" (mandatory).
     * Transitory fields are excluded.</p>
     *
     * @param fields the list of fields to inspect
     * @return a list of camelCase names for required fields
     */
    public List<String> collectRequiredFields(List<FieldNode> fields) {
        List<String> required = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory() && "M".equals(field.getOptionality())) {
                required.add(field.getCamelCaseName());
            }
        }
        return required;
    }

    /**
     * Generates a complete Object Schema including properties and required list.
     *
     * <p>The generated schema follows this structure:</p>
     * <pre>
     * type: object
     * required:
     *   - fieldA
     *   - fieldB
     * properties:
     *   fieldA:
     *     type: string
     *   fieldB:
     *     $ref: './FieldB.yaml'
     * </pre>
     *
     * @param fields the list of child fields for this object
     * @return the complete Object Schema as a Map
     */
    public Map<String, Object> generateObjectSchema(List<FieldNode> fields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        // Filter transitory fields
        List<FieldNode> filteredFields = filterTransitoryFields(fields);

        // required list
        List<String> required = collectRequiredFields(filteredFields);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        // properties
        Map<String, Object> properties = new LinkedHashMap<>();
        for (FieldNode field : filteredFields) {
            properties.put(field.getCamelCaseName(), mapToSchema(field));
        }
        schema.put("properties", properties);

        return schema;
    }

    /**
     * Parses occurrenceCount to extract maxItems value.
     *
     * <p>Supports formats like:</p>
     * <ul>
     *   <li>"0..9" returns 9</li>
     *   <li>"1..5" returns 5</li>
     *   <li>"0..N" returns null (unbounded)</li>
     *   <li>"1..N" returns null (unbounded)</li>
     *   <li>"0..*" returns null (unbounded)</li>
     * </ul>
     *
     * @param occurrenceCount the occurrence count string
     * @return the maxItems value, or null if unbounded
     */
    private Integer parseMaxItems(String occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount.isEmpty()) {
            return null;
        }

        // Parse "0..9" format
        if (occurrenceCount.contains("..")) {
            String[] parts = occurrenceCount.split("\\.\\.");
            if (parts.length == 2) {
                String max = parts[1].trim();
                if ("N".equalsIgnoreCase(max) || "*".equals(max)) {
                    return null; // Unbounded
                }
                try {
                    return Integer.parseInt(max);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Generates a $ref path for nested objects and array items.
     *
     * @param className the class name
     * @param schemaDir the schema directory (e.g., "request", "response", "common")
     * @return the relative $ref path
     */
    public String generateRefPath(String className, String schemaDir) {
        return "./" + className + ".yaml";
    }

    /**
     * Determines if a field should be included in the OpenAPI Schema.
     *
     * @param field the field to check
     * @return true if the field should be included; false for transitory fields
     */
    public boolean shouldIncludeInSchema(FieldNode field) {
        return !field.isTransitory();
    }
}
