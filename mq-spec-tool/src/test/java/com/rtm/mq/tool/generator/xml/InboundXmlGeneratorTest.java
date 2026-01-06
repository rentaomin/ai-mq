package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.OutputConfig;
import com.rtm.mq.tool.config.XmlConfig;
import com.rtm.mq.tool.exception.GenerationException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InboundXmlGenerator.
 *
 * Tests map to Acceptance Criteria from T-202:
 * AC1-15 as specified in the task spec.
 */
class InboundXmlGeneratorTest {

    private InboundXmlGenerator generator;
    private Config config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setDefaults();

        // Configure output directory
        OutputConfig outputConfig = new OutputConfig();
        outputConfig.setRootDir(tempDir.toString());
        config.setOutput(outputConfig);

        // Set up XML configuration
        XmlConfig.NamespaceConfig namespaceConfig = new XmlConfig.NamespaceConfig();
        namespaceConfig.setOutbound("http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0");
        namespaceConfig.setInbound("http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0");
        config.getXml().setNamespace(namespaceConfig);

        XmlConfig.ProjectConfig projectConfig = new XmlConfig.ProjectConfig();
        projectConfig.setGroupId("com.rtm");
        projectConfig.setArtifactId("test");
        config.getXml().setProject(projectConfig);

        generator = new InboundXmlGenerator(config);
    }

    // AC1: Generated XML file conforms to Spring beans XML format
    @Test
    void testGeneratedXmlConformsToSpringBeansFormat() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateInbound();

        // Verify XML declaration (AC12)
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have correct XML declaration");
    }

    // AC2: Root element is <beans:beans> with correct namespace declarations
    @Test
    void testRootElementHasCorrectNamespaces() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("<beans:beans"), "Should have beans:beans root element");
        assertTrue(xml.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""),
            "Should have xsi namespace");
        assertTrue(xml.contains("xmlns=\"http://www.hsbc.com/schema/esf-hosadapter2/fixlength-2.0.0\""),
            "Should have fix-length namespace");
        assertTrue(xml.contains("xmlns:beans=\"http://www.springframework.org/schema/beans\""),
            "Should have beans namespace");
    }

    // AC3: <fix-length-inbound-converter> element with id="resp_converter" and codeGen="true"
    @Test
    void testFixLengthInboundConverterElement() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("<fix-length-inbound-converter"), "Should have inbound converter element");
        assertTrue(xml.contains("id=\"resp_converter\""), "Should have correct id");
        assertTrue(xml.contains("codeGen=\"true\""), "Should have codeGen attribute");
    }

    // AC4: <message> element forType attribute format {groupId}.{artifactId}.{ClassName}Response
    @Test
    void testMessageElementForTypeFormat() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId("CreateApplication");
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("<message"), "Should have message element");
        assertTrue(xml.contains("forType=\"com.rtm.test.CreateApplicationResponse\""),
            "Should have correct forType format with Response suffix");
    }

    // AC5: Field order matches input JSON Tree (Response part)
    @Test
    void testFieldOrderPreserved() {
        MessageModel model = createModelWithOrderedFields();
        generator.setModel(model);

        String xml = generator.generateInbound();

        int firstFieldPos = xml.indexOf("name=\"returnCode\"");
        int secondFieldPos = xml.indexOf("name=\"returnMessage\"");
        int thirdFieldPos = xml.indexOf("name=\"responseData\"");

        assertTrue(firstFieldPos > 0, "First field should exist");
        assertTrue(secondFieldPos > 0, "Second field should exist");
        assertTrue(thirdFieldPos > 0, "Third field should exist");
        assertTrue(firstFieldPos < secondFieldPos, "First field should come before second");
        assertTrue(secondFieldPos < thirdFieldPos, "Second field should come before third");
    }

    // AC6: Transitory groupId field correctly generated (no name, transitory="true")
    @Test
    void testTransitoryGroupIdField() {
        MessageModel model = createModelWithTransitoryGroupId();
        generator.setModel(model);

        String xml = generator.generateInbound();

        // Transitory groupId field should NOT have name attribute
        assertTrue(xml.contains("transitory=\"true\""), "Should have transitory attribute");
        assertTrue(xml.contains("defaultValue=\"RESPGRP\""), "Should have groupId as defaultValue");
        assertTrue(xml.contains("<field type=\"DataField\""), "Should have DataField type");
    }

    // AC7: Transitory occurrenceCount field correctly generated (counterFieldConverter)
    @Test
    void testTransitoryOccurrenceCountField() {
        MessageModel model = createModelWithTransitoryOccurrenceCount();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("transitory=\"true\""), "Should have transitory attribute");
        assertTrue(xml.contains("counterFieldConverter"), "Should use counterFieldConverter");
        assertTrue(xml.contains("pad=\"0\""), "Should have pad=0 for right alignment");
        assertTrue(xml.contains("alignRight=\"true\""), "Should have alignRight=true");
    }

    // AC8: CompositeField correctly generates nested structure
    @Test
    void testCompositeFieldNested() {
        MessageModel model = createModelWithNestedObject();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("type=\"CompositeField\""), "Should have CompositeField type");
        assertTrue(xml.contains("name=\"responseData\""), "Should have nested object name");
        assertTrue(xml.contains("forType=\"com.rtm.test.ResponseData\""),
            "Should have nested object forType");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"applicationId\""), "Should have nested child field");
    }

    // AC9: RepeatingField correctly generates array structure with fixedCount
    @Test
    void testRepeatingFieldArray() {
        MessageModel model = createModelWithArrayField();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.contains("type=\"RepeatingField\""), "Should have RepeatingField type");
        assertTrue(xml.contains("name=\"errorDetails\""), "Should have array field name");
        assertTrue(xml.contains("fixedCount=\"10\""), "Should have fixedCount attribute");
        // Verify nested child field exists
        assertTrue(xml.contains("name=\"errorCode\""), "Should have array child field");
    }

    // AC10: File output to {output-root}/xml/inbound-converter.xml
    @Test
    void testFileOutputLocation() throws IOException {
        MessageModel model = createValidModel();
        generator.setModel(model);

        generator.generateInbound();
        generator.writeOutput();

        Path expectedPath = tempDir.resolve("xml").resolve("inbound-converter.xml");
        assertTrue(Files.exists(expectedPath), "Output file should exist at expected location");
    }

    // AC11: UTF-8 encoding
    @Test
    void testUtf8Encoding() throws IOException {
        MessageModel model = createValidModel();
        generator.setModel(model);

        generator.generateInbound();
        generator.writeOutput();

        Path outputPath = tempDir.resolve("xml").resolve("inbound-converter.xml");
        String content = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("encoding=\"UTF-8\""), "Should specify UTF-8 encoding");
    }

    // AC12: Correct XML declaration <?xml version="1.0" encoding="UTF-8"?>
    @Test
    void testXmlDeclaration() {
        MessageModel model = createValidModel();
        generator.setModel(model);

        String xml = generator.generateInbound();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have correct XML declaration");
    }

    // AC13: Response empty generates empty XML file (only root elements, no exception)
    @Test
    void testEmptyResponseGeneratesEmptyXml() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);
        model.setResponse(new FieldGroup()); // Empty response

        generator.setModel(model);

        // Should NOT throw exception (unlike OutboundXmlGenerator)
        String xml = assertDoesNotThrow(() -> generator.generateInbound(),
            "Empty response should not throw exception");

        // Should have minimal XML structure
        assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
            "Should have XML declaration");
        assertTrue(xml.contains("<beans:beans"), "Should have beans:beans root");
        assertTrue(xml.contains("<fix-length-inbound-converter"), "Should have converter element");
        assertTrue(xml.contains("id=\"resp_converter\""), "Should have resp_converter id");
        // Should NOT have <message> element
        assertFalse(xml.contains("<message"), "Should not have message element for empty response");
    }

    @Test
    void testNullResponseGeneratesEmptyXml() {
        MessageModel model = new MessageModel();
        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOp");
        model.setMetadata(metadata);
        model.setResponse(null); // Null response

        generator.setModel(model);

        String xml = assertDoesNotThrow(() -> generator.generateInbound(),
            "Null response should not throw exception");

        assertTrue(xml.contains("<fix-length-inbound-converter"), "Should have converter element");
        assertFalse(xml.contains("<message"), "Should not have message element for null response");
    }

    // AC14: Missing operationId with non-empty Response throws ParseException
    @Test
    void testMissingOperationIdThrowsException() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId(null);

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateInbound());

        assertTrue(ex.getMessage().contains("Operation ID is required"),
            "Should indicate missing operationId");
    }

    @Test
    void testMissingOperationIdBlankThrowsException() {
        MessageModel model = createValidModel();
        model.getMetadata().setOperationId("   ");

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateInbound());

        assertTrue(ex.getMessage().contains("Operation ID is required"),
            "Should indicate missing operationId");
    }

    // AC15: XmlGenerator interface correctly implemented
    @Test
    void testImplementsXmlGeneratorInterface() {
        assertTrue(generator instanceof XmlGenerator, "Should implement XmlGenerator interface");
    }

    @Test
    void testGetType() {
        assertEquals("xml-inbound", generator.getType());
    }

    @Test
    void testGenerateOutboundThrowsUnsupportedOperation() {
        assertThrows(UnsupportedOperationException.class, () -> generator.generateOutbound());
    }

    @Test
    void testGenerateReturnsArtifactMap() {
        MessageModel model = createValidModel();

        Map<String, String> artifacts = generator.generate(model, tempDir);

        assertEquals(1, artifacts.size(), "Should return one artifact");
        assertTrue(artifacts.containsKey("xml/inbound-converter.xml"),
            "Should have correct artifact key");
        assertNotNull(artifacts.get("xml/inbound-converter.xml"),
            "Should have content");
    }

    @Test
    void testNullModelThrowsException() {
        generator.setModel(null);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateInbound());

        assertTrue(ex.getMessage().contains("MessageModel is null"),
            "Should indicate null model");
    }

    @Test
    void testNullMetadataWithNonEmptyResponseThrowsException() {
        MessageModel model = createValidModel();
        model.setMetadata(null);

        generator.setModel(model);

        GenerationException ex = assertThrows(GenerationException.class,
            () -> generator.generateInbound());

        assertTrue(ex.getMessage().contains("Metadata is required"),
            "Should indicate missing metadata");
    }

    @Test
    void testGetOutputPath() {
        Path expectedPath = tempDir.resolve("xml").resolve("inbound-converter.xml");
        assertEquals(expectedPath.toString(), generator.getOutputPath().toString());
    }

    @Test
    void testGetGeneratedContentBeforeGeneration() {
        assertNull(generator.getGeneratedContent());
    }

    @Test
    void testGetGeneratedContentAfterGeneration() {
        MessageModel model = createValidModel();
        generator.setModel(model);
        generator.generateInbound();

        assertNotNull(generator.getGeneratedContent());
        assertTrue(generator.getGeneratedContent().contains("<?xml"));
    }

    @Test
    void testWriteOutputBeforeGenerationThrowsException() {
        assertThrows(GenerationException.class, () -> generator.writeOutput());
    }

    @Test
    void testHasResponseDataTrue() {
        MessageModel model = createValidModel();
        assertTrue(generator.hasResponseData(model), "Should return true for model with response data");
    }

    @Test
    void testHasResponseDataFalseEmpty() {
        MessageModel model = new MessageModel();
        model.setResponse(new FieldGroup());
        assertFalse(generator.hasResponseData(model), "Should return false for empty response");
    }

    @Test
    void testHasResponseDataFalseNull() {
        MessageModel model = new MessageModel();
        model.setResponse(null);
        assertFalse(generator.hasResponseData(model), "Should return false for null response");
    }

    @Test
    void testDataFieldWithStringType() {
        MessageModel model = createValidModel();
        FieldNode stringField = FieldNode.builder()
            .camelCaseName("returnMessage")
            .dataType("String")
            .length(100)
            .build();
        model.getResponse().getFields().clear();
        model.getResponse().addField(stringField);

        generator.setModel(model);
        String xml = generator.generateInbound();

        assertTrue(xml.contains("name=\"returnMessage\""), "Should have field name");
        assertTrue(xml.contains("length=\"100\""), "Should have length");
        assertTrue(xml.contains("nullPad=\" \""), "String should have space nullPad");
        assertTrue(xml.contains("stringFieldConverter"), "Should use string converter");
    }

    @Test
    void testDataFieldWithNumericType() {
        MessageModel model = createValidModel();
        FieldNode numericField = FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("Number")
            .length(4)
            .build();
        model.getResponse().getFields().clear();
        model.getResponse().addField(numericField);

        generator.setModel(model);
        String xml = generator.generateInbound();

        assertTrue(xml.contains("name=\"returnCode\""), "Should have field name");
        assertTrue(xml.contains("length=\"4\""), "Should have length");
        assertTrue(xml.contains("pad=\"0\""), "Numeric should have 0 pad");
        assertTrue(xml.contains("alignRight=\"true\""), "Numeric should be right aligned");
    }

    // Helper methods to create test models

    private MessageModel createValidModel() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();
        FieldNode field = FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("String")
            .length(4)
            .build();
        response.addField(field);
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithOrderedFields() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("TestOperation");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();
        response.addField(FieldNode.builder().camelCaseName("returnCode").dataType("String").length(4).build());
        response.addField(FieldNode.builder().camelCaseName("returnMessage").dataType("String").length(100).build());
        response.addField(FieldNode.builder().camelCaseName("responseData").dataType("String").length(50).build());
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithTransitoryGroupId() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();
        // Transitory groupId field - no name, has groupId
        FieldNode transitoryField = FieldNode.builder()
            .isTransitory(true)
            .groupId("RESPGRP")
            .length(10)
            .build();
        response.addField(transitoryField);

        // Regular field
        FieldNode regularField = FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("String")
            .length(4)
            .build();
        response.addField(regularField);
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithTransitoryOccurrenceCount() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();
        // Transitory occurrenceCount field
        FieldNode transitoryField = FieldNode.builder()
            .isTransitory(true)
            .occurrenceCount("0..9")
            .length(4)
            .build();
        response.addField(transitoryField);

        // Regular field
        FieldNode regularField = FieldNode.builder()
            .camelCaseName("returnCode")
            .dataType("String")
            .length(4)
            .build();
        response.addField(regularField);
        model.setResponse(response);

        return model;
    }

    private MessageModel createModelWithNestedObject() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();

        // Nested object field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("applicationId")
            .dataType("String")
            .length(20)
            .build());

        FieldNode nestedObject = FieldNode.builder()
            .isObject(true)
            .camelCaseName("responseData")
            .className("ResponseData")
            .children(childFields)
            .build();
        response.addField(nestedObject);

        model.setResponse(response);
        return model;
    }

    private MessageModel createModelWithArrayField() {
        MessageModel model = new MessageModel();

        Metadata metadata = new Metadata();
        metadata.setOperationId("CreateApplication");
        model.setMetadata(metadata);

        FieldGroup response = new FieldGroup();

        // Array field
        List<FieldNode> childFields = new ArrayList<>();
        childFields.add(FieldNode.builder()
            .camelCaseName("errorCode")
            .dataType("String")
            .length(10)
            .build());

        FieldNode arrayField = FieldNode.builder()
            .isArray(true)
            .camelCaseName("errorDetails")
            .className("ErrorDetail")
            .occurrenceCount("0..10")
            .children(childFields)
            .build();
        response.addField(arrayField);

        model.setResponse(response);
        return model;
    }
}
