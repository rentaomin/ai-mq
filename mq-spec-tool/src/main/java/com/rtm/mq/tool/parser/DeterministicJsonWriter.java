package com.rtm.mq.tool.parser;

import com.google.gson.*;
import com.rtm.mq.tool.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Deterministic JSON writer for serializing MessageModel to intermediate JSON tree.
 *
 * <p>This writer guarantees byte-level identical output for the same input,
 * ensuring reproducibility and facilitating version control and change detection.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Fixed field order - fields always appear in the same order</li>
 *   <li>UTF-8 encoding - consistent character encoding</li>
 *   <li>Pretty printing - 2-space indentation for readability</li>
 *   <li>Null serialization - null fields are explicitly included</li>
 *   <li>No HTML escaping - preserves Unicode characters</li>
 * </ul>
 */
public class DeterministicJsonWriter {

    private final Gson gson;

    /**
     * Constructs a new DeterministicJsonWriter with deterministic serialization settings.
     */
    public DeterministicJsonWriter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .registerTypeAdapter(FieldNode.class, new FieldNodeSerializer())
            .create();
    }

    /**
     * Writes a MessageModel to a JSON file.
     *
     * <p>The output file will be written in UTF-8 encoding with pretty-printed formatting.
     * Parent directories will be created automatically if they don't exist.</p>
     *
     * @param model the message model to serialize
     * @param outputPath the output file path
     * @throws IOException if an I/O error occurs during writing
     */
    public void write(MessageModel model, Path outputPath) throws IOException {
        String json = serialize(model);

        // Ensure parent directory exists
        Files.createDirectories(outputPath.getParent());

        // Write using UTF-8 encoding
        Files.writeString(outputPath, json, StandardCharsets.UTF_8);
    }

    /**
     * Serializes a MessageModel to a JSON string.
     *
     * <p>The returned string is deterministic - the same input will always
     * produce the exact same output string.</p>
     *
     * @param model the message model to serialize
     * @return the JSON string representation
     */
    public String serialize(MessageModel model) {
        return gson.toJson(model);
    }

    /**
     * Gets the resource path to the JSON Schema definition.
     *
     * @return the schema resource path
     */
    public static String getSchemaPath() {
        return "/schema/spec-tree-schema.json";
    }

    /**
     * Custom serializer for FieldNode to ensure deterministic field ordering.
     *
     * <p>Fields are serialized in a fixed order to guarantee consistent output
     * regardless of Java object field ordering or reflection ordering.</p>
     */
    private static class FieldNodeSerializer implements JsonSerializer<FieldNode> {
        @Override
        public JsonElement serialize(FieldNode node, java.lang.reflect.Type typeOfSrc,
                                    JsonSerializationContext context) {
            JsonObject obj = new JsonObject();

            // Add fields in fixed order
            obj.addProperty("originalName", node.getOriginalName());
            obj.addProperty("camelCaseName", node.getCamelCaseName());
            obj.addProperty("className", node.getClassName());
            obj.addProperty("segLevel", node.getSegLevel());
            obj.add("length", node.getLength() != null ?
                new JsonPrimitive(node.getLength()) : JsonNull.INSTANCE);
            obj.addProperty("dataType", node.getDataType());
            obj.addProperty("optionality", node.getOptionality());
            obj.addProperty("defaultValue", node.getDefaultValue());
            obj.addProperty("hardCodeValue", node.getHardCodeValue());
            obj.addProperty("groupId", node.getGroupId());
            obj.addProperty("occurrenceCount", node.getOccurrenceCount());
            obj.addProperty("isArray", node.isArray());
            obj.addProperty("isObject", node.isObject());
            obj.addProperty("isTransitory", node.isTransitory());

            // Serialize children array
            JsonArray children = new JsonArray();
            for (FieldNode child : node.getChildren()) {
                children.add(serialize(child, typeOfSrc, context));
            }
            obj.add("children", children);

            // Serialize _source object
            JsonObject source = new JsonObject();
            source.addProperty("sheetName", node.getSource().getSheetName());
            source.addProperty("rowIndex", node.getSource().getRowIndex());
            obj.add("_source", source);

            return obj;
        }
    }
}
