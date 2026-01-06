package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;

import java.util.*;

/**
 * OpenAPI Schema type mapper.
 * Maps FieldNode to OpenAPI 3.x Schema definitions.
 */
public class OpenApiTypeMapper {

    private final Config config;

    public OpenApiTypeMapper(Config config) {
        this.config = config;
    }

    /**
     * Maps FieldNode to OpenAPI Schema Map.
     * The returned Map can be directly serialized to YAML.
     *
     * @param field field node
     * @return Schema definition (Map structure)
     */
    public Map<String, Object> mapToSchema(FieldNode field) {
        Map<String, Object> schema = new LinkedHashMap<>();

        // Array type
        if (field.isArray()) {
            schema.put("type", "array");
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("$ref", "./" + field.getClassName() + ".yaml");
            schema.put("items", items);

            // Array maxItems (based on occurrenceCount parsing, e.g. 0..9)
            Integer maxItems = parseMaxItems(field.getOccurrenceCount());
            if (maxItems != null) {
                schema.put("maxItems", maxItems);
            }
            return schema;
        }

        // Object type (use $ref reference)
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
     * Maps primitive type to OpenAPI type and format.
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
     * Filters transitory fields (groupId, occurrenceCount).
     * These fields should not appear in OpenAPI Schema.
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
     * Collects required field list.
     * Based on optionality == "M".
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
     * Generates complete Object Schema (including properties and required).
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
     * Parses occurrenceCount to get maxItems.
     * Format examples: "0..9", "1..N", "0..N"
     *
     * @return maxItems value, returns null if N
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
                    return null; // Unlimited
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
     * Generates $ref path.
     * Used for nested object and array item references.
     *
     * @param className class name
     * @param schemaDir Schema directory (e.g. "request", "response", "common")
     * @return relative $ref path
     */
    public String generateRefPath(String className, String schemaDir) {
        return "./" + className + ".yaml";
    }

    /**
     * Determines if field should be included in OpenAPI Schema.
     */
    public boolean shouldIncludeInSchema(FieldNode field) {
        return !field.isTransitory();
    }
}
