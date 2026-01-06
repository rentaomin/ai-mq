package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.OpenApiConfig;
import com.rtm.mq.tool.config.OutputConfig;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiGeneratorImpl.
 */
class OpenApiGeneratorImplTest {

    private Config config;
    private OpenApiGeneratorImpl generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();

        OpenApiConfig openApiConfig = new OpenApiConfig();
        openApiConfig.setTitle("Test API");
        openApiConfig.setApiVersion("1.0.0");
        openApiConfig.setDescription("Test API Description");
        openApiConfig.setServerUrl("http://test-server:8080");
        config.setOpenapi(openApiConfig);

        OutputConfig outputConfig = new OutputConfig();
        outputConfig.setRootDir(tempDir.toString());
        config.setOutput(outputConfig);

        generator = new OpenApiGeneratorImpl(config);
    }

    @Test
    void testGeneratorType() {
        assertEquals("openapi", generator.getType());
    }

    @Test
    void testGenerateWithValidModel() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);

        assertNotNull(result);
        assertTrue(result.containsKey("openapi/api.yaml"));

        String yaml = result.get("openapi/api.yaml");
        assertNotNull(yaml);
        assertTrue(yaml.contains("openapi: 3.0.3"));
        assertTrue(yaml.contains("title: Test API"));
        assertTrue(yaml.contains("version: 1.0.0"));
        assertTrue(yaml.contains("description: Test API Description"));
    }

    @Test
    void testGenerateWithMissingOperationId() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        // operationId is null
        model.setMetadata(metadata);

        assertThrows(GenerationException.class, () -> {
            generator.generate(model, tempDir);
        });
    }

    @Test
    void testGenerateWithBlankOperationId() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("   ");
        model.setMetadata(metadata);

        assertThrows(GenerationException.class, () -> {
            generator.generate(model, tempDir);
        });
    }

    @Test
    void testOpenApiVersionDeclaration() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("openapi: 3.0.3"));
    }

    @Test
    void testInfoSection() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("info:"));
        assertTrue(yaml.contains("title: Test API"));
        assertTrue(yaml.contains("version: 1.0.0"));
        assertTrue(yaml.contains("description: Test API Description"));
    }

    @Test
    void testInfoSectionWithDefaultValues() {
        // Reset config with no openapi settings
        Config emptyConfig = new Config();
        emptyConfig.setDefaults();
        OutputConfig outputConfig = new OutputConfig();
        outputConfig.setRootDir(tempDir.toString());
        emptyConfig.setOutput(outputConfig);

        OpenApiGeneratorImpl genWithDefaults = new OpenApiGeneratorImpl(emptyConfig);
        MessageModel model = createTestModel();

        Map<String, String> result = genWithDefaults.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Should use operation-based defaults
        assertTrue(yaml.contains("title: TestOperation API"));
        assertTrue(yaml.contains("description: Generated from MQ message specification"));
    }

    @Test
    void testServersSection() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("servers:"));
        assertTrue(yaml.contains("url: http://test-server:8080"));
        assertTrue(yaml.contains("description: Default server"));
    }

    @Test
    void testPathsSection() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("paths:"));
        assertTrue(yaml.contains("/test-operation:"));
        assertTrue(yaml.contains("post:"));
        assertTrue(yaml.contains("operationId: TestOperation"));
    }

    @Test
    void testRequestBodyInPath() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("requestBody:"));
        assertTrue(yaml.contains("required: true"));
        assertTrue(yaml.contains("application/json:"));
        assertTrue(yaml.contains("$ref: '#/components/schemas/TestOperationRequest'")
                || yaml.contains("$ref: \"#/components/schemas/TestOperationRequest\""));
    }

    @Test
    void testResponsesSection() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("responses:"));
        assertTrue(yaml.contains("200:"));
        assertTrue(yaml.contains("description: Successful response"));
        assertTrue(yaml.contains("default:"));
        assertTrue(yaml.contains("description: Error response"));
    }

    @Test
    void testComponentsSchemas() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("components:"));
        assertTrue(yaml.contains("schemas:"));
        assertTrue(yaml.contains("TestOperationRequest:"));
        assertTrue(yaml.contains("TestOperationResponse:"));
    }

    @Test
    void testRequiredFieldsList() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("required:"));
        assertTrue(yaml.contains("- mandatoryField"));
    }

    @Test
    void testFieldPropertiesGeneration() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("properties:"));
        assertTrue(yaml.contains("mandatoryField:"));
        assertTrue(yaml.contains("optionalField:"));
        assertTrue(yaml.contains("type: string"));
        assertTrue(yaml.contains("maxLength: 20"));
    }

    @Test
    void testTransitoryFieldsExcluded() {
        MessageModel model = createModelWithTransitoryFields();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Transitory fields should NOT appear
        assertFalse(yaml.contains("groupId:"));
        assertFalse(yaml.contains("occurrenceCount:"));
        // Regular fields should appear
        assertTrue(yaml.contains("regularField:"));
    }

    @Test
    void testArrayTypeGeneration() {
        MessageModel model = createModelWithArrayField();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("type: array"));
        assertTrue(yaml.contains("items:"));
        assertTrue(yaml.contains("maxItems: 9"));
    }

    @Test
    void testNestedObjectReference() {
        MessageModel model = createModelWithNestedObject();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Nested object should be in schemas
        assertTrue(yaml.contains("NestedObject:"));
    }

    @Test
    void testCamelToKebabConversion() {
        MessageModel model = createTestModel();
        model.getMetadata().setOperationId("CreateApplication");

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("/create-application:"));
    }

    @Test
    void testEmptyRequestDoesNotGenerateRequestBody() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);
        model.setRequest(new FieldGroup()); // Empty request

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Empty request should not generate requestBody
        assertFalse(yaml.contains("requestBody:"));
    }

    @Test
    void testEmptyResponseDoesNotGenerateResponseContent() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("input")
                .dataType("string")
                .length(10)
                .build());
        model.setRequest(request);
        model.setResponse(new FieldGroup()); // Empty response

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Response 200 should exist but without content
        assertTrue(yaml.contains("200:"));
        assertTrue(yaml.contains("description: Successful response"));
    }

    @Test
    void testDefaultValueGeneration() {
        MessageModel model = createModelWithDefaultValue();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("default: defaultValue"));
    }

    @Test
    void testFieldOrderPreserved() {
        MessageModel model = createModelWithOrderedFields();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        // Fields should appear in the order they were added
        int firstIdx = yaml.indexOf("firstField:");
        int secondIdx = yaml.indexOf("secondField:");
        int thirdIdx = yaml.indexOf("thirdField:");

        assertTrue(firstIdx < secondIdx);
        assertTrue(secondIdx < thirdIdx);
    }

    @Test
    void testGeneratedSchemaNames() {
        MessageModel model = createTestModel();

        generator.generate(model, tempDir);
        List<String> schemaNames = generator.getGeneratedSchemaNames();

        assertNotNull(schemaNames);
        assertTrue(schemaNames.contains("TestOperationRequest"));
        assertTrue(schemaNames.contains("TestOperationResponse"));
    }

    @Test
    void testGeneratedContent() {
        MessageModel model = createTestModel();

        generator.generate(model, tempDir);
        String content = generator.getGeneratedContent();

        assertNotNull(content);
        assertTrue(content.startsWith("# Generated by MQ Tool"));
    }

    @Test
    void testOutputPath() {
        Path outputPath = generator.getOutputPath();

        assertNotNull(outputPath);
        assertTrue(outputPath.toString().endsWith("api.yaml"));
        assertTrue(outputPath.toString().contains("openapi"));
    }

    @Test
    void testGenerateMainApi() {
        MessageModel model = createTestModel();
        generator.generate(model, tempDir);

        String mainApi = generator.generateMainApi();

        assertNotNull(mainApi);
        assertEquals(generator.getGeneratedContent(), mainApi);
    }

    @Test
    void testGenerateSchemaWithNullName() {
        MessageModel model = createTestModel();
        generator.generate(model, tempDir);

        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateSchema(null);
        });
    }

    @Test
    void testGenerateSchemaWithEmptyName() {
        MessageModel model = createTestModel();
        generator.generate(model, tempDir);

        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateSchema("");
        });
    }

    @Test
    void testGenerateSchemaWithNonExistentName() {
        MessageModel model = createTestModel();
        generator.generate(model, tempDir);

        assertThrows(GenerationException.class, () -> {
            generator.generateSchema("NonExistentSchema");
        });
    }

    @Test
    void testYamlGeneratedComment() {
        MessageModel model = createTestModel();

        Map<String, String> result = generator.generate(model, tempDir);
        String yaml = result.get("openapi/api.yaml");

        assertTrue(yaml.contains("# Generated by MQ Tool"));
        assertTrue(yaml.contains("# DO NOT EDIT"));
    }

    // Helper methods to create test models

    private MessageModel createTestModel() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOperation");
        model.setMetadata(metadata);

        // Request with mandatory and optional fields
        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("mandatoryField")
                .dataType("string")
                .length(20)
                .optionality("M")
                .build());
        request.addField(FieldNode.builder()
                .camelCaseName("optionalField")
                .dataType("string")
                .length(50)
                .optionality("O")
                .build());
        model.setRequest(request);

        // Response
        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("resultCode")
                .dataType("string")
                .length(4)
                .optionality("M")
                .build());
        response.addField(FieldNode.builder()
                .camelCaseName("resultMessage")
                .dataType("string")
                .length(100)
                .optionality("O")
                .build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithTransitoryFields() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("TransitoryTest");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("regularField")
                .dataType("string")
                .length(10)
                .build());
        request.addField(FieldNode.builder()
                .camelCaseName("groupId")
                .dataType("string")
                .isTransitory(true)
                .build());
        request.addField(FieldNode.builder()
                .camelCaseName("occurrenceCount")
                .dataType("string")
                .isTransitory(true)
                .build());
        model.setRequest(request);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithArrayField() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("ArrayTest");
        model.setMetadata(metadata);

        List<FieldNode> arrayChildren = new ArrayList<>();
        arrayChildren.add(FieldNode.builder()
                .camelCaseName("itemId")
                .dataType("string")
                .length(10)
                .build());

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("items")
                .className("ItemArray")
                .isArray(true)
                .occurrenceCount("0..9")
                .children(arrayChildren)
                .build());
        model.setRequest(request);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithNestedObject() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("NestedTest");
        model.setMetadata(metadata);

        List<FieldNode> nestedChildren = new ArrayList<>();
        nestedChildren.add(FieldNode.builder()
                .camelCaseName("nestedField")
                .dataType("string")
                .length(10)
                .build());

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("nested")
                .className("NestedObject")
                .isObject(true)
                .children(nestedChildren)
                .build());
        model.setRequest(request);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithDefaultValue() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("DefaultTest");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("fieldWithDefault")
                .dataType("string")
                .length(20)
                .defaultValue("defaultValue")
                .build());
        model.setRequest(request);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithOrderedFields() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("OrderTest");
        model.setMetadata(metadata);

        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
                .camelCaseName("firstField")
                .dataType("string")
                .length(10)
                .build());
        request.addField(FieldNode.builder()
                .camelCaseName("secondField")
                .dataType("string")
                .length(10)
                .build());
        request.addField(FieldNode.builder()
                .camelCaseName("thirdField")
                .dataType("string")
                .length(10)
                .build());
        model.setRequest(request);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder()
                .camelCaseName("result")
                .dataType("string")
                .length(10)
                .build());
        model.setResponse(response);

        return model;
    }
}
