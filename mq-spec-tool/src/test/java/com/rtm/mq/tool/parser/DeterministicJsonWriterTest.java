package com.rtm.mq.tool.parser;

import com.google.gson.JsonParser;
import com.rtm.mq.tool.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeterministicJsonWriter.
 */
class DeterministicJsonWriterTest {

    private DeterministicJsonWriter writer;
    private MessageModel testModel;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        writer = new DeterministicJsonWriter();
        testModel = createTestMessageModel();
    }

    @Test
    void testSerialize_producesValidJson() {
        String json = writer.serialize(testModel);

        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertDoesNotThrow(() -> JsonParser.parseString(json));
    }

    @Test
    void testSerialize_containsMetadata() {
        String json = writer.serialize(testModel);

        assertTrue(json.contains("\"sourceFile\""));
        assertTrue(json.contains("\"parseTimestamp\""));
        assertTrue(json.contains("\"parserVersion\""));
        assertTrue(json.contains("\"operationId\""));
        assertTrue(json.contains("test-spec.xlsx"));
        assertTrue(json.contains("CreateAppSMP"));
    }

    @Test
    void testSerialize_containsAllSections() {
        String json = writer.serialize(testModel);

        assertTrue(json.contains("\"metadata\""));
        assertTrue(json.contains("\"sharedHeader\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"response\""));
    }

    @Test
    void testSerialize_preservesFieldOrder() {
        String json = writer.serialize(testModel);

        // Check that metadata fields appear in order
        int sourceFilePos = json.indexOf("\"sourceFile\"");
        int timestampPos = json.indexOf("\"parseTimestamp\"");
        int versionPos = json.indexOf("\"parserVersion\"");
        int opIdPos = json.indexOf("\"operationId\"");

        assertTrue(sourceFilePos < timestampPos);
        assertTrue(timestampPos < versionPos);
        assertTrue(versionPos < opIdPos);
    }

    @Test
    void testSerialize_includesNullFields() {
        String json = writer.serialize(testModel);

        // Verify that null fields are explicitly serialized
        assertTrue(json.contains("\"sharedHeaderFile\": null"));
        assertTrue(json.contains("\"className\": null"));
    }

    @Test
    void testSerialize_fieldNodeStructure() {
        // Create a simple model with one field
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setSourceFile("test.xlsx");
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setParserVersion("1.0.0");
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        FieldNode field = FieldNode.builder()
            .originalName("TestField")
            .camelCaseName("testField")
            .segLevel(1)
            .length(10)
            .dataType("A/N")
            .optionality("M")
            .isArray(false)
            .isObject(false)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 5))
            .build();
        request.addField(field);
        model.setRequest(request);

        model.setSharedHeader(new FieldGroup());
        model.setResponse(new FieldGroup());

        String json = writer.serialize(model);

        // Verify field node structure
        assertTrue(json.contains("\"originalName\": \"TestField\""));
        assertTrue(json.contains("\"camelCaseName\": \"testField\""));
        assertTrue(json.contains("\"segLevel\": 1"));
        assertTrue(json.contains("\"length\": 10"));
        assertTrue(json.contains("\"dataType\": \"A/N\""));
        assertTrue(json.contains("\"optionality\": \"M\""));
        assertTrue(json.contains("\"isArray\": false"));
        assertTrue(json.contains("\"isObject\": false"));
        assertTrue(json.contains("\"isTransitory\": false"));
        assertTrue(json.contains("\"_source\""));
        assertTrue(json.contains("\"sheetName\": \"Request\""));
        assertTrue(json.contains("\"rowIndex\": 5"));
    }

    @Test
    void testSerialize_nestedChildren() {
        // Create model with nested structure
        MessageModel model = createModelWithNestedChildren();
        String json = writer.serialize(model);

        assertTrue(json.contains("\"children\""));
        assertTrue(json.contains("ParentObject"));
        assertTrue(json.contains("ChildField1"));
        assertTrue(json.contains("ChildField2"));
    }

    @Test
    void testSerialize_deterministicOutput() {
        // Serialize the same model multiple times
        String json1 = writer.serialize(testModel);
        String json2 = writer.serialize(testModel);
        String json3 = writer.serialize(testModel);

        // All outputs should be byte-for-byte identical
        assertEquals(json1, json2);
        assertEquals(json2, json3);
    }

    @Test
    void testWrite_createsFile() throws IOException {
        Path outputPath = tempDir.resolve("output/spec-tree.json");

        writer.write(testModel, outputPath);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testWrite_createsParentDirectories() throws IOException {
        Path outputPath = tempDir.resolve("deeply/nested/path/spec-tree.json");

        writer.write(testModel, outputPath);

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.isDirectory(outputPath.getParent()));
    }

    @Test
    void testWrite_utf8Encoding() throws IOException {
        // Create model with Unicode characters
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setSourceFile("测试文件.xlsx");
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setParserVersion("1.0.0");
        metadata.setOperationId("TestOp");
        metadata.setOperationName("创建应用");
        model.setMetadata(metadata);
        model.setSharedHeader(new FieldGroup());
        model.setRequest(new FieldGroup());
        model.setResponse(new FieldGroup());

        Path outputPath = tempDir.resolve("unicode-test.json");
        writer.write(model, outputPath);

        String content = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("测试文件.xlsx"));
        assertTrue(content.contains("创建应用"));
    }

    @Test
    void testWrite_prettyPrinting() throws IOException {
        Path outputPath = tempDir.resolve("pretty-test.json");

        writer.write(testModel, outputPath);

        String content = Files.readString(outputPath, StandardCharsets.UTF_8);

        // Verify pretty printing with indentation
        assertTrue(content.contains("\n"));
        assertTrue(content.contains("  ")); // 2-space indentation
    }

    @Test
    void testWrite_byteLevelIdenticalOutput() throws IOException {
        Path outputPath1 = tempDir.resolve("output1.json");
        Path outputPath2 = tempDir.resolve("output2.json");

        writer.write(testModel, outputPath1);
        writer.write(testModel, outputPath2);

        byte[] bytes1 = Files.readAllBytes(outputPath1);
        byte[] bytes2 = Files.readAllBytes(outputPath2);

        assertArrayEquals(bytes1, bytes2);
    }

    @Test
    void testGetSchemaPath() {
        String schemaPath = DeterministicJsonWriter.getSchemaPath();
        assertEquals("/schema/spec-tree-schema.json", schemaPath);
    }

    @Test
    void testSerialize_arrayField() {
        MessageModel model = createModelWithArrayField();
        String json = writer.serialize(model);

        assertTrue(json.contains("\"isArray\": true"));
        assertTrue(json.contains("\"occurrenceCount\": \"0..N\""));
    }

    @Test
    void testSerialize_objectField() {
        MessageModel model = createModelWithObjectField();
        String json = writer.serialize(model);

        assertTrue(json.contains("\"isObject\": true"));
        assertTrue(json.contains("\"className\": \"CreateApplication\""));
    }

    @Test
    void testSerialize_transitoryField() {
        MessageModel model = createModelWithTransitoryField();
        String json = writer.serialize(model);

        assertTrue(json.contains("\"isTransitory\": true"));
    }

    @Test
    void testSerialize_allFieldAttributes() {
        MessageModel model = createModelWithAllAttributes();
        String json = writer.serialize(model);

        assertTrue(json.contains("\"defaultValue\": \"DEFAULT\""));
        assertTrue(json.contains("\"hardCodeValue\": \"HARDCODE\""));
        assertTrue(json.contains("\"groupId\": \"GROUP1\""));
        assertTrue(json.contains("\"occurrenceCount\": \"1..1\""));
    }

    // Helper methods

    private MessageModel createTestMessageModel() {
        MessageModel model = new MessageModel();

        // Create metadata
        Metadata metadata = new Metadata();
        metadata.setSourceFile("test-spec.xlsx");
        metadata.setSharedHeaderFile(null);
        metadata.setParseTimestamp("2026-01-04T10:00:00Z");
        metadata.setParserVersion("1.0.0");
        metadata.setOperationName("Create application from SMP");
        metadata.setOperationId("CreateAppSMP");
        metadata.setVersion("01.00");
        model.setMetadata(metadata);

        // Create empty field groups
        model.setSharedHeader(new FieldGroup());
        model.setRequest(new FieldGroup());
        model.setResponse(new FieldGroup());

        return model;
    }

    private MessageModel createModelWithNestedChildren() {
        MessageModel model = createTestMessageModel();

        FieldGroup request = new FieldGroup();

        // Create parent object
        FieldNode parent = FieldNode.builder()
            .originalName("ParentObject")
            .camelCaseName("parentObject")
            .className("ParentObject")
            .segLevel(1)
            .isArray(false)
            .isObject(true)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 1))
            .build();

        // Create children
        List<FieldNode> children = new ArrayList<>();
        children.add(FieldNode.builder()
            .originalName("ChildField1")
            .camelCaseName("childField1")
            .segLevel(2)
            .dataType("A/N")
            .isArray(false)
            .isObject(false)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 2))
            .build());
        children.add(FieldNode.builder()
            .originalName("ChildField2")
            .camelCaseName("childField2")
            .segLevel(2)
            .dataType("N")
            .isArray(false)
            .isObject(false)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 3))
            .build());

        parent.getChildren().addAll(children);
        request.addField(parent);
        model.setRequest(request);

        return model;
    }

    private MessageModel createModelWithArrayField() {
        MessageModel model = createTestMessageModel();
        FieldGroup request = new FieldGroup();

        FieldNode arrayField = FieldNode.builder()
            .originalName("ItemList")
            .camelCaseName("itemList")
            .className("Item")
            .segLevel(1)
            .isArray(true)
            .isObject(true)
            .isTransitory(false)
            .occurrenceCount("0..N")
            .source(new SourceMetadata("Request", 1))
            .build();

        request.addField(arrayField);
        model.setRequest(request);
        return model;
    }

    private MessageModel createModelWithObjectField() {
        MessageModel model = createTestMessageModel();
        FieldGroup request = new FieldGroup();

        FieldNode objectField = FieldNode.builder()
            .originalName("CreateApp:CreateApplication")
            .camelCaseName("createApp")
            .className("CreateApplication")
            .segLevel(1)
            .isArray(false)
            .isObject(true)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 1))
            .build();

        request.addField(objectField);
        model.setRequest(request);
        return model;
    }

    private MessageModel createModelWithTransitoryField() {
        MessageModel model = createTestMessageModel();
        FieldGroup request = new FieldGroup();

        FieldNode transitoryField = FieldNode.builder()
            .originalName("TempField")
            .camelCaseName("tempField")
            .segLevel(1)
            .isArray(false)
            .isObject(false)
            .isTransitory(true)
            .source(new SourceMetadata("Request", 1))
            .build();

        request.addField(transitoryField);
        model.setRequest(request);
        return model;
    }

    private MessageModel createModelWithAllAttributes() {
        MessageModel model = createTestMessageModel();
        FieldGroup request = new FieldGroup();

        FieldNode field = FieldNode.builder()
            .originalName("CompleteField")
            .camelCaseName("completeField")
            .className(null)
            .segLevel(1)
            .length(50)
            .dataType("A/N")
            .optionality("M")
            .defaultValue("DEFAULT")
            .hardCodeValue("HARDCODE")
            .groupId("GROUP1")
            .occurrenceCount("1..1")
            .isArray(false)
            .isObject(false)
            .isTransitory(false)
            .source(new SourceMetadata("Request", 10))
            .build();

        request.addField(field);
        model.setRequest(request);
        return model;
    }
}
